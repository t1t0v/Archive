package genesis.team.addon.modules.render;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HoleESP extends Module {
    public HoleESP(){
        super(Genesis.Render, "hole-esp-plus", "");
    }

    public enum RenderMode{
        Lines,
        Box
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder().name("horizontal-radius").description("Horizontal radius in which to search for holes.").defaultValue(10).min(0).sliderMax(32).build());
    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder().name("vertical-radius").description("Vertical radius in which to search for holes.").defaultValue(5).min(0).sliderMax(32).build());
    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder().name("min-height").description("Minimum hole height required to be rendered.").defaultValue(3).min(1).sliderMin(1).build());
    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder().name("doubles").description("Highlights double holes that can be stood across.").defaultValue(true).build());
    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder().name("ignore-own").description("Ignores rendering the hole you are currently standing in.").defaultValue(false).build());
    private final Setting<Boolean> webs = sgGeneral.add(new BoolSetting.Builder().name("webs").description("Whether to show holes that have webs inside of them.").defaultValue(false).build());

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Which way to render surround.").defaultValue(RenderMode.Box).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").description("The height of rendering.").defaultValue(0.2).min(0).build());
    private final Setting<Integer> fadeTick = sgRender.add(new IntSetting.Builder().name("fade-tick").description("The slot auto move moves beds to.").defaultValue(20).build());

    private final Setting<Boolean> topQuad = sgRender.add(new BoolSetting.Builder().name("top-quad").description("Whether to render a quad at the top of the hole.").defaultValue(true).visible(() -> renderMode.get() == RenderMode.Lines).build());
    private final Setting<Boolean> bottomQuad = sgRender.add(new BoolSetting.Builder().name("bottom-quad").description("Whether to render a quad at the bottom of the hole.").defaultValue(false).visible(() -> renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> bedrockColorTop = sgRender.add(new ColorSetting.Builder().name("bedrock-top").description("The top color for holes that are completely bedrock.").defaultValue(new SettingColor(100, 255, 0, 200)).visible(() -> renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> bedrockColorBottom = sgRender.add(new ColorSetting.Builder().name("bedrock-bottom").description("The bottom color for holes that are completely bedrock.").defaultValue(new SettingColor(100, 255, 0, 0)).visible(() -> renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> obsidianColorTop = sgRender.add(new ColorSetting.Builder().name("obsidian-top").description("The top color for holes that are completely obsidian.").defaultValue(new SettingColor(255, 0, 0, 200)).visible(() -> renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> obsidianColorBottom = sgRender.add(new ColorSetting.Builder().name("obsidian-bottom").description("The bottom color for holes that are completely obsidian.").defaultValue(new SettingColor(255, 0, 0, 0)).visible(() -> renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> mixedColorTop = sgRender.add(new ColorSetting.Builder().name("mixed-top").description("The top color for holes that have mixed bedrock and obsidian.").defaultValue(new SettingColor(255, 127, 0, 200)).visible(() -> renderMode.get() == RenderMode.Lines).build());
    private final Setting<SettingColor> mixedColorBottom = sgRender.add(new ColorSetting.Builder().name("mixed-bottom").description("The bottom color for holes that have mixed bedrock and obsidian.").defaultValue(new SettingColor(255, 127, 0, 0)).visible(() -> renderMode.get() == RenderMode.Lines).build());

    private final Setting<SettingColor> bedrockColorSide = sgRender.add(new ColorSetting.Builder().name("bedrock-side").description("The top color for holes that are completely bedrock.").defaultValue(new SettingColor(100, 255, 0, 200)).visible(() -> renderMode.get() == RenderMode.Box).build());
    private final Setting<SettingColor> bedrockColorLine = sgRender.add(new ColorSetting.Builder().name("bedrock-line").description("The bottom color for holes that are completely bedrock.").defaultValue(new SettingColor(100, 255, 0, 0)).visible(() -> renderMode.get() == RenderMode.Box).build());
    private final Setting<SettingColor> obsidianColorSide = sgRender.add(new ColorSetting.Builder().name("obsidian-side").description("The top color for holes that are completely obsidian.").defaultValue(new SettingColor(255, 0, 0, 200)).visible(() -> renderMode.get() == RenderMode.Box).build());
    private final Setting<SettingColor> obsidianColorLine = sgRender.add(new ColorSetting.Builder().name("obsidian-line").description("The bottom color for holes that are completely obsidian.").defaultValue(new SettingColor(255, 0, 0, 0)).visible(() -> renderMode.get() == RenderMode.Box).build());
    private final Setting<SettingColor> mixedColorSide = sgRender.add(new ColorSetting.Builder().name("mixed-side").description("The top color for holes that have mixed bedrock and obsidian.").defaultValue(new SettingColor(255, 127, 0, 200)).visible(() -> renderMode.get() == RenderMode.Box).build());
    private final Setting<SettingColor> mixedColorLine = sgRender.add(new ColorSetting.Builder().name("mixed-line").description("The bottom color for holes that have mixed bedrock and obsidian.").defaultValue(new SettingColor(255, 127, 0, 0)).visible(() -> renderMode.get() == RenderMode.Box).build());



    private final byte NULL = 0;

    private final Pool<Hole> holeBlockPool = new Pool<>(Hole::new);
    private final List<Hole> holeBlocks = new ArrayList<>();


    private final List<Hole> allHole = new ArrayList<>();


    @EventHandler
    private void onTick(TickEvent.Pre event){
        holeBlocks.forEach(Hole::tick);
        holeBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        List<BlockPos> blocks = getHoles();

        for (BlockPos pos : blocks){
            if (!validHole(pos)) return;

            int bedrock = 0, obsidian = 0;
            Direction air = null;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue;

                BlockState state = mc.world.getBlockState(pos.offset(direction));

                if (state.getBlock() == Blocks.BEDROCK) bedrock++;
                else if (state.getBlock() == Blocks.OBSIDIAN) obsidian++;
                else if (direction == Direction.DOWN) return;
                else if (validHole(pos.offset(direction)) && air == null) {
                    for (Direction dir : Direction.values()) {
                        if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                        BlockState blockState1 = mc.world.getBlockState(pos.offset(direction).offset(dir));

                        if (blockState1.getBlock() == Blocks.BEDROCK) bedrock++;
                        else if (blockState1.getBlock() == Blocks.OBSIDIAN) obsidian++;
                        else return;
                    }

                    air = direction;
                }
            }

            if (obsidian + bedrock == 5 && air == null) {
                holeBlocks.add(holeBlockPool.get().set(pos, obsidian == 5 ? Hole.Type.Obsidian : (bedrock == 5 ? Hole.Type.Bedrock : Hole.Type.Mixed), NULL, fadeTick.get()));
            }
            else if (obsidian + bedrock == 8 && doubles.get() && air != null) {
                holeBlocks.add(holeBlockPool.get().set(pos, obsidian == 8 ? Hole.Type.Obsidian : (bedrock == 8 ? Hole.Type.Bedrock : Hole.Type.Mixed), Dir.get(air), fadeTick.get()));
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (Hole hole : holeBlocks){
            BlockPos pos = hole.blockPos;
            if (!allHole.contains(hole)){
                allHole.add(hole);
                hole.renderAdd(event.renderer, shapeMode.get(), height.get(), topQuad.get(), bottomQuad.get());
            }else {
                hole.renderDefault(event.renderer, shapeMode.get(), height.get(), topQuad.get(), bottomQuad.get());
            }
        }
    }

    private boolean validHole(BlockPos pos) {
        if ((ignoreOwn.get() && (mc.player.getBlockPos().equals(pos)))) return false;

        if (!webs.get() && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) return false;

        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;

        for (int i = 0; i < holeHeight.get(); i++) {
            if (((AbstractBlockAccessor) mc.world.getBlockState(pos.up(i)).getBlock()).isCollidable()) return false;
        }

        return true;
    }

    private List<BlockPos> getHoles(){
        assert mc.player != null;
        return BlockUtil.getSphere(mc.player.getBlockPos(), horizontalRadius.get(), verticalRadius.get()).stream().filter(block -> !EntityUtils.intersectsWithEntity(new Box(block), entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity) && BlockUtil.isAir(block) && BlockUtil.isAir(block.up()) && BlockUtil.isAir(block.up(2)) && (BlockUtil.isHole(block) || (BlockUtil.isDoubleHole(block) && doubles.get()))).collect(Collectors.toList());
    }

    public class Hole{
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public byte exclude;
        public Type type;
        public int ticks;

        public Hole set(BlockPos blockPos, Type type, byte exclude, int tick) {
            this.blockPos.set(blockPos);
            this.exclude = exclude;
            this.type = type;
            ticks = tick;

            return this;
        }

        public Color getTopColor() {
            return switch (this.type) {
                case Obsidian -> Modules.get().get(HoleESP.class).obsidianColorTop.get();
                case Bedrock  -> Modules.get().get(HoleESP.class).bedrockColorTop.get();
                default       -> Modules.get().get(HoleESP.class).mixedColorTop.get();
            };
        }

        public Color getBottomColor() {
            return switch (this.type) {
                case Obsidian -> Modules.get().get(HoleESP.class).obsidianColorBottom.get();
                case Bedrock  -> Modules.get().get(HoleESP.class).bedrockColorBottom.get();
                default       -> Modules.get().get(HoleESP.class).mixedColorBottom.get();
            };
        }

        public Color getSideColor(){
            return switch (this.type){
                case Obsidian -> Modules.get().get(HoleESP.class).obsidianColorSide.get();
                case Bedrock  -> Modules.get().get(HoleESP.class).bedrockColorSide.get();
                default       -> Modules.get().get(HoleESP.class).mixedColorSide.get();
            };
        }

        public Color getLineColor(){
            return switch (this.type){
                case Obsidian -> Modules.get().get(HoleESP.class).obsidianColorLine.get();
                case Bedrock  -> Modules.get().get(HoleESP.class).bedrockColorLine.get();
                default       -> Modules.get().get(HoleESP.class).mixedColorLine.get();
            };
        }

        public void tick() {
            ticks--;
        }

        public void renderDefault(Renderer3D renderer, ShapeMode mode, double height, boolean topQuad, boolean bottomQuad){
            switch (renderMode.get()){
                case Lines -> {
                    int x = blockPos.getX();
                    int y = blockPos.getY();
                    int z = blockPos.getZ();
                    Color top = getTopColor();
                    Color bottom = getBottomColor();

                    int originalTopA = top.a;
                    int originalBottompA = bottom.a;
                    if (mode.lines()) {
                        if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.NORTH)) renderer.line(x, y, z, x, y + height, z, bottom, top);
                        if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x, y, z + 1, x, y + height, z + 1, bottom, top);
                        if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.NORTH)) renderer.line(x + 1, y, z, x + 1, y + height, z, bottom, top);
                        if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x + 1, y, z + 1, x + 1, y + height, z + 1, bottom, top);

                        if (Dir.isNot(exclude, Dir.NORTH)) renderer.line(x, y, z, x + 1, y, z, bottom);
                        if (Dir.isNot(exclude, Dir.NORTH)) renderer.line(x, y + height, z, x + 1, y + height, z, top);
                        if (Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x, y, z + 1, x + 1, y, z + 1, bottom);
                        if (Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x, y + height, z + 1, x + 1, y + height, z + 1, top);

                        if (Dir.isNot(exclude, Dir.WEST)) renderer.line(x, y, z, x, y, z + 1, bottom);
                        if (Dir.isNot(exclude, Dir.WEST)) renderer.line(x, y + height, z, x, y + height, z + 1, top);
                        if (Dir.isNot(exclude, Dir.EAST)) renderer.line(x + 1, y, z, x + 1, y, z + 1, bottom);
                        if (Dir.isNot(exclude, Dir.EAST)) renderer.line(x + 1, y + height, z, x + 1, y + height, z + 1, top);
                    }

                    if (mode.sides()) {
                        top.a = originalTopA / 2;
                        bottom.a = originalBottompA / 2;

                        if (Dir.isNot(exclude, Dir.UP) && topQuad) renderer.quad(x, y + height, z, x, y + height, z + 1, x + 1, y + height, z + 1, x + 1, y + height, z, top); // Top
                        if (Dir.isNot(exclude, Dir.DOWN) && bottomQuad) renderer.quad(x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, bottom); // Bottom

                        if (Dir.isNot(exclude, Dir.NORTH)) renderer.gradientQuadVertical(x, y, z, x + 1, y + height, z, top, bottom); // North
                        if (Dir.isNot(exclude, Dir.SOUTH)) renderer.gradientQuadVertical(x, y, z + 1, x + 1, y + height, z + 1, top, bottom); // South

                        if (Dir.isNot(exclude, Dir.WEST)) renderer.gradientQuadVertical(x, y, z, x, y + height, z + 1, top, bottom); // West
                        if (Dir.isNot(exclude, Dir.EAST)) renderer.gradientQuadVertical(x + 1, y, z, x + 1, y + height, z + 1, top, bottom); // East

                        top.a = originalTopA;
                        bottom.a = originalBottompA;
                    }
                }
                case Box -> {
                    Color sides = getSideColor();
                    Color lines = getLineColor();
                    int preSideA = sides.a;
                    int preLineA = lines.a;

                    sides.a *= (double) ticks / 10;
                    lines.a *= (double) ticks / 10;

                    renderer.box(blockPos, sides, lines, mode, 0);

                    sides.a = preSideA;
                    lines.a = preLineA;
                }
            }
        }

        public void renderAdd(Renderer3D renderer, ShapeMode shapeMode, double height, boolean topQuad, boolean bottomQuad){
            int x = blockPos.getX();
            int y = blockPos.getY();
            int z = blockPos.getZ();

            Color top = getTopColor();
            Color bottom = getBottomColor();

            int originalTopA = top.a;
            int originalBottompA = bottom.a;

            if (top.a <= 255){
                top.a /= (double) ticks / 0.5;
            }
            if (bottom.a <= 255){
                bottom.a /= (double) ticks / 0.5;
            }


            if (shapeMode.lines()) {
                if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.NORTH)) renderer.line(x, y, z, x, y + height, z, bottom, top);
                if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x, y, z + 1, x, y + height, z + 1, bottom, top);
                if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.NORTH)) renderer.line(x + 1, y, z, x + 1, y + height, z, bottom, top);
                if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x + 1, y, z + 1, x + 1, y + height, z + 1, bottom, top);

                if (Dir.isNot(exclude, Dir.NORTH)) renderer.line(x, y, z, x + 1, y, z, bottom);
                if (Dir.isNot(exclude, Dir.NORTH)) renderer.line(x, y + height, z, x + 1, y + height, z, top);
                if (Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x, y, z + 1, x + 1, y, z + 1, bottom);
                if (Dir.isNot(exclude, Dir.SOUTH)) renderer.line(x, y + height, z + 1, x + 1, y + height, z + 1, top);

                if (Dir.isNot(exclude, Dir.WEST)) renderer.line(x, y, z, x, y, z + 1, bottom);
                if (Dir.isNot(exclude, Dir.WEST)) renderer.line(x, y + height, z, x, y + height, z + 1, top);
                if (Dir.isNot(exclude, Dir.EAST)) renderer.line(x + 1, y, z, x + 1, y, z + 1, bottom);
                if (Dir.isNot(exclude, Dir.EAST)) renderer.line(x + 1, y + height, z, x + 1, y + height, z + 1, top);
            }

            if (shapeMode.sides()) {
                top.a = originalTopA / 2;
                bottom.a = originalBottompA / 2;

                if (Dir.isNot(exclude, Dir.UP) && topQuad) renderer.quad(x, y + height, z, x, y + height, z + 1, x + 1, y + height, z + 1, x + 1, y + height, z, top); // Top
                if (Dir.isNot(exclude, Dir.DOWN) && bottomQuad) renderer.quad(x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, bottom); // Bottom

                if (Dir.isNot(exclude, Dir.NORTH)) renderer.gradientQuadVertical(x, y, z, x + 1, y + height, z, top, bottom); // North
                if (Dir.isNot(exclude, Dir.SOUTH)) renderer.gradientQuadVertical(x, y, z + 1, x + 1, y + height, z + 1, top, bottom); // South

                if (Dir.isNot(exclude, Dir.WEST)) renderer.gradientQuadVertical(x, y, z, x, y + height, z + 1, top, bottom); // West
                if (Dir.isNot(exclude, Dir.EAST)) renderer.gradientQuadVertical(x + 1, y, z, x + 1, y + height, z + 1, top, bottom); // East

                top.a = originalTopA;
                bottom.a = originalBottompA;
            }
        }

        public enum Type {
            Bedrock,
            Obsidian,
            Mixed
        }
    }
}
