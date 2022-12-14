package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import genesis.team.addon.util.ProximaUtil.EntityUtil;
import genesis.team.addon.util.ProximaUtil.Timer;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static genesis.team.addon.util.ProximaUtil.RenderUtil.*;


public class AnchorBoomer extends Module {
    public AnchorBoomer(){
        super(Genesis.Combat, "anchor-boomer", "");
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
        Lines,
        BedForm
    }

    private final SettingGroup sgDebug = settings.createGroup("Debug");
    private final Setting<Boolean> debugChat = sgDebug.add(new BoolSetting.Builder().name("debug-chat").description("Automatically rotates you towards the city block.").defaultValue(false).build());

    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final Setting<Double> enemyRange = sgTarget.add(new DoubleSetting.Builder().name("enemy-range").description("The radius in which players get targeted.").defaultValue(10).min(0).sliderMax(15).build());
    private final Setting<TargetMode> targetMode = sgTarget.add(new EnumSetting.Builder<TargetMode>().name("target-mode").description("Which way to swap.").defaultValue(TargetMode.Nearest).build());
    private final Setting<Boolean> ignoreBedrockBurrow = sgTarget.add(new BoolSetting.Builder().name("ignore-bedrock-burrow").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> stopPlaceBurrowed = sgTarget.add(new BoolSetting.Builder().name("stop-place-burrowed").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> stopPlaceSelfHole = sgTarget.add(new BoolSetting.Builder().name("stop-place-self-hole").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgFindPos = settings.createGroup("Find Pos");
    private final Setting<SortMode> sortMode = sgFindPos.add(new EnumSetting.Builder<SortMode>().name("sort-mode").description("Which way to swap.").defaultValue(SortMode.MinSelfHealth).build());

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final Setting<Boolean> oneDotTwelve = sgPlace.add(new BoolSetting.Builder().name("1.12-place").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> aSync = sgPlace.add(new BoolSetting.Builder().name("async").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> xPlaceRange = sgPlace.add(new DoubleSetting.Builder().name("x-range").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> yPlaceRange = sgPlace.add(new DoubleSetting.Builder().name("y-range").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(10).build());

    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("break-pange").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> xBreakRange = sgBreak.add(new DoubleSetting.Builder().name("x-range").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> yBreakRange = sgBreak.add(new DoubleSetting.Builder().name("y-range").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(10).build());

    private final SettingGroup sgFind = settings.createGroup("Find");
    private final Setting<Integer> findPlaceDelay = sgFind.add(new IntSetting.Builder().name("find-place-delay").description("The delay between placing beds in ticks.").defaultValue(7).min(0).sliderMax(20).build());
    private final Setting<Integer> findBreakDelay = sgFind.add(new IntSetting.Builder().name("find-break-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());

    private final SettingGroup sgMove = settings.createGroup("Move");
    public final Setting<Double> minSpeed = sgMove.add(new DoubleSetting.Builder().name("min-speed").description("The range at which players can be targeted.").defaultValue(3).min(0.0).sliderMax(36).build());
    private final Setting<Integer> movePlaceDelay = sgMove.add(new IntSetting.Builder().name("move-place-delay").description("The delay between placing beds in ticks.").defaultValue(7).min(0).sliderMax(20).build());
    private final Setting<Integer> moveBreakDelay = sgMove.add(new IntSetting.Builder().name("move-break-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());

    private final SettingGroup sgTop = settings.createGroup("Top");
    private final Setting<Integer> topPlaceDelay = sgTop.add(new IntSetting.Builder().name("top-place-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Integer> topBreakDelay = sgTop.add(new IntSetting.Builder().name("top-break-delay").description("The delay between placing beds in ticks.").defaultValue(8).min(0).sliderMax(20).build());

    private final SettingGroup sgSurround = settings.createGroup("Surround");
    private final Setting<Integer> surroundPlaceDelay = sgSurround.add(new IntSetting.Builder().name("surround-place-delay").description("The delay between placing beds in ticks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Integer> surroundBreakDelay = sgSurround.add(new IntSetting.Builder().name("surround-break-delay").description("The delay between placing beds in ticks.").defaultValue(7).min(0).sliderMax(20).build());

    private final SettingGroup sgDamages = settings.createGroup("Damages");
    public final Setting<Double> minDamage = sgDamages.add(new DoubleSetting.Builder().name("min-damage").description("The range at which players can be targeted.").defaultValue(6).min(0.0).sliderMax(36).build());
    private final Setting<Double> maxSelfDamage = sgDamages.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum damage to inflict on yourself.").defaultValue(4.5).range(0, 36).sliderMax(36).build());
    private final Setting<Boolean> lethalDamage = sgDamages.add(new BoolSetting.Builder().name("lethal-damage").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    public final Setting<Double> minHealth = sgDamages.add(new DoubleSetting.Builder().name("min-health").description("The range at which players can be targeted.").defaultValue(3).min(0.0).sliderMax(36).visible(lethalDamage::get).build());
    private final Setting<Boolean> antiSuicide = sgDamages.add(new BoolSetting.Builder().name("anti-suicide").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

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

    private final SettingGroup sgAutoRefill = settings.createGroup("Auto Refill");
    private final Setting<Boolean> autoMove = sgAutoRefill.add(new BoolSetting.Builder().name("auto-move").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());
    private final Setting<Integer> respawnMoveSlot = sgAutoRefill.add(new IntSetting.Builder().name("respawn-anchor-move-slot").description("The slot auto move moves beds to.").defaultValue(9).range(1, 9).sliderRange(1, 9).visible(autoMove::get).build());
    private final Setting<Integer> glowstoneMoveSlot = sgAutoRefill.add(new IntSetting.Builder().name("glowstone-move-slot").description("The slot auto move moves beds to.").defaultValue(8).range(1, 9).sliderRange(1, 9).visible(autoMove::get).build());
    private final Setting<Boolean> autoSwitch = sgAutoRefill.add(new BoolSetting.Builder().name("auto-switch").description("Switches to and from beds automatically.").defaultValue(true).build());
    private final Setting<Boolean> silentSwitch = sgAutoRefill.add(new BoolSetting.Builder().name("silent-switch").description("Switches to and from beds automatically.").defaultValue(true).build());

    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final Setting<Boolean> checkDimension = sgMisc.add(new BoolSetting.Builder().name("check-dimension").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> checkItems = sgMisc.add(new BoolSetting.Builder().name("check-items").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Whether to swing hand clientside clientside.").defaultValue(true).build());
    private final Setting<Boolean> renderMine = sgRender.add(new BoolSetting.Builder().name("render-mine").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<ShapeMode> mineShapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("mine-shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(renderMine::get).build());
    private final Setting<SettingColor> mineLineColor = sgRender.add(new ColorSetting.Builder().name("mine-line-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(renderMine::get).build());
    private final Setting<SettingColor> mineSideColor = sgRender.add(new ColorSetting.Builder().name("mine-side-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(renderMine::get).build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<Boolean> fade = sgRender.add(new BoolSetting.Builder().name("fade").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<Integer> fadeTick = sgRender.add(new IntSetting.Builder().name("fade-tick").description("The slot auto move moves beds to.").defaultValue(8).visible(fade::get).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(render::get).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(render::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(render::get).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(render::get).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(render::get).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());

    private List<BlockPos> array = new ArrayList<>();
    private double placeMs;
    private double breakMs;

    private double time;
    private double currentDamage;

    private BlockPos breakPos;

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();

    private BlockPos minePos;

    private BlockPos renderPos;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();

    }

    @Override
    public void onActivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        if (mc.world.getDimension().respawnAnchorWorks() && checkDimension.get()){
            warning("It is overworld...toggle");
            toggle();
            return;
        }

        FindItemResult respawnAnchor = InvUtils.find(itemStack -> itemStack.getItem() == Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.find(itemStack -> itemStack.getItem() == Items.GLOWSTONE);

        if (checkItems.get() && (!respawnAnchor.found() || !glowStone.found())) return;

        if (autoMove.get()) {
            if (respawnAnchor.found() && respawnAnchor.slot() != respawnMoveSlot.get() - 1) {
                InvUtils.move().from(respawnAnchor.slot()).toHotbar(respawnMoveSlot.get() - 1);
            }
            if (glowStone.found() && glowStone.slot() != glowstoneMoveSlot.get() - 1) {
                InvUtils.move().from(glowStone.slot()).toHotbar(glowstoneMoveSlot.get() - 1);
            }
        }

        List<PlayerEntity> playerArray = EntityUtil.getTargetsInRange(enemyRange.get());
        if (ignoreBedrockBurrow.get()) playerArray = playerArray.stream().filter(player -> BlockUtil.getBlock(player.getBlockPos()) != Blocks.BEDROCK).collect(Collectors.toList());

        switch (targetMode.get()){
            case Nearest -> playerArray.sort(Comparator.comparing(player -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), player.getBlockPos())));
            case MinHealth -> playerArray.sort(Comparator.comparing(LivingEntity::getHealth));
            case Openest -> playerArray.sort(Comparator.comparing(this::checkOpenest));
        }

        if (playerArray.isEmpty()) return;

        if ((breakBurrow.get().isPressed() || autoMineBurrow.get()) && !(BlockUtil.getBlock(playerArray.get(0).getBlockPos()) instanceof RespawnAnchorBlock) && EntityUtil.isBurrowed(playerArray.get(0)) && BlockUtil.getState(playerArray.get(0).getBlockPos()).getHardness(mc.world, playerArray.get(0).getBlockPos()) != -1 && BlockUtil.distance(new BlockPos(mc.player.getEyePos()),  playerArray.get(0).getBlockPos()) <= placeRange.get()){
            if (onlyInHoleBurrow.get() && !EntityUtil.isInHole(false, playerArray.get(0))) return;
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
        if ((breakTop.get().isPressed() || autoMineTop.get()) && !(BlockUtil.getBlock(playerArray.get(0).getBlockPos().up(2)) instanceof RespawnAnchorBlock) && !BlockUtil.isAir(playerArray.get(0).getBlockPos().up(2)) && BlockUtil.getState(playerArray.get(0).getBlockPos().up(2)).getHardness(mc.world, playerArray.get(0).getBlockPos().up(2)) != -1 && BlockUtil.distance(new BlockPos(mc.player.getEyePos()),  playerArray.get(0).getBlockPos().up(2)) <= placeRange.get()){
            if (onlyInHoleTop.get() && !EntityUtil.isInHole(false, playerArray.get(0))) return;
            if (onlyOnGroundTop.get() && !mc.player.isOnGround()) return;
            minePos = playerArray.get(0).getBlockPos().up(2);
            mine(playerArray.get(0).getBlockPos().up(2), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem).slot(), topMineMode.get());
            return;
        }

        if (playerArray.isEmpty()) return;
        PlayerEntity target = playerArray.get(0);

        //Find Pos
        if (EntityUtil.isInHole(false, target)){
            array = new ArrayList<>(){{
                if ((BlockUtil.isAir(target.getBlockPos().up(2)) || BlockUtil.getBlock(target.getBlockPos().up(2)) == Blocks.LAVA || BlockUtil.getBlock(target.getBlockPos().up(2)) == Blocks.FIRE) && !isSelfTraped(target) && BlockUtil.isAir(target.getBlockPos().up())){
                    placeMs = topPlaceDelay.get() * 50f;
                    breakMs = topBreakDelay.get() * 50f;
                    add(target.getBlockPos().up(2));
                }
            }};
        }
        else if (isSafeFromFacePlace(target) && !EntityUtil.isInHole(false, target) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), target.getBlockPos()) <= placeRange.get()){
            placeMs = surroundPlaceDelay.get() * 50f;
            breakMs = surroundBreakDelay.get() * 50f;
            array = new ArrayList<>(){{
                List<BlockPos> sur = EntityUtil.getSurroundBlocks(target);
                sur = sur.stream().filter(block -> BlockUtil.isAir(block) || BlockUtil.getBlock(block) == Blocks.FIRE || BlockUtil.getBlock(block) == Blocks.WATER || BlockUtil.getBlock(block) == Blocks.LAVA).collect(Collectors.toList());
                sur.sort(Comparator.comparing(block -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), block)));
                if (!sur.isEmpty()){
                    add(sur.get(0));
                }
            }};
        }
        else {
            if ((aSync.get() && breakPos == null) || !aSync.get()){
                if (EntityUtil.getPlayerSpeed(target) > minSpeed.get()){
                    placeMs = movePlaceDelay.get() * 50f;
                    breakMs = moveBreakDelay.get() * 50f;
                }else {
                    placeMs = findPlaceDelay.get() * 50f;
                    breakMs = findBreakDelay.get() * 50f;
                }
                Runnable task = () -> {
                    try {
                        time = System.currentTimeMillis();
                        array = sortBedPos(getPosAround(), target);
                    }catch (IndexOutOfBoundsException ignored){
                    }
                };
                Thread thread = new Thread(task);
                if (thread.getThreadGroup().activeCount() > 1 || thread.getState() == Thread.State.TERMINATED){
                    thread.start();
                    if (debugChat.get()) info("Time: " + (System.currentTimeMillis() - time) + "; BestDamage: " + currentDamage);
                }else {
                    thread.interrupt();
                }
            }
        }

        if (breakPos == null){
            breakPos = findBreak(target);
        }

        if (breakPos != null && breakTimer.hasPassed(breakMs)){
            breakAnchor(breakPos);
        }
        if (breakPos == null){
            if (array.isEmpty()) return;
            BlockPos pos = array.get(0);
            if (pos == null) return;
            if (stopPlaceBurrowed.get() && EntityUtil.isBurrowed(target)) return;
            if (stopPlaceSelfHole.get() && target.getBlockPos() == mc.player.getBlockPos());

            if (placeTimer.hasPassed(placeMs) && placeBed(pos, target)){
                placeTimer.reset();
                breakTimer.reset();
            }
        }

    }

    private boolean placeBed(BlockPos pos, PlayerEntity target){
        if (pos == null) return false;
        array.clear();

        FindItemResult anchor = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.RESPAWN_ANCHOR);
        if (anchor.getHand() == null && !autoSwitch.get()) return false;

        currentDamage = DamageUtils.bedDamage(target, Utils.vec3d(pos));

        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> {
            int prev = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = anchor.slot();
            mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, true));
            if (silentSwitch.get()) mc.player.getInventory().selectedSlot = prev;
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            breakPos = pos;
            renderPos = pos;
            renderBlocks.add(renderBlockPool.get().set(pos, fadeTick.get()));
        });
        return true;
    }

    private List<BlockPos> sortBedPos(List<BlockPos> array, PlayerEntity target){
        List<Pair<BlockPos, Double>> newArray = new ArrayList<>();
        for (int i = 0; i < array.size(); i++){
            double damage;
            double selfdamage;
            damage = DamageUtils.bedDamage(target, new Vec3d(array.get(i).getX(), array.get(i).getY(), array.get(i).getZ()));
            assert mc.player != null;
            selfdamage = DamageUtils.bedDamage(mc.player, new Vec3d(array.get(i).getX(), array.get(i).getY(), array.get(i).getZ()));
            if ((damage >= minDamage.get() || (target.getHealth() - damage <= minHealth.get() && lethalDamage.get())) && selfdamage <= maxSelfDamage.get()){
                newArray.add(new Pair<>(array.get(i), damage));
            }
        }
        newArray.sort(Comparator.comparing(this::sortMaxDamage));

        List<BlockPos> firstArray = new ArrayList<>();
        List<BlockPos> secondArray = new ArrayList<>();

        double damage = newArray.get(0).getRight();
        for (int i = 0; i < newArray.size(); i++){
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
            case MinSelfHealth -> firstArray.sort(Comparator.comparing(bedblock -> DamageUtils.bedDamage(mc.player, Utils.vec3d(bedblock))));
        }
        firstArray.addAll(secondArray);
        return firstArray;
    }

    private List<BlockPos> getPosAround(){
        return BlockUtil.getSphere(new BlockPos(new BlockPos(mc.player.getEyePos())), xPlaceRange.get().floatValue(), yPlaceRange.get().intValue(), false, true, 0).stream().filter(this::canAnchorPlace).collect(Collectors.toList());
    }

    private boolean canAnchorPlace(BlockPos blockPos){
        Box box = new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 0.6, blockPos.getZ() + 1);
        if (!BlockUtil.isAir(blockPos) && BlockUtil.getBlock(blockPos) != Blocks.LAVA && BlockUtil.getBlock(blockPos) != Blocks.WATER && BlockUtil.getBlock(blockPos) != Blocks.FIRE) return false;
        if (EntityUtils.intersectsWithEntity(box, entity -> entity instanceof EndCrystalEntity || entity instanceof PlayerEntity)) {
            return false;
        }
        if (PlayerUtils.distanceTo(blockPos) > placeRange.get()) return false;
        return !oneDotTwelve.get() || !BlockUtil.isAir(blockPos.down());
    }

    private void breakAnchor(BlockPos pos) {
        if (pos == null) return;
        breakPos = null;
        assert mc.world != null;
        if (!(mc.world.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock)) return;
        FindItemResult glowStone = InvUtils.find(itemStack -> itemStack.getItem() == Items.GLOWSTONE);
        if (mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) > 0){
            if (swing.get()) mc.player.swingHand(Hand.OFF_HAND);
            mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
        }else {
            int prev = mc.player.getInventory().selectedSlot;
            InvUtils.swap(glowStone.slot(), true);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            if (silentSwitch.get()) mc.player.getInventory().selectedSlot = prev;
        }
    }

    private BlockPos findBreak(PlayerEntity target){
        List<BlockPos> array = BlockUtil.getSphere(new BlockPos(new BlockPos(mc.player.getEyePos())), xBreakRange.get().floatValue(), yBreakRange.get().intValue(), false, true, 0).stream().filter(blockPos -> BlockUtil.getBlock(blockPos) instanceof RespawnAnchorBlock).collect(Collectors.toList());

        array = array.stream().filter(blockPos -> {
            Vec3d anchorVec = Utils.vec3d(blockPos);
            if (PlayerUtils.distanceTo(blockPos) > breakRange.get()) return false;
            if (DamageUtils.bedDamage(target, anchorVec) < minDamage.get() && DamageUtils.bedDamage(mc.player, anchorVec) > maxSelfDamage.get()) return false;
            if ((antiSuicide.get() && PlayerUtils.getTotalHealth() - DamageUtils.bedDamage(mc.player, anchorVec) < 0)) return false;
            return !lethalDamage.get() || !(PlayerUtils.getTotalHealth() - DamageUtils.bedDamage(mc.player, anchorVec) < minHealth.get());
        }).collect(Collectors.toList());

        array.sort(Comparator.comparing(blockPos -> sortMaxBreakDamage(DamageUtils.bedDamage(target, Utils.vec3d(blockPos)))));
        if (array.isEmpty()) return null;
        return array.get(0);
    }

    private double sortMaxBreakDamage(Double currentDamage){
        return 400 - currentDamage;
    }

    private double sortMaxDamage(Pair<BlockPos, Double> arrays){
        return 400 - arrays.getRight();
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

    private int checkOpenest(PlayerEntity player){
        if (EntityUtil.isSelfTraped(player, enemyRange.get()) && EntityUtil.isInHole(false, player)) return 2;
        else if (EntityUtil.isInHole(false, player) && !EntityUtil.isSelfTraped(player, enemyRange.get())) return 1;
        else return 0;
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
            if (BlockUtil.isAir(pos) || BlockUtil.getBlock(pos) instanceof RespawnAnchorBlock || BlockUtil.getBlock(pos) == Blocks.LAVA || BlockUtil.getBlock(pos) == Blocks.FIRE) return false;
        }
        return true;
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
            if (BlockUtil.isAir(pos) || BlockUtil.getBlock(pos) instanceof RespawnAnchorBlock || BlockUtil.getBlock(pos) == Blocks.LAVA || BlockUtil.getBlock(pos) == Blocks.FIRE) return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            if (renderMine.get() && minePos != null && !BlockUtil.isAir(minePos)){
                event.renderer.box(minePos, mineSideColor.get(), mineLineColor.get(), mineShapeMode.get(), 0);
            }
            if (fade.get()) {
                renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get()));
            }else {
                if (renderPos == null) return;
                renderD(event, renderPos, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor.get(), shapeMode.get(), width.get());
            }
        }
    }

    private void renderD(Render3DEvent event, BlockPos pos, Color side1, Color side2, Color line1, Color line2, ShapeMode shapeMode, int width){
        if (shapeMode.equals(ShapeMode.Lines) || shapeMode.equals(ShapeMode.Both)){
            S(event, pos, 0.99,0, 0.01, line1, line2);
            TAB(event, pos, 0.99, 0.01, true,true, line1, line2);

            if (width == 2){
                S(event, pos, 0.98,0, 0.02, line1, line2);
                TAB(event, pos, 0.98, 0.02,true, true, line1, line2);
            }
            if (width == 3){
                S(event, pos, 0.97,0, 0.03, line1, line2);
                TAB(event, pos, 0.97, 0.03, true, true,line1, line2);
            }
            if (width == 4){
                S(event, pos, 0.96,0, 0.04, line1, line2);
                TAB(event, pos, 0.96, 0.04,true, true, line1, line2);
            }
        }
        if (shapeMode.equals(ShapeMode.Sides) || shapeMode.equals(ShapeMode.Both)){
            FS(event, pos,0,true, true, side1, side2);
        }
    }


    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos, int ticks) {
            pos.set(blockPos);
            this.ticks = ticks;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color side1, Color side2, Color line1, Color line2, ShapeMode shapeMode, int width) {
            int preSideA = side1.a;
            int preSideB = side2.a;
            int preLineA = line1.a;
            int preLineB = line2.a;

            side1.a *= (double) ticks / 10;
            side2.a *= (double) ticks / 10;
            line1.a *= (double) ticks / 10;
            line2.a *= (double) ticks / 10;

            if (shapeMode.equals(ShapeMode.Lines) || shapeMode.equals(ShapeMode.Both)) {
                S(event, pos, 0.99, 0, 0.01, line1, line2);
                TAB(event, pos, 0.99, 0.01, true, true, line1, line2);

                if (width == 2) {
                    S(event, pos, 0.98, 0, 0.02, line1, line2);
                    TAB(event, pos, 0.98, 0.02, true, true, line1, line2);
                }
                if (width == 3) {
                    S(event, pos, 0.97, 0, 0.03, line1, line2);
                    TAB(event, pos, 0.97, 0.03, true, true, line1, line2);
                }
                if (width == 4) {
                    S(event, pos, 0.96, 0, 0.04, line1, line2);
                    TAB(event, pos, 0.96, 0.04, true, true, line1, line2);
                }
            }
            if (shapeMode.equals(ShapeMode.Sides) || shapeMode.equals(ShapeMode.Both)) {
                FS(event, pos, 0, true, true, side1, side2);
            }

            side1.a = preSideA;
            side2.a = preSideB;
            line1.a = preLineA;
            line2.a = preLineB;
        }
    }
}
