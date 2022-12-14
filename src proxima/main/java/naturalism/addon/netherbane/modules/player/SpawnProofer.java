package naturalism.addon.netherbane.modules.player;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.Timer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static naturalism.addon.netherbane.utils.RenderUtil.*;

public class SpawnProofer extends Module {
    public SpawnProofer(){
        super(NetherBane.PLAYERPLUS, "spawn-proofer-plus", "");
    }

    public enum Mode {
        Always,
        Potential,
        Both,
        None
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");

    private final Setting<Double> xPlaceRange = sgDefault.add(new DoubleSetting.Builder().name("x-range").description("The range at which players can be targeted.").defaultValue(5.5).min(0.0).sliderMax(10).build());
    private final Setting<Double> yPlaceRange = sgDefault.add(new DoubleSetting.Builder().name("y-range").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(10).build());
    private final Setting<Integer> delay = sgDefault.add(new IntSetting.Builder().name("place-delay").description("How many ticks between block placements.").defaultValue(1).build());
    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(false).build());

    private final Setting<Boolean> aboveBlock = sgDefault.add(new BoolSetting.Builder().name("above-block").description("Automatically rotates you towards the city block.").defaultValue(false).build());
    private final Setting<Boolean> ignoreBedrock = sgDefault.add(new BoolSetting.Builder().name("ignore-bedrock").description("Automatically rotates you towards the city block.").defaultValue(false).build());

    private final Setting<List<Block>> blocks = sgDefault.add(new BlockListSetting.Builder().name("blocks").description("Block to use for spawn proofing").defaultValue(Blocks.TORCH, Blocks.STONE_BUTTON, Blocks.STONE_SLAB).build());

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


    List<BlockPos> array = new ArrayList<>();
    private final Timer timer = new Timer();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    @EventHandler
    private void onTickPre(TickEvent.Pre event){
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        this.array.clear();
        if (aboveBlock.get()){
            List<BlockPos> array = BlockUtil.getSphere(mc.player.getBlockPos(), xPlaceRange.get().intValue(), yPlaceRange.get().intValue()).stream().filter(blockPos -> !BlockUtil.isAir(blockPos) && BlockUtil.isAir(blockPos.up())).collect(Collectors.toList());
            if (ignoreBedrock.get()) array = array.stream().filter(blockPos -> BlockUtil.getBlock(blockPos) != Blocks.BEDROCK).collect(Collectors.toList());
            array = array.stream().filter(blocksPos -> !blocks.get().contains(BlockUtil.getBlock(blocksPos))).collect(Collectors.toList());
            array.sort(Comparator.comparing(blockPos -> BlockUtil.distance(mc.player.getBlockPos(), blockPos)));
            this.array.addAll(array);
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event){
        if (array.isEmpty()) return;
        // Find slot
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!block.found()) {
            error("Found none of the chosen blocks in hotbar");
            toggle();
            return;
        }

        array.forEach(blockPos -> {
            if (timer.hasPassed(delay.get() * 50f)){
                BlockUtils.place(blockPos.up(), block, rotate.get(), 50);
                renderBlocks.add(renderBlockPool.get().set(blockPos, fadeTick.get()));
                timer.reset();
            }
        });
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

}
