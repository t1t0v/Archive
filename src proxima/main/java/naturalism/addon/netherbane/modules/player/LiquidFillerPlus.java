package naturalism.addon.netherbane.modules.player;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.PlayerUtil;
import naturalism.addon.netherbane.utils.Timer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LiquidFillerPlus extends Module {
    public LiquidFillerPlus(){
        super(NetherBane.PLAYERPLUS, "liquid-filler-plus", "");
    }

    public enum Mode{
        Bucket,
        Block
    }

    public enum PlaceIn {
        Lava,
        Water,
        Both
    }


    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>().name("mode").description("Block breaking method").defaultValue(Mode.Bucket).build());
    private final Setting<PlaceIn> placeInLiquids = sgDefault.add(new EnumSetting.Builder<PlaceIn>().name("place-in").description("What type of liquids to place in.").defaultValue(PlaceIn.Lava).build());
    private final Setting<Double> xPlaceRange = sgDefault.add(new DoubleSetting.Builder().name("x-range").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> yPlaceRange = sgDefault.add(new DoubleSetting.Builder().name("y-range").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(10).build());
    private final Setting<Integer> tick = sgDefault.add(new IntSetting.Builder().name("delay").description("The delay between breaks.").defaultValue(0).min(0).sliderMax(20).build());


    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());


    private BlockPos render;
    private final Timer timer = new Timer();

    @EventHandler
    private void onTick(TickEvent.Pre event){
        switch (mode.get()){
            case Bucket -> {
                List<BlockPos> array = BlockUtil.getSphere(mc.player.getBlockPos(), xPlaceRange.get().intValue(), yPlaceRange.get().intValue()).stream().filter(this::isValidLiquid).collect(Collectors.toList());
                array.sort(Comparator.comparing(blockPos -> BlockUtil.distance(mc.player.getBlockPos(), blockPos)));
                if (array.isEmpty()) return;

                BlockPos mainSource = array.get(0);
                render = mainSource;
                array.remove(0);
                List<BlockPos> subArray = new ArrayList<>(array);
                for (BlockPos pos : subArray){
                    FindItemResult bucket = InvUtils.findInHotbar(Items.BUCKET);
                    FindItemResult lavaBucket = InvUtils.findInHotbar(Items.LAVA_BUCKET);
                    if (timer.hasPassed(tick.get() * 50F)){
                        if (bucket.found()) {
                            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> {
                                mc.player.getInventory().selectedSlot = bucket.slot();
                                mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                            });
                        }
                        else if (lavaBucket.found()){
                            Rotations.rotate(Rotations.getYaw(mainSource), Rotations.getPitch(mainSource), () -> {
                                mc.player.getInventory().selectedSlot = lavaBucket.slot();
                                mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                            });

                        }
                        timer.reset();
                    }
                }
            }
        }
    }

    private boolean isValidLiquid(BlockPos pos){
        switch (placeInLiquids.get()){
            case Lava -> {
                if (BlockUtil.getBlock(pos) != Blocks.LAVA && BlockUtil.getState(pos).getFluidState().getLevel() != 8 && !BlockUtil.getState(pos).getFluidState().isStill()) return false;
            }
            case Water -> {
                if (BlockUtil.getBlock(pos) != Blocks.WATER && BlockUtil.getState(pos).getFluidState().getLevel() != 8 && !BlockUtil.getState(pos).getFluidState().isStill()) return false;
            }
            case Both -> {
                if (BlockUtil.getBlock(pos) != Blocks.LAVA && BlockUtil.getBlock(pos) != Blocks.WATER && BlockUtil.getState(pos).getFluidState().getLevel() != 8 && !BlockUtil.getState(pos).getFluidState().isStill()) return false;
            }
        }
        if (!PlayerUtil.canSeePos(pos)) return false;
        return true;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if (render != null){
            event.renderer.box(render, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
