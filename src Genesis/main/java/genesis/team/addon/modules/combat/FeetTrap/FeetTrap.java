package genesis.team.addon.modules.combat.FeetTrap;

import genesis.team.addon.Genesis;
import genesis.team.addon.modules.combat.AutoCrystalRewrite.BlockPoz;
import genesis.team.addon.modules.combat.AutoCrystalRewrite.TimerUtils;
import genesis.team.addon.util.CombatUtil.RenderUtils;
import genesis.team.addon.util.InfoUtil.RenderInfo;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeetTrap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<Integer> blockPerInterval = sgGeneral.add(new IntSetting.Builder().name("blocks-per-interval").defaultValue(3).range(1, 5).build());
    public final Setting<Integer> intervalDelay = sgGeneral.add(new IntSetting.Builder().name("interval-delay").defaultValue(1).range(0, 3).build());
    public final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder().name("anti-break").defaultValue(false).build());
    public final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(false).build());
    public final Setting<Integer> collisionPassed = sgGeneral.add(new IntSetting.Builder().name("collision-passed").defaultValue(1500).range(1250, 5000).build());
    public final Setting<Boolean> removeOnClient = sgGeneral.add(new BoolSetting.Builder().name("remove-on-client").defaultValue(false).build());
    public final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder().name("auto-disable").defaultValue(false).build());

    private final Setting<RenderUtils.RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderUtils.RenderMode>().name("render").defaultValue(RenderUtils.RenderMode.Box).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 170, 10)).visible(() -> RenderUtils.visibleSide(shapeMode.get())).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(255, 0, 170, 90)).visible(() -> RenderUtils.visibleLine(shapeMode.get())).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").defaultValue(0.99).sliderRange(0, 1).visible(() -> RenderUtils.visibleHeight(renderMode.get())).build());

    private List<BlockPoz> poses = new ArrayList<>();
    private final List<BlockPoz> queue = new ArrayList<>();

    private final TimerUtils unsurroundedTimer = new TimerUtils();
    private final TimerUtils oneTickTimer = new TimerUtils();

    private int interval;

    public FeetTrap() {
        super(Genesis.Combat, "feet-trap", "Automatically places blocks around your feet.");
    }

    @Override
    public void onActivate() {
        queue.clear();

        interval = 0;
        unsurroundedTimer.reset();
        oneTickTimer.reset();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (autoDisable.get() && ((mc.options.jumpKey.isPressed() || mc.player.input.jumping) || mc.player.prevY < mc.player.getPos().getY())) {
            toggle();
            return;
        }

        if (interval > 0) interval--;
        if (interval > 0) return;

        Map<Integer, BlockBreakingInfo> blocks = ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos();
        BlockPoz ownBreakingPos = new BlockPoz(((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getCurrentBreakingBlockPos());
        ArrayList<BlockPoz> boobies = FTUtils.getSurroundBlocks(mc.player);

        blocks.values().forEach(info -> {
            BlockPoz pos = new BlockPoz(info.getPos());
            if (antiBreak.get() && !pos.equals(ownBreakingPos) && info.getStage() >= 0) {
                if (boobies.contains(pos)) queue.addAll(getBlocksAround(pos));
            }
            if (removeOnClient.get() && !pos.equals(ownBreakingPos) && info.getStage() >= 8) {
                if (boobies.contains(pos)) mc.world.setBlockState(pos,Blocks.AIR.getDefaultState());
            }
        });


        poses = getPositions(mc.player);
        poses.addAll(queue);

        FindItemResult block = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (poses.isEmpty() || !block.found()) {
            if (FTUtils.isSurrounded(mc.player)) unsurroundedTimer.reset();
            return;
        }

        for (int i = 0; i <= blockPerInterval.get(); i++) {
            if (poses.size() > i) {
                FTUtils.place(poses.get(i), rotate.get(),block.slot(), true);
                queue.remove(poses.get(i));
            }
        }
        interval = intervalDelay.get();
    }

    public ArrayList<BlockPoz> getPositions(PlayerEntity player) {
        ArrayList<BlockPoz> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPoz blockPos : FTUtils.getSphere(new BlockPoz(player.getBlockPos()), 3, 1)) {
            if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) continue;
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;
            if (unsurroundedTimer.passedMillis(collisionPassed.get().longValue()) && !mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent()))
                continue;


            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(blockPos.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(blockPos);
            }
        }
        return positions;
    }

    private List<BlockPoz> getBlocksAround(BlockPoz blockPos) {
        List<BlockPoz> blocks = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            if (FTUtils.hasEntity(new Box(blockPos.offset(direction)))) continue;
            if (!mc.world.isAir(blockPos.offset(direction))) continue;
            if (queue.contains(blockPos.offset(direction))) continue;

            blocks.add(blockPos.offset(direction));
        }

        return blocks;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (renderMode.get() == RenderUtils.RenderMode.None || poses.isEmpty()) return;
        RenderInfo ri = new RenderInfo(event, renderMode.get(), shapeMode.get());

        poses.forEach(blockPos -> RenderUtils.render(ri, blockPos, sideColor.get(), lineColor.get(), height.get()));
    }
}
