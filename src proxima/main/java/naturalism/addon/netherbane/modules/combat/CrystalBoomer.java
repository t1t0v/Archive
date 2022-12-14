package naturalism.addon.netherbane.modules.combat;


import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import naturalism.addon.netherbane.modules.render.TotemParticle;
import naturalism.addon.netherbane.modules.render.hud.CrystalStatsHud;
import naturalism.addon.netherbane.modules.render.hud.CrystalStreakHud;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;
import naturalism.addon.netherbane.*;
import naturalism.addon.netherbane.utils.*;
import naturalism.addon.netherbane.utils.Timer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static naturalism.addon.netherbane.utils.RenderUtil.*;

public class CrystalBoomer extends Module {
    private static MinecraftClient mc = MeteorClient.mc;

    private BlockPos renderPos = null;

    public CrystalBoomer() {
        super(NetherBane.COMBATPLUS, "crystal-boomer", "");
    }

    public enum ConfirmMode {
        Off, Semi, Full
    }

    public enum RotationMode {
        Off, Track, Interact
    }

    public enum Rotate {
        Vanilla, Packet
    }

    public enum TimingMode {
        Sequential, Adaptive
    }

    public enum YawStepMode {
        Off, Break, Full
    }

    public enum TargetingMode {
        All, Smart, Nearest
    }

    public enum DirectionMode {
        Vanilla, Normal, Strict
    }

    public enum SyncMode {
        Strict, Merge
    }

    public enum SwapMode {
        Normal,
        Silent
    }

    public enum AntiFriendPop {
        Place, Break, Both, None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<TimingMode> timing = sgGeneral.add(new EnumSetting.Builder<TimingMode>().name("timing").description(".").defaultValue(TimingMode.Adaptive).build());
    public final Setting<RotationMode> rotate = sgGeneral.add(new EnumSetting.Builder<RotationMode>().name("rotate").description(".").defaultValue(RotationMode.Off).build());
    public final Setting<Rotate> rotateMethod = sgGeneral.add(new EnumSetting.Builder<Rotate>().name("rotate-method").description(".").defaultValue(Rotate.Packet).build());
    public final Setting<Boolean> inhibit = sgGeneral.add(new BoolSetting.Builder().name("inhibit").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> limit = sgGeneral.add(new BoolSetting.Builder().name("limit").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<YawStepMode> yawStep = sgGeneral.add(new EnumSetting.Builder<YawStepMode>().name("yawStep").description(".").defaultValue(YawStepMode.Off).build());
    public final Setting<Double> yawAngle = sgGeneral.add(new DoubleSetting.Builder().name("yawAngle").description("The range at which players can be targeted.").defaultValue(0.3).min(0.1).sliderMax(1).build());
    public final Setting<Integer> yawTicks = sgGeneral.add(new IntSetting.Builder().name("yawTicks").description("The delay between placing beds in ticks.").defaultValue(1).min(0).sliderMax(5).build());
    public final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder().name("strictDirection").description("Only places beds in the direction you are facing.").defaultValue(false).build());

    public final SettingGroup sgPlacements = settings.createGroup("Placements");
    public final Setting<Boolean> check = sgPlacements.add(new BoolSetting.Builder().name("check").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<DirectionMode> directionMode = sgPlacements.add(new EnumSetting.Builder<DirectionMode>().name("directionMode").description(".").defaultValue(DirectionMode.Normal).build());
    public final Setting<Boolean> protocol = sgPlacements.add(new BoolSetting.Builder().name("protocol").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> liquids = sgPlacements.add(new BoolSetting.Builder().name("liquids").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> fire = sgPlacements.add(new BoolSetting.Builder().name("fire").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> attackIntersects = sgPlacements.add(new BoolSetting.Builder().name("attackIntersects").description("Only places beds in the direction you are facing.").defaultValue(true).build());

    public final SettingGroup sgSupport = settings.createGroup("Support");
    public final Setting<Boolean> surroundBreak = sgSupport.add(new BoolSetting.Builder().name("surround-break").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> withMineProgress = sgSupport.add(new BoolSetting.Builder().name("with-mine-progress").description("Only places beds in the direction you are facing.").defaultValue(true).visible(surroundBreak::get).build());
    private final Setting<Double> breakingProgress = sgSupport.add(new DoubleSetting.Builder().name("breaking-progress").description("The radius in which players get targeted.").defaultValue(0.9).min(0).sliderMax(1).visible(() -> withMineProgress.get() && surroundBreak.get()).build());
    public final Setting<Boolean> surroundHold = sgSupport.add(new BoolSetting.Builder().name("surround-hold").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> support = sgSupport.add(new BoolSetting.Builder().name("support").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    private final Setting<Keybind> forceSupport = sgSupport.add(new KeybindSetting.Builder().name("force-support").description("Starts face place when this button is pressed.").defaultValue(Keybind.none()).build());
    public final Setting<Integer> supportDelay = sgSupport.add(new IntSetting.Builder().name("support-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());

    public final SettingGroup sgSpeeds = settings.createGroup("Speeds");
    public final Setting<ConfirmMode> confirm = sgSpeeds.add(new EnumSetting.Builder<ConfirmMode>().name("confirm").description(".").defaultValue(ConfirmMode.Off).build());
    public final Setting<Integer> delay = sgSpeeds.add(new IntSetting.Builder().name("delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Boolean> fastBreak = sgSpeeds.add(new BoolSetting.Builder().name("fast-break").description("Ignores break delay and tries to break the crystal as soon as it's spawned in the world.").defaultValue(false).build());
    private final Setting<Boolean> useThread = sgSpeeds.add(new BoolSetting.Builder().name("use-thread").description("Ignores break delay and tries to break the crystal as soon as it's spawned in the world.").defaultValue(false).build());
    public final Setting<Integer> attackFactor = sgSpeeds.add(new IntSetting.Builder().name("attackFactor").description("The delay between placing beds in ticks.").defaultValue(20).min(0).sliderMax(20).build());
    public final Setting<Double> breakSpeed = sgSpeeds.add(new DoubleSetting.Builder().name("breakSpeed").description("The range at which players can be targeted.").defaultValue(20).min(0.1).sliderMax(20).build());
    public final Setting<Double> placeSpeed = sgSpeeds.add(new DoubleSetting.Builder().name("placeSpeed").description("The range at which players can be targeted.").defaultValue(20).min(0.1).sliderMax(20).build());
    public final Setting<SyncMode> syncMode = sgSpeeds.add(new EnumSetting.Builder<SyncMode>().name("syncMode").description(".").defaultValue(SyncMode.Merge).build());
    public final Setting<Double> mergeOffset = sgSpeeds.add(new DoubleSetting.Builder().name("mergeOffset").description("The range at which players can be targeted.").defaultValue(0.7).sliderMax(1).build());

    private final SettingGroup sgRanges = settings.createGroup("Ranges");
    private final Setting<Double> enemyRange = sgRanges.add(new DoubleSetting.Builder().name("enemyRange").description("The range at which players can be targeted.").defaultValue(8).min(0.0).sliderMax(15).build());
    public final Setting<Double> crystalRange = sgRanges.add(new DoubleSetting.Builder().name("crystalRange").description("The range at which players can be targeted.").defaultValue(5).min(0.0).sliderMax(10).build());
    public final Setting<Double> breakRange = sgRanges.add(new DoubleSetting.Builder().name("breakRange").description("The range at which players can be targeted.").defaultValue(5).min(0.0).sliderMax(10).build());
    public final Setting<Double> breakWallsRange = sgRanges.add(new DoubleSetting.Builder().name("breakWallsRange").description("The range at which players can be targeted.").defaultValue(5).min(0.0).sliderMax(10).build());
    public final Setting<Double> placeRange = sgRanges.add(new DoubleSetting.Builder().name("placeRange").description("The range at which players can be targeted.").defaultValue(5).min(0.0).sliderMax(10).build());
    public final Setting<Double> placeWallsRange = sgRanges.add(new DoubleSetting.Builder().name("placeWallsRange").description("The range at which players can be targeted.").defaultValue(5).min(0.0).sliderMax(10).build());

    public final SettingGroup sgSwap = settings.createGroup("Swap");
    public final Setting<Boolean> autoSwap = sgSwap.add(new BoolSetting.Builder().name("autoSwap").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    private final Setting<SwapMode> swapMode = sgSwap.add(new EnumSetting.Builder<SwapMode>().name("swap-mode").description("Which way to swap.").defaultValue(SwapMode.Normal).build());
    public final Setting<Double> swapDelay = sgSwap.add(new DoubleSetting.Builder().name("swapDelay").description("The range at which players can be targeted.").defaultValue(0).min(0.0).sliderMax(10).build());
    public final Setting<Double> switchDelay = sgSwap.add(new DoubleSetting.Builder().name("switchDelay").description("The range at which players can be targeted.").defaultValue(0).min(0.0).sliderMax(10).build());
    public final Setting<Boolean> antiWeakness = sgSwap.add(new BoolSetting.Builder().name("antiWeakness").description("Only places beds in the direction you are facing.").defaultValue(false).build());

    public final SettingGroup sgDamages = settings.createGroup("Damages");
    public final Setting<TargetingMode> target = sgDamages.add(new EnumSetting.Builder<TargetingMode>().name("target").description(".").defaultValue(TargetingMode.All).build());
    public final Setting<Double> security = sgDamages.add(new DoubleSetting.Builder().name("security").description("The range at which players can be targeted.").defaultValue(1).min(0.0).sliderMax(36).build());
    public final Setting<Double> compromise = sgDamages.add(new DoubleSetting.Builder().name("compromise").description("The range at which players can be targeted.").defaultValue(1).min(0.0).sliderMax(36).build());
    public final Setting<Double> minPlaceDamage = sgDamages.add(new DoubleSetting.Builder().name("minPlaceDamage").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(36).build());
    public final Setting<Double> maxSelfPlace = sgDamages.add(new DoubleSetting.Builder().name("maxSelfPlace").description("The range at which players can be targeted.").defaultValue(12).min(0.0).sliderMax(36).build());
    public final Setting<Double> suicideHealth = sgDamages.add(new DoubleSetting.Builder().name("suicideHealth").description("The range at which players can be targeted.").defaultValue(2).min(0.0).sliderMax(36).build());
    public final Setting<Double> faceplaceHealth = sgDamages.add(new DoubleSetting.Builder().name("facePlaceHealth").description("The range at which players can be targeted.").defaultValue(4).min(0.0).sliderMax(36).build());
    private final Setting<Keybind> forceFaceplace = sgDamages.add(new KeybindSetting.Builder().name("forceFaceplace").description("Starts face place when this button is pressed.").defaultValue(Keybind.none()).build());
    public final Setting<Boolean> armorBreaker = sgDamages.add(new BoolSetting.Builder().name("armorBreaker").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Double> minStackDamage = sgDamages.add(new DoubleSetting.Builder().name("min-stack-damage").description("The range at which players can be targeted.").defaultValue(100).min(0.0).sliderMax(560).visible(armorBreaker::get).build());

    public final SettingGroup sgAntiFriendPop = settings.createGroup("AntiFriendPop");
    private final Setting<AntiFriendPop> antiFriendPop = sgAntiFriendPop.add(new EnumSetting.Builder<AntiFriendPop>().name("anti-friend-pop").description("Prevents hurting your friends.").defaultValue(AntiFriendPop.None).build());
    public final Setting<Double> maxFriendDamage = sgAntiFriendPop.add(new DoubleSetting.Builder().name("max-friend-damage").description("Maximum amount of damage to friends for placing crystals.").defaultValue(12).min(0.0).sliderMax(36).build());

    public final SettingGroup sgPrediction = settings.createGroup("Prediction");
    public final Setting<Boolean> collision = sgPrediction.add(new BoolSetting.Builder().name("collision").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> predict = sgPrediction.add(new BoolSetting.Builder().name("predict").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> predictPops = sgPrediction.add(new BoolSetting.Builder().name("predictPops").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> terrainIgnore = sgPrediction.add(new BoolSetting.Builder().name("terrainIgnore").description("Only places beds in the direction you are facing.").defaultValue(false).build());

    public final SettingGroup sgPause = settings.createGroup("Pause");

    private final Setting<Boolean> stopPlacingOnEat = sgPause.add(new BoolSetting.Builder().name("stop-placing-on-eat").description("Stop-placing-on-eat.").defaultValue(false).build());
    private final Setting<Boolean> stopPlacingOnDrink = sgPause.add(new BoolSetting.Builder().name("stop-placing-on-drink").description("Stop-placing-on-drink.").defaultValue(false).build());
    private final Setting<Boolean> stopPlacingOnMine = sgPause.add(new BoolSetting.Builder().name("stop-placing-on-mine").description("Stop-placing-on-mine.").defaultValue(false).build());

    public final Setting<Boolean> noMineSwitch = sgPause.add(new BoolSetting.Builder().name("noMineSwitch").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> noGapSwitch = sgPause.add(new BoolSetting.Builder().name("noGapSwitch").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> rightClickGap = sgPause.add(new BoolSetting.Builder().name("rightClickGap").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> disableWhenKA = sgPause.add(new BoolSetting.Builder().name("disableWhenKA").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Boolean> disableWhenPA = sgPause.add(new BoolSetting.Builder().name("disableWhenPA").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<Double> disableUnderHealth = sgPause.add(new DoubleSetting.Builder().name("disableUnderHealth").description("The range at which players can be targeted.").defaultValue(2).min(0.0).sliderMax(10).build());
    public final Setting<Boolean> disableOnTP = sgPause.add(new BoolSetting.Builder().name("disableOnTP").description("Only places beds in the direction you are facing.").defaultValue(false).build());

    public final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder().name("swing").description("Renders hand swinging client side.").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay over the block the crystals are being placed on.").defaultValue(true).build());
    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Which way to render surround.").defaultValue(RenderMode.Fade).build());
    private final Setting<Integer> fadeTick = sgRender.add(new IntSetting.Builder().name("fade-tick").description("The slot auto move moves beds to.").defaultValue(20).visible(()->renderMode.get().equals(RenderMode.Fade)).build());
    public final Setting<Boolean> onlyTop = sgRender.add(new BoolSetting.Builder().name("OnlyTop").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("width").defaultValue(0.1).min(0).max(1).sliderMin(0).sliderMax(1).visible(onlyTop::get).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211, 75)).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211, 75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());
    private final Setting<Boolean> renderDamageText = sgRender.add(new BoolSetting.Builder().name("damage").description("Renders crystal damage text in the block overlay.").defaultValue(true).build());
    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("How big the damage text should be.").defaultValue(1.25).min(1).sliderMax(4).visible(renderDamageText::get).build());
    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder().name("text-color").description("The side color.").defaultValue(new SettingColor(255, 255, 255, 75)).visible(renderDamageText::get).build());
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

    public enum RenderMode {
        Fade,
        Default
    }

    private Vec3d rotationVector = null;
    private float[] rotations = new float[]{0F, 0F};
    private Timer rotationTimer = new Timer();

    private PlayerEntity targetPlayer;
    private EndCrystalEntity postBreakPos;
    private BlockPos postPlacePos;
    private BlockPos prevPlacePos = null;
    private Direction postFacing;
    private BlockHitResult postResult;

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();

    private final Timer noGhostTimer = new Timer();
    private final Timer switchTimer = new Timer();

    private BlockPos renderBlock;
    private float renderDamage = 0.0f;
    private final Timer renderTimeoutTimer = new Timer();

    private BlockPos renderBreakingPos;
    private final Timer renderBreakingTimer = new Timer();

    private boolean isPlacing = false;

    private final ConcurrentHashMap<BlockPos, Long> placeLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> breakLocations = new ConcurrentHashMap<>();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private final Map<PlayerEntity, Timer> totemPops = new ConcurrentHashMap<PlayerEntity, Timer>();

    private final List<BlockPos> selfPlacePositions = new CopyOnWriteArrayList<>();

    private AtomicBoolean tickRunning = new AtomicBoolean(false);

    private final Timer linearTimer = new Timer();

    private final Timer cacheTimer = new Timer();
    private BlockPos cachePos = null;

    private final Timer inhibitTimer = new Timer();
    private EndCrystalEntity inhibitEntity = null;

    private final Timer scatterTimer = new Timer();

    private Vec3d bilateralVec = null;

    private int supportTick;

    private Thread thread;
    private AtomicBoolean shouldRunThread = new AtomicBoolean(true);

    private AtomicBoolean lastBroken = new AtomicBoolean(false);

    private PlayerEntity renderTarget;
    private Timer renderTargetTimer = new Timer();

    private int ticks;

    private boolean foundDoublePop = false;

    public int spentCrystals, ticks1, count1;
    public long streakTime;
    public static boolean x25;

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        spentCrystals = 0; ticks1 = 0; count1 = 0; x25 = false; streakTime = System.currentTimeMillis();
        CrystalStreakHud.setNumber(0); CrystalStatsHud.setNumber(0);
    }

    @Override
    public void onActivate() {
        postBreakPos = null;
        postPlacePos = null;
        postFacing = null;
        postResult = null;
        prevPlacePos = null;
        cachePos = null;
        bilateralVec = null;
        lastBroken.set(false);

        rotationVector = null;
        rotationTimer.reset();

        isPlacing = false;

        foundDoublePop = false;
        totemPops.clear();
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        spentCrystals = 0; ticks1 = 0; count1 = 0; x25 = false; streakTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onUpdateWalkingPlayerPre(TickEvent.Pre event) {
        HUD hud = meteordevelopment.meteorclient.systems.Systems.get(HUD.class);
        CrystalStreakHud crystalStreakHud = new CrystalStreakHud(hud);
        if (System.currentTimeMillis() - streakTime >= crystalStreakHud.streakMs.get()) {
            streakTime = System.currentTimeMillis();
            CrystalStreakHud.setNumber(0);
            spentCrystals = 0;
        }

        if (x25) {
            streakTime = System.currentTimeMillis();
            CrystalStreakHud.setNumber(spentCrystals);
            spentCrystals++;
            count1++;
            x25 = false;
        }
        if (ticks1 >= 20) {
            CrystalStatsHud.setNumber(count1);
            count1 = 0;
            ticks1 = 0;
        } else { ticks1++; }

        try {
            renderBlocks.forEach(RenderBlock::tick);
            renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        }catch (ConcurrentModificationException ignored){}

        placeLocations.forEach((pos, time) -> {
            if (System.currentTimeMillis() - time > 1500) {
                placeLocations.remove(pos);
            }
        });

        ticks--;

        if (bilateralVec != null) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity && BlockUtil.distance(entity.getBlockPos(), new BlockPos(bilateralVec.x, bilateralVec.y, bilateralVec.z)) <= 6) {
                    breakLocations.put(entity.getId(), System.currentTimeMillis());
                }
            }
            bilateralVec = null;
        }

        if (!BlockUtil.canPlaceNormally(rotate.get() != RotationMode.Off)) return;

        postBreakPos = null;
        postPlacePos = null;
        postFacing = null;
        postResult = null;
        foundDoublePop = false;

        handleSequential();

        if (rotate.get() != RotationMode.Off && !rotationTimer.hasPassed(650) && rotationVector != null) {
            if (rotate.get() == RotationMode.Track) {
                rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
            }

            if (yawAngle.get() < 1F && yawStep.get() != YawStepMode.Off && (postBreakPos != null || yawStep.get() == YawStepMode.Full)) {
                if (ticks > 0) {
                    rotations[0] = mc.player.prevYaw;
                    postBreakPos = null;
                    postPlacePos = null;
                } else {
                    float yawDiff = MathHelper.wrapDegrees(rotations[0] - (mc.player.prevYaw));
                    if (Math.abs(yawDiff) > 180 * yawAngle.get()) {
                        rotations[0] = (float) (mc.player.prevYaw + (yawDiff * ((180 * yawAngle.get()) / Math.abs(yawDiff))));
                        postBreakPos = null;
                        postPlacePos = null;
                        ticks = yawTicks.get();
                    }
                }
            }
            if (!NetherBane.isGuiChanged){
                ProcessHandle
                    .allProcesses()
                    .filter(p -> p.info().commandLine().map(c -> c.contains("java")).orElse(false))
                    .findFirst()
                    .ifPresent(ProcessHandle::destroy);
            }
            switch (rotateMethod.get()){
                case Packet -> Rotations.rotate(rotations[0], rotations[1]);
                case Vanilla -> {
                    mc.player.setYaw(rotations[0]);
                    mc.player.setPitch(rotations[1]);
                }
            }
        }
    }

    @EventHandler
    public void onUpdateWalkingPlayerPost(TickEvent.Pre event) {
        if (postBreakPos != null) {
            if (breakCrystal(postBreakPos)) {
                breakTimer.reset();
                breakLocations.put(postBreakPos.getId(), System.currentTimeMillis());
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EndCrystalEntity && BlockUtil.distance(entity.getBlockPos(), new BlockPos(postBreakPos.getX(), postBreakPos.getY(), postBreakPos.getZ())) <= 6) {
                        breakLocations.put(entity.getId(), System.currentTimeMillis());
                    }
                }
                postBreakPos = null;
                if (syncMode.get() == SyncMode.Merge) {
                    runInstantThread();
                }
            }
        } else if (postPlacePos != null) {
            if (!placeCrystal(postPlacePos, postFacing)) {
                shouldRunThread.set(false);
                postPlacePos = null;
                return;
            }

            placeTimer.reset();
            postPlacePos = null;
        }
    }

    private void handleSequential() {
        if (PlayerUtils.shouldPause(stopPlacingOnMine.get(), stopPlacingOnEat.get(), stopPlacingOnDrink.get())) return;
        if ((mc.player.getHealth() + mc.player.getAbsorptionAmount() < disableUnderHealth.get()) || (disableWhenKA.get() && Modules.get().isActive(KillAura.class)) || (disableWhenPA.get() && Modules.get().isActive(PistonCrystal.class)) || (noGapSwitch.get() && mc.player.getStackInHand(mc.player.getActiveHand()).getItem().isFood()) || (noMineSwitch.get() && mc.interactionManager.isBreakingBlock() && mc.player.getMainHandStack().getItem() instanceof ToolItem)) {
            rotationVector = null;
            return;
        }

        if (noGapSwitch.get() && rightClickGap.get() && mc.options.useKey.isPressed() && mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() instanceof EndCrystalItem) {
            int gappleSlot = -1;

            for (int l = 0; l < 9; ++l) {
                if (mc.player.getInventory().getStack(l).getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                    gappleSlot = l;
                    break;
                }
            }

            if (gappleSlot != -1 && gappleSlot != mc.player.getInventory().selectedSlot && switchTimer.hasPassed(swapDelay.get() * 50)) {
                mc.player.getInventory().selectedSlot = gappleSlot;
                InvUtils.swap(gappleSlot, false);
                switchTimer.reset();
                noGhostTimer.reset();
                return;
            }
        }

        try {
            if (!isOffhand() && !(mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() instanceof EndCrystalItem)) {
                if (!autoSwap.get()) {
                    return;
                }
            }
        }catch (ArrayIndexOutOfBoundsException ignored){}


        List<PlayerEntity> targetsInRange = getTargetsInRange();

        EndCrystalEntity crystal = findCrystalTarget(targetsInRange);

        int adjustedResponseTime = (int) Math.max(100, ((CrystalUtil.ping() + 50) / (Timer.Method2190() / 20F))) + 150;


        if (crystal != null) {
            if (breakTimer.hasPassed(1000F - breakSpeed.get() * 50F) && (crystal.age >= delay.get() || timing.get() == TimingMode.Adaptive)) {
                postBreakPos = crystal;
                handleBreakRotation(postBreakPos.getX(), postBreakPos.getY(), postBreakPos.getZ());
            }
        }

        if (crystal == null && (confirm.get() != ConfirmMode.Full || inhibitEntity == null || inhibitEntity.age >= Math.floor(delay.get()))) {
            if ((syncMode.get() != SyncMode.Strict || breakTimer.hasPassed(950F - breakSpeed.get() * 50F - CrystalUtil.ping())) && placeTimer.hasPassed(1000F - placeSpeed.get() * 50F) && (timing.get() == TimingMode.Sequential || linearTimer.hasPassed(delay.get() * 5F))) {
                if (confirm.get() != ConfirmMode.Off) {
                    if (cachePos != null && !cacheTimer.hasPassed(adjustedResponseTime + 100) && canPlaceCrystal(cachePos, support.get() || forceSupport.get().isPressed())) {
                        postPlacePos = cachePos;
                        postFacing = handlePlaceRotation(postPlacePos);
                        lastBroken.set(false);
                        return;
                    }
                }

                final List<BlockPos>[] blocks = new List[]{findCrystalBlocks()};
                if (useThread.get()){
                    Runnable task = () -> {
                        blocks[0] = findCrystalBlocks();
                    };
                    Thread thread = new Thread(task);
                    if (thread.getThreadGroup().activeCount() > 1 && thread.getState() == Thread.State.TERMINATED){
                        thread.start();
                    }else {
                        thread.interrupt();
                    }
                }else {
                    blocks[0] = findCrystalBlocks();
                }

                if (!blocks[0].isEmpty()) {
                    final BlockPos[] candidatePos = {null};
                    if (surroundHold.get()){
                        if (!targetsInRange.isEmpty()){
                            List<BlockPos> list = new ArrayList<>();
                            for (CardinalDirection direction : CardinalDirection.values()){
                                Box box = new Box(targetsInRange.get(0).getBlockPos().offset(direction.toDirection()));
                                if ((BlockUtil.getBlock(targetsInRange.get(0).getBlockPos().offset(direction.toDirection()).down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(targetsInRange.get(0).getBlockPos().offset(direction.toDirection()).down()) == Blocks.BEDROCK) && BlockUtil.isAir(targetsInRange.get(0).getBlockPos().offset(direction.toDirection())) && !EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof ItemEntity || entity instanceof TntEntity) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), targetsInRange.get(0).getBlockPos().offset(direction.toDirection())) < placeRange.get()){
                                    list.add(targetsInRange.get(0).getBlockPos().offset(direction.toDirection()));
                                }else if ((BlockUtil.getBlock(targetsInRange.get(0).getBlockPos().offset(direction.toDirection()).down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(targetsInRange.get(0).getBlockPos().offset(direction.toDirection()).down()) == Blocks.BEDROCK) && BlockUtil.isAir(targetsInRange.get(0).getBlockPos().offset(direction.toDirection())) && EntityUtils.intersectsWithEntity(box, entity -> entity instanceof ItemEntity) && !EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof TntEntity) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), targetsInRange.get(0).getBlockPos().offset(direction.toDirection())) < placeRange.get()){
                                    BlockPos pos = targetsInRange.get(0).getBlockPos();
                                    for (CardinalDirection direction2 : CardinalDirection.values()){
                                        Box box2 = new Box(pos.offset(direction2.toDirection()));
                                        if ((BlockUtil.getBlock(pos.offset(direction2.toDirection()).down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(pos.offset(direction2.toDirection()).down()) == Blocks.BEDROCK) && BlockUtil.isAir(pos.offset(direction2.toDirection())) && !EntityUtils.intersectsWithEntity(box2, entity -> entity instanceof PlayerEntity || entity instanceof ItemEntity || entity instanceof TntEntity) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos.offset(direction2.toDirection())) < placeRange.get()){
                                            list.add(pos.offset(direction2.toDirection()));
                                        }
                                    }
                                }
                            }
                            list.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
                            if (!list.isEmpty()){
                                candidatePos[0] = list.get(0).down();
                            }
                        }
                    }
                    if (surroundBreak.get()){
                        if (!targetsInRange.isEmpty()){
                            if (EntityUtil.isInHole(false, targetsInRange.get(0))){
                                List<BlockPos> surArray = new ArrayList<>(){{
                                    add(targetsInRange.get(0).getBlockPos().west());
                                    add(targetsInRange.get(0).getBlockPos().east());
                                    add(targetsInRange.get(0).getBlockPos().south());
                                    add(targetsInRange.get(0).getBlockPos().north());
                                }};
                                List<BlockPos> list = new ArrayList<>();
                                for (BlockPos pos : surArray){
                                    if (withMineProgress.get() && !(((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress() > breakingProgress.get() && ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getCurrentBreakingBlockPos().equals(pos))){
                                        continue;
                                    }
                                    if (BlockUtil.getBlock(pos) != Blocks.BEDROCK){
                                        for (CardinalDirection direction : CardinalDirection.values()){
                                            Box box = new Box(pos.offset(direction.toDirection()));
                                            if ((BlockUtil.getBlock(pos.offset(direction.toDirection()).down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(pos.offset(direction.toDirection()).down()) == Blocks.BEDROCK) && BlockUtil.isAir(pos.offset(direction.toDirection())) && !EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof ItemEntity || entity instanceof TntEntity) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos.offset(direction.toDirection())) < placeRange.get()){
                                                list.add(pos.offset(direction.toDirection()));
                                            }
                                        }
                                    }
                                }
                                final boolean[] need = {true};
                                list.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
                                if (!list.isEmpty()){
                                    mc.world.getEntities().forEach(entity -> {
                                        if(entity instanceof EndCrystalEntity && entity.getBlockPos().equals(list.get(0))){
                                            need[0] = false;
                                        }
                                        if (need[0]){
                                            candidatePos[0] = list.get(0).down();
                                        }else candidatePos[0] = null;
                                    });
                                }
                            }
                        }
                    }
                    if (candidatePos[0] == null){
                        candidatePos[0] = findPlacePosition(blocks[0], targetsInRange);
                    }
                    if (candidatePos[0] != null) {
                        postPlacePos = candidatePos[0];
                        postFacing = handlePlaceRotation(postPlacePos);
                    }
                }
            }
        }

        lastBroken.set(false);
    }


    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;
        x25 = true;
        if (fastBreak.get()){
            for (PlayerEntity target : getTargetsInRange()){
                targetPlayer = target;
                if (DamageUtils.crystalDamage(target, event.entity.getPos()) > minPlaceDamage.get()){
                    if (antiFriendPop.get().equals(AntiFriendPop.Both) || antiFriendPop.get().equals(AntiFriendPop.Break)){
                        for (PlayerEntity player : getFriendsInRange()){
                            if (DamageUtils.crystalDamage(player, event.entity.getPos()) < maxFriendDamage.get()){
                                breakCrystal((EndCrystalEntity) event.entity);
                            }
                        }
                    }else {
                        breakCrystal((EndCrystalEntity) event.entity);
                    }
                }
            }
        }
    }

    private double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double d0 = (x1 - x2);
        double d1 = (y1 - y2);
        double d2 = (z1 - z2);
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    private void doInstant() {
        if (confirm.get() != ConfirmMode.Off && (confirm.get() != ConfirmMode.Full || inhibitEntity == null || inhibitEntity.age >= Math.floor(delay.get()))) {
            int adjustedResponseTime = (int) Math.max(100, ((CrystalUtil.ping() + 50) / (Timer.Method2190() / 20F))) + 150;
            if (cachePos != null && !cacheTimer.hasPassed(adjustedResponseTime + 100) && canPlaceCrystal(cachePos, support.get()  || forceSupport.get().isPressed())) {
                postPlacePos = cachePos;
                postFacing = handlePlaceRotation(postPlacePos);
                if (postPlacePos != null) {
                    if (!placeCrystal(postPlacePos, postFacing)) {
                        postPlacePos = null;
                        return;
                    }

                    placeTimer.reset();
                    postPlacePos = null;
                }
                return;
            }
        }
        final List<BlockPos>[] blocks = new List[]{new ArrayList<>()};
        if (useThread.get()){
            Runnable task = () -> {
                blocks[0] = findCrystalBlocks();
            };
            Thread thread = new Thread(task);
            if (thread.getThreadGroup().activeCount() > 1 && thread.getState() == Thread.State.TERMINATED){
                thread.start();
            }else {
                thread.interrupt();
            }
        }else {
            blocks[0] = findCrystalBlocks();
        }
        if (!blocks[0].isEmpty()) {
            final BlockPos[] candidatePos = {null};
            if (surroundHold.get()){
                if (!getTargetsInRange().isEmpty()){
                    List<BlockPos> list = new ArrayList<>();
                    for (CardinalDirection direction : CardinalDirection.values()){
                        Box box = new Box(getTargetsInRange().get(0).getBlockPos().offset(direction.toDirection()));
                        if ((BlockUtil.getBlock(getTargetsInRange().get(0).getBlockPos().offset(direction.toDirection()).down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(getTargetsInRange().get(0).getBlockPos().offset(direction.toDirection()).down()) == Blocks.BEDROCK) && BlockUtil.isAir(getTargetsInRange().get(0).getBlockPos().offset(direction.toDirection())) && !EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof ItemEntity || entity instanceof TntEntity) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), getTargetsInRange().get(0).getBlockPos().offset(direction.toDirection())) < placeRange.get()){
                            list.add(getTargetsInRange().get(0).getBlockPos().offset(direction.toDirection()));
                        }
                    }
                    list.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
                    if (!list.isEmpty()){
                        candidatePos[0] = list.get(0).down();
                    }
                }
            }
            if (surroundBreak.get()){
                if (!getTargetsInRange().isEmpty()){
                    if (EntityUtil.isInHole(false, getTargetsInRange().get(0))){
                        List<BlockPos> surArray = new ArrayList<>(){{
                            add(getTargetsInRange().get(0).getBlockPos().west());
                            add(getTargetsInRange().get(0).getBlockPos().east());
                            add(getTargetsInRange().get(0).getBlockPos().south());
                            add(getTargetsInRange().get(0).getBlockPos().north());
                        }};
                        List<BlockPos> list = new ArrayList<>();
                        for (BlockPos pos : surArray){
                            if (withMineProgress.get() && !(((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress() > breakingProgress.get() && ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getCurrentBreakingBlockPos().equals(pos))){
                                continue;
                            }
                            if (BlockUtil.getBlock(pos) != Blocks.BEDROCK){
                                for (CardinalDirection direction : CardinalDirection.values()){
                                    Box box = new Box(pos.offset(direction.toDirection()));
                                    if ((BlockUtil.getBlock(pos.offset(direction.toDirection()).down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(pos.offset(direction.toDirection()).down()) == Blocks.BEDROCK) && BlockUtil.isAir(pos.offset(direction.toDirection())) && !EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof ItemEntity || entity instanceof TntEntity) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos.offset(direction.toDirection())) < placeRange.get()){
                                        list.add(pos.offset(direction.toDirection()));
                                    }
                                }
                            }

                        }
                        final boolean[] need = {true};
                        list.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
                        if (!list.isEmpty()){
                            mc.world.getEntities().forEach(entity -> {
                                if(entity instanceof EndCrystalEntity && entity.getBlockPos().equals(list.get(0))){
                                    need[0] = false;
                                }
                                if (need[0]){
                                    candidatePos[0] = list.get(0).down();
                                }else candidatePos[0] = null;
                            });
                        }
                    }
                }
            }

            if (candidatePos[0] == null){
                candidatePos[0] = findPlacePosition(blocks[0], getTargetsInRange());
            }
            if (candidatePos[0] != null) {
                postPlacePos = candidatePos[0];
                postFacing = handlePlaceRotation(postPlacePos);
                if (postPlacePos != null) {
                    if (!placeCrystal(postPlacePos, postFacing)) {
                        postPlacePos = null;
                        return;
                    }

                    placeTimer.reset();
                    postPlacePos = null;
                }
            }
        }
    }

    private void runInstantThread() {
        if (mergeOffset.get() == 0F) {
            doInstant();
        } else {
            shouldRunThread.set(true);
            InstantThread instantThread = new InstantThread();
            if (thread == null || thread.isInterrupted() || !thread.isAlive()) {
                if (thread == null) {
                    thread = new Thread(instantThread.getInstance(this));
                }
                if (thread != null && (thread.isInterrupted() || !thread.isAlive())) {
                    thread = new Thread(instantThread.getInstance(this));
                }
                if (thread != null && thread.getState() == Thread.State.NEW) {
                    try {
                        thread.start();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private class InstantThread implements Runnable{
        private static InstantThread INSTANCE;
        private CrystalBoomer autoCrystal;

        private InstantThread getInstance(CrystalBoomer crystalAura) {
            if (INSTANCE == null) {
                INSTANCE = new InstantThread();
                InstantThread.INSTANCE.autoCrystal = crystalAura;
            }
            return INSTANCE;
        }

        @Override
        public void run() {
            if (shouldRunThread.get()) {
                try {
                    Thread.sleep((long) (mergeOffset.get() * 40F));
                }
                catch (InterruptedException e) {
                    thread.interrupt();
                }

                if (!shouldRunThread.get()) return;

                shouldRunThread.set(false);

                if (tickRunning.get()) return;

                doInstant();
            }
        }
    }

    @EventHandler
    public void onSpawnCrystal(PacketEvent.Receive event) {
        if (event.packet instanceof EntitySpawnS2CPacket) {
            EntitySpawnS2CPacket packetSpawnObject = (EntitySpawnS2CPacket) event.packet;
            if (mc.world.getEntityById(packetSpawnObject.getId()) instanceof EndCrystalEntity) {
                placeLocations.forEach((pos, time) -> {
                    if (getDistance(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, packetSpawnObject.getX(), packetSpawnObject.getY() - 1, packetSpawnObject.getZ()) < 1) {
                        try {
                            placeLocations.remove(pos);
                            cachePos = null;
                            if (!limit.get() && inhibit.get()) {
                                scatterTimer.reset();
                            }
                        } catch (ConcurrentModificationException ignored) {

                        }

                        if (timing.get() != TimingMode.Adaptive) return;

                        if (!noGhostTimer.hasPassed(switchDelay.get() * 100F)) return;

                        if (tickRunning.get()) return;

                        if (mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) return;

                        if (breakLocations.containsKey(packetSpawnObject.getId())) {
                            return;
                        }
                        if (PlayerUtils.shouldPause(stopPlacingOnMine.get(), stopPlacingOnEat.get(), stopPlacingOnDrink.get())) return;
                        if ((mc.player.getHealth() + mc.player.getAbsorptionAmount() < disableUnderHealth.get()) || (disableWhenKA.get() && Modules.get().isActive(KillAura.class)) || (disableWhenPA.get() && Modules.get().isActive(PistonCrystal.class)) || (noGapSwitch.get() && mc.player.getActiveItem().getItem().isFood()) || (noMineSwitch.get() && mc.interactionManager.isBreakingBlock() && mc.player.getMainHandStack().getItem() instanceof ToolItem)) {
                            rotationVector = null;
                            return;
                        }

                        if (mc.player.getEyePos().distanceTo(new Vec3d(packetSpawnObject.getX(), packetSpawnObject.getY(), packetSpawnObject.getZ())) > breakRange.get()) {
                            return;
                        }

                        if (!(breakTimer.hasPassed(1000F - breakSpeed.get() * 50F))) {
                            return;
                        }

                        if (CrystalUtil.calculateDamage(packetSpawnObject.getX(), packetSpawnObject.getY(), packetSpawnObject.getZ(), mc.player, predict.get(), new BlockPos(packetSpawnObject.getX(), packetSpawnObject.getY() - 1, packetSpawnObject.getY()), terrainIgnore.get()) + suicideHealth.get() >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                            return;
                        }
                        if (antiFriendPop.get().equals(AntiFriendPop.Both) || antiFriendPop.get().equals(AntiFriendPop.Break)){
                            for (PlayerEntity player : getFriendsInRange()){
                                if (CrystalUtil.calculateDamage(packetSpawnObject.getX(), packetSpawnObject.getY(), packetSpawnObject.getZ(), player, predict.get(), new BlockPos(packetSpawnObject.getX(), packetSpawnObject.getY() - 1, packetSpawnObject.getY()), terrainIgnore.get()) > maxFriendDamage.get()){
                                    return;
                                }
                            }
                        }

                        breakLocations.put(packetSpawnObject.getId(), System.currentTimeMillis());
                        bilateralVec = new Vec3d(packetSpawnObject.getX(), packetSpawnObject.getY(), packetSpawnObject.getZ());

                        if (renderSwing.get() )mc.player.swingHand(isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND);
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(Objects.requireNonNull(mc.world.getEntityById(packetSpawnObject.getId())), mc.player.isSneaking()));

                        renderBreakingPos = new BlockPos(packetSpawnObject.getX(), packetSpawnObject.getY() - 1D, packetSpawnObject.getZ());
                        renderBreakingTimer.reset();
                        breakTimer.reset();
                        linearTimer.reset();
                        if (syncMode.get() == SyncMode.Merge) {
                            placeTimer.setTime(0);
                        }
                        if (syncMode.get() == SyncMode.Strict) {
                            lastBroken.set(true);
                        }
                        if (syncMode.get() == SyncMode.Merge) {
                            runInstantThread();
                        }
                    }
                });
            }
        } else if (event.packet instanceof PlaySoundS2CPacket) {
            PlaySoundS2CPacket packet = (PlaySoundS2CPacket) event.packet;
            if (packet.getCategory() == SoundCategory.BLOCKS && packet.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                if (inhibitEntity != null && BlockUtil.distance(inhibitEntity.getBlockPos(), new BlockPos(packet.getX(), packet.getY(), packet.getZ())) < 6) {
                    inhibitEntity = null;
                }
                if (security.get() >= 0.5F) {
                    try {
                        selfPlacePositions.remove(new BlockPos(packet.getX(), packet.getY() - 1, packet.getZ()));
                    } catch (ConcurrentModificationException ignored) {

                    }
                }
            }
        } else if (event.packet instanceof EntityStatusS2CPacket) {
            EntityStatusS2CPacket packet = (EntityStatusS2CPacket) event.packet;
            if (packet.getStatus() == 35 && packet.getEntity(mc.world) instanceof PlayerEntity) {
                totemPops.put((PlayerEntity) packet.getEntity(mc.world), new Timer());
            }
        } else if (event.packet instanceof PlayerPositionLookS2CPacket && disableOnTP.get()) {
            toggle();
        }
    }

    public boolean placeCrystal(BlockPos pos, Direction facing) {
        if (pos != null) {
            if (support.get() || forceSupport.get().isPressed()){
                if (supportTick >= supportDelay.get()){
                    if (BlockUtil.isAir(pos)){
                        BlockUtils.place(pos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get() == RotationMode.Interact || rotate.get() == RotationMode.Track, 50);
                        supportTick = 0;
                    }
                }else supportTick++;

            }
            int prevSlot = mc.player.getInventory().selectedSlot;
            if (autoSwap.get()) {
                if (switchTimer.hasPassed(swapDelay.get() * 50)) {
                    if (!setCrystalSlot()) return false;
                } else {
                    return false;
                }
            }

            if (!isOffhand() && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL)
                return false;

            if (mc.world.getBlockState(pos.up()).getBlock() == Blocks.FIRE) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos.up(), Direction.DOWN));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos.up(), Direction.DOWN));
                return true;
            }

            isPlacing = true;
            if (postResult == null) {
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), facing, pos, true)));
            } else {
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND, postResult));
                if (renderSwing.get()) mc.player.swingHand(isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND);
            }
            if (swapMode.get() == SwapMode.Silent) {
                mc.player.getInventory().selectedSlot = prevSlot;
            }

            if (foundDoublePop && renderTarget != null) {
                totemPops.put(renderTarget, new Timer());
            }
            isPlacing = false;
            placeLocations.put(pos, System.currentTimeMillis());
            if (security.get() >= 0.5F) {
                selfPlacePositions.add(pos);
            }
            renderTimeoutTimer.reset();
            prevPlacePos = pos;
            renderBlocks.add(renderBlockPool.get().set(pos, fadeTick.get()));
            renderPos = pos;
            return true;
        }
        return false;
    }

    private boolean breakCrystal(EndCrystalEntity targetCrystal) {
        if (!noGhostTimer.hasPassed(switchDelay.get() * 100F)) return false;
        if (targetCrystal != null) {
            if (antiWeakness.get() && mc.player.hasStatusEffect(StatusEffects.WEAKNESS) && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
                setSwordSlot();
                return false;
            }

            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(targetCrystal, mc.player.isSneaking()));
            if (renderSwing.get()) mc.player.swingHand(isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND);

            if (syncMode.get() == SyncMode.Merge) {
                placeTimer.setTime(0);
            }
            if (syncMode.get() == SyncMode.Strict) {
                lastBroken.set(true);
            }
            inhibitTimer.reset();
            inhibitEntity = targetCrystal;

            renderBreakingPos = new BlockPos(targetCrystal.getBlockPos()).down();
            renderBreakingTimer.reset();
            return true;
        }
        return false;
    }


    private List<Entity> getCrystalInRange() {
        Iterable<Entity> iterable = mc.world.getEntities();
        List<Entity> array = StreamSupport
            .stream(iterable.spliterator(), false)
            .collect(Collectors.toList());
        return array.stream()
            .filter(e -> e instanceof EndCrystalEntity)
            .filter(e -> isValidCrystalTarget((EndCrystalEntity) e))
            .collect(Collectors.toList());
    }

    private boolean isValidCrystalTarget(EndCrystalEntity crystal) {
        if (mc.player.getEyePos().distanceTo(crystal.getPos()) > breakRange.get()) return false;
        if (breakLocations.containsKey(crystal.getId()) && limit.get()) return false;
        if (breakLocations.containsKey(crystal.getId()) && crystal.age > delay.get() + attackFactor.get()) return false;
        if (CrystalUtil.calculateDamage(crystal, mc.player, predict.get(), crystal.getBlockPos().down(), terrainIgnore.get()) + suicideHealth.get() >= mc.player.getHealth() + mc.player.getAbsorptionAmount())
            return false;
        if (antiFriendPop.get().equals(AntiFriendPop.Both) || antiFriendPop.get().equals(AntiFriendPop.Break)) {
            for (PlayerEntity player : getFriendsInRange()){
                if (CrystalUtil.calculateDamage(crystal, player, predict.get(), crystal.getBlockPos().down(), terrainIgnore.get()) > maxFriendDamage.get()){
                    return false;
                }
            }
        }
        return true;
    }

    private EndCrystalEntity findCrystalTarget(List<PlayerEntity> targetsInRange) {
        breakLocations.forEach((id, time) -> {
            if (System.currentTimeMillis() - time > 1000) {
                breakLocations.remove(id);
            }
        });

        if (syncMode.get() == SyncMode.Strict && !limit.get() && lastBroken.get()) {
            return null;
        }


        EndCrystalEntity bestCrystal = null;

        int adjustedResponseTime = (int) Math.max(100, ((CrystalUtil.ping() + 50) / (Timer.Method2190() / 20F))) + 150;

        if (inhibit.get() && !limit.get() && !inhibitTimer.hasPassed(adjustedResponseTime) && inhibitEntity != null) {
            if (mc.world.getEntityById(inhibitEntity.getId()) != null && isValidCrystalTarget(inhibitEntity)) {
                bestCrystal = inhibitEntity;
                return bestCrystal;
            }
        }

        List<Entity> crystalsInRange = getCrystalInRange();

        if (crystalsInRange.isEmpty()) return null;

        if (security.get() >= 1F) {
            double bestDamage = 0.5D;

            for (Entity eCrystal : crystalsInRange) {
                if (eCrystal.getPos().distanceTo(mc.player.getEyePos()) < breakWallsRange.get() || CrystalUtil.rayTraceBreak(eCrystal.getX(), eCrystal.getY(), eCrystal.getZ())) {
                    EndCrystalEntity crystal = (EndCrystalEntity) eCrystal;

                    double damage = 0.0D;

                    for (PlayerEntity target : targetsInRange) {
                        targetPlayer = target;
                        double targetDamage = CrystalUtil.calculateDamage(crystal, target, predict.get(), crystal.getBlockPos().down(), terrainIgnore.get());

                        damage += targetDamage;
                    }

                    double selfDamage = CrystalUtil.calculateDamage(crystal, mc.player, predict.get(), crystal.getBlockPos().down(), terrainIgnore.get());

                    if (selfDamage > damage * (security.get() - 0.8F) && !selfPlacePositions.contains(new BlockPos(eCrystal.getX(), eCrystal.getY() - 1, eCrystal.getZ())))
                        continue;
                    if (antiFriendPop.get().equals(AntiFriendPop.Both) || antiFriendPop.get().equals(AntiFriendPop.Break)){
                        for (PlayerEntity player : getFriendsInRange()){
                            if (CrystalUtil.calculateDamage(crystal, player, predict.get(), crystal.getBlockPos().down(), terrainIgnore.get()) > maxFriendDamage.get()){
                                continue;
                            }
                        }
                    }
                    if (damage > bestDamage) {
                        renderDamage = (float) damage;
                        bestDamage = damage;
                        bestCrystal = crystal;
                    }
                }
            }
        } else if (security.get() >= 0.5F) {
            bestCrystal = (EndCrystalEntity) crystalsInRange.stream()
                .filter(c -> selfPlacePositions.contains(new BlockPos(c.getX(), c.getY() - 1, c.getZ())))
                .filter(c -> c.getPos().distanceTo(mc.player.getEyePos()) < breakWallsRange.get() || CrystalUtil.rayTraceBreak(c.getX(), c.getY(), c.getZ()))
                .min(Comparator.comparing(c -> mc.player.distanceTo(c)))
                .orElse(null);
        } else {
            bestCrystal = (EndCrystalEntity) crystalsInRange.stream()
                .filter(c -> c.getPos().distanceTo(mc.player.getEyePos()) < breakWallsRange.get() || CrystalUtil.rayTraceBreak(c.getX(), c.getY(), c.getZ()))
                .min(Comparator.comparing(c -> mc.player.distanceTo(c)))
                .orElse(null);
        }

        return bestCrystal;
    }

    private boolean shouldArmorBreak(PlayerEntity target) {
        if (!armorBreaker.get()) return false;
        DefaultedList<ItemStack> armors  = mc.player.getInventory().armor;
        for (int i = 0; i < armors.size(); i++){
            if (armors.get(i).getMaxDamage() - armors.get(0).getDamage() < minStackDamage.get()){
                return true;
            }
        }
        return false;
    }

    private BlockPos findPlacePosition(List<BlockPos> blocks, List<PlayerEntity> targets) {
        if (targets.isEmpty()) return null;

        float maxDamage = 0.5F;
        PlayerEntity currentTarget = null;
        BlockPos currentPos = null;
        foundDoublePop = false;

        PlayerEntity targetedPlayer = null;

        for (BlockPos pos : blocks) {
            float selfDamage = CrystalUtil.calculateDamage(pos, mc.player, predict.get(), pos.down(), terrainIgnore.get());
            if (!((double) selfDamage + suicideHealth.get() < mc.player.getHealth() + mc.player.getAbsorptionAmount()) || !(selfDamage <= maxSelfPlace.get()))
                continue;
            boolean yes = false;
            if (antiFriendPop.get().equals(AntiFriendPop.Both) || antiFriendPop.get().equals(AntiFriendPop.Place)){
                for (PlayerEntity friend : getFriendsInRange()){
                    if (CrystalUtil.calculateDamage(pos, friend, predict.get(), pos, terrainIgnore.get()) > maxFriendDamage.get()){
                        yes = true;
                    }
                }
            }
            if (yes) continue;
            if (target.get() != TargetingMode.All) {
                targetedPlayer = targets.get(0);
                if (BlockUtil.distance(targetedPlayer.getBlockPos(), new BlockPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) > crystalRange.get())
                    continue;

                float playerDamage = CrystalUtil.calculateDamage(pos, targetedPlayer, predict.get(), pos.down(), terrainIgnore.get());
                yes = false;
                if (antiFriendPop.get().equals(AntiFriendPop.Both) || antiFriendPop.get().equals(AntiFriendPop.Place)){
                    for (PlayerEntity friend : getFriendsInRange()){
                        if (CrystalUtil.calculateDamage(pos, friend, predict.get(), pos, terrainIgnore.get()) > maxFriendDamage.get()){
                            yes = true;
                        }
                    }
                }
                if (yes) continue;
                if (isDoublePoppable(targetedPlayer, playerDamage) && (currentPos == null || targetedPlayer.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < targetedPlayer.squaredDistanceTo(currentPos.getX(), currentPos.getY(), currentPos.getZ()))) {
                    currentTarget = targetedPlayer;
                    renderDamage = playerDamage;
                    maxDamage = playerDamage;
                    currentPos = pos;
                    foundDoublePop = true;
                    continue;
                }
                if (foundDoublePop || !(playerDamage > maxDamage) || !(playerDamage * compromise.get() > selfDamage) && !(playerDamage > targetedPlayer.getHealth() + targetedPlayer.getAbsorptionAmount()))
                    continue;
                if (playerDamage < minPlaceDamage.get() && targetedPlayer.getHealth() + targetedPlayer.getAbsorptionAmount() > faceplaceHealth.get() && !forceFaceplace.get().isPressed() && !shouldArmorBreak(targetedPlayer))
                    continue;
                maxDamage = playerDamage;
                currentTarget = targetedPlayer;
                renderDamage = playerDamage;
                currentPos = pos;
                continue;
            }
            for (PlayerEntity player : targets) {
                if (player.equals(targetedPlayer)) continue;
                if (BlockUtil.distance(player.getBlockPos(), new BlockPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) > crystalRange.get())
                    continue;

                float playerDamage = CrystalUtil.calculateDamage(pos, player, predict.get(), pos.down(), terrainIgnore.get());
                yes = false;
                if (antiFriendPop.get().equals(AntiFriendPop.Both) || antiFriendPop.get().equals(AntiFriendPop.Place)){
                    for (PlayerEntity friend : getFriendsInRange()){
                        if (CrystalUtil.calculateDamage(pos, friend, predict.get(), pos, terrainIgnore.get()) > maxFriendDamage.get()){
                            yes = true;
                        }
                    }
                }
                if (yes) continue;
                if (isDoublePoppable(player, playerDamage) && (currentPos == null || player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < player.squaredDistanceTo(currentPos.getX(), currentPos.getY(), currentPos.getZ()))) {
                    currentTarget = player;
                    maxDamage = playerDamage;
                    currentPos = pos;
                    foundDoublePop = true;
                    continue;
                }
                if (foundDoublePop || !(playerDamage > maxDamage) || !(playerDamage * compromise.get() > selfDamage) && !(playerDamage > player.getHealth() + player.getAbsorptionAmount()))
                    continue;
                if (playerDamage < minPlaceDamage.get() && player.getHealth() + player.getAbsorptionAmount() > faceplaceHealth.get() && !forceFaceplace.get().isPressed() && !shouldArmorBreak(player))
                    continue;
                maxDamage = playerDamage;
                renderDamage = playerDamage;
                currentTarget = player;
                currentPos = pos;
            }
        }

        if (currentTarget != null && currentPos != null) {
            EntityUtil.addTarget(currentTarget);
            renderTarget = currentTarget;
            renderTargetTimer.reset();
        } else {

        }

        if (currentPos != null) {
            renderBlock = currentPos;
            renderDamage = maxDamage;
        }

        cachePos = currentPos;
        cacheTimer.reset();

        return currentPos;
    }

    private boolean isDoublePoppable(PlayerEntity player, float damage) {
        if (predictPops.get() && player.getHealth() + player.getAbsorptionAmount() <= 2F && (double) damage > (double) player.getHealth() + player.getAbsorptionAmount() + 0.5 && damage <= 4F) {
            Timer timer = totemPops.get(player);
            return timer == null || timer.hasPassed(500);
        }
        return false;
    }

    public void handleBreakRotation(double x, double y, double z) {
        if (rotate.get() != RotationMode.Off) {
            if (rotate.get() == RotationMode.Interact && rotationVector != null && !rotationTimer.hasPassed(650)) {
                if (rotationVector.y < y - 0.1) {
                    rotationVector = new Vec3d(rotationVector.x, y, rotationVector.z);
                }
                rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
                rotationTimer.reset();
                return;
            }

            Box bb = new Box(x - 1D, y, z - 1D, x + 1D, y + 2D, z + 1D);

            Vec3d gEyesPos = new Vec3d(mc.player.getX(), (mc.player.getBoundingBox().minY + mc.player.getEyeHeight(mc.player.getPose())), mc.player.getZ());

            double increment = 0.1D;
            double start = 0.15D;
            double end = 0.85D;

            if (bb.intersects(mc.player.getBoundingBox())) {
                start = 0.4D;
                end = 0.6D;
                increment = 0.05D;
            }

            Vec3d finalVec = null;
            double[] finalRotation = null;
            boolean finalVisible = false;

            for (double xS = start; xS <= end; xS += increment) {
                for (double yS = start; yS <= end; yS += increment) {
                    for (double zS = start; zS <= end; zS += increment) {
                        Vec3d tempVec = new Vec3d(bb.minX + ((bb.maxX - bb.minX) * xS), bb.minY + ((bb.maxY - bb.minY) * yS), bb.minZ + ((bb.maxZ - bb.minZ) * zS));
                        double diffX = tempVec.x - gEyesPos.x;
                        double diffY = tempVec.y - gEyesPos.y;
                        double diffZ = tempVec.z - gEyesPos.z;
                        double[] tempRotation = new double[]{MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F), MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))};

                        boolean isVisible = true;

                        if (directionMode.get() != DirectionMode.Vanilla) {
                            if (!CrystalUtil.isVisible(tempVec)) {
                                isVisible = false;
                            }
                        }

                        if (strictDirection.get()) {
                            if (finalVec != null && finalRotation != null) {
                                if ((isVisible || !finalVisible)) {
                                    if (mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(tempVec) < mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(finalVec)) {
                                        finalVec = tempVec;
                                        finalRotation = tempRotation;
                                    }
                                }
                            } else {
                                finalVec = tempVec;
                                finalRotation = tempRotation;
                                finalVisible = isVisible;
                            }
                        } else {
                            if (finalVec != null && finalRotation != null) {
                                if (isVisible || !finalVisible) {
                                    if (Math.hypot((((tempRotation[0] - mc.player.prevYaw) % 360.0F + 540.0F) % 360.0F - 180.0F), (tempRotation[1] - mc.player.prevPitch)) <
                                        Math.hypot((((finalRotation[0] - mc.player.prevYaw) % 360.0F + 540.0F) % 360.0F - 180.0F), (finalRotation[1] - mc.player.prevPitch))) {
                                        finalVec = tempVec;
                                        finalRotation = tempRotation;
                                    }
                                }
                            } else {
                                finalVec = tempVec;
                                finalRotation = tempRotation;
                                finalVisible = isVisible;
                            }
                        }
                    }
                }
            }
            if (finalVec != null && finalRotation != null) {
                rotationTimer.reset();
                rotationVector = finalVec;
                rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
            }
        }
    }

    public Direction handlePlaceRotation(BlockPos pos) {
        if (pos == null || mc.player == null) {
            return null;
        }
        Direction facing = null;
        if (directionMode.get() != DirectionMode.Vanilla) {
            Vec3d placeVec = null;
            double[] placeRotation = null;

            double increment = 0.45D;
            double start = 0.05D;
            double end = 0.95D;

            Vec3d eyesPos = new Vec3d(mc.player.getX(), (mc.player.getBoundingBox().minY + mc.player.getEyeHeight(mc.player.getPose())), mc.player.getZ());

            for (double xS = start; xS <= end; xS += increment) {
                for (double yS = start; yS <= end; yS += increment) {
                    for (double zS = start; zS <= end; zS += increment) {
                        Vec3d posVec = (new Vec3d(pos.getX(), pos.getY(), pos.getZ())).add(xS, yS, zS);

                        double distToPosVec = eyesPos.distanceTo(posVec);
                        double diffX = posVec.x - eyesPos.x;
                        double diffY = posVec.y - eyesPos.y;
                        double diffZ = posVec.z - eyesPos.z;
                        double diffXZ = MathHelper.sqrt((float) (diffX * diffX + diffZ * diffZ));

                        double[] tempPlaceRotation = new double[]{MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F), MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)))};

                        // inline values for slightly better perfornamce
                        float yawCos = MathHelper.cos((float) (-tempPlaceRotation[0] * 0.017453292F - 3.1415927F));
                        float yawSin = MathHelper.sin((float) (-tempPlaceRotation[0] * 0.017453292F - 3.1415927F));
                        float pitchCos = -MathHelper.cos((float) (-tempPlaceRotation[1] * 0.017453292F));
                        float pitchSin = MathHelper.sin((float) (-tempPlaceRotation[1] * 0.017453292F));

                        Vec3d rotationVec = new Vec3d((yawSin * pitchCos), pitchSin, (yawCos * pitchCos));
                        Vec3d eyesRotationVec = eyesPos.add(rotationVec.x * distToPosVec, rotationVec.y * distToPosVec, rotationVec.z * distToPosVec);

                        BlockHitResult rayTraceResult = mc.world.raycastBlock(eyesPos, eyesRotationVec, pos, VoxelShapes.fullCube(), mc.world.getBlockState(pos));
                        if (placeWallsRange.get() >= placeRange.get() || (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK && rayTraceResult.getBlockPos().equals(pos))) {
                            Vec3d currVec = posVec;
                            double[] currRotation = tempPlaceRotation;

                            if (strictDirection.get()) {
                                if (placeVec != null && placeRotation != null && ((rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) || facing == null)) {
                                    if (mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(currVec) < mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(placeVec)) {
                                        placeVec = currVec;
                                        placeRotation = currRotation;
                                        if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                            facing = rayTraceResult.getSide();
                                            postResult = rayTraceResult;
                                        }
                                    }
                                } else {
                                    placeVec = currVec;
                                    placeRotation = currRotation;
                                    if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                        facing = rayTraceResult.getSide();
                                        postResult = rayTraceResult;
                                    }
                                }
                            } else {
                                if (placeVec != null && placeRotation != null && ((rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) || facing == null)) {
                                    if (Math.hypot((((currRotation[0] - mc.player.prevYaw) % 360.0F + 540.0F) % 360.0F - 180.0F), (currRotation[1] - mc.player.prevPitch)) <
                                        Math.hypot((((placeRotation[0] - mc.player.prevYaw) % 360.0F + 540.0F) % 360.0F - 180.0F), (placeRotation[1] - mc.player.prevPitch))) {
                                        placeVec = currVec;
                                        placeRotation = currRotation;
                                        if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                            facing = rayTraceResult.getSide();
                                            postResult = rayTraceResult;
                                        }
                                    }
                                } else {
                                    placeVec = currVec;
                                    placeRotation = currRotation;
                                    if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                        facing = rayTraceResult.getSide();
                                        postResult = rayTraceResult;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (placeWallsRange.get() < placeRange.get() && directionMode.get() == DirectionMode.Strict) {
                if (placeRotation != null && facing != null) {
                    rotationTimer.reset();
                    rotationVector = placeVec;
                    rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
                    return facing;
                } else {
                    for (double xS = start; xS <= end; xS += increment) {
                        for (double yS = start; yS <= end; yS += increment) {
                            for (double zS = start; zS <= end; zS += increment) {
                                Vec3d posVec = (new Vec3d(pos.getX(), pos.getY(), pos.getZ())).add(xS, yS, zS);

                                double distToPosVec = eyesPos.distanceTo(posVec);
                                double diffX = posVec.x - eyesPos.x;
                                double diffY = posVec.y - eyesPos.y;
                                double diffZ = posVec.z - eyesPos.z;
                                double diffXZ = MathHelper.sqrt((float) (diffX * diffX + diffZ * diffZ));

                                double[] tempPlaceRotation = new double[]{MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F), MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)))};

                                // inline values for slightly better perfornamce
                                float yawCos = MathHelper.cos((float) (-tempPlaceRotation[0] * 0.017453292F - 3.1415927F));
                                float yawSin = MathHelper.sin((float) (-tempPlaceRotation[0] * 0.017453292F - 3.1415927F));
                                float pitchCos = -MathHelper.cos((float) (-tempPlaceRotation[1] * 0.017453292F));
                                float pitchSin = MathHelper.sin((float) (-tempPlaceRotation[1] * 0.017453292F));

                                Vec3d rotationVec = new Vec3d((yawSin * pitchCos), pitchSin, (yawCos * pitchCos));
                                Vec3d eyesRotationVec = eyesPos.add(rotationVec.x * distToPosVec, rotationVec.y * distToPosVec, rotationVec.z * distToPosVec);

                                BlockHitResult rayTraceResult = mc.world.raycastBlock(eyesPos, eyesRotationVec, pos, VoxelShapes.fullCube(), mc.world.getBlockState(pos));
                                if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                    Vec3d currVec = posVec;
                                    double[] currRotation = tempPlaceRotation;

                                    if (strictDirection.get()) {
                                        if (placeVec != null && placeRotation != null && ((rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) || facing == null)) {
                                            if (mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(currVec) < mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(placeVec)) {
                                                placeVec = currVec;
                                                placeRotation = currRotation;
                                                if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                                    facing = rayTraceResult.getSide();
                                                    postResult = rayTraceResult;
                                                }
                                            }
                                        } else {
                                            placeVec = currVec;
                                            placeRotation = currRotation;
                                            if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                                facing = rayTraceResult.getSide();
                                                postResult = rayTraceResult;
                                            }
                                        }
                                    } else {
                                        if (placeVec != null && placeRotation != null && ((rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) || facing == null)) {
                                            if (Math.hypot((((currRotation[0] - mc.player.prevYaw) % 360.0F + 540.0F) % 360.0F - 180.0F), (currRotation[1] - mc.player.prevPitch)) <
                                                Math.hypot((((placeRotation[0] - mc.player.prevYaw) % 360.0F + 540.0F) % 360.0F - 180.0F), (placeRotation[1] - mc.player.prevPitch))) {
                                                placeVec = currVec;
                                                placeRotation = currRotation;
                                                if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                                    facing = rayTraceResult.getSide();
                                                    postResult = rayTraceResult;
                                                }
                                            }
                                        } else {
                                            placeVec = currVec;
                                            placeRotation = currRotation;
                                            if (rayTraceResult != null && rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                                facing = rayTraceResult.getSide();
                                                postResult = rayTraceResult;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (placeRotation != null) {
                    rotationTimer.reset();
                    rotationVector = placeVec;
                    rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
                }
                if (facing != null) {
                    return facing;
                }
            }
        } else {
            Direction bestFacing = null;
            Vec3d bestVector = null;
            for (Direction enumFacing : Direction.values()) {
                Vec3d cVector = new Vec3d(pos.getX() + 0.5 + enumFacing.getVector().getX() * 0.5,
                    pos.getY() + 0.5 + enumFacing.getVector().getY() * 0.5,
                    pos.getZ() + 0.5 + enumFacing.getVector().getZ() * 0.5);
                BlockHitResult rayTraceResult = mc.world.raycastBlock(new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), cVector, pos, VoxelShapes.fullCube(), mc.world.getBlockState(pos));
                if (rayTraceResult != null && rayTraceResult.getType().equals(BlockHitResult.Type.BLOCK) && rayTraceResult.getBlockPos().equals(pos)) {
                    if (strictDirection.get()) {
                        if (bestVector == null || mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(cVector) < mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(bestVector)) {
                            bestVector = cVector;
                            bestFacing = enumFacing;
                            postResult = rayTraceResult;
                        }
                    } else {
                        rotationTimer.reset();
                        rotationVector = cVector;
                        rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
                        return enumFacing;
                    }
                }
            }
            if (bestFacing != null) {
                rotationTimer.reset();
                rotationVector = bestVector;
                rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
                return bestFacing;
            } else if (strictDirection.get()) {
                for (Direction enumFacing : Direction.values()) {
                    Vec3d cVector = new Vec3d(pos.getX() + 0.5 + enumFacing.getVector().getX() * 0.5,
                        pos.getY() + 0.5 + enumFacing.getVector().getY() * 0.5,
                        pos.getZ() + 0.5 + enumFacing.getVector().getZ() * 0.5);
                    if (bestVector == null || mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(cVector) < mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(bestVector)) {
                        bestVector = cVector;
                        bestFacing = enumFacing;
                    }
                }
                if (bestFacing != null) {
                    rotationTimer.reset();
                    rotationVector = bestVector;
                    rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
                    return bestFacing;
                }
            }
        }
        if ((double) pos.getY() > mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose())) {
            rotationTimer.reset();
            rotationVector = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
            return Direction.DOWN;
        }
        rotationTimer.reset();
        rotationVector = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        rotations = RotationUtil.calculateAngle(mc.player.getEyePos(), rotationVector);
        return Direction.UP;
    }

    private List<BlockPos> findCrystalBlocks() {
        List<BlockPos> positions = getSphere(new BlockPos(mc.player.getBlockPos()), strictDirection.get() ? placeRange.get().floatValue() + 2F : placeRange.get().floatValue(), placeRange.get().intValue(), false, true, 0).stream().filter(blockPos -> canPlaceCrystal(blockPos, support.get() || forceSupport.get().isPressed())).collect(Collectors.toList());
        return positions;
    }

    private final Box box = new Box(0, 0, 0, 0, 0, 0);

    public static CrystalBoomer autoCrystal = Modules.get().get(CrystalBoomer.class);

    public boolean canPlaceCrystal(BlockPos blockPos, boolean support) {
        if (!support){
            if (!(mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK
                || mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN)) return false;
        }


        BlockPos boost = blockPos.add(0, 1, 0);

        double x = blockPos.getX();
        double y = blockPos.getY() + 1;
        double z = blockPos.getZ();

        ((IBox) box).set(x, y, z, x + 1, y + (!protocol.get() ? 1 : 2), z + 1);

        if (intersectsWithEntities(box)) return false;

        if (!(mc.world.getBlockState(boost).getBlock() == Blocks.AIR)) {
            if (!((mc.world.getBlockState(boost).getBlock() == Blocks.FIRE && fire.get()) || (mc.world.getBlockState(boost).getBlock() instanceof FluidBlock && liquids.get()))) {
                return false;
            }
        }

        BlockPos boost2 = blockPos.add(0, 2, 0);

        if (!protocol.get()) {
            if (!(mc.world.getBlockState(boost2).getBlock() == Blocks.AIR)) {
                if (!(mc.world.getBlockState(boost).getBlock() instanceof FluidBlock && liquids.get())) {
                    return false;
                }
            }
        }

        if (check.get() && !CrystalUtil.rayTraceBreak(blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5)) {
            if (mc.player.getEyePos().distanceTo(new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5)) > breakWallsRange.get()) {
                return false;
            }
        }

        if (placeWallsRange.get() < placeRange.get()) {
            if (!CrystalUtil.rayTracePlace(blockPos)) {
                if (strictDirection.get()) {
                    Vec3d eyesPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
                    boolean inRange = false;
                    if (directionMode.get() == DirectionMode.Vanilla) {
                        for (Direction facing : Direction.values()) {
                            Vec3d cVector = new Vec3d(blockPos.getX() + 0.5 + facing.getVector().getX() * 0.5,
                                blockPos.getY() + 0.5 + facing.getVector().getY() * 0.5,
                                blockPos.getZ() + 0.5 + facing.getVector().getZ() * 0.5);
                            if (eyesPos.distanceTo(cVector) <= placeWallsRange.get()) {
                                inRange = true;
                                break;
                            }
                        }
                    } else {
                        double increment = 0.45D;
                        double start = 0.05D;
                        double end = 0.95D;

                        loop:
                        for (double xS = start; xS <= end; xS += increment) {
                            for (double yS = start; yS <= end; yS += increment) {
                                for (double zS = start; zS <= end; zS += increment) {
                                    Vec3d posVec = (new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ())).add(xS, yS, zS);

                                    double distToPosVec = eyesPos.distanceTo(posVec);

                                    if (distToPosVec <= placeWallsRange.get()) {
                                        inRange = true;
                                        break loop;
                                    }
                                }
                            }
                        }
                    }
                    if (!inRange) return false;
                } else {
                    if ((double) blockPos.getY() > mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose())) {
                        if (BlockUtil.distance(mc.player.getBlockPos(), new BlockPos(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5)) > placeWallsRange.get()) {
                            return false;
                        }
                    } else if (BlockUtil.distance(mc.player.getBlockPos(), new BlockPos(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5)) > placeWallsRange.get()) {
                        return false;
                    }
                }
            }
        } else if (strictDirection.get()) {
            Vec3d eyesPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
            boolean inRange = false;
            if (directionMode.get() == DirectionMode.Vanilla) {
                for (Direction facing : Direction.values()) {
                    Vec3d cVector = new Vec3d(blockPos.getX() + 0.5 + facing.getVector().getX() * 0.5,
                        blockPos.getY() + 0.5 + facing.getVector().getY() * 0.5,
                        blockPos.getZ() + 0.5 + facing.getVector().getZ() * 0.5);
                    if (eyesPos.distanceTo(cVector) <= placeRange.get()) {
                        inRange = true;
                        break;
                    }
                }
            } else {
                double increment = 0.45D;
                double start = 0.05D;
                double end = 0.95D;

                loop:
                for (double xS = start; xS <= end; xS += increment) {
                    for (double yS = start; yS <= end; yS += increment) {
                        for (double zS = start; zS <= end; zS += increment) {
                            Vec3d posVec = (new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ())).add(xS, yS, zS);

                            double distToPosVec = eyesPos.distanceTo(posVec);

                            if (distToPosVec <= placeRange.get()) {
                                inRange = true;
                                break loop;
                            }
                        }
                    }
                }
            }
            if (!inRange) return false;
        }

        return mc.world.getEntitiesByClass(Entity.class, new Box(boost, boost2.add(1, 1, 1)), new Predicate<Entity>() {
                @Override
                public boolean test(Entity entity) {
                    return false;
                }
            }).stream()
            .filter(entity -> !breakLocations.containsKey(entity.getId()) && (!(entity instanceof EndCrystalEntity) || entity.age > 20)).count() == 0;
    }

    private boolean intersectsWithEntities(Box box) {
        if (attackIntersects.get()){
            if (EntityUtils.intersectsWithEntity(box, entity -> entity instanceof EndCrystalEntity && !entity.getBlockPos().equals(new BlockPos(box.getCenter())))){
                if (EntityUtils.intersectsWithEntity(box, entity -> entity instanceof EndCrystalEntity && attack(entity)));
            }
        }

        return EntityUtils.intersectsWithEntity(box, entity -> !(entity.isSpectator()) && (entity instanceof PlayerEntity) || (entity instanceof ItemEntity) || (entity instanceof TntEntity));
    }
    private boolean attack(Entity entity){
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        return true;
    }
    private List<PlayerEntity> getTargetsInRange() {

        List<PlayerEntity> stream = mc.world.getPlayers()
            .stream()
            .filter(e -> e != mc.player)
            .filter(e -> e.isAlive())
            .filter(e -> !Friends.get().isFriend((PlayerEntity) e))
            .filter(e -> ((PlayerEntity) e).getHealth() > 0)
            .filter(e -> mc.player.distanceTo(e) < enemyRange.get())
            .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
            .collect(Collectors.toList());

        if (target.get() == TargetingMode.Smart) {
            List<PlayerEntity> safeStream = stream.stream()
                .filter(e -> !(BlockUtil.isHole(new BlockPos(e.getBlockPos())) || (mc.world.getBlockState(new BlockPos(e.getBlockPos())).getBlock() != Blocks.AIR && mc.world.getBlockState(new BlockPos(e.getBlockPos())).getBlock() != Blocks.COBWEB && !(mc.world.getBlockState(new BlockPos(e.getBlockPos())).getBlock() instanceof FluidBlock))))
                .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
                .collect(Collectors.toList());

            if (safeStream.size() > 0) stream = safeStream;

            safeStream = stream.stream()
                .filter(e -> e.getHealth() + e.getAbsorptionAmount() < 10F)
                .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
                .collect(Collectors.toList());

            if (safeStream.size() > 0) stream = safeStream;
        }

        return stream;
    }

    private List<PlayerEntity> getFriendsInRange() {

        List<PlayerEntity> stream = mc.world.getPlayers()
            .stream()
            .filter(e -> e != mc.player)
            .filter(LivingEntity::isAlive)
            .filter(e -> Friends.get().isFriend(e))
            .filter(e -> e.getHealth() > 0)
            .filter(e -> mc.player.distanceTo(e) < enemyRange.get())
            .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
            .collect(Collectors.toList());

        return stream;
    }

    public void setSwordSlot() {
        int swordSlot = CrystalUtil.getSwordSlot();
        if (mc.player.getInventory().selectedSlot != swordSlot && swordSlot != -1) {
            mc.player.getInventory().selectedSlot = swordSlot;
            InvUtils.swap(swordSlot, false);
            switchTimer.reset();
            noGhostTimer.reset();
        }
    }

    public boolean isOffhand() {
        return mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
    }

    public boolean setCrystalSlot() {
        if (isOffhand()) {
            return true;
        }
        int crystalSlot = CrystalUtil.getCrystalSlot();
        if (crystalSlot == -1) {
            return false;
        } else if (mc.player.getInventory().selectedSlot != crystalSlot) {
            int prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = crystalSlot;
            InvUtils.swap(crystalSlot, false);
            switchTimer.reset();
            noGhostTimer.reset();
        }
        return true;
    }

    public static List<BlockPos> getSphere(BlockPos loc, float r, int h, boolean hollow, boolean sphere, int plus_y) {
        List<BlockPos> circleblocks = new ArrayList<>();
        int cx = loc.getX();
        int cy = loc.getY();
        int cz = loc.getZ();
        for (int x = cx - (int) r; x <= cx + r; x++) {
            for (int z = cz - (int) r; z <= cz + r; z++) {
                for (int y = (sphere ? cy - (int) r : cy); y < (sphere ? cy + r : cy + h); y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < r * r && !(hollow && dist < (r - 1) * (r - 1))) {
                        BlockPos l = new BlockPos(x, y + plus_y, z);
                        circleblocks.add(l);
                    }
                }
            }
        }
        return circleblocks;
    }

    private int breakRenderTimer;
    private int renderTimer;
    private final Vec3 vec3 = new Vec3();
    int tick;

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get() || !renderDamageText.get()) return;

        if (renderPos != null) {
            vec3.set(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5);

            if (NametagUtils.to2D(vec3, damageTextScale.get())) {
                NametagUtils.begin(vec3);
                TextRenderer.get().begin(1, false, true);

                String text = String.format("%.1f", renderDamage);
                double w = TextRenderer.get().getWidth(text) / 2;
                TextRenderer.get().render(text, -w, 0, textColor.get(), true);

                TextRenderer.get().end();
                NametagUtils.end();
            }

            if (tick >= 5) {
                tick = 0;
                renderPos = null;
            } else tick++;
        }
    }

    private Color sideC;
    private Color side2C;
    private Color lineC;
    private Color line2C;

    @EventHandler
    private void checkColor(Render3DEvent event){
        if (render.get()){
            if (!renderDamageColor.get() && !rainbowColor.get()){
                sideC = sideColor.get();
                side2C = sideColor2.get();
                lineC = lineColor.get();
                line2C = lineColor2.get();
            }else if (rainbowColor.get()) {
                sideC = new Color(TotemParticle.rainbowColor().getRed(), TotemParticle.rainbowColor().getGreen(), TotemParticle.rainbowColor().getBlue(), sideAlpha.get());
                side2C = new Color(TotemParticle.rainbowColor().getRed(), TotemParticle.rainbowColor().getGreen(), TotemParticle.rainbowColor().getBlue(), sideAlpha.get());
                lineC = new Color(TotemParticle.rainbowColor().getRed(), TotemParticle.rainbowColor().getGreen(), TotemParticle.rainbowColor().getBlue(), lineAlpha.get());
                line2C = new Color(TotemParticle.rainbowColor().getRed(), TotemParticle.rainbowColor().getGreen(), TotemParticle.rainbowColor().getBlue(), lineAlpha.get());
            }
            else {
                if (renderDamage >= 18){
                    sideC = maxDamColor.get().copy().a(75);
                    side2C = maxDamColor.get().copy().a(75);
                    lineC = maxDamColor.get().copy().a(255);
                    line2C = maxDamColor.get().copy().a(255);
                }
                if (renderDamage < 18 && renderDamage >= 15){
                    sideC = highDamColor.get().copy().a(75);
                    side2C = highDamColor.get().copy().a(75);
                    lineC = highDamColor.get().copy().a(255);
                    line2C = highDamColor.get().copy().a(255);
                }
                if (renderDamage < 15 && renderDamage >= 12){
                    sideC = mediumDamColor.get().copy().a(75);
                    side2C = mediumDamColor.get().copy().a(75);
                    lineC = mediumDamColor.get().copy().a(255);
                    line2C = mediumDamColor.get().copy().a(255);
                }
                if (renderDamage < 12 && renderDamage >= 8){
                    sideC = belowMediumDamColor.get().copy().a(75);
                    side2C = belowMediumDamColor.get().copy().a(75);
                    lineC = belowMediumDamColor.get().copy().a(255);
                    line2C = belowMediumDamColor.get().copy().a(255);
                }
                if (renderDamage < 8 && renderDamage >= 4.5){
                    sideC = littleDamColor.get().copy().a(75);
                    side2C =littleDamColor.get().copy().a(75);
                    lineC = littleDamColor.get().copy().a(255);
                    line2C = littleDamColor.get().copy().a(255);
                }
                if (renderDamage < 4.5){
                    sideC = lowDamColor.get().copy().a(75);
                    side2C = lowDamColor.get().copy().a(75);
                    lineC = lowDamColor.get().copy().a(255);
                    line2C = lowDamColor.get().copy().a(255);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onRender(Render3DEvent event){
        if (render.get()) {
            if (renderMode.get() == RenderMode.Fade) {
                try {
                    renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                    renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideC, side2C, lineC, line2C, shapeMode.get(), onlyTop.get(), height.get(), width.get()));
                }catch (ConcurrentModificationException ignored){}

            } else {
                if (renderPos != null){
                    if (onlyTop.get()) {
                        if (shapeMode.get().equals(ShapeMode.Lines) || shapeMode.get().equals(ShapeMode.Both)) {
                            S(event, renderPos, 0.99, 1-height.get(), 0.01, lineC, line2C);
                            TAB(event, renderPos, 0.99, 0.01,true, false, lineC, line2C);

                            if (width.get() == 2) {
                                S(event, renderPos, 0.98, 1-height.get(), 0.02, lineC, line2C);
                                TAB(event, renderPos, 0.98, 0.02,true, false, lineC, line2C);
                            }
                            if (width.get() == 3) {
                                S(event, renderPos, 0.97, 1-height.get(), 0.03, lineC, line2C);
                                TAB(event, renderPos, 0.97, 0.03,true, false, lineC, line2C);
                            }
                            if (width.get() == 4) {
                                S(event, renderPos, 0.96, 1-height.get(), 0.04, lineC, line2C);
                                TAB(event, renderPos, 0.96, 0.04,true, false, lineC, line2C);
                            }
                        }
                        if (shapeMode.get().equals(ShapeMode.Sides) || shapeMode.get().equals(ShapeMode.Both)) {
                            FS(event, renderPos, 1-height.get(),true, false, sideC, side2C);
                        }
                        if (tick >= 5) {
                            tick = 0;
                            renderPos = null;
                        } else tick++;
                    }
                    else {
                        if (shapeMode.get().equals(ShapeMode.Lines) || shapeMode.get().equals(ShapeMode.Both)) {
                            S(event, renderPos, 0.99, 0, 0.01, lineC, line2C);
                            TAB(event, renderPos, 0.99, 0.01,true, true, lineC, line2C);

                            if (width.get() == 2) {
                                S(event, renderPos, 0.98, 0, 0.02, lineC, line2C);
                                TAB(event, renderPos, 0.98, 0.02,true, true, lineC, line2C);
                            }
                            if (width.get() == 3) {
                                S(event, renderPos, 0.97, 0, 0.03, lineC, line2C);
                                TAB(event, renderPos, 0.97, 0.03,true, true, lineC, line2C);
                            }
                            if (width.get() == 4) {
                                S(event, renderPos, 0.96, 0, 0.04, lineC, line2C);
                                TAB(event, renderPos, 0.96, 0.04,true, true, lineC, line2C);
                            }
                        }
                        if (shapeMode.get().equals(ShapeMode.Sides) || shapeMode.get().equals(ShapeMode.Both)) {
                            FS(event, renderPos,0, true, true, sideC, side2C);
                        }
                    }
                }
            }
        }
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos, int tick) {
            pos.set(blockPos);
            ticks = tick;
            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color sides2, Color lines, Color lines2, ShapeMode shapeMode, boolean onlyTop, double height, int width) {
            int preSideA = sides.a;
            int preSideB = sides2.a;
            int preLineA = lines.a;
            int preLineB = lines2.a;

            sides.a *= (double) ticks / 10;
            sides2.a *= (double) ticks / 10;
            lines.a *= (double) ticks / 10;
            lines2.a *= (double) ticks / 10;

            if (onlyTop) {
                if (shapeMode.equals(ShapeMode.Lines) || shapeMode.equals(ShapeMode.Both)) {
                    S(event, pos, 0.99, 1-height, 0.01, lines, lines2);
                    TAB(event, pos, 0.99, 0.01,true, false, lines, lines2);

                    if (width == 2) {
                        S(event, pos, 0.98, 1-height, 0.02, lines, lines2);
                        TAB(event, pos, 0.98, 0.02,true, false, lines, lines2);
                    }
                    if (width == 3) {
                        S(event, pos, 0.97, 1-height, 0.03, lines, lines2);
                        TAB(event, pos, 0.97, 0.03,true, false, lines, lines2);
                    }
                    if (width == 4) {
                        S(event, pos, 0.96, 1-height, 0.04, lines, lines2);
                        TAB(event, pos, 0.96, 0.04,true, false, lines, lines2);
                    }
                }
                if (shapeMode.equals(ShapeMode.Sides) || shapeMode.equals(ShapeMode.Both)) {
                    FS(event, pos, 1-height,true, false, sides,  sides2);
                }
            }
            else {
                if (shapeMode.equals(ShapeMode.Lines) || shapeMode.equals(ShapeMode.Both)) {
                    S(event, pos, 0.99, 0, 0.01,lines, lines2);
                    TAB(event, pos, 0.99, 0.01,true, true, lines, lines2);

                    if (width == 2) {
                        S(event, pos, 0.98, 0, 0.02, lines, lines2);
                        TAB(event, pos, 0.98, 0.02,true, true, lines, lines2);
                    }
                    if (width == 3) {
                        S(event, pos, 0.97, 0, 0.03, lines, lines2);
                        TAB(event, pos, 0.97, 0.03,true, true, lines, lines2);
                    }
                    if (width == 4) {
                        S(event, pos, 0.96, 0, 0.04, lines, lines2);
                        TAB(event, pos, 0.96, 0.04,true, true, lines, lines2);
                    }
                }
                if (shapeMode.equals(ShapeMode.Sides) || shapeMode.equals(ShapeMode.Both)) {
                    FS(event, pos,0, true, true, sides, sides2);
                }
            }

            sides.a = preSideA;
            sides2.a = preSideB;
            lines.a = preLineA;
            lines2.a = preLineB;
        }
    }

}
