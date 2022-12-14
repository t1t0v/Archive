package genesis.team.addon.modules.combat.MiningTS;

import genesis.team.addon.Genesis;
import genesis.team.addon.modules.combat.AutoCrystalRewrite.BlockPoz;
import genesis.team.addon.modules.combat.AutoCrystalRewrite.TimerUtils;
import genesis.team.addon.util.CombatUtil.RenderUtils;
import genesis.team.addon.util.InfoUtil.RenderInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;

import java.util.Comparator;
import java.util.List;

public class MiningTS extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<Boolean> autoCity = sgGeneral.add(new BoolSetting.Builder().name("auto-city").defaultValue(false).build());
    public final Setting<Pickaxe> pickaxe = sgGeneral.add(new EnumSetting.Builder<Pickaxe>().name("pickaxe").defaultValue(Pickaxe.Fastest).build());
    public final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder().name("action-delay").defaultValue(0).range(0, 10).build());
    public final Setting<Boolean> limit = sgGeneral.add(new BoolSetting.Builder().name("limit").defaultValue(false).build());
    public final Setting<Boolean> surroundOnly = sgGeneral.add(new BoolSetting.Builder().name("surround-only").defaultValue(false).build());
    public final Setting<Boolean> fastBreak = sgGeneral.add(new BoolSetting.Builder().name("fast-break").defaultValue(false).build());
    public final Setting<Boolean> haste = sgGeneral.add(new BoolSetting.Builder().name("haste").defaultValue(false).build());
    public final Setting<Integer> amplifier = sgGeneral.add(new IntSetting.Builder().name("amplifier").defaultValue(1).range(1, 2).build());
    public final Setting<Boolean> ignoreAir = sgGeneral.add(new BoolSetting.Builder().name("ignore-air").defaultValue(false).build());
    public final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());

    private final Setting<RenderUtils.RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderUtils.RenderMode>().name("render").defaultValue(RenderUtils.RenderMode.Box).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 170, 10)).visible(() -> RenderUtils.visibleSide(shapeMode.get())).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(255, 0, 170, 90)).visible(() -> RenderUtils.visibleLine(shapeMode.get())).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").defaultValue(0.99).sliderRange(0, 1).visible(() -> RenderUtils.visibleHeight(renderMode.get())).build());

    private PlayerEntity target;

    private BlockPoz blockPos = null;
    private Direction direction = null;

    private int breakTimes;

    private final TimerUtils timer = new TimerUtils();

    public MiningTS() {
        super(Genesis.Combat, "mining-TS", "Even I don't know why this module called like that.");
    }

    @Override
    public void onActivate() {
        breakTimes = 0;

        if (autoCity.get()) {
            target = TargetUtils.getPlayerTarget(6.5, SortPriority.LowestDistance);
            if (TargetUtils.isBadTarget(target, 6)) {
                info("Target not found, ignoring...");
            } else {
                List<BlockPoz> blocks = MTSUtils.getBlocksAround(target);
                blocks = blocks.stream().filter(blockPos -> {
                    if (blockPos.isOf(Blocks.BEDROCK)) return false;
                    return !blockPos.isAir();
                }).sorted(Comparator.comparingDouble(MTSUtils::distanceTo)).toList();

                if (blocks.isEmpty()) {
                    info("Vulnerable positions is empty, ignoring...");
                } else {
                    blockPos = blocks.get(0);
                    direction = mc.player.getY() > blockPos.getY() ? Direction.UP : Direction.DOWN;

                    int slot = pickSlot();
                    if (slot == 420) {
                        info("Pickaxe not found.");
                        return;
                    }

                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                    if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

                    MTSUtils.progress = 0;
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        blockPos = null;
        direction = null;
        if (mc.player.hasStatusEffect(StatusEffects.HASTE)) mc.player.removeStatusEffect(StatusEffects.HASTE);
    }

    @EventHandler
    public void onStartBreaking(StartBreakingBlockEvent event) {
        BlockPoz blockPos = new BlockPoz(event.blockPos);

        if (!blockPos.isBreakable()) return;
        if (surroundOnly.get() && !MTSUtils.isPlayerNear(blockPos)) return;

        this.blockPos = blockPos;
        this.direction = event.direction;

        breakTimes = 0;
        MTSUtils.progress = 0;
    }


    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (blockPos == null) return;
        if (haste.get()) addhaste(mc.player);
        int slot = pickSlot();
        if (!MTSUtils.canBreak(slot, blockPos)) {
            timer.reset();
        } else swap(slot);
    }

    private boolean swap(int slot) {
        if (slot == 420 || MTSUtils.progress < 1 || (ignoreAir.get() && blockPos.isAir()) || (limit.get() && breakTimes >= 1) || !timer.passedTicks(actionDelay.get()))
            return false;

        move(mc.player.getInventory().selectedSlot, slot);
        mine(blockPos);
        move(mc.player.getInventory().selectedSlot, slot);
        timer.reset();
        return true;
    }

    private int pickSlot() {
        FindItemResult pick = pickaxe.get() == Pickaxe.Fastest ? InvUtils.findFastestTool(blockPos.getState()) : InvUtils.find(Items.GOLDEN_PICKAXE, Items.IRON_PICKAXE);
        return pick.found() ? pick.slot() : 420;
    }

    private void move(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }

    private void mine(BlockPoz blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
        if (swing.get()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (fastBreak.get()) blockPos.setState(Blocks.AIR);
        breakTimes++;
    }

    private void addhaste(PlayerEntity player) {
        if (!mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 255, amplifier.get() - 1, false, false, true));
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (renderMode.get() == RenderUtils.RenderMode.None) return;
        if (blockPos == null) return;
        if (limit.get() && breakTimes >= 1) return;
        RenderInfo ri = new RenderInfo(event, renderMode.get(), shapeMode.get());

        RenderUtils.render(ri, blockPos, sideColor.get(), lineColor.get(), height.get());
    }

    public enum Pickaxe {
        Fastest, NoDrop
    }
}
