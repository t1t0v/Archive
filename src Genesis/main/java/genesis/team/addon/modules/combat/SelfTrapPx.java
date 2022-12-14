package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import genesis.team.addon.util.ProximaUtil.EntityUtil;
import genesis.team.addon.util.ProximaUtil.PlayerUtil;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static genesis.team.addon.util.ProximaUtil.RenderUtil.*;


public class SelfTrapPx extends Module {
    public SelfTrapPx(){
        super(Genesis.Px, "self-trapV2", "-");

    }

    public enum modes {
        AntiFacePlace,
        Full,
        Top,
        DoubleTop,
        DoubleFull,
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> blockPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("Block placements per tick.").defaultValue(4).min(1).sliderMin(1).sliderMax(10).build());
    private final Setting<modes> mode = sgGeneral.add(new EnumSetting.Builder<modes>().name("mode").description("Which positions to place on your top half.").defaultValue(modes.Top).build());
    private final Setting<Boolean> obsidianCheck = sgGeneral.add(new BoolSetting.Builder().name("obsidian-check").description("Checks if obsidian in hotbar. If not, toggles.").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotating towards blocks being placed.").defaultValue(false).build());
    private final Setting<Boolean> sneakOnly = sgGeneral.add(new BoolSetting.Builder().name("require-sneak").description("Only activate while you're sneaking.").defaultValue(false).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Won't place unless you're in a hole").defaultValue(true).build());
    private final Setting<Center> center = sgGeneral.add(new EnumSetting.Builder<Center>().name("center").description("Teleports you to the center of the block.").defaultValue(Center.Incomplete).build());
    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder().name("turn-off").description("Turns off after placing.").defaultValue(true).build());

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final Setting<Boolean> putOutTheFire = sgPlace.add(new BoolSetting.Builder().name("put-out-the-fire").description("Breaks dangerous crystals automaticly.").defaultValue(true).build());
    private final Setting<Boolean> packetPlace = sgPlace.add(new BoolSetting.Builder().name("packet-place").description("Places blocks with packets and lever resistance.").defaultValue(true).build());
    private final Setting<Boolean> allBlocks = sgPlace.add(new BoolSetting.Builder().name("all-blocks").description("Prevents you from falling when 1 blocks below got broken.").defaultValue(false).build());
    private final Setting<List<Block>> blocks = sgPlace.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final Setting<Boolean> disableJump = sgToggle.add(new BoolSetting.Builder().name("toggle-on-jump").description("Disable if you jump.").defaultValue(true).build());
    private final Setting<Boolean> disableYchange = sgToggle.add(new BoolSetting.Builder().name("toggle-on-y-change").description("Disable if your Y coord changes.").defaultValue(false).build());
    private final Setting<Boolean> disableOnChorus = sgToggle.add(new BoolSetting.Builder().name("toggle-on-chorus").description("").defaultValue(true).build());
    private final Setting<Boolean> disableOnPearl = sgToggle.add(new BoolSetting.Builder().name("toggle-on-pearl").description("").defaultValue(true).build());
    private final Setting<Boolean> stopPlacingOnEat = sgToggle.add(new BoolSetting.Builder().name("stop-placing-on-eat").description("").defaultValue(false).build());
    private final Setting<Boolean> stopPlacingOnDrink = sgToggle.add(new BoolSetting.Builder().name("stop-placing-on-drink").description("").defaultValue(false).build());
    private final Setting<Boolean> stopPlacingOnMine = sgToggle.add(new BoolSetting.Builder().name("stop-placing-on-mine").description("").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Render surround.").defaultValue(false).build());
    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Which way to render surround.").defaultValue(RenderMode.Fade).build());
    private final Setting<Boolean> alwaysRender = sgRender.add(new BoolSetting.Builder().name("always").description("Render surround blocks always.").defaultValue(false).visible(render::get).visible(() -> renderMode.get() != RenderMode.Fade).build());
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Render swing.").defaultValue(false).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    public enum RenderMode {
        Fade,
        Default
    }

    public enum Center {
        Never,
        OnActivate,
        Incomplete,
        Always
    }

    private final ArrayList<Vec3d> top = new ArrayList<Vec3d>() {{
        add(new Vec3d(0, 2, 0));
    }};

    private final ArrayList<Vec3d> doubleTop = new ArrayList<Vec3d>() {{
        add(new Vec3d(0, 2, 0));
        add(new Vec3d(0, 3, 0));
    }};

    private final ArrayList<Vec3d> full = new ArrayList<Vec3d>() {{
        add(new Vec3d(0, 2, 0));
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    private final ArrayList<Vec3d> doublefull = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
        add(new Vec3d(1, 2, 0));
        add(new Vec3d(-1, 2, 0));
        add(new Vec3d(0, 2, 1));
        add(new Vec3d(0, 2, -1));
        add(new Vec3d(0, 2, 0));
        add(new Vec3d(0, 3, 0));
    }};

    private final ArrayList<Vec3d> antiFacePlace = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.RESPAWN_ANCHOR ||
            block == Blocks.ANVIL;
    }

    @Override
    public void onActivate() {
        if (center.get() == Center.OnActivate) PlayerUtils.centerPlayer();
    }

    @EventHandler
    private void finishUsing(FinishUsingItemEvent event){
        if (disableOnChorus.get() && event.itemStack.getItem() == Items.CHORUS_FRUIT){
            toggle();
            return;
        }
    }

    @EventHandler
    private void onUsing(InteractItemEvent event){
        if (disableOnPearl.get() && mc.player.getStackInHand(event.hand).getItem() == Items.ENDER_PEARL){
            toggle();
            return;
        }
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event){
        if (onlyInHole.get() && !PlayerUtils.isInHole(false)) return;
        if (PlayerUtils.shouldPause(stopPlacingOnMine.get(), stopPlacingOnEat.get(), stopPlacingOnDrink.get())) return;
        if ((disableJump.get() && (mc.options.jumpKey.isPressed() || mc.player.input.jumping)) || (disableYchange.get() && mc.player.prevY < mc.player.getY())) {
            toggle();
            return;
        }
        doIt();
        if (putOutTheFire.get()){
            BlockPos ppos = mc.player.getBlockPos();
            for (int i = 0; i < getTrapDesign().toArray().length; i++) {
                BlockPos bb = ppos.add(getTrapDesign().get(i).x, getTrapDesign().get(i).y, getTrapDesign().get(i).z);
                if (mc.world.getBlockState(bb).getBlock() == Blocks.FIRE) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bb, Direction.DOWN));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bb, Direction.DOWN));
                }
            }
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event){
        if (!PlayerUtils.isInHole(false)) return;
        if (PlayerUtils.shouldPause(stopPlacingOnMine.get(), stopPlacingOnEat.get(), stopPlacingOnDrink.get())) return;
        doIt();
    }

    private void doIt() {
        int bpt = 0;
        if (center.get() == Center.Always) PlayerUtils.centerPlayer();
        FindItemResult obi = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!obi.found()) {
            if (allBlocks.get()) obi = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem);
        }

        if (sneakOnly.get() && !mc.options.sneakKey.isPressed()) return;
        if (obsidianCheck.get()) {
            if (!obi.found()) {
                error("Cannot find obsidian in hotbar.");
                toggle();
                return;
            }
        }
        if (BlockUtil.isVecComplete(getTrapDesign())) {
            if (turnOff.get()) {
                info("SelfTrap Complete.");
                toggle();
            }
        }
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        int safe = 0;

        if (EntityUtil.isInHole(false, mc.player)) safe = 4;

        int prevSlot = mc.player.getInventory().selectedSlot;
        if (mc.player == null) return;
        BlockPos ppos = mc.player.getBlockPos();
        boolean complete = safe == 4;
        if (!complete && center.get() == Center.Incomplete) PlayerUtils.centerPlayer();

        for (Vec3d blockPos : full){
            BlockPos bb = ppos.add(blockPos.x, blockPos.y, blockPos.z);
            if (BlockUtil.getBlock(bb) instanceof BedBlock){
                boolean wasSneaking = mc.player.isSneaking();
                if (wasSneaking) mc.player.setSneaking(false);
                assert mc.interactionManager != null;
                mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, bb, false));
                mc.player.setSneaking(wasSneaking);
            }
        }

        for (int i = 0; i < getTrapDesign().toArray().length; i++) {
            if (bpt >= blockPerTick.get()) return;
            BlockPos bb = ppos.add(getTrapDesign().get(i).x, getTrapDesign().get(i).y, getTrapDesign().get(i).z);
            if (BlockUtil.isAir(bb) || BlockUtil.getBlock(bb) == Blocks.LAVA || BlockUtil.getBlock(bb) == Blocks.WATER || BlockUtil.getBlock(bb) == Blocks.FIRE) {
                if (packetPlace.get()) {
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(bb), Rotations.getPitch(bb));
                    InvUtils.swap(obi.slot(), false);
                    PlayerUtil.placeBlock(bb, Hand.MAIN_HAND, true);
                    mc.player.getInventory().selectedSlot = prevSlot;
                } else BlockUtils.place(bb, obi, rotate.get(), 0, false);
                bpt++;
                if (swing.get()) {
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                renderBlocks.add(renderBlockPool.get().set(bb));
            }
        }
    }


    private ArrayList<Vec3d> getTrapDesign() {
        ArrayList<Vec3d> trapDesign = new ArrayList<Vec3d>();
        switch (mode.get()) {
            case Full -> { trapDesign.addAll(full); }
            case Top -> { trapDesign.addAll(top); }
            case DoubleTop -> { trapDesign.addAll(doubleTop); }
            case AntiFacePlace -> { trapDesign.addAll(antiFacePlace); }
            case DoubleFull -> {trapDesign.addAll(doublefull);}
        }
        return trapDesign;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            if (renderMode.get() == RenderMode.Fade) {
                renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get()));
            } else {
                BlockPos bruh = mc.player.getBlockPos();
                for (Vec3d b: getTrapDesign()) {
                    BlockPos bb = bruh.add(b.x, b.y, b.z);
                    if (BlockUtil.getBlock(bb) == Blocks.AIR) renderD(event, bb, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get());
                    if (alwaysRender.get()) renderD(event, bb, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get());
                }
            }
        }
    }

    private void renderD(Render3DEvent event, BlockPos pos, Color side1, Color side2, Color line1, Color line2, ShapeMode shapeMode, int width){
        if (shapeMode.equals(ShapeMode.Lines) || shapeMode.equals(ShapeMode.Both)){
            S(event, pos, 0.98,0, 0.02, line1, line2);
            TAB(event, pos, 0.98, 0.02, true,true, line1, line2);

            if (width == 2){
                S(event, pos, 0.96,0, 0.04, line1, line2);
                TAB(event, pos, 0.96, 0.04,true, true, line1, line2);
            }
            if (width == 3){
                S(event, pos, 0.94,0, 0.06, line1, line2);
                TAB(event, pos, 0.94, 0.06, true, true,line1, line2);
            }
            if (width == 4){
                S(event, pos, 0.92,0, 0.08, line1, line2);
                TAB(event, pos, 0.92, 0.08,true, true, line1, line2);
            }
        }
        if (shapeMode.equals(ShapeMode.Sides) || shapeMode.equals(ShapeMode.Both)){
            FS(event, pos,0,true, true, side1, side2);
        }
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = 10;

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

            if (shapeMode.equals(ShapeMode.Lines) || shapeMode.equals(ShapeMode.Both)){
                S(event, pos, 0.98,0, 0.02, line1, line2);
                TAB(event, pos, 0.98, 0.02, true,true, line1, line2);

                if (width == 2){
                    S(event, pos, 0.96,0, 0.04, line1, line2);
                    TAB(event, pos, 0.96, 0.04,true, true, line1, line2);
                }
                if (width == 3){
                    S(event, pos, 0.94,0, 0.06, line1, line2);
                    TAB(event, pos, 0.94, 0.06, true, true,line1, line2);
                }
                if (width == 4){
                    S(event, pos, 0.92,0, 0.08, line1, line2);
                    TAB(event, pos, 0.92, 0.08,true, true, line1, line2);
                }
            }
            if (shapeMode.equals(ShapeMode.Sides) || shapeMode.equals(ShapeMode.Both)){
                FS(event, pos,0,true, true, side1, side2);
            }

            side1.a = preSideA;
            side2.a = preSideB;
            line1.a = preLineA;
            line2.a = preLineB;
        }
    }
}
