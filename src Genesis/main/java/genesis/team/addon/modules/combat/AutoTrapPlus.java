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
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

import static genesis.team.addon.util.ProximaUtil.RenderUtil.*;

public class AutoTrapPlus extends Module {
    public AutoTrapPlus(){
        super(Genesis.Combat, "auto-trap-plus", "-");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("target-range").description("The range players can be targeted.").defaultValue(4).build());
    private final Setting<Integer> placeRange = sgGeneral.add(new IntSetting.Builder().name("place-range").description("The range players can be targeted.").defaultValue(4).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("place-delay").description("How many ticks between block placements.").defaultValue(1).build());

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("blocks").description("What blocks to use for surround.").defaultValue(Blocks.OBSIDIAN).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotates towards blocks when placing.").defaultValue(true).build());
    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder().name("self-toggle").description("Turns off after placing all blocks.").defaultValue(true).build());

    // Render
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

    private final Timer placeTimer = new Timer();
    private final List<BlockPos> array = new ArrayList<>();
    private boolean placed;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        array.clear();
    }

    @Override
    public void onActivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
        array.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        if (array.isEmpty() && placed && selfToggle.get()){
            placed = false;
            toggle();
            return;
        }
        array.clear();
        for(PlayerEntity player : EntityUtil.getTargetsInRange(range.get().doubleValue())){
            List<Direction> directions = new ArrayList<>(){{
                add(Direction.EAST);
                add(Direction.WEST);
                add(Direction.SOUTH);
                add(Direction.NORTH);
                add(Direction.UP);
            }};
            directions.sort(Comparator.comparing(direction -> getPriority(direction, player)));
            for (Direction direction : directions){
                BlockPos pos = player.getBlockPos().up().offset(direction);
                Box box = new Box(pos);
                if (EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity) ){
                    for (Direction direction1 : directions){
                        BlockPos pos1 = pos.offset(direction1);
                        if (pos1 == pos.offset(Direction.UP)) continue;
                        Box box1 = new Box(pos1);
                        if (EntityUtils.intersectsWithEntity(box1, entity -> entity instanceof PlayerEntity)){
                            for (CardinalDirection direction2 : CardinalDirection.values()) {
                                BlockPos pos2 = pos1.offset(direction2.toDirection());
                                if (pos2 == pos1.offset(Direction.UP)) continue;
                                Box box2 = new Box(pos2);
                                if (!EntityUtils.intersectsWithEntity(box2, entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity)  && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos2) <= placeRange.get()  && (BlockUtil.isAir(pos2) || BlockUtil.getBlock(pos2) == Blocks.LAVA  || BlockUtil.getBlock(pos2) == Blocks.WATER  || BlockUtil.getBlock(pos2) == Blocks.FIRE) && !array.contains(pos2)) array.add(pos2);
                            }
                        }
                        else {
                            if (BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos1) <= placeRange.get() && (BlockUtil.isAir(pos1) || BlockUtil.getBlock(pos1) == Blocks.LAVA  || BlockUtil.getBlock(pos1) == Blocks.WATER  || BlockUtil.getBlock(pos1) == Blocks.FIRE) && !array.contains(pos1)) array.add(pos1);
                        }
                    }
                }else {
                    if (BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos) <= placeRange.get() && (BlockUtil.isAir(pos) || BlockUtil.getBlock(pos) == Blocks.LAVA  || BlockUtil.getBlock(pos) == Blocks.WATER  || BlockUtil.getBlock(pos) == Blocks.FIRE) && !array.contains(pos))  array.add(pos);
                }
            }
            try {
                if (array.isEmpty()) return;
                for (BlockPos pos : array){
                    if (BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos) > placeRange.get()){
                        array.remove(pos);
                    }
                    if (placeTimer.hasPassed(delay.get() * 50f)){
                        BlockUtils.place(pos, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 50, false);
                        array.remove(pos);
                        renderBlocks.add(renderBlockPool.get().set(pos, fadeTick.get()));
                        placeTimer.reset();
                        placed = true;
                    }
                }
            }catch (ConcurrentModificationException ignored){}
        }
    }

    private int getPriority(Direction direction, PlayerEntity player){
        if (direction == EntityUtil.getMovementDirection(player)) return 0;
        else return 1;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if (array.isEmpty()) return;
        if (fade.get()) {
            renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get()));
        } else {
            for (BlockPos pos : array){
                if (BlockUtil.isAir(pos)){
                    renderD(event, pos, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get());
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

    private int isMoveDir(Direction direction, Direction moveD){
        if (direction == moveD) return 0;
        else return 1;
    }
}
