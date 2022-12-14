package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.Timer;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static naturalism.addon.netherbane.utils.RenderUtil.*;

public class CevBreaker extends Module {
    public CevBreaker(){
        super(NetherBane.COMBATPLUS, "cev-miner", "Place a crystal over the player's head and explodes");

    }

    public enum CrystalMode {
        Smart,
        Strict
    }

    public enum MineMode {
        Vanilla,
        Packet
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Double> enemyRange = sgDefault.add(new DoubleSetting.Builder().name("enemy-range").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Double> cevRange = sgDefault.add(new DoubleSetting.Builder().name("cev-range").description("The radius in which cev get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Boolean> onlyTopBlock = sgDefault.add(new BoolSetting.Builder().name("only-top-block").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Integer> supportDelay = sgDefault.add(new IntSetting.Builder().name("support-delay").defaultValue(6).min(4).max(20).sliderMin(4).sliderMax(20).build());
    private final Setting<Boolean> onlyHole = sgDefault.add(new BoolSetting.Builder().name("only-hole").description(".").defaultValue(true).build());
    private final Setting<Boolean> selfToggle = sgDefault.add(new BoolSetting.Builder().name("self-toggle").description(".").defaultValue(false).build());
    private final Setting<Double> minHealth = sgDefault.add(new DoubleSetting.Builder().name("min-health").description("The minimum health required for Bed Aura to work.").defaultValue(4).min(0).sliderMax(36).max(36).build());

    private final SettingGroup sgMining = settings.createGroup("Mining");
    private final Setting<MineMode> mineMode = sgMining.add(new EnumSetting.Builder<MineMode>().name("mine-mode").description(".").defaultValue(MineMode.Packet).build());
    private final Setting<Boolean> silentSwitch = sgMining.add(new BoolSetting.Builder().name("silent-switch").description("instant.").defaultValue(false).build());
    private final Setting<Boolean> instant = sgMining.add(new BoolSetting.Builder().name("instant").description("instant.").defaultValue(false).build());
    private final Setting<Boolean> bypass = sgMining.add(new BoolSetting.Builder().name("bypass").description("instant.").defaultValue(true).visible(instant::get).build());
    private final Setting<Integer> delay = sgMining.add(new IntSetting.Builder().name("delay").defaultValue(0).min(0).max(20).sliderMin(0).sliderMax(20).visible(instant::get).build());

    private final SettingGroup sgCrystal = settings.createGroup("Crystal");
    private final Setting<CrystalMode> crystalMode = sgCrystal.add(new EnumSetting.Builder<CrystalMode>().name("crystal-mode").description(".").defaultValue(CrystalMode.Smart).visible(() -> !instant.get() && !mineMode.get().equals(MineMode.Packet)).build());
    private final Setting<Double> breakingProgress = sgCrystal.add(new DoubleSetting.Builder().name("breaking-progress").description("The radius in which players get targeted.").defaultValue(0.9).min(0).sliderMax(1).visible(() -> crystalMode.get() == CrystalMode.Smart && !instant.get() && !mineMode.get().equals(MineMode.Packet)).build());

    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnCA = sgPause.add(new BoolSetting.Builder().name("pause-on-CA").description("Pause while Crystal Aura is active.").defaultValue(false).build());
    private final Setting<Boolean> pauseOnBA = sgPause.add(new BoolSetting.Builder().name("pause-on-BA").description("Pause while Bed Aura is active.").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the current block being mined.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());


    private int i;
    private BlockPos renderPos;
    private Timer timer = new Timer();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, -1, 0);
    private Direction direction;
    private BlockPos pos;

    @EventHandler
    private void onBreak(BreakBlockEvent event){
        if (event.blockPos.equals(pos)){
            if (selfToggle.get()){
                toggle();
                return;
            }
        }
    }
    @Override
    public void onDeactivate() {
        i = 0;
        renderPos = null;
        timer.reset();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (EntityUtils.getTotalHealth(mc.player) <= minHealth.get()) return;

        if (PlayerUtils.shouldPause(false, pauseOnEat.get(), pauseOnDrink.get())) return;
        CrystalBoomer ca = Modules.get().get(CrystalBoomer.class);
        if (pauseOnCA.get() && ca.isActive()) return;
        BedBoomer ba = Modules.get().get(BedBoomer.class);
        if (pauseOnBA.get() && ba.isActive()) return;

        List<PlayerEntity> players = EntityUtil.getTargetsInRange(enemyRange.get());
        players = players.stream().filter(this::isValidTarget).collect(Collectors.toList());
        players.sort(Comparator.comparing(playerEntity -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), playerEntity.getBlockPos())));
        if (players.isEmpty()) return;
        PlayerEntity target = players.get(0);
        if (onlyHole.get() && !EntityUtil.isInHole(true, target)) return;
        BlockPos pos = getBlock(target);
        this.pos = pos;
        if (pos == null) return;
        if (BlockUtil.distance(new BlockPos(mc.player.getEyePos()), this.pos) > cevRange.get() || !BlockUtil.isAir(pos.up())){
            this.pos = null;
            return;
        }
        mc.world.getEntities().forEach(entity -> {
            if (entity instanceof EndCrystalEntity && entity.getBlockPos().equals(pos.up())){
                if (DamageUtils.crystalDamage(target, entity.getPos()) > 8){
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                }
            }
        });
        if (BlockUtil.getBlock(pos) == Blocks.OBSIDIAN){
            if (crystalMode.get().equals(CrystalMode.Smart) && crystalMode.isVisible() && (((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress() >= breakingProgress.get())){
                placeCrystal(pos);
            }else if (crystalMode.get().equals(CrystalMode.Strict) || instant.get() || mineMode.get().equals(MineMode.Packet)){
                placeCrystal(pos);
            }

            if (BlockUtils.canBreak(pos)){
                if (instant.get()) {
                    if (i == 0){
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
                        i++;
                    }
                    if (bypass.get()){
                        if(timer.hasPassed(delay.get() * 50f)){
                            if (direction == null) direction = Direction.UP;
                            mc.interactionManager.updateBlockBreakingProgress(pos, direction);
                            mc.interactionManager.cancelBlockBreaking();
                            timer.reset();
                        }
                    }
                    else {
                        instaMine(pos);
                    }
                }else{
                    if (rotate.get()) {
                        BlockPos finalPos1 = pos;
                        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> simpleMine(finalPos1));
                    }
                    else simpleMine(pos);
                }
            }
        }
        if (BlockUtil.isAir(pos)){
            placeObsidian(pos);
        }
    }

    private int timerPlace;

    private void placeObsidian(BlockPos pos){
        if (timerPlace <= 0){
            BlockUtils.place(pos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 50, false);
            timerPlace = supportDelay.get();
        }else timerPlace--;
    }

    private void instaMine(BlockPos pos){
        int slot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE).slot();
        if (mc.player.getAbilities().creativeMode) slot = mc.player.getInventory().selectedSlot;
        int prev = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        if (timer.hasPassed(delay.get() * 50f)) {
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP)));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
            }
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            timer.reset();
        }
        if (silentSwitch.get()) mc.player.getInventory().selectedSlot = prev;
    }

    private void simpleMine(BlockPos pos){
        int slot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE).slot();
        if (mc.player.getAbilities().creativeMode) slot = mc.player.getInventory().selectedSlot;
        int prev = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        if (mineMode.get().equals(MineMode.Vanilla)) BlockUtils.breakBlock(pos, swing.get());
        else { mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN));
            if(swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
        }
        if (silentSwitch.get()) mc.player.getInventory().selectedSlot =  prev;

    }

    private void placeCrystal(BlockPos pos){
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL){
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos,true));
        }
        else if (mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL){
            FindItemResult crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL);
            if (!crystalSlot.found()) {
                error("No crystals");
                return;
            }
            int prev = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = crystalSlot.slot();
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos,true));
            mc.player.getInventory().selectedSlot = prev;
        }
    }

    private BlockPos getBlock(PlayerEntity target){
        BlockPos pos = target.getBlockPos();
        if (BlockUtil.isAir(pos.up(3)) && (BlockUtil.getBlock(pos.up(2)) == Blocks.OBSIDIAN || BlockUtil.getBlock(pos.up(2)) == Blocks.AIR)){
            return pos.up(2);
        }
        if (!onlyTopBlock.get()){
            List<BlockPos> array = new ArrayList<>();
            for (BlockPos ppos : cevArray){
                BlockPos newPos = pos.add(ppos);
                if (BlockUtil.isAir(newPos.up()) && ((BlockUtil.getBlock(newPos) == Blocks.OBSIDIAN || BlockUtil.getBlock(newPos) == Blocks.AIR)) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), newPos) <= cevRange.get()) array.add(newPos);
            }
            array.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
            if (array.isEmpty()) return null;
            return array.get(0);
        }
        return null;
    }

    private boolean isValidTarget(PlayerEntity target){
        BlockPos pos = target.getBlockPos();
        if (onlyTopBlock.get() && !BlockUtil.isAir(pos.up(3))) return false;
        else {
            List<BlockPos> array = new ArrayList<>();
            for (BlockPos ppos : posArray){
                BlockPos newPos = pos.add(ppos);
                if (BlockUtil.isAir(newPos.up())) array.add(newPos);
            }
            if (array.size() == 0) return false;
        }

        return true;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (instant.get() && bypass.get()){
            timer.reset();
            direction = event.direction;
            blockPos.set(event.blockPos);
            cum();
            event.cancel();
        }
    }

    private void cum() {
        if (!wontMine()) {
            if (rotate.get()) {
                if (direction == null) direction = Direction.UP;
                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
            }
        }
    }

    private boolean wontMine() {
        return blockPos.getY() == -1 || !BlockUtils.canBreak(blockPos);
    }

    private List<BlockPos> cevArray = new ArrayList<>(){{
        add(new BlockPos(1, 1, 0));
        add(new BlockPos(-1, 1, 0));
        add(new BlockPos(0, 1, 1));
        add(new BlockPos(0, 1, -1));
    }};

    private List<BlockPos> posArray = new ArrayList<>(){{
            add(new BlockPos(0, 2, 0));
            add(new BlockPos(1, 1, 0));
            add(new BlockPos(-1, 1, 0));
            add(new BlockPos(0, 1, 1));
            add(new BlockPos(0, 1, -1));
    }};


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && pos != null && !mc.world.getBlockState(pos).isAir()) {
            if (shapeMode.get().equals(ShapeMode.Lines) || shapeMode.get().equals(ShapeMode.Both)){
                S(event, pos, 0.99,0, 0.01, lineColor.get(), lineColor2.get());
                TAB(event, pos, 0.99, 0.01,true, true, lineColor.get(), lineColor2.get());

                if (width.get() == 2){
                    S(event, pos, 0.98,0, 0.02, lineColor.get(), lineColor2.get());
                    TAB(event, pos, 0.98, 0.02,true, true, lineColor.get(), lineColor2.get());
                }
                if (width.get() == 3){
                    S(event, pos, 0.97,0, 0.03, lineColor.get(), lineColor2.get());
                    TAB(event, pos, 0.97, 0.03,true, true, lineColor.get(), lineColor2.get());
                }
                if (width.get() == 4){
                    S(event, pos, 0.96,0, 0.04, lineColor.get(), lineColor2.get());
                    TAB(event, pos, 0.96, 0.04,true, true, lineColor.get(), lineColor2.get());
                }
            }
            if (shapeMode.get().equals(ShapeMode.Sides) || shapeMode.get().equals(ShapeMode.Both)){
                FS(event, pos,0, true, true, sideColor.get(), sideColor2.get());
            }
            if (BlockUtil.distance(mc.player.getBlockPos(), pos) > 6){
                pos = null;
            }
        }
    }


}
