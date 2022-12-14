package naturalism.addon.netherbane.modules.combat;


import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.InvUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static naturalism.addon.netherbane.utils.RenderUtil.*;

public class TNTAura extends Module {
    public TNTAura(){
        super(NetherBane.COMBATPLUS, "TNT-boomer","----------------------------");

    }

    public enum tntPlaceMode{
        HEAD,
        LEGS,
        MIX
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<tntPlaceMode> tntPlaceModeSetting = sgGeneral.add(new EnumSetting.Builder<tntPlaceMode>().name("tnt-place-mode").description("How to select the player to target.").defaultValue(tntPlaceMode.HEAD).build());
    private final Setting<Integer> trapDelay = sgGeneral.add(new IntSetting.Builder().name("trap-delay").description("How many ticks between block placements.").defaultValue(1).build());
    private final Setting<Integer> tntDelay = sgGeneral.add(new IntSetting.Builder().name("tnt-delay").description("How many ticks between block placements.").defaultValue(1).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to select the player to target.").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).build());
    private final Setting<Boolean> breakBurrow = sgGeneral.add(new BoolSetting.Builder().name("break-burrow").description("Break target's self-trap automatically.").defaultValue(false).build());
    private final Setting<Boolean> breakSelfTrap = sgGeneral.add(new BoolSetting.Builder().name("break-self-trap").description("Break target's self-trap automatically.").defaultValue(false).build());
    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder().name("instant").description("instant.").defaultValue(false).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(0).min(0).max(20).sliderMin(0).sliderMax(20).visible(instant::get).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Rotates towards blocks when placing.").defaultValue(true).visible(() -> tntPlaceModeSetting.get().equals(tntPlaceMode.HEAD)).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotates towards blocks when placing.").defaultValue(true).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Render surround.").defaultValue(false).build());
    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Which way to render surround.").defaultValue(RenderMode.Fade).build());
    private final Setting<Integer> fadeTick = sgRender.add(new IntSetting.Builder().name("fade-tick").description("The slot auto move moves beds to.").defaultValue(8).visible(() -> renderMode.get() == RenderMode.Fade).build());
    private final Setting<Boolean> alwaysRender = sgRender.add(new BoolSetting.Builder().name("always").description("Render surround blocks always.").defaultValue(false).visible(render::get).visible(() -> renderMode.get() != RenderMode.Fade).build());
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Render swing.").defaultValue(false).build());
    private final Setting<ShapeMode> tntShapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("tnt-shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> tntSideColor = sgRender.add(new ColorSetting.Builder().name("tnt-side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> tntSideColor2 = sgRender.add(new ColorSetting.Builder().name("tnt-side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> tntLineColor = sgRender.add(new ColorSetting.Builder().name("tnt-line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> tntLineColor2 = sgRender.add(new ColorSetting.Builder().name("tnt-line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<ShapeMode> trapShapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("trap-shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> trapSideColor = sgRender.add(new ColorSetting.Builder().name("trap-side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> trapSideColor2 = sgRender.add(new ColorSetting.Builder().name("trap-side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> trapLineColor = sgRender.add(new ColorSetting.Builder().name("trap-line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> trapLineColor2 = sgRender.add(new ColorSetting.Builder().name("trap-line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());

    public enum RenderMode {
        Fade,
        Default
    }
    private final Pool<RenderBlock> renderTNTBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderTNTBlocks = new ArrayList<>();
    private final Pool<RenderBlock> renderTrapBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderTrapBlocks = new ArrayList<>();
    private final ArrayList<Vec3d> trap = new ArrayList<Vec3d>() {{
        add(new Vec3d(0,3,0));
        add(new Vec3d(1, 2, 0));
        add(new Vec3d(-1, 2, 0));
        add(new Vec3d(0, 2, 1));
        add(new Vec3d(0, 2, -1));
    }};

    private final ArrayList<Vec3d> surr = new ArrayList<Vec3d>() {{
        add(new Vec3d(0,-1,0));
        add(new Vec3d(1, 0, 0));
        add(new Vec3d(-1, 0, 0));
        add(new Vec3d(0, 0, 1));
        add(new Vec3d(0, 0, -1));
    }};

    private PlayerEntity target;
    private int timer;
    private int ticks;
    private int i;

    @Override
    public void onDeactivate() {
        i = 0;
        for (RenderBlock renderBlock : renderTNTBlocks) renderTNTBlockPool.free(renderBlock);
        renderTNTBlocks.clear();
        for (RenderBlock renderBlock : renderTrapBlocks) renderTrapBlockPool.free(renderBlock);
        renderTrapBlocks.clear();
    }

    @Override
    public void onActivate() {
        for (RenderBlock renderBlock : renderTNTBlocks) renderTNTBlockPool.free(renderBlock);
        renderTNTBlocks.clear();
        for (RenderBlock renderBlock : renderTrapBlocks) renderTrapBlockPool.free(renderBlock);
        renderTrapBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (TargetUtils.isBadTarget(target, targetRange.get())) target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (target == null){
            ChatUtils.error("No target found... disabling.");
            toggle();
            return;
        }

        BlockPos ppos = target.getBlockPos();
        renderTNTBlocks.forEach(RenderBlock::tick);
        renderTNTBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        renderTrapBlocks.forEach(RenderBlock::tick);
        renderTrapBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        if (BlockUtil.getBlock(ppos) != Blocks.AIR && BlockUtil.getBlock(ppos) != Blocks.TNT){
            if (breakBurrow.get()){
                FindItemResult pick = InvUtil.findPick();
                if (pick.found()) {
                    InvUtil.updateSlot(pick.slot());
                    if (!instant.get()) BlockUtils.breakBlock(ppos.up(2), true);
                    if (instant.get()){
                        if (i == 0){
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, ppos.up(2), Direction.UP));
                            if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                            i++;
                        }
                        if (ticks >= delay.get()) {
                            ticks = 0;
                            if (rotate.get()) {
                                Rotations.rotate(Rotations.getYaw(ppos.up(2)), Rotations.getPitch(ppos.up(2)), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, ppos.up(2), Direction.UP)));
                            } else {
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, ppos.up(2), Direction.UP));
                            }
                            if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        } else ticks++;
                    }
                    return;
                }
            }
        }

        if (BlockUtil.getBlock(ppos.up(2)) != Blocks.AIR && BlockUtil.getBlock(ppos.up(2)) != Blocks.TNT){
            if (breakSelfTrap.get()) {
                FindItemResult pick = InvUtil.findPick();
                if (pick.found()) {
                    InvUtil.updateSlot(pick.slot());
                    if (!instant.get()) BlockUtils.breakBlock(ppos.up(2), true);
                    if (instant.get()){
                        if (i == 0){
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, ppos.up(2), Direction.UP));
                            if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                            i++;
                        }
                        if (ticks >= delay.get()) {
                            ticks = 0;
                            if (rotate.get()) {
                                Rotations.rotate(Rotations.getYaw(ppos.up(2)), Rotations.getPitch(ppos.up(2)), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, ppos.up(2), Direction.UP)));
                            } else {
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, ppos.up(2), Direction.UP));
                            }
                            if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        } else ticks++;
                    }
                    return;
                }
            }
        }

        if (tntPlaceModeSetting.get().equals(tntPlaceMode.HEAD) || tntPlaceModeSetting.get().equals(tntPlaceMode.MIX)){
            if (tntPlaceModeSetting.get().equals(tntPlaceMode.HEAD)){
                if (onlyInHole.get() && !EntityUtil.isInHole(false, target)) return;
            }
            for (Vec3d b : getTrapDesign()) {
                BlockPos pos = ppos.add(b.x, b.y, b.z);

                if (mc.world.getBlockState(pos).isAir()) {
                    if (ticks >= trapDelay.get()) {
                        BlockUtils.place(pos, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 100, swing.get(), false);
                        renderTrapBlocks.add(renderTrapBlockPool.get().set(pos, fadeTick.get()));
                        ticks = 0;
                    }else ticks++;
                }
            }

            if (isTraped(target)) {
                if (timer >= tntDelay.get()){
                    BlockUtils.place(target.getBlockPos().up(2), InvUtils.findInHotbar(Items.TNT), rotate.get(), 100, true);
                    renderTNTBlocks.add(renderTNTBlockPool.get().set(target.getBlockPos().up(2), fadeTick.get()));
                    timer = 0;
                }else timer++;
            }

            if (BlockUtil.getBlock(target.getBlockPos().up(2)) == Blocks.TNT){
                flintAndSteel(target.getBlockPos().up(2));
            }
        }

        if (tntPlaceModeSetting.get().equals(tntPlaceMode.LEGS) || tntPlaceModeSetting.get().equals(tntPlaceMode.MIX)){
            for (Vec3d b : getSurrDesign()) {
                BlockPos pos = ppos.add(b.x, b.y, b.z);

                if (mc.world.getBlockState(pos).isAir()) {
                    if (timer >= tntDelay.get()){
                        BlockUtils.place(pos, InvUtils.findInHotbar(Items.TNT), rotate.get(), 100, true);
                        timer = 0;
                    }else timer++;
                }

                if (BlockUtil.getBlock(pos) == Blocks.TNT){
                    flintAndSteel(pos);
                }
            }
        }
    }

    private void flintAndSteel(BlockPos pos){
        FindItemResult flint = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);

        int fire = flint.slot();

        if (fire == -1) return;

        if (fire != -1) {
            mc.player.getInventory().selectedSlot = fire;
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND,
                new BlockHitResult(mc.player.getPos(), Direction.UP, pos, true));
            if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    private boolean isTraped(LivingEntity entity){
        if (BlockUtil.getBlock(entity.getBlockPos().add(0,3,0)) != Blocks.AIR &&
            BlockUtil.getBlock(entity.getBlockPos().add(1,2,0)) != Blocks.AIR &&
        BlockUtil.getBlock(entity.getBlockPos().add(-1,2,0)) != Blocks.AIR &&
        BlockUtil.getBlock(entity.getBlockPos().add(0,2,1)) != Blocks.AIR &&
        BlockUtil.getBlock(entity.getBlockPos().add(0,2,1)) != Blocks.AIR
        ){
            return true;
        }
        return false;
    }

    private ArrayList<Vec3d> getTrapDesign() {
        ArrayList<Vec3d> trapDesign = new ArrayList<Vec3d>(trap);
        return trapDesign;
    }

    private ArrayList<Vec3d> getSurrDesign() {
        ArrayList<Vec3d> surrDesign = new ArrayList<Vec3d>(surr);
        return surrDesign;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            if (renderMode.get() == RenderMode.Fade) {
                renderTNTBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderTNTBlocks.forEach(renderBlock -> renderBlock.render(event, tntSideColor.get(), tntSideColor2.get(), tntLineColor.get(), tntLineColor2.get(), tntShapeMode.get(), width.get()));
                renderTrapBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderTrapBlocks.forEach(renderBlock -> renderBlock.render(event, trapSideColor.get(), trapSideColor2.get(), trapLineColor.get(), trapLineColor2.get(), trapShapeMode.get(), width.get()));
            } else {
                BlockPos bruh = target.getBlockPos();
                if (BlockUtil.getBlock(bruh.up(2)) == Blocks.AIR) renderD(event, bruh.up(2), tntSideColor.get(), tntSideColor2.get(), tntLineColor.get(), tntLineColor2.get(), tntShapeMode.get(), width.get());
                if (alwaysRender.get()) renderD(event, bruh.up(2), tntSideColor.get(), tntSideColor2.get(), tntLineColor.get(), tntLineColor2.get(), tntShapeMode.get(), width.get());
                for (Vec3d b: getTrapDesign()) {
                    BlockPos bb = bruh.add(b.x, b.y, b.z);
                    if (BlockUtil.getBlock(bb) == Blocks.AIR) renderD(event, bb, trapSideColor.get(), trapSideColor2.get(), trapLineColor.get(), trapLineColor2.get(), trapShapeMode.get(), width.get());
                    if (alwaysRender.get()) renderD(event, bb, trapSideColor.get(), trapSideColor2.get(), trapLineColor.get(), trapLineColor2.get(), trapShapeMode.get(), width.get());
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
