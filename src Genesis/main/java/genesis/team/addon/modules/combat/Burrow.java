package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.modules.misc.OffHando;
import genesis.team.addon.util.InfoUtil.BlockInfo;
import genesis.team.addon.util.InfoUtil.EntityInfo;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Collections;
import java.util.List;


public class Burrow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<BurrowMode> burrowMode = sgGeneral.add(new EnumSetting.Builder<BurrowMode>().name("mode").description("Mode for placing burrow block").defaultValue(BurrowMode.OneTime).build());
    private final Setting<Integer> reBurrow = sgGeneral.add(new IntSetting.Builder().name("re-burrow").description("Delay between re-burrowing.").defaultValue(5).min(1).sliderMax(20).visible(() -> burrowMode.get() == BurrowMode.Multiply).build());
    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder().name("center").description("Teleporting to center.").defaultValue(false).build());
    private final Setting<Boolean> onlyHole = sgGeneral.add(new BoolSetting.Builder().name("only-hole").description("Prevent working if player isnt in hole").defaultValue(false).build());
    private final Setting<Double> rubberbandHeight = sgGeneral.add(new DoubleSetting.Builder().name("rubberband-height").description("How far to attempt to cause rubberband.").defaultValue(12).sliderMin(-30).sliderMax(30).build());
    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder().name("timer").description("Timer").defaultValue(1.00).min(0.01).sliderMax(10).build());
    private final Setting<List<net.minecraft.block.Block>> block = sgGeneral.add(new BlockListSetting.Builder().name("block").description("Which blocks used for burrow.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    private final BlockPos.Mutable pos = new BlockPos.Mutable();
    private int ticks;
    private int slot = -1;

    public Burrow() {
        super(Genesis.Combat, "burrow-plus", "Clips you into a block.");
    }

    @Override
    public void onActivate() {
        ticks = 0;

        slot = InvUtils.findInHotbar(itemStack -> block.get().contains(net.minecraft.block.Block.getBlockFromItem(itemStack.getItem()))).slot();
        if (Modules.get().get(OffHando.class).isActive() && Modules.get().get(OffHando.class).anvilOnBurrow.get()) slot = 45;

        if (slot == -1) {
            toggle();
            return;
        }

        if (!BlockInfo.isReplaceable(EntityInfo.getBlockPos(mc.player))) {
            if (burrowMode.get() == BurrowMode.Multiply) {
                ticks = reBurrow.get();
            } else {
                toggle();
                return;
            }
        }
        if (EntityInfo.isSurrounded(mc.player) && onlyHole.get()) {
            toggle();
            return;
        }
        if (!shouldBurrow2()) {
            toggle();
            return;
        }
        pos.set(EntityInfo.getBlockPos(mc.player));
        Modules.get().get(Timer.class).setOverride(this.timer.get());
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (shouldBurrow()) {
            if (!EntityInfo.isOnGround(mc.player)) {
                toggle();
                return;
            }
            if (burrowMode.get() == BurrowMode.Multiply) {
                if (ticks <= 0) {
                    burrow();
                    ticks = reBurrow.get();
                } else {
                    ticks--;
                }
            } else {
                burrow();
                toggle();
            }
        } else {
            pos.set(mc.player.getBlockPos());
        }
    }

    private boolean shouldBurrow() {
        return BlockInfo.isReplaceable(EntityInfo.getBlockPos(mc.player));
    }

    private boolean blockFilter(net.minecraft.block.Block block) {
        return block.getBlastResistance() >= 600;
    }

    private void burrow() {
        if (center.get()) PlayerUtils.centerPlayer();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), true));
        InvUtils.swap(slot, true);

        mc.interactionManager.interactBlock(mc.player, slot == 45 ? Hand.OFF_HAND : Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(slot == 45 ? Hand.OFF_HAND : Hand.MAIN_HAND));
        InvUtils.swapBack();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ(), false));
    }

    private boolean shouldBurrow2() {
        BlockState blockState1 = mc.world.getBlockState(pos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        BlockState blockState2 = mc.world.getBlockState(pos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState3 = mc.world.getBlockState(pos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState4 = mc.world.getBlockState(pos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        boolean air1 = blockState1.getMaterial().isReplaceable();
        boolean air2 = blockState2.getMaterial().isReplaceable();
        boolean air3 = blockState3.getMaterial().isReplaceable();
        boolean air4 = blockState4.getMaterial().isReplaceable();
        return air1 & air2 & air3 & air4;
    }

    public enum BurrowMode {
        Multiply,
        OneTime
    }
}
