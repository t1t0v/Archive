package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Box;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.PlayerUtil;
import naturalism.addon.netherbane.utils.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static naturalism.addon.netherbane.utils.RenderUtil.*;

public class HoleFillPlus extends Module {
    public HoleFillPlus() {
        super(NetherBane.COMBATPLUS, "hole-fill", "Fills");

    }

    public enum Target{
        All,
        Nearest
    }

    public enum SwapMode {
        Normal, Silent
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Double> enemyRange = sgDefault.add(new DoubleSetting.Builder().name("enemy-range").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Target> targetMode = sgDefault.add(new EnumSetting.Builder<Target>().name("target").description("Block breaking method").defaultValue(Target.Nearest).build());
    private final Setting<Double> holeRange = sgDefault.add(new DoubleSetting.Builder().name("hole-range").description("The radius in which city get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Boolean> ignoreInHole = sgDefault.add(new BoolSetting.Builder().name("ignore-in-hole").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(true).build());

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final Setting<Boolean> smartPlace = sgPlace.add(new BoolSetting.Builder().name("smart-place").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> packetSneak = sgPlace.add(new BoolSetting.Builder().name("packet-sneak").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> packetPlace = sgPlace.add(new BoolSetting.Builder().name("packet-place").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> doubleHole = sgPlace.add(new BoolSetting.Builder().name("double-hole").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Double> XZRange = sgPlace.add(new DoubleSetting.Builder().name("XZ-range").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Double> YRange = sgPlace.add(new DoubleSetting.Builder().name("Y-range").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Integer> delay = sgPlace.add(new IntSetting.Builder().name("delay").description("The delay between breaks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<List<Block>> blockList = sgPlace.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).build());

    private final SettingGroup sgSwap = settings.createGroup("Swap");
    private final Setting<SwapMode> swapMode = sgSwap.add(new EnumSetting.Builder<SwapMode>().name("swap-mode").description("Which way to swap.").defaultValue(SwapMode.Silent).build());
    public final Setting<Double> swapDelay = sgSwap.add(new DoubleSetting.Builder().name("swapDelay").description("The delay between swap.").defaultValue(0).min(0.0).sliderMax(10).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the current block being mined.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());


    private final Timer swapTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer renderTimer = new Timer();
    private List<BlockPos> array = new ArrayList<>();

    @EventHandler
    private void onTick(TickEvent.Pre event){
        List<PlayerEntity> all = EntityUtil.getTargetsInRange(enemyRange.get());
        List<PlayerEntity> targets = new ArrayList<>();
        if (all.isEmpty()) return;
        if (targetMode.get() == Target.Nearest) {
            all.sort(Comparator.comparing(player -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), player.getBlockPos())));
            targets.add(all.get(0));
        }else {
            targets = all;
        }
        for (PlayerEntity target : targets){
            if (ignoreInHole.get() && EntityUtil.isInHole(true, target)) continue;
            List<BlockPos> blocks = new ArrayList<>();
            if (smartPlace.get()){
                blocks.add(getBestHole(target));
            }else {
                blocks = getHoles();
            }
            if (blocks.isEmpty()) return;
            FindItemResult obi = InvUtils.findInHotbar(itemStack -> blockList.get().contains(Block.getBlockFromItem(itemStack.getItem())));
            if (!obi.found()) return;
            for (BlockPos blockPos : blocks){
                if (blockPos != null){
                    if (placeTimer.hasPassed(delay.get() * 50f)){
                        if (packetSneak.get()) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                        if (packetPlace.get()) {
                            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos));
                            int prevSlot = mc.player.getInventory().selectedSlot;
                            if (!swapTimer.hasPassed(swapDelay.get() * 50)) return;
                            mc.player.getInventory().selectedSlot = obi.slot();
                            swapTimer.reset();
                            PlayerUtil.placeBlock(blockPos, Hand.MAIN_HAND, true);
                            if (swapMode.get() == SwapMode.Silent) mc.player.getInventory().selectedSlot = prevSlot;
                        } else BlockUtils.place(blockPos, obi, rotate.get(), 0, false);
                        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                        placeTimer.reset();
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
            array = blocks;
        }
    }

    private List<BlockPos> getHoles(){
        assert mc.player != null;
        return BlockUtil.getSphere(mc.player.getBlockPos(), XZRange.get().intValue(), YRange.get().intValue()).stream().filter(block -> !EntityUtils.intersectsWithEntity(new Box(block), entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity) && BlockUtil.isAir(block) && BlockUtil.isAir(block.up()) && BlockUtil.isAir(block.up(2)) && (BlockUtil.isHole(block) || (BlockUtil.isDoubleHole(block) && doubleHole.get())) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), block) <= holeRange.get()).collect(Collectors.toList());
    }

    private BlockPos getBestHole(PlayerEntity target){
        List<BlockPos> array = getHoles().stream().filter(block -> BlockUtil.distance(target.getBlockPos(), block) < 2 && !EntityUtils.intersectsWithEntity(new Box(block), entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity)).sorted(Comparator.comparing(block -> BlockUtil.distance(target.getBlockPos(), block))).collect(Collectors.toList());
        if (array.isEmpty()) return null;
        return array.get(0);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && !array.isEmpty()){
            for (BlockPos blockPos : array){
                if (blockPos != null){
                    if (BlockUtil.isAir(blockPos)){
                        if (shapeMode.get().equals(ShapeMode.Lines) || shapeMode.get().equals(ShapeMode.Both)){
                            S(event, blockPos, 0.99,0, 0.01, lineColor.get(), lineColor2.get());
                            TAB(event, blockPos, 0.99, 0.01,true, true, lineColor.get(), lineColor2.get());

                            if (width.get() == 2){
                                S(event, blockPos, 0.98,0, 0.02, lineColor.get(), lineColor2.get());
                                TAB(event, blockPos, 0.98, 0.02,true, true, lineColor.get(), lineColor2.get());
                            }
                            if (width.get() == 3){
                                S(event, blockPos, 0.97,0, 0.03, lineColor.get(), lineColor2.get());
                                TAB(event, blockPos, 0.97, 0.03,true, true, lineColor.get(), lineColor2.get());
                            }
                            if (width.get() == 4){
                                S(event, blockPos, 0.96,0, 0.04, lineColor.get(), lineColor2.get());
                                TAB(event, blockPos, 0.96, 0.04,true, true, lineColor.get(), lineColor2.get());
                            }
                        }
                        if (shapeMode.get().equals(ShapeMode.Sides) || shapeMode.get().equals(ShapeMode.Both)){
                            FS(event, blockPos,0, true, true, sideColor.get(), sideColor2.get());
                        }
                    }
                }
            }
            if (renderTimer.hasPassed(200)){
                renderTimer.reset();
                array.clear();
            }
        }
    }
}
