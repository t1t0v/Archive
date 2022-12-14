package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IExplosion;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;
import naturalism.addon.netherbane.utils.PlayerUtil;
import naturalism.addon.netherbane.utils.Timer;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public class BedPlacer extends Module {
    public BedPlacer(){
        super(NetherBane.COMBATPLUS, "bed-placer", "");
    }

    public enum TargetMode{
        Nearest,
        MinHealth,
        Openest
    }

    public enum SortMode{
        CloseToTarget,
        CloseToPlayer,
        MinSelfHealth
    }

    public enum MineMode {
        Vanilla,
        Packet
    }

    public enum RenderMode{
        SingleBox,
        DoubleBox,
        Lines,
        BedForm
    }

    public enum BoxMode{
        Head,
        Feet
    }

    private final SettingGroup sgDebug = settings.createGroup("Debug");
    private final Setting<Boolean> debugChat = sgDebug.add(new BoolSetting.Builder().name("debug-chat").description("Automatically rotates you towards the city block.").defaultValue(false).build());

    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final Setting<Double> enemyRange = sgTarget.add(new DoubleSetting.Builder().name("enemy-range").description("The radius in which players get targeted.").defaultValue(10).min(0).sliderMax(15).build());
    private final Setting<TargetMode> targetMode = sgTarget.add(new EnumSetting.Builder<TargetMode>().name("target-mode").description("Which way to swap.").defaultValue(TargetMode.Nearest).build());
    private final Setting<Boolean> ignoreBedrockBurrow = sgTarget.add(new BoolSetting.Builder().name("ignore-bedrock-burrow").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> stopPlaceBurrowed = sgTarget.add(new BoolSetting.Builder().name("stop-place-burrowed").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> stopPlaceSelfHole = sgTarget.add(new BoolSetting.Builder().name("stop-place-self-hole").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgFindBedPos = settings.createGroup("Find Pos");
    private final Setting<SortMode> sortMode = sgFindBedPos.add(new EnumSetting.Builder<SortMode>().name("sort-mode").description("Which way to swap.").defaultValue(SortMode.MinSelfHealth).build());

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final Setting<Boolean> prediction = sgPlace.add(new BoolSetting.Builder().name("predict").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> ignoreTerrain = sgPlace.add(new BoolSetting.Builder().name("ignore-terrain").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> ignoreBed = sgPlace.add(new BoolSetting.Builder().name("ignore-bed").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> oneDotTwelve = sgPlace.add(new BoolSetting.Builder().name("1.12-place").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> meteorPlace = sgPlace.add(new BoolSetting.Builder().name("meteor-place").description("Will not place and break beds if they will kill you.").defaultValue(false).build());
    private final Setting<Boolean> testPlace = sgPlace.add(new BoolSetting.Builder().name("test-place").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> xPlaceRange = sgPlace.add(new DoubleSetting.Builder().name("x-range").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> yPlaceRange = sgPlace.add(new DoubleSetting.Builder().name("y-range").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(10).build());

    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final Setting<Boolean> rotateBreak = sgBreak.add(new BoolSetting.Builder().name("rotate-break").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> testBreak = sgBreak.add(new BoolSetting.Builder().name("test-break").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> packetBreak = sgBreak.add(new BoolSetting.Builder().name("packet-break").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> strictDirection = sgBreak.add(new BoolSetting.Builder().name("strict-direction").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> antiMine = sgBreak.add(new BoolSetting.Builder().name("anti-mine").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> useTimer = sgBreak.add(new BoolSetting.Builder().name("use-timer").description("Automatically rotates you towards the city block.").defaultValue(true).visible(antiMine::get).build());
    private final Setting<Integer> timerDelay = sgBreak.add(new IntSetting.Builder().name("timer-delay").description("The delay between placing beds in ticks.").defaultValue(1).min(0).sliderMax(20).visible(() -> antiMine.get() && useTimer.get()).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("break-pange").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> xBreakRange = sgBreak.add(new DoubleSetting.Builder().name("x-range").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> yBreakRange = sgBreak.add(new DoubleSetting.Builder().name("y-range").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(10).build());
    private final Setting<Hand> handMode = sgBreak.add(new EnumSetting.Builder<Hand>().name("hand-mode").description("Which way to swap.").defaultValue(Hand.OFF_HAND).build());

    private final SettingGroup sgFind = settings.createGroup("Find");
    private final Setting<Integer> findPlaceDelay = sgFind.add(new IntSetting.Builder().name("find-place-delay").description("The delay between placing beds in ticks.").defaultValue(7).min(0).sliderMax(20).build());
    private final Setting<Integer> findBreakDelay = sgFind.add(new IntSetting.Builder().name("find-break-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());

    private final SettingGroup sgMove = settings.createGroup("Move");
    public final Setting<Double> minSpeed = sgMove.add(new DoubleSetting.Builder().name("min-speed").description("The range at which players can be targeted.").defaultValue(3).min(0.0).sliderMax(36).build());
    private final Setting<Integer> movePlaceDelay = sgMove.add(new IntSetting.Builder().name("move-place-delay").description("The delay between placing beds in ticks.").defaultValue(7).min(0).sliderMax(20).build());
    private final Setting<Integer> moveBreakDelay = sgMove.add(new IntSetting.Builder().name("move-break-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());

    private final SettingGroup sgHole = settings.createGroup("Hole");
    private final Setting<Integer> holePlaceDelay = sgHole.add(new IntSetting.Builder().name("hole-place-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Integer> holeBreakDelay = sgHole.add(new IntSetting.Builder().name("hole-break-delay").description("The delay between placing beds in ticks.").defaultValue(8).min(0).sliderMax(20).build());

    private final SettingGroup sgTop = settings.createGroup("Top");
    private final Setting<Integer> topPlaceDelay = sgTop.add(new IntSetting.Builder().name("top-place-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Integer> topBreakDelay = sgTop.add(new IntSetting.Builder().name("top-break-delay").description("The delay between placing beds in ticks.").defaultValue(8).min(0).sliderMax(20).build());

    private final SettingGroup sgSurround = settings.createGroup("Surround");
    private final Setting<Integer> surroundPlaceDelay = sgSurround.add(new IntSetting.Builder().name("surround-place-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Integer> surroundBreakDelay = sgSurround.add(new IntSetting.Builder().name("surround-break-delay").description("The delay between placing beds in ticks.").defaultValue(7).min(0).sliderMax(20).build());

    private final SettingGroup sgPressing = settings.createGroup("Pressing");
    private final Setting<Boolean> pressing = sgPressing.add(new BoolSetting.Builder().name("pressing").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> onlyOnHole = sgPressing.add(new BoolSetting.Builder().name("only-on-hole").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Integer> pressBreakDelay = sgPressing.add(new IntSetting.Builder().name("press-break-delay").description("The delay between placing beds in ticks.").defaultValue(9).min(0).sliderMax(20).visible(pressing::get).build());

    private final SettingGroup sgDamages = settings.createGroup("Damages");
    public final Setting<Double> minDamage = sgDamages.add(new DoubleSetting.Builder().name("min-damage").description("The range at which players can be targeted.").defaultValue(6).min(0.0).sliderMax(36).build());
    private final Setting<Double> maxSelfDamage = sgDamages.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum damage to inflict on yourself.").defaultValue(4.5).range(0, 36).sliderMax(36).build());
    private final Setting<Boolean> lethalDamage = sgDamages.add(new BoolSetting.Builder().name("lethal-damage").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    public final Setting<Double> minHealth = sgDamages.add(new DoubleSetting.Builder().name("min-health").description("The range at which players can be targeted.").defaultValue(3).min(0.0).sliderMax(36).visible(lethalDamage::get).build());
    private final Setting<Boolean> antiSuicide = sgDamages.add(new BoolSetting.Builder().name("anti-suicide").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgTrapBreak = settings.createGroup("Trap Break");
    private final Setting<MineMode> trapMineMode = sgTrapBreak.add(new EnumSetting.Builder<MineMode>().name("mining").description("Block breaking method").defaultValue(MineMode.Packet).build());
    private final Setting<Boolean> onlyInHoleTrap = sgTrapBreak.add(new BoolSetting.Builder().name("only-in-hole").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> onlyOnGroundTrap = sgTrapBreak.add(new BoolSetting.Builder().name("only-on-ground").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Keybind> breakTrap = sgTrapBreak.add(new KeybindSetting.Builder().name("break-trap").description("Change the pickaxe slot to an iron one when pressing the button.").defaultValue(Keybind.fromKey(8)).build());
    private final Setting<Boolean> autoMineTrap = sgTrapBreak.add(new BoolSetting.Builder().name("auto-mine-trap").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgTopBreak = settings.createGroup("Top Break");
    private final Setting<MineMode> topMineMode = sgTopBreak.add(new EnumSetting.Builder<MineMode>().name("mining").description("Block breaking method").defaultValue(MineMode.Packet).build());
    private final Setting<Boolean> onlyInHoleTop = sgTopBreak.add(new BoolSetting.Builder().name("only-in-hole").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> onlyOnGroundTop = sgTopBreak.add(new BoolSetting.Builder().name("only-on-ground").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Keybind> breakTop = sgTopBreak.add(new KeybindSetting.Builder().name("break-top").description("Change the pickaxe slot to an iron one when pressing the button.").defaultValue(Keybind.fromKey(8)).build());
    private final Setting<Boolean> autoMineTop = sgTopBreak.add(new BoolSetting.Builder().name("auto-mine-top").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgBurrowBreak = settings.createGroup("Burrow Break");
    private final Setting<MineMode> burrowMineMode = sgBurrowBreak.add(new EnumSetting.Builder<MineMode>().name("mining").description("Block breaking method").defaultValue(MineMode.Packet).build());
    private final Setting<Boolean> onlyInHoleBurrow = sgBurrowBreak.add(new BoolSetting.Builder().name("only-in-hole").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> onlyOnGroundBurrow = sgBurrowBreak.add(new BoolSetting.Builder().name("only-on-ground").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Keybind> breakBurrow = sgBurrowBreak.add(new KeybindSetting.Builder().name("break-burrow").description("Change the pickaxe slot to an iron one when pressing the button.").defaultValue(Keybind.fromKey(8)).build());
    private final Setting<Boolean> autoMineBurrow = sgBurrowBreak.add(new BoolSetting.Builder().name("auto-mine-burrow").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgStringWeb = settings.createGroup("String/Web Break");
    private final Setting<MineMode> stringwebMineMode = sgStringWeb.add(new EnumSetting.Builder<MineMode>().name("mining").description("Block breaking method").defaultValue(MineMode.Packet).build());
    private final Setting<Boolean> onlyInHoleString = sgStringWeb.add(new BoolSetting.Builder().name("only-in-hole").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> onlyOnGroundString = sgStringWeb.add(new BoolSetting.Builder().name("only-on-ground").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> webBreaker = sgStringWeb.add(new BoolSetting.Builder().name("web-breaker").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> stringBreaker = sgStringWeb.add(new BoolSetting.Builder().name("string-breaker").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgAutoRefill = settings.createGroup("Auto Refill");
    private final Setting<Boolean> autoMove = sgAutoRefill.add(new BoolSetting.Builder().name("auto-move").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());
    private final Setting<Integer> autoMoveSlot = sgAutoRefill.add(new IntSetting.Builder().name("auto-move-slot").description("The slot auto move moves beds to.").defaultValue(9).range(1, 9).sliderRange(1, 9).visible(autoMove::get).build());
    private final Setting<Boolean> autoSwitch = sgAutoRefill.add(new BoolSetting.Builder().name("auto-switch").description("Switches to and from beds automatically.").defaultValue(true).build());
    private final Setting<Boolean> silentSwitch = sgAutoRefill.add(new BoolSetting.Builder().name("silent-switch").description("Switches to and from beds automatically.").defaultValue(true).build());

    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final Setting<Boolean> checkDimension = sgMisc.add(new BoolSetting.Builder().name("check-dimension").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> checkBed = sgMisc.add(new BoolSetting.Builder().name("check-bed").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> trap = sgMisc.add(new BoolSetting.Builder().name("trap").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgMisc.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(false).build());
    private final Setting<Boolean> pauseOnDrink = sgMisc.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(false).build());
    private final Setting<Boolean> pauseOnMine = sgMisc.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while drinking.").defaultValue(false).build());
    private final Setting<Boolean> pauseOnCA = sgMisc.add(new BoolSetting.Builder().name("pause-on-CA").description("Pause while Crystal Aura is active.").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Whether to swing hand clientside clientside.").defaultValue(true).build());
    private final Setting<Boolean> renderMine = sgRender.add(new BoolSetting.Builder().name("render-mine").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<Boolean> mineFade = sgRender.add(new BoolSetting.Builder().name("fade").description("Renders the block where it is placing a bed.").defaultValue(true).visible(renderMine::get).build());
    private final Setting<Integer> mineFadeTick = sgRender.add(new IntSetting.Builder().name("fade-tick").description("The slot auto move moves beds to.").defaultValue(8).visible(() -> renderMine.get() && mineFade.get()).build());
    private final Setting<ShapeMode> mineShapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(renderMine::get).build());
    private final Setting<SettingColor> mineLineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,366)).visible(renderMine::get).build());
    private final Setting<SettingColor> mineSideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211, 75)).visible(renderMine::get).build());
    private final Setting<SettingColor> mineLineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,366)).visible(renderMine::get).build());
    private final Setting<SettingColor> mineSideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211, 75)).visible(renderMine::get).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());

    private final Setting<Boolean> renderArray = sgRender.add(new BoolSetting.Builder().name("render-array").description("Whether to swing hand clientside clientside.").defaultValue(true).build());
    private final Setting<ShapeMode> arrayshapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("array-shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> arraylineColor = sgRender.add(new ColorSetting.Builder().name("array-line-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> arraysideColor = sgRender.add(new ColorSetting.Builder().name("array-side-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).build());


    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<Integer> renderTick = sgRender.add(new IntSetting.Builder().name("render-tick").description("The slot auto move moves beds to.").defaultValue(8).visible(render::get).build());

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Which way to render surround.").defaultValue(RenderMode.SingleBox).build());
    private final Setting<BoxMode> boxMode = sgRender.add(new EnumSetting.Builder<BoxMode>().name("box-mode").description("Which way to render surround.").defaultValue(BoxMode.Head).build());
    private final Setting<Boolean> fade = sgRender.add(new BoolSetting.Builder().name("fade").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<Integer> fadeTick = sgRender.add(new IntSetting.Builder().name("fade-tick").description("The slot auto move moves beds to.").defaultValue(8).visible(fade::get).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(() -> render.get() && renderMode.get() == RenderMode.SingleBox).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(() -> render.get() && renderMode.get() == RenderMode.SingleBox).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(() -> render.get() && renderMode.get() == RenderMode.SingleBox).build());

    private final Setting<SettingColor> pillowSideColor = sgRender.add(new ColorSetting.Builder().name("pillow-side-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(() -> render.get() && renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> pillowLineColor = sgRender.add(new ColorSetting.Builder().name("pillow-line-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(() -> render.get() && renderMode.get() == RenderMode.Lines).build());

    private final Setting<SettingColor> bedSpreadSideColor = sgRender.add(new ColorSetting.Builder().name("bedSpread-side-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(() -> render.get() && renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> bedSpreadLineColor = sgRender.add(new ColorSetting.Builder().name("bedSpread-line-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(() -> render.get() && renderMode.get() == RenderMode.Lines).build());

    private final Setting<SettingColor> woodSideColor = sgRender.add(new ColorSetting.Builder().name("wood-side-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(() -> render.get() && renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> woodLineColor = sgRender.add(new ColorSetting.Builder().name("wood-line-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(() -> render.get() && renderMode.get() == RenderMode.Lines).build());

    private final Setting<Boolean> renderExtra = sgRender.add(new BoolSetting.Builder().name("render-extra").description("Renders a extra element in the middle of the bed.").defaultValue(false).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<Boolean> shrink = sgRender.add(new BoolSetting.Builder().name("shrink").description("Shrinks the block overlay after a while.").defaultValue(false).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<Integer> shrinkTicks = sgRender.add(new IntSetting.Builder().name("shrink-ticks").description("How many ticks to wait before shrinking the block.").defaultValue(5).min(0).max(50).sliderMax(25).visible(() -> render.get() && shrink.get() && renderMode.get() == RenderMode.BedForm).noSlider().build());
    private final Setting<Double> shrinkSpeed = sgRender.add(new DoubleSetting.Builder().name("shrink-speed").description("How fast to shrink the overlay.").defaultValue(2.5).min(1).max(50).sliderMin(1).sliderMax(25).visible(() -> render.get() && shrink.get() && renderMode.get() == RenderMode.BedForm).noSlider().build());
    private final Setting<Boolean> renderInnerLines = sgRender.add(new BoolSetting.Builder().name("render-inner-lines").description("Renders the inner lines of the bed.").defaultValue(true).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<Boolean> renderInnerSides = sgRender.add(new BoolSetting.Builder().name("render-inner-sides").description("Renders the inner sides of the bed.").defaultValue(true).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<Double> feetLength = sgRender.add(new DoubleSetting.Builder().name("feet-lenght").description("How long the feet of the bed are.").defaultValue(1.875).min(1.25).max(3).sliderMin(1.25).sliderMax(3).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<Double> bedHeight = sgRender.add(new DoubleSetting.Builder().name("bed-height").description("How high the bed is.").defaultValue(5.62).min(4.5).max(6.5).sliderMin(4.5).sliderMax(6.5).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<SettingColor> sideColorTop = sgRender.add(new ColorSetting.Builder().name("side-color-top").description("The top side color of the bed.").defaultValue(new SettingColor(205, 0, 255, 5, false)).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<SettingColor> sideColorBottom = sgRender.add(new ColorSetting.Builder().name("side-color-bottom").description("The bottom side color of the bed.").defaultValue(new SettingColor(205, 0, 255, 25, false)).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<SettingColor> lineColorTop = sgRender.add(new ColorSetting.Builder().name("line-color-top").description("The top line color of the bed.").defaultValue(new SettingColor(205, 0, 255, 50, false)).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());
    private final Setting<SettingColor> lineColorBottom = sgRender.add(new ColorSetting.Builder().name("line-color-bottom").description("The bottom line color of the bed.").defaultValue(new SettingColor(205, 0, 255, 255, false)).visible(() -> render.get() && renderMode.get() == RenderMode.BedForm).build());

    private final Setting<Boolean> rainbowColor = sgRender.add(new BoolSetting.Builder().name("rainbow-color").description("Renders crystal damage text in the block overlay.").defaultValue(false).build());
    private final Setting<Integer> sideAlpha = sgRender.add(new IntSetting.Builder().name("side-alpha").description("The slot auto move moves beds to.").defaultValue(80).visible(rainbowColor::get).build());
    private final Setting<Integer> lineAlpha = sgRender.add(new IntSetting.Builder().name("line-alpha").description("The slot auto move moves beds to.").defaultValue(255).visible(rainbowColor::get).build());
    private final Setting<Boolean> renderDamageColor = sgRender.add(new BoolSetting.Builder().name("damage-color").description("Renders crystal damage text in the block overlay.").defaultValue(false).build());
    private final Setting<SettingColor> maxDamColor = sgRender.add(new ColorSetting.Builder().name("maxDamColor").description("The side color.").defaultValue(new SettingColor(255, 15, 15, 75)).visible(renderDamageColor::get).build());
    private final Setting<SettingColor> highDamColor = sgRender.add(new ColorSetting.Builder().name("highDamColor").description("The side color.").defaultValue(new SettingColor(255, 59, 174, 75)).visible(renderDamageColor::get).build());
    private final Setting<SettingColor> mediumDamColor = sgRender.add(new ColorSetting.Builder().name("mediumDamColor").description("The side color.").defaultValue(new SettingColor(210, 87, 255, 75)).visible(renderDamageColor::get).build());
    private final Setting<SettingColor> belowMediumDamColor = sgRender.add(new ColorSetting.Builder().name("belowMediumDamColor").description("The side color.").defaultValue(new SettingColor(34, 167, 255, 75)).visible(renderDamageColor::get).build());
    private final Setting<SettingColor> littleDamColor = sgRender.add(new ColorSetting.Builder().name("littleDamColor").description("The side color.").defaultValue(new SettingColor(255, 228, 15, 75)).visible(renderDamageColor::get).build());
    private final Setting<SettingColor> lowDamColor = sgRender.add(new ColorSetting.Builder().name("lowDamColor").description("The side color.").defaultValue(new SettingColor(15, 255, 86, 75)).visible(renderDamageColor::get).build());


    private final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder().name("render-damage").description("Renders the current block being mined.").defaultValue(true).build());
    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder().name("text-scale").description("How big the damage text should be.").defaultValue(1.25).min(1).sliderMax(4).visible(renderDamage::get).build());
    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder().name("text-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).visible(renderDamage::get).build());

    private List<BlockPos> array = new ArrayList<>();
    private double placeMs;
    private double breakMs;

    private double time;
    private double currentDamage;
    private Direction direction;

    private BlockPos breakPos;
    private BlockPos headPos;
    private BlockPos feetPos;
    private static BlockPos head;
    private final Vec3 vec3 = new Vec3();

    private boolean isStartBreakingBed;

    private PlayerEntity target;

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer mineTimer = new Timer();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private BlockPos minePos;

    @Override
    public void onActivate() {
        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
        explosion = new Explosion(mc.world, null, 0, 0, 0, 5.0F, true, Explosion.DestructionType.DESTROY);
        if (mc.player != null) raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
    }

    @Override
    public void onDeactivate() {
        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
    }

    @EventHandler
    private void onMs(Render3DEvent event){
        if (target == null) return;
        PlayerEntity finalTarget2 = target;

        if (!EntityUtil.isInHole(true, finalTarget2) || EntityUtil.getPlayerSpeed(finalTarget2) > 1 || BlockUtil.distance(new BlockPos(mc.player.getEyePos()), finalTarget2.getBlockPos()) >= placeRange.get()){
            AtomicReference<List<Pair<BlockPos, Double>>> pairs = new AtomicReference<>();
            Runnable task = () -> {
                time = System.currentTimeMillis();
                pairs.set(giveDamage(getPosAround(), target));
                array = getSortPos(pairs.get());
            };
            Thread thread = new Thread(task);

            if (thread.getThreadGroup().activeCount() > 1 || thread.getState() == Thread.State.TERMINATED){
                thread.start();
                if (debugChat.get()) info("Time: " + (System.currentTimeMillis() - time) + "; BestDamage: " + currentDamage + "; Direction: " + (direction == null ? "None" : direction.toString()) + "; Size: " + array.size());
            }else {
                thread.interrupt();
            }

        }
        else if (EntityUtil.isInHole(true, finalTarget2) && !isSafeFromFacePlace(finalTarget2) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), finalTarget2.getBlockPos().up()) <= placeRange.get()){
            array = new ArrayList<>(){{
                add(finalTarget2.getBlockPos().up());
            }};
        }
        else if (!EntityUtil.isInHole(true, finalTarget2) && isSafeFromFacePlace(finalTarget2) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), finalTarget2.getBlockPos().up()) <= placeRange.get()){
            array = new ArrayList<>(){{
                add(finalTarget2.getBlockPos());
            }};
        }
        else if (EntityUtil.isInHole(true, finalTarget2) && isSafeFromFacePlace(finalTarget2) && !isSelfTraped(finalTarget2) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), finalTarget2.getBlockPos().up()) <= placeRange.get()){
            array = new ArrayList<>(){{
                add(finalTarget2.getBlockPos().up(2));
            }};
        }
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event){
        assert mc.world != null;
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        if (mc.world.getDimension().isBedWorking() && checkDimension.get()){
            warning("It is overworld...toggle");
            toggle();
            return;
        }

        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (checkBed.get() && !bed.found()) return;
        if (autoMove.get()) {
            if (bed.found() && bed.slot() != autoMoveSlot.get() - 1) {
                InvUtils.move().from(bed.slot()).toHotbar(autoMoveSlot.get() - 1);
            }
        }

        //Find Target
        List<PlayerEntity> playerArray = EntityUtil.getTargetsInRange(enemyRange.get());
        if (ignoreBedrockBurrow.get()) playerArray = playerArray.stream().filter(player -> BlockUtil.getBlock(player.getBlockPos()) != Blocks.BEDROCK).collect(Collectors.toList());

        switch (targetMode.get()){
            case Nearest -> playerArray.sort(Comparator.comparing(player -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), player.getBlockPos())));
            case MinHealth -> playerArray.sort(Comparator.comparing(LivingEntity::getHealth));
            case Openest -> playerArray.sort(Comparator.comparing(this::checkOpenest));
        }

        if (playerArray.isEmpty()) return;
        //Automation
        if ((breakBurrow.get().isPressed() || autoMineBurrow.get()) && !(BlockUtil.getBlock(playerArray.get(0).getBlockPos()) instanceof BedBlock) && EntityUtil.isBurrowed(playerArray.get(0)) && BlockUtil.getState(playerArray.get(0).getBlockPos()).getHardness(mc.world, playerArray.get(0).getBlockPos()) != -1 && BlockUtil.distance(new BlockPos(mc.player.getEyePos()),  playerArray.get(0).getBlockPos()) <= placeRange.get()){
            if (onlyInHoleBurrow.get() && !EntityUtil.isInHole(true, playerArray.get(0))) return;
            if (onlyOnGroundBurrow.get() && !mc.player.isOnGround()) return;
            if (BlockUtil.getState(playerArray.get(0).getBlockPos()).getHardness(mc.world, playerArray.get(0).getBlockPos()) == 0){
                minePos = playerArray.get(0).getBlockPos();

                mine(playerArray.get(0).getBlockPos(), mc.player.getInventory().selectedSlot, burrowMineMode.get());
                return;
            } else {
                minePos = playerArray.get(0).getBlockPos();
                mine(playerArray.get(0).getBlockPos(), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem).slot(), burrowMineMode.get());
                return;
            }
        }
        try {
            if ((breakTrap.get().isPressed() || autoMineTrap.get()) && isSelfTraped(playerArray.get(0)) && !(BlockUtil.getBlock(getBestTrapPos(playerArray.get(0))) instanceof BedBlock) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), getBestTrapPos(playerArray.get(0))) <= placeRange.get()){
                if (onlyInHoleTrap.get() && !EntityUtil.isInHole(true, playerArray.get(0))) return;
                if (onlyOnGroundTrap.get() && !mc.player.isOnGround()) return;
                if (getBestTrapPos(playerArray.get(0)) != null){
                    minePos = getBestTrapPos(playerArray.get(0));
                    mine(getBestTrapPos(playerArray.get(0)), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem).slot(), trapMineMode.get());
                    return;

                }
            }
        }catch (NullPointerException ignored){}
        if ((breakTop.get().isPressed() || autoMineTop.get()) && !(BlockUtil.getBlock(playerArray.get(0).getBlockPos().up(2)) instanceof BedBlock) && !BlockUtil.isAir(playerArray.get(0).getBlockPos().up(2)) && BlockUtil.getState(playerArray.get(0).getBlockPos().up(2)).getHardness(mc.world, playerArray.get(0).getBlockPos().up(2)) != -1 && BlockUtil.distance(new BlockPos(mc.player.getEyePos()),  playerArray.get(0).getBlockPos().up(2)) <= placeRange.get()){
            if (onlyInHoleTop.get() && !EntityUtil.isInHole(true, playerArray.get(0))) return;
            if (onlyOnGroundTop.get() && !mc.player.isOnGround()) return;
            minePos = playerArray.get(0).getBlockPos().up(2);
            mine(playerArray.get(0).getBlockPos().up(2), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem).slot(), topMineMode.get());
            return;
        }

        if (playerArray.isEmpty()) return;
        PlayerEntity target = playerArray.get(0);

        this.target = target;

        if (breakPos == null){
            breakPos = findBreak(target);
        }

        if (breakPos != null && (breakTimer.hasPassed(breakMs) || (isStartBreakingBed && antiMine.get() && (useTimer.get() && mineTimer.hasPassed(timerDelay.get() * 50F))))){
            breakBed(breakPos);
            isStartBreakingBed = false;
        }

        //Find Pos



        if (breakPos == null){
            try {
                if (array.isEmpty()) return;
                Pair<BlockPos, BlockPos> pair = findPlacePos(array);
                if (pair == null) return;
                if (stringBreaker.get() && BlockUtil.getBlock(pair.getLeft()) == Block.getBlockFromItem(Items.STRING)){
                    if (onlyInHoleString.get() && !EntityUtil.isInHole(true, playerArray.get(0))) return;
                    if (onlyOnGroundString.get() && !mc.player.isOnGround()) return;
                    mine(pair.getLeft(), mc.player.getInventory().selectedSlot, stringwebMineMode.get());
                    return;
                }
                if (webBreaker.get() && InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof SwordItem).found() && BlockUtil.getBlock(pair.getLeft()) == Blocks.COBWEB){
                    if (onlyInHoleString.get() && !EntityUtil.isInHole(true, playerArray.get(0))) return;
                    if (onlyOnGroundString.get() && !mc.player.isOnGround()) return;
                    mine(pair.getLeft(), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof SwordItem).slot(), stringwebMineMode.get());
                    return;
                }
                if (stopPlaceBurrowed.get() && EntityUtil.isBurrowed(target)) return;
                if (stopPlaceSelfHole.get() && target.getBlockPos() == mc.player.getBlockPos());

                if (EntityUtil.getPlayerSpeed(target) > minSpeed.get() && !pair.getLeft().equals(target.getBlockPos()) && !pair.getLeft().equals(target.getBlockPos().up()) && !pair.getLeft().equals(target.getBlockPos().up(2))){
                    placeMs = movePlaceDelay.get() * 50f;
                    breakMs = moveBreakDelay.get() * 50f;
                }else if (EntityUtil.getPlayerSpeed(target) <= minSpeed.get() && pair.getLeft().equals(target.getBlockPos()) && pair.getLeft().equals(target.getBlockPos().up()) && pair.getLeft().equals(target.getBlockPos().up(2))){
                    placeMs = findPlaceDelay.get() * 50f;
                    breakMs = findBreakDelay.get() * 50f;
                }

                if (pair.getLeft().equals(target.getBlockPos().up()) && EntityUtil.isInHole(true, target)){
                    placeMs = holePlaceDelay.get() * 50F;
                    breakMs = holeBreakDelay.get() * 50F;
                    if (trap.get()){
                        BlockUtils.place(target.getBlockPos().up(2), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem), false, 50);
                    }
                }
                if (pair.getLeft().equals(target.getBlockPos().up(2)) && EntityUtil.isInHole(true, target)){
                    placeMs = topPlaceDelay.get() * 50F;
                    breakMs = topBreakDelay.get() * 50F;
                }
                if (pair.getLeft().equals(target.getBlockPos())){
                    placeMs = surroundPlaceDelay.get() * 50F;
                    breakMs = surroundBreakDelay.get() * 50F;
                }

                if (pair.getLeft().equals(target.getBlockPos().up()) && pressing.get()){
                    if ((onlyOnHole.get() && EntityUtil.isInHole(true, target)) || !onlyOnHole.get()){
                        placeMs = 0;
                        breakMs = pressBreakDelay.get() * 50F;
                    }
                }

                if (placeTimer.hasPassed(placeMs) && placeBed(pair, target)){
                    head = pair.getLeft();
                    placeTimer.reset();
                    breakTimer.reset();
                }
            }catch (NullPointerException ignored){}
        }
    }

    @EventHandler
    private void startBreakingBed(StartBreakingBlockEvent event){
        if (breakPos == null && !antiMine.get()) return;
        if (event.blockPos.equals(breakPos)){
            isStartBreakingBed = true;
            mineTimer.reset();
        }
    }

    private boolean placeBed(Pair<BlockPos, BlockPos> pair, PlayerEntity target){
        if (pair.getRight() == null || pair.getLeft() == null) return false;
        array.clear();
        headPos = null;
        feetPos = null;

        FindItemResult bed = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (bed.getHand() == null && !autoSwitch.get()) return false;
        Direction direction = Direction.EAST;

        if (pair.getRight().equals(pair.getLeft().west())) direction = Direction.EAST;
        if (pair.getRight().equals(pair.getLeft().east())) direction = Direction.WEST;
        if (pair.getRight().equals(pair.getLeft().south())) direction = Direction.NORTH;
        if (pair.getRight().equals(pair.getLeft().north())) direction = Direction.SOUTH;

        Direction finalDirection1 = direction;
        currentDamage = bedDamage(target, pair.getLeft(), ignoreBed.get());

        this.direction = direction;
        Rotations.rotate(direction.asRotation(), Rotations.getPitch(pair.getLeft()), () -> {
            int prev = mc.player.getInventory().selectedSlot;
            if (testPlace.get()){
                PlayerUtil.place(pair.getRight(), Hand.MAIN_HAND, bed.slot(), false, true);
            }
            else if (meteorPlace.get()){
                BlockUtils.place(pair.getRight(), bed, false, 0);
            }
            else {
                mc.player.getInventory().selectedSlot = bed.slot();
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pair.getRight()), Direction.UP, pair.getRight(), false));
            }
            if (silentSwitch.get()) mc.player.getInventory().selectedSlot = prev;
            headPos = pair.getLeft();
            feetPos = pair.getRight();
            renderBlocks.add(renderBlockPool.get().setBlock(headPos, feetPos, fadeTick.get()));
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            breakPos = pair.getRight();
        });
        return true;
    }

    private Pair<BlockPos, BlockPos> findPlacePos(List<BlockPos> array){
        List<BlockPos> newArray = new ArrayList<>();
        if (array.size() > 1000){
            newArray = array.subList(0, 1000);
        }else newArray = array;

        if (newArray.isEmpty()) return null;
        List<BlockPos> placePos = new ArrayList<>();

        for (CardinalDirection cardinalDirection : CardinalDirection.values()){
            BlockPos blockPos = newArray.get(0).offset(cardinalDirection.toDirection());
            Box box = new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 0.6, blockPos.getZ() + 1);
            if ((BlockUtil.isAir(blockPos) || BlockUtil.getBlock(blockPos) instanceof BedBlock || BlockUtil.getBlock(blockPos) == Blocks.LAVA || BlockUtil.getBlock(blockPos) == Blocks.FIRE) && !EntityUtils.intersectsWithEntity(box, entity -> entity instanceof EndCrystalEntity || entity instanceof PlayerEntity) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos) <= placeRange.get()){
                placePos.add(blockPos);
            }
        }

        if (placePos.isEmpty()) {
            array.remove(0);
            return findPlacePos(array);
        }
        else {
            placePos.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
            return new Pair<>(newArray.get(0), placePos.get(0));
        }
    }


    private List<BlockPos> getSortPos(List<Pair<BlockPos, Double>> array){
        List<BlockPos> firstArray = new ArrayList<>();
        List<BlockPos> secondArray = new ArrayList<>();

        int newSize = array.size();
        double damage = array.get(0).getRight();
        for (int i = 0; i < newSize; i++){
            if (array.get(i).getRight() == damage){
                firstArray.add(array.get(i).getLeft());
            }
            else {
                secondArray.add(array.get(i).getLeft());
            }
        }

        firstArray.addAll(secondArray);
        return firstArray;
    }

    private List<Pair<BlockPos, Double>> giveDamage(List<BlockPos> array, PlayerEntity target){
        List<Pair<BlockPos, Double>> newArray = new ArrayList<>();
        int size = array.size();
        for (int i = 0; i < size; i++){
            double damage;
            double selfdamage;
            damage = bedDamage(target, array.get(i), ignoreBed.get());
            assert mc.player != null;
            selfdamage = bedDamage(mc.player, array.get(i), ignoreBed.get());
            if ((damage >= minDamage.get() || (target.getHealth() - damage <= minHealth.get() && lethalDamage.get())) && selfdamage <= maxSelfDamage.get()){
                newArray.add(new Pair<>(array.get(i), damage));
            }
        }
        newArray.sort(Comparator.comparing(this::sortMaxDamage));
        return newArray;
    }

    private List<BlockPos> sortBedPos(List<BlockPos> array, PlayerEntity target){
        List<Pair<BlockPos, Double>> newArray = new ArrayList<>();
        int size = array.size();
        try{
            for (int i = 0; i < size; i++){
                double damage;
                double selfdamage;
                damage = bedDamage(target, array.get(i), ignoreBed.get());
                assert mc.player != null;
                selfdamage = bedDamage(mc.player, array.get(i), ignoreBed.get());
                if ((damage >= minDamage.get() || (target.getHealth() - damage <= minHealth.get() && lethalDamage.get())) && selfdamage <= maxSelfDamage.get()){
                    newArray.add(new Pair<>(array.get(i), damage));
                }
            }
            newArray.sort(Comparator.comparing(this::sortMaxDamage));

            List<BlockPos> firstArray = new ArrayList<>();
            List<BlockPos> secondArray = new ArrayList<>();

            int newSize = newArray.size();
            double damage = newArray.get(0).getRight();
            for (int i = 0; i < newSize; i++){
                if (newArray.get(i).getRight() == damage){
                    firstArray.add(newArray.get(i).getLeft());
                }
                else {
                    secondArray.add(newArray.get(i).getLeft());
                }
            }
            switch (sortMode.get()) {
                case CloseToPlayer -> firstArray.sort(Comparator.comparing(bedBlock -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), bedBlock)));
                case CloseToTarget -> firstArray.sort(Comparator.comparing(bedBlock -> BlockUtil.distance(target.getBlockPos(), bedBlock)));
                case MinSelfHealth -> firstArray.sort(Comparator.comparing(bedblock -> bedDamage(mc.player, bedblock, ignoreBed.get())));
            }
            firstArray.addAll(secondArray);
            return firstArray;
        }catch (ConcurrentModificationException ignored){}
        return null;
    }

    private void breakBed(BlockPos pos) {
        if (pos == null) return;
        breakPos = null;
        assert mc.world != null;
        if (!(mc.world.getBlockState(pos).getBlock() instanceof BedBlock)) return;
        assert mc.player != null;
        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) mc.player.setSneaking(false);
        assert mc.interactionManager != null;
        if (testBreak.get()){
            BlockHitResult result = strictDirection.get() ? mc.world.raycastBlock(mc.player.getEyePos(), new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), pos, VoxelShapes.UNBOUNDED, BlockUtil.getState(pos)) : null;
            Vec3d hitVec = new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5, 0.5, 0.5);
            Direction facing = result == null || result.getSide() == null ? Direction.UP : result.getSide();
            BlockUtil.rightClickBlock(pos, hitVec, handMode.get(), facing, packetBreak.get(), swing.get());
        }else {
            if (rotateBreak.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, true));
        }

        mc.player.setSneaking(wasSneaking);
    }

    private BlockPos findBreak(PlayerEntity target){
        List<BlockPos> array = BlockUtil.getSphere(new BlockPos(new BlockPos(mc.player.getEyePos())), xBreakRange.get().intValue(), yBreakRange.get().intValue()).stream().filter(blockPos -> BlockUtil.getBlock(blockPos) instanceof BedBlock).collect(Collectors.toList());

        array = array.stream().filter(blockPos -> {
            Vec3d bedVec = Utils.vec3d(blockPos);
            if (PlayerUtils.distanceTo(blockPos) > breakRange.get()) return false;
            if (bedDamage(target, blockPos, ignoreBed.get()) < minDamage.get() && bedDamage(mc.player, blockPos, ignoreBed.get()) > maxSelfDamage.get()) return false;
            if ((antiSuicide.get() && PlayerUtils.getTotalHealth() - bedDamage(mc.player, blockPos, ignoreBed.get()) < 0)) return false;
            if (lethalDamage.get() && PlayerUtils.getTotalHealth() - bedDamage(mc.player, blockPos, ignoreBed.get()) < minHealth.get()) return false;
            return true;
        }).collect(Collectors.toList());

        array.sort(Comparator.comparing(blockPos -> sortMaxBreakDamage(bedDamage(target, blockPos, ignoreBed.get()))));
        if (array.isEmpty()) return null;
        return array.get(0);
    }

    private double sortMaxBreakDamage(Double currentDamage){
        return 400 - currentDamage;
    }

    private double sortMaxDamage(Pair<BlockPos, Double> arrays){
        return 400 - arrays.getRight();
    }

    private int checkOpenest(PlayerEntity player){
        if (EntityUtil.isSelfTraped(player, enemyRange.get()) && EntityUtil.isInHole(false, player)) return 2;
        else if (EntityUtil.isInHole(false, player) && !EntityUtil.isSelfTraped(player, enemyRange.get())) return 1;
        else return 0;
    }

    private void mine(BlockPos pos, int slot, MineMode mineMode){
        if (pos == null) return;
        mc.player.getInventory().selectedSlot = slot;
        if (mineMode == MineMode.Vanilla) BlockUtils.breakBlock(pos, swing.get());
        else {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN));
            if(swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
        }
    }

    private boolean isSelfTraped(PlayerEntity player){
        List<BlockPos> selftrapArray = new ArrayList<>(){{
            add(player.getBlockPos().up().west());
            add(player.getBlockPos().up().east());
            add(player.getBlockPos().up().south());
            add(player.getBlockPos().up().north());
            add(player.getBlockPos().up(2));
        }};
        selftrapArray = selftrapArray.stream().filter(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos) < placeRange.get()).collect(Collectors.toList());
        for (BlockPos pos : selftrapArray){
            if (BlockUtil.isAir(pos) || BlockUtil.getBlock(pos) instanceof BedBlock || BlockUtil.getBlock(pos) == Blocks.LAVA || BlockUtil.getBlock(pos) == Blocks.FIRE) return false;
        }
        return true;
    }

    @EventHandler
    private void arrayRender(Render3DEvent event){
        try {
            if (renderArray.get() && !array.isEmpty()){
                array.forEach(blockPos -> {
                    event.renderer.box(blockPos, arraysideColor.get(), arraylineColor.get(), arrayshapeMode.get(), 0);
                });
            }
        }catch (NullPointerException ignored){}
    }

    private boolean isSafeFromFacePlace(PlayerEntity player){
        List<BlockPos> selftrapArray = new ArrayList<>(){{
            add(player.getBlockPos().up().west());
            add(player.getBlockPos().up().east());
            add(player.getBlockPos().up().south());
            add(player.getBlockPos().up().north());
        }};
        selftrapArray =  selftrapArray.stream().filter(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos) < placeRange.get()).collect(Collectors.toList());
        for (BlockPos pos : selftrapArray){
            if (BlockUtil.isAir(pos) || BlockUtil.getBlock(pos) instanceof BedBlock || BlockUtil.getBlock(pos) == Blocks.LAVA || BlockUtil.getBlock(pos) == Blocks.FIRE) return false;
        }
        return true;
    }

    private Explosion explosion = null;
    private RaycastContext raycastContext = null;

    private double bedDamage(PlayerEntity player, BlockPos pos, boolean ignoreBed) {
        if (player == null || player.getAbilities().creativeMode && !(player instanceof FakePlayerEntity)) return 0;

        Vec3d position = Vec3d.ofCenter(pos);
        if (explosion == null) explosion = new Explosion(mc.world, null, position.x, position.y, position.z, 5.0F, true, Explosion.DestructionType.DESTROY);
        else ((IExplosion) explosion).set(position, 5.0F, true);

        double distance = Math.sqrt(player.squaredDistanceTo(position));
        if (distance > 10) return 0;

        if (raycastContext == null) raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);

        double exposure = getExposure(position, player, prediction.get(), raycastContext, ignoreTerrain.get(), ignoreBed);
        double impact = (1.0 - (distance / 10.0)) * exposure;
        double damage = (impact * impact + impact) / 2 * 7 * (5 * 2) + 1;

        // Damage calculation
        damage = getDamageForDifficulty(damage);
        damage = resistanceReduction(player, damage);
        damage = getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());
        damage = blastProtReduction(player, damage, explosion);

        return damage < 0 ? 0 : damage;
    }

    private double getExposure(Vec3d source, Entity entity, boolean predictMovement, RaycastContext context, boolean ignoreTerrain, boolean ignoreBed) {
        Box box = entity.getBoundingBox();

        if (predictMovement && !entity.isOnGround()) {
            Vec3d v = entity.getVelocity();
            box = box.offset(v.x, v.y, v.z);
        }

        double d = 1 / ((box.maxX - box.minX) * 2 + 1);
        double e = 1 / ((box.maxY - box.minY) * 2 + 1);
        double f = 1 / ((box.maxZ - box.minZ) * 2 + 1);
        double g = (1 - Math.floor(1 / d) * d) / 2;
        double h = (1 - Math.floor(1 / f) * f) / 2;

        if (!(d < 0) && !(e < 0) && !(f < 0)) {
            int i = 0;
            int j = 0;

            for (double k = 0; k <= 1; k += d) {
                for (double l = 0; l <= 1; l += e) {
                    for (double m = 0; m <= 1; m += f) {
                        double n = MathHelper.lerp(k, box.minX, box.maxX);
                        double o = MathHelper.lerp(l, box.minY, box.maxY);
                        double p = MathHelper.lerp(m, box.minZ, box.maxZ);

                        ((IRaycastContext) context).set(new Vec3d(n + g, o, p + h), source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
                        if (raycast(context, ignoreTerrain, ignoreBed).getType() == HitResult.Type.MISS) i++;

                        j++;
                    }
                }
            }

            return (double) i / j;
        }

        return 0;
    }

    private BlockHitResult raycast(RaycastContext context, boolean ignoreTerrain, boolean ignoreBed) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycast, pos) -> {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock().getBlastResistance() < 600 && ignoreTerrain) state = Blocks.AIR.getDefaultState();
            if (ignoreBed && state.getBlock() instanceof BedBlock) state = Blocks.AIR.getDefaultState();

            Vec3d vec1 = raycast.getStart();
            Vec3d vec2 = raycast.getEnd();

            VoxelShape shape = raycast.getBlockShape(state, mc.world, pos);
            BlockHitResult result1 = mc.world.raycastBlock(vec1, vec2, pos, shape, state);
            VoxelShape voxelShape2 = VoxelShapes.empty();
            BlockHitResult result2 = voxelShape2.raycast(vec1, vec2, pos);

            double d = result1 == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(result1.getPos());
            double e = result2 == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(result2.getPos());

            return d <= e ? result1 : result2;
        }, (raycast) -> {
            Vec3d vec = raycast.getStart().subtract(raycast.getEnd());
            return BlockHitResult.createMissed(raycast.getEnd(), Direction.getFacing(vec.x, vec.y, vec.z), new BlockPos(raycast.getEnd()));
        });
    }

    private double getDamageForDifficulty(double damage) {
        return switch (mc.world.getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY     -> Math.min(damage / 2 + 1, damage);
            case HARD     -> damage * 3 / 2;
            default       -> damage;
        };
    }

    private double blastProtReduction(Entity player, double damage, Explosion explosion) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.explosion(explosion));
        if (protLevel > 20) protLevel = 20;

        damage *= (1 - (protLevel / 25.0));
        return damage < 0 ? 0 : damage;
    }

    private double resistanceReduction(LivingEntity player, double damage) {
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int lvl = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= (1 - (lvl * 0.2));
        }

        return damage < 0 ? 0 : damage;
    }

    private float getDamageLeft(float damage, float armor, float armorToughness) {
        float f = 2.0F + armorToughness / 4.0F;
        float g = MathHelper.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        return damage * (1.0F - g / 25.0F);
    }

    private BlockPos getBestTrapPos(PlayerEntity player){
        List<BlockPos> selftrapArray = new ArrayList<>(){{
            add(player.getBlockPos().up().west());
            add(player.getBlockPos().up().east());
            add(player.getBlockPos().up().south());
            add(player.getBlockPos().up().north());
        }};

        selftrapArray = selftrapArray.stream().filter(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos) < placeRange.get() && !BlockUtil.isAir(blockPos) && !(BlockUtil.getBlock(blockPos) instanceof BedBlock) && BlockUtil.getState(blockPos).getHardness(mc.world, blockPos) != -1).collect(Collectors.toList());
        selftrapArray.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
        if (selftrapArray.isEmpty()) return null;

        return selftrapArray.get(0);
    }

    private List<BlockPos> getPosAround(){
        return BlockUtil.getSphere(mc.player.getBlockPos(), xPlaceRange.get().intValue(), yPlaceRange.get().intValue()).stream().filter(this::canBedPlace).collect(Collectors.toList());
    }

    private boolean canBedPlace(BlockPos blockPos){
        Box box = new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 0.6, blockPos.getZ() + 1);
        if (!BlockUtil.isAir(blockPos) && !(BlockUtil.getBlock(blockPos) instanceof BedBlock) && BlockUtil.getBlock(blockPos) != Blocks.LAVA && BlockUtil.getBlock(blockPos) != Blocks.FIRE) return false;
        if ((!BlockUtil.isAir(blockPos.west()) && !(BlockUtil.getBlock(blockPos.west()) instanceof BedBlock) && BlockUtil.getBlock(blockPos.west()) != Blocks.LAVA && BlockUtil.getBlock(blockPos.west()) != Blocks.FIRE) &&
            (!BlockUtil.isAir(blockPos.east()) && !(BlockUtil.getBlock(blockPos.east()) instanceof BedBlock) && BlockUtil.getBlock(blockPos.east()) != Blocks.LAVA && BlockUtil.getBlock(blockPos.east()) != Blocks.FIRE) &&
            (!BlockUtil.isAir(blockPos.south()) && !(BlockUtil.getBlock(blockPos.south()) instanceof BedBlock) && BlockUtil.getBlock(blockPos.south()) != Blocks.LAVA && BlockUtil.getBlock(blockPos.south()) != Blocks.FIRE) &&
            (!BlockUtil.isAir(blockPos.north()) && !(BlockUtil.getBlock(blockPos.north()) instanceof BedBlock) && BlockUtil.getBlock(blockPos.north()) != Blocks.LAVA && BlockUtil.getBlock(blockPos.north()) != Blocks.FIRE)
        ) return false;
        if (EntityUtils.intersectsWithEntity(box, entity -> entity instanceof EndCrystalEntity)) {
            return false;
        }
        if (oneDotTwelve.get() && BlockUtil.isAir(blockPos.down())) return false;
        return true;
    }


    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (render.get()){
            switch (renderMode.get()) {
                case BedForm -> {
                    if (!renderBlocks.isEmpty()) {
                        renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
                        renderBlocks.forEach(block -> block.renderBedForm(event, shapeMode.get()));
                    }
                }
                case DoubleBox -> {
                    if (fade.get()) {
                        renderBlocks.sort(Comparator.comparingInt(o -> o.ticks));
                        renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
                    } else {
                        if (headPos == null) return;
                        Box box = new Box(headPos.getX(), headPos.getY(), headPos.getZ(), headPos.getX() + 1, headPos.getY() + 0.6, headPos.getZ() + 1);
                        Box box2 = new Box(feetPos.getX(), feetPos.getY(), feetPos.getZ(), feetPos.getX() + 1, feetPos.getY() + 0.6, feetPos.getZ() + 1);

                        event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                        event.renderer.box(box2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    }
                }
                case SingleBox -> {
                    if (sideColor.get() == null || lineColor.get() == null) return;
                    if (fade.get()) {
                        renderBlocks.sort(Comparator.comparingInt(o -> o.ticks));
                        renderBlocks.forEach(renderBlock -> renderBlock.renderPart(event, boxMode.get(), sideColor.get(), lineColor.get(), shapeMode.get()));
                    } else {
                        if (headPos == null) return;
                        switch (boxMode.get()) {
                            case Head -> {
                                Box box = new Box(headPos.getX(), headPos.getY(), headPos.getZ(), headPos.getX() + 1, headPos.getY() + 1, headPos.getZ() + 1);
                                event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            }
                            case Feet -> {
                                Box box2 = new Box(feetPos.getX(), feetPos.getY(), feetPos.getZ(), feetPos.getX() + 1, feetPos.getY() + 1, feetPos.getZ() + 1);
                                event.renderer.box(box2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                            }
                        }
                    }
                }
                case Lines -> {
                    renderBlocks.sort(Comparator.comparingInt(o -> o.ticks));
                    renderBlocks.forEach(renderBlock -> renderBlock.renderLines(event, pillowSideColor.get(), bedSpreadSideColor.get(), woodSideColor.get(), pillowLineColor.get(), bedSpreadLineColor.get(), woodLineColor.get(), shapeMode.get()));
                }
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get() || !renderDamage.get()) return;

        if (headPos != null && feetPos != null) {
            vec3.set(headPos.getX() + 0.5, headPos.getY() + 0.5, headPos.getZ() + 0.5);
            if (NametagUtils.to2D(vec3, damageTextScale.get())) {
                NametagUtils.begin(vec3);
                TextRenderer.get().begin(1, false, true);

                String text = String.format("%.2f", currentDamage);
                double w = TextRenderer.get().getWidth(text) / 2;
                TextRenderer.get().render(text, -w, 0, textColor.get(), true);

                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }


    public class RenderBlock {
        public int ticks;
        public double offset;

        public BlockPos.Mutable head = new BlockPos.Mutable();
        public BlockPos.Mutable feet = new BlockPos.Mutable();
        private Color sidesTop;
        private Color sidesBottom;
        private Color linesTop;
        private Color linesBottom;


        public RenderBlock setBlock(BlockPos head, BlockPos feet, int tick) {
            this.head.set(head);
            this.feet.set(feet);
            ticks = tick;

            sidesTop = sideColorTop.get();
            sidesBottom = sideColorBottom.get();
            linesTop = lineColorTop.get();
            linesBottom = lineColorBottom.get();

            offset = 1;
            return this;
        }


        public void tick() {
            ticks--;
            if (shrink.get() && offset >= 0 && fadeTick.get() - ticks >= shrinkTicks.get()) offset -= shrinkSpeed.get() / 100;
        }

        public void renderPart(Render3DEvent event, BoxMode boxMode, Color side1, Color line1, ShapeMode shapeMode) {
            int preSideA = side1.a;
            int preLineA = line1.a;

            side1.a *= (double) ticks / 10;
            line1.a *= (double) ticks / 10;

            switch (boxMode){
                case Head -> {
                    Box box = new Box(head.getX(), head.getY(), head.getZ(), head.getX() + 1, head.getY() + 1, head.getZ() + 1);
                    event.renderer.box(box, side1, line1, shapeMode, 0);
                }
                case Feet -> {
                    Box box2 = new Box(feet.getX(), feet.getY(), feet.getZ(), feet.getX() + 1, feet.getY() + 1, feet.getZ() + 1);
                    event.renderer.box(box2, side1, line1, shapeMode, 0);
                }
            }

            side1.a = preSideA;
            line1.a = preLineA;
        }

        public void render(Render3DEvent event, Color side1, Color line1, ShapeMode shapeMode) {
            int preSideA = side1.a;
            int preLineA = line1.a;

            side1.a *= (double) ticks / 10;
            line1.a *= (double) ticks / 10;

            Box box = new Box(head.getX(), head.getY(), head.getZ(), head.getX() + 1, head.getY() + 0.6, head.getZ() + 1);
            Box box2 = new Box(feet.getX(), feet.getY(), feet.getZ(), feet.getX() + 1, feet.getY() + 0.6, feet.getZ() + 1);

            event.renderer.box(box, side1, line1, shapeMode, 0);
            event.renderer.box(box2, side1, line1, shapeMode, 0);

            side1.a = preSideA;
            line1.a = preLineA;
        }

        public void renderLines(Render3DEvent event, Color side1, Color side2, Color side3, Color line1, Color line2, Color line3, ShapeMode shapeMode) {
            int preSideA = side1.a;
            int preSideB = side2.a;
            int preSideC = side2.a;
            int preLineA = line1.a;
            int preLineB = line2.a;
            int preLineC = line2.a;

            side1.a *= (double) ticks / 10;
            side2.a *= (double) ticks / 10;
            side3.a *= (double) ticks / 10;
            line1.a *= (double) ticks / 10;
            line2.a *= (double) ticks / 10;
            line3.a *= (double) ticks / 10;

            int x = head.getX();
            int y = head.getY();
            int z = head.getZ();

            Direction dir = Direction.EAST;
            if (feet.equals(head.west())) dir = Direction.EAST;
            if (feet.equals(head.east())) dir = Direction.WEST;
            if (feet.equals(head.south())) dir = Direction.NORTH;
            if (feet.equals(head.north())) dir = Direction.SOUTH;

            switch (dir) {
                case NORTH -> {
                    event.renderer.box(x, y + 0.31, z, x + 1, y + 0.6, z + 0.49, side1, line1, shapeMode, 0);
                    event.renderer.box(x, y + 0.31, z + 0.51, x + 1, y + 0.6, z + 2, side2, line2, shapeMode, 0);
                    event.renderer.box(x, y, z, x + 1, y + 0.29, z + 2, side3, line3, shapeMode, 0);
                }
                case SOUTH -> {
                    event.renderer.box(x, y + 0.31, z + 1, x + 1, y + 0.6, z + 0.49, side1, line1, shapeMode, 0);
                    event.renderer.box(x, y + 0.31, z + 0.51, x + 1, y + 0.6, z - 1, side2, line2, shapeMode, 0);
                    event.renderer.box(x, y, z - 1, x + 1, y + 0.29, z + 1, side3, line3, shapeMode, 0);
                }
                case EAST -> {
                    event.renderer.box(x + 1, y + 0.31, z, x + 0.49, y + 0.6, z + 1, side1, line1, shapeMode, 0);
                    event.renderer.box(x + 0.51, y + 0.31, z, x - 1, y + 0.6, z + 1, side2, line2, shapeMode, 0);
                    event.renderer.box(x - 1, y, z, x + 1, y + 0.29, z + 1, side3, line3, shapeMode, 0);
                }

                //event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case WEST -> {
                    event.renderer.box(x, y + 0.31, z, x + 0.49, y + 0.6, z + 1, side1, line1, shapeMode, 0);
                    event.renderer.box(x + 0.49, y + 0.31, z, x + 2, y + 0.6, z + 1, side2, line2, shapeMode, 0);
                    event.renderer.box(x, y, z, x + 2, y + 0.29, z + 1, side3, line3, shapeMode, 0);
                }
            }

            side1.a = preSideA;
            side2.a = preSideB;
            side3.a = preSideC;
            line1.a = preLineA;
            line2.a = preLineB;
            line3.a = preLineC;
        }

        public void renderBedForm(Render3DEvent event, ShapeMode shapeMode) {
            if (sidesTop == null || sidesBottom == null || linesTop == null || linesBottom == null || head == null || feet == null) return;

            int preSideTopA = sidesTop.a;
            int preSideBottomA = sidesBottom.a;
            int preLineTopA = linesTop.a;
            int preLineBottomA = linesBottom.a;

            sidesTop.a *= (double) ticks / 8;
            sidesBottom.a *= (double) ticks / 8;
            linesTop.a *= (double) ticks / 8;
            linesBottom.a *= (double) ticks / 8;

            double x = head.getX() + (shrink.get() ? 0.5 - offset / 2 : 0);
            double y = head.getY() + (shrink.get() ? 0.5 - offset / 2 : 0);
            double z = head.getZ() + (shrink.get() ? 0.5 - offset / 2 : 0);

            double px3 = feetLength.get() / 10 * (shrink.get() ? offset : 1);
            double px8 = bedHeight.get() / 10 * (shrink.get() ? offset : 1);

            double px16 = 1 * (shrink.get() ? offset : 1);
            double px32 = 2 * (shrink.get() ? offset : 1);

            Direction dir = Direction.EAST;

            if (feet.equals(head.west())) dir = Direction.WEST;
            if (feet.equals(head.east())) dir = Direction.EAST;
            if (feet.equals(head.south())) dir = Direction.SOUTH;
            if (feet.equals(head.north())) dir = Direction.NORTH;

            if (dir == Direction.NORTH) z -= 1;
            else if (dir == Direction.WEST) x -= 1;

            // Lines

            if (shapeMode.lines()) {
                if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                    // Edges

                    renderEdgeLines(x, y, z, px3, 1, event);
                    renderEdgeLines(x, y, z + px32 - px3, px3, 2, event);
                    renderEdgeLines(x + px16 - px3, y, z, px3, 3, event);
                    renderEdgeLines(x + px16 - px3, y, z + px32 - px3, px3, 4, event);

                    // High Lines

                    event.renderer.line(x, y + px3, z, x, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x, y + px3, z + px32, x, y + px8, z + px32, linesBottom, linesTop);
                    event.renderer.line(x + px16, y + px3, z, x + px16, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x + px16, y + px3, z + px32, x + px16, y + px8, z + px32, linesBottom, linesTop);

                    // Connections

                    event.renderer.line(x + px3, y + px3, z, x + px16 - px3, y + px3, z, linesBottom);
                    event.renderer.line(x + px3, y + px3, z + px32, x + px16 - px3, y + px3, z + px32, linesBottom);

                    event.renderer.line(x, y + px3, z + px3, x, y + px3, z + px32 - px3, linesBottom);
                    event.renderer.line(x + px16, y + px3, z + px3, x + px16, y + px3, z + px32 - px3, linesBottom);

                    if (renderExtra.get()) event.renderer.line(x, y + px3, z, x + px16, y + px8, z + px32, linesBottom, linesTop);

                    // Top

                    event.renderer.line(x, y + px8, z, x + px16, y + px8, z, linesTop);
                    event.renderer.line(x, y + px8, z + px32, x + px16, y + px8, z + px32, linesTop);
                    event.renderer.line(x, y + px8, z, x , y + px8, z + px32, linesTop);
                    event.renderer.line(x + px16, y + px8, z, x + px16, y + px8, z + px32, linesTop);
                } else {
                    // Edges

                    renderEdgeLines(x, y, z, px3, 1, event);
                    renderEdgeLines(x, y, z + px16 - px3, px3, 2, event);
                    renderEdgeLines(x + px32 - px3, y, z, px3, 3, event);
                    renderEdgeLines(x + px32 - px3, y, z + px16 - px3, px3, 4, event);

                    // High Lines

                    event.renderer.line(x, y + px3, z, x, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x + px32, y + px3, z, x + px32, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x, y + px3, z + px16, x, y + px8, z + px16, linesBottom, linesTop);
                    event.renderer.line(x + px32, y + px3, z + px16, x + px32, y + px8, z + px16, linesBottom, linesTop);

                    // Connections

                    event.renderer.line(x, y + px3, z + px3, x, y + px3, z + px16 - px3, linesBottom);
                    event.renderer.line(x + px32, y + px3, z + px3, x + px32, y + px3, z + px16 - px3, linesBottom);

                    event.renderer.line(x + px3, y + px3, z, x + px32 - px3, y + px3, z, linesBottom);
                    event.renderer.line(x + px3, y + px3, z + px16, x + px32 - px3, y + px3, z + px16, linesBottom);

                    if (renderExtra.get()) event.renderer.line(x, y + px8, z, x + px32, y + px3, z + px16, linesBottom, linesTop);

                    // Top

                    event.renderer.line(x, y + px8, z, x, y + px8, z + px16, linesTop);
                    event.renderer.line(x + px32, y + px8, z, x + px32, y + px8, z + px16, linesTop);
                    event.renderer.line(x, y + px8, z, x + px32 , y + px8, z, linesTop);
                    event.renderer.line(x, y + px8, z + px16, x + px32, y + px8, z + px16, linesTop);
                }
            }

            // Sides

            if (shapeMode.sides()) {
                if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                    // Horizontal

                    // Bottom

                    if (renderInnerSides.get()) {
                        sideHorizontal(x, y, z, x + px3, z + px3, event, sidesBottom, sidesBottom);
                        sideHorizontal(x + px16 - px3, y, z, x + px16, z + px3, event, sidesBottom, sidesBottom);
                        sideHorizontal(x, y, z + px32 - px3, x + px3, z + px32, event, sidesBottom, sidesBottom);
                        sideHorizontal(x + px16 - px3, y, z + px32 - px3, x + px16, z + px32, event, sidesBottom, sidesBottom);
                    }

                    // Middle & Top

                    if (renderInnerSides.get()) {
                        sideHorizontal(x + px3, y + px3, z, x + px16 - px3, z + px3, event, sidesBottom, sidesBottom);
                        sideHorizontal(x + px3, y + px3, z + px32 - px3, x + px16 - px3, z + px32, event, sidesBottom, sidesBottom);

                        sideHorizontal(x, y + px3, z + px3, x + px16, z + px32 - px3, event, sidesBottom, sidesBottom);
                    } else {
                        sideHorizontal(x, y + px3, z, x + px16, z + px32, event, sidesBottom, sidesBottom);
                    }

                    sideHorizontal(x, y + px8, z, x + px16, z + px32, event, sidesTop, sidesTop);

                    // Vertical

                    // Edges

                    renderEdgeSides(x, y, z, px3, 1, event);
                    renderEdgeSides(x, y, z + px32 - px3, px3, 2, event);
                    renderEdgeSides(x + px16 - px3, y, z, px3, 3, event);
                    renderEdgeSides(x + px16 - px3, y, z + px32 - px3, px3, 4, event);

                    // Sides

                    sideVertical(x, y + px3, z, x + px16, y + px8, z, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z + px32, x + px16, y + px8, z + px32, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z, x, y + px8, z + px32, event, sidesBottom, sidesTop);
                    sideVertical(x + px16, y + px3, z, x + px16, y + px8, z + px32, event, sidesBottom, sidesTop);
                } else {
                    // Horizontal

                    // Bottom

                    if (renderInnerSides.get()) {
                        sideHorizontal(x, y, z, x + px3, z + px3, event, sidesBottom, sidesBottom);
                        sideHorizontal(x, y, z + px16 - px3, x + px3, z + px16, event, sidesBottom, sidesBottom);
                        sideHorizontal(x + px32 - px3, y, z, x + px32, z + px3, event, sidesBottom, sidesBottom);
                        sideHorizontal(x + px32 - px3, y, z + px16 - px3, x + px32, z + px16, event, sidesBottom, sidesBottom);
                    }

                    // Middle & Top

                    if (renderInnerSides.get()) {
                        sideHorizontal(x, y + px3, z + px3, x + px3, z + px16 - px3, event, sidesBottom, sidesBottom);
                        sideHorizontal(x + px32 - px3, y + px3, z + px3, x + px32, z + px16 - px3, event, sidesBottom, sidesBottom);

                        sideHorizontal(x + px3, y + px3, z, x + px32 - px3, z + px16, event, sidesBottom, sidesBottom);
                    } else {
                        sideHorizontal(x, y + px3, z, x + px32, z + px16, event, sidesBottom, sidesBottom);
                    }

                    sideHorizontal(x, y + px8, z, x + px32, z + px16, event, sidesTop, sidesTop);

                    // Vertical

                    // Edges

                    renderEdgeSides(x, y, z, px3, 1, event);
                    renderEdgeSides(x + px32 - px3, y, z, px3, 3, event);
                    renderEdgeSides(x, y, z + px16 - px3, px3, 2, event);
                    renderEdgeSides(x + px32 - px3, y, z + px16 - px3, px3, 4, event);

                    // Sides

                    sideVertical(x, y + px3, z, x, y + px8, z + px16, event, sidesBottom, sidesTop);
                    sideVertical(x + px32, y + px3, z, x + px32, y + px8, z + px16, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z, x + px32, y + px8, z, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z + px16, x + px32, y + px8, z + px16, event, sidesBottom, sidesTop);
                }
            }

            // Resetting the Colors

            sidesTop.a = preSideTopA;
            sidesBottom.a = preSideBottomA;
            linesTop.a = preLineTopA;
            linesBottom.a = preLineBottomA;
        }

        // Render Utils

        private void renderEdgeLines(double x, double y, double z, double px3, int edge, Render3DEvent event) {
            if (renderInnerLines.get()) edge = 0;

            // Horizontal

            if (edge != 2 && edge != 4) event.renderer.line(x, y, z, x + px3, y, z, linesBottom);
            if (edge != 3 && edge != 4) event.renderer.line(x, y, z, x, y, z + px3, linesBottom);

            if (edge != 1 && edge != 2) event.renderer.line(x + px3, y, z, x + px3, y, z + px3, linesBottom);
            if (edge != 1 && edge != 3) event.renderer.line(x, y, z + px3, x + px3, y, z + px3, linesBottom);

            // Vertical

            if (edge != 4) event.renderer.line(x, y, z, x, y + px3, z, linesBottom);
            if (edge != 2) event.renderer.line(x + px3, y, z, x + px3, y + px3, z, linesBottom);
            if (edge != 3) event.renderer.line(x, y, z + px3, x, y + px3, z + px3, linesBottom);
            if (edge != 1) event.renderer.line(x + px3, y, z + px3, x + px3, y + px3, z + px3, linesBottom);
        }

        private void renderEdgeSides(double x, double y, double z, double px3, int edge, Render3DEvent event) {
            if (renderInnerSides.get()) edge = 0;

            // Horizontal

            if (edge != 4 && edge != 2) sideVertical(x, y, z, x + px3, y + px3, z, event, sidesBottom, sidesBottom);
            if (edge != 4 && edge != 3) sideVertical(x, y, z, x, y + px3, z + px3, event, sidesBottom, sidesBottom);
            if (edge != 1 && edge != 2) sideVertical(x + px3, y, z, x + px3, y + px3, z + px3, event, sidesBottom, sidesBottom);
            if (edge != 1 && edge != 3) sideVertical(x, y, z + px3, x + px3, y + px3, z + px3, event, sidesBottom, sidesBottom);
        }

        public void sideHorizontal(double x1, double y, double z1, double x2, double z2, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            side(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, event, bottomSideColor, topSideColor);
        }

        public void sideVertical(double x1, double y1, double z1, double x2, double y2, double z2, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            side(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, event, bottomSideColor, topSideColor);
        }

        private void side(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            event.renderer.quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, topSideColor, topSideColor, bottomSideColor, bottomSideColor);
        }
    }
}
