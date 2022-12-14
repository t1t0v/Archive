package genesis.team.addon.modules.misc;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import genesis.team.addon.util.ProximaUtil.RotationUtil;
import genesis.team.addon.util.ProximaUtil.Timer;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

import static genesis.team.addon.util.ProximaUtil.RenderUtil.*;

public class AutoBuild extends Module {
    public AutoBuild(){
        super(Genesis.Misc, "auto-build", "");
    }

    public enum Building{
        Portal,
        Penis,
        Swastika
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Building> building = sgDefault.add(new EnumSetting.Builder<Building>().name("building").description("Block breaking method").defaultValue(Building.Portal).build());

    private final Setting<Integer> tick = sgDefault.add(new IntSetting.Builder().name("delay").description("The delay between breaks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Integer> forward = sgDefault.add(new IntSetting.Builder().name("forward +").description("The delay between breaks.").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> stopMove = sgDefault.add(new BoolSetting.Builder().name("stop-moving").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> toggle = sgDefault.add(new BoolSetting.Builder().name("toggle").description("Automatically rotates you towards the city block.").defaultValue(true).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<Boolean> fade = sgRender.add(new BoolSetting.Builder().name("fade").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    private final Setting<Integer> fadeTick = sgRender.add(new IntSetting.Builder().name("fade-tick").description("The slot auto move moves beds to.").defaultValue(8).visible(fade::get).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(render::get).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(render::get).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(render::get).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(render::get).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(render::get).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private final Timer placeTimer = new Timer();

    List<Pair<BlockPos, Item>> toBuild = new ArrayList<>();

    @EventHandler
    private void onMove(PlayerMoveEvent event){
        if (stopMove.get() && !toBuild.isEmpty()){
            ((IVec3d)event.movement).set(0.0D, 0.0D, 0.0D);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        toBuild.clear();
        Direction direction = mc.player.getHorizontalFacing();

        switch (building.get()){
            case Portal -> {
                getPortalArray(direction).forEach(blockPos -> {
                    toBuild.add(new Pair<>(blockPos, Items.OBSIDIAN));
                });
                toBuild.add(new Pair<>(mc.player.getBlockPos().offset(direction, forward.get()).up(), Items.FLINT_AND_STEEL));
            }
            case Penis -> {
                getPenisArray(direction).forEach(blockPos -> {
                    toBuild.add(new Pair<>(blockPos, Items.OBSIDIAN));
                });
            }
            case Swastika -> {
                getSwastika(direction).forEach(blockPos -> {
                    toBuild.add(new Pair<>(blockPos, Items.OBSIDIAN));
                });
            }
        }
        if (toBuild.isEmpty()) return;
        try {
            toBuild.forEach(pair -> {
                if (placeTimer.hasPassed(tick.get() * 50F) && BlockUtil.isAir(pair.getLeft())){
                    BlockUtils.place(pair.getLeft(), InvUtils.findInHotbar(pair.getRight()), rotate.get(), 50);
                    placeTimer.reset();
                    renderBlocks.add(renderBlockPool.get().set(pair.getLeft(), fadeTick.get()));
                    toBuild.remove(pair);
                }
            });
        }catch (ConcurrentModificationException ignored){}

        if (toBuild.isEmpty() && toggle.get()) {
            toggle();
        }
    }

    private List<BlockPos> getSwastika(Direction direction){
        List<BlockPos> toBuild = new ArrayList<>();
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)));

        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).up())) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).up());
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).up(2))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).up(2));

        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)).up(2))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)).up(2));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up(2))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up(2));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up(3))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up(3));

        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up(2))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up(2));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction), 2).up(2))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction), 2).up(2));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction), 2).up())) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction), 2).up());

        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).up(3))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).up(3));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).up(4))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).up(4));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up(4))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up(4));
        return toBuild;
    }

    private List<BlockPos> getPenisArray(Direction direction){
        List<BlockPos> toBuild = new ArrayList<>();
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).up())) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).up());
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).up(2))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).up(2));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).up(3))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).up(3));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)));
        return toBuild;
    }

    private List<BlockPos> getPortalArray(Direction direction){
        List<BlockPos> toBuild = new ArrayList<>();
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up())) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up());
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up())) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up());
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up(2))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up(2));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up(2))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up(2));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up(3))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction), 2).up(3));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up(3))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getCounterClockWise(direction)).up(3));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).up(4))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).up(4));
        if (BlockUtil.isAir(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)).up(4))) toBuild.add(mc.player.getBlockPos().offset(direction, forward.get()).offset(RotationUtil.getClockWise(direction)).up(4));
        return toBuild;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if (fade.get()) {
            renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get()));
        } else {
            if (toBuild.isEmpty()) return;
            for (Pair<BlockPos, Item> pair : toBuild){
                if (BlockUtil.isAir(pair.getLeft())){
                    renderD(event, pair.getLeft(), sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get());
                }
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
