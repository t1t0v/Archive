package genesis.team.addon.modules.combat.FeetTrap;

import genesis.team.addon.modules.combat.AutoCrystalRewrite.BlockPoz;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FTUtils {
    public static List<BlockPoz> getSphere(BlockPoz centerPos, double radius, double height) {
        ArrayList<BlockPoz> blocks = new ArrayList<>();

        for (int i = centerPos.getX() - (int) radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - (int) height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - (int) radius; k < centerPos.getZ() + radius; k++) {
                    BlockPoz pos = new BlockPoz(i, j, k);

                    if (distanceTo(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    public static double distanceTo(BlockPoz BlockPoz1, BlockPoz BlockPoz2) {
        double d = BlockPoz1.getX() - BlockPoz2.getX();
        double e = BlockPoz1.getY() - BlockPoz2.getY();
        double f = BlockPoz1.getZ() - BlockPoz2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static void place(BlockPoz pos, boolean rotate,int slot, boolean clientSwing) {
        if (pos != null) {
            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(slot, false);
            if (rotate) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
            BlockHitResult result = new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.DOWN, pos, true);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
            if (clientSwing) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            InvUtils.swap(prevSlot, false);
        }
    }

    public static boolean isSurrounded(PlayerEntity p){
            ArrayList<BlockPoz> positions = new ArrayList<>();
            List<Entity> getEntityBoxes;

            for (BlockPoz BlockPoz : getSphere(new BlockPoz(p.getBlockPos()), 3, 1)) {
                if (!mc.world.getBlockState(BlockPoz).getMaterial().isReplaceable()) continue;
                getEntityBoxes = mc.world.getOtherEntities(null, new Box(BlockPoz), entity -> entity == p);
                if (!getEntityBoxes.isEmpty()) continue;

                for (Direction direction : Direction.values()) {
                    if (direction == Direction.UP || direction == Direction.DOWN) continue;

                    getEntityBoxes = mc.world.getOtherEntities(null, new Box(BlockPoz.offset(direction)), entity -> entity == p);
                    if (!getEntityBoxes.isEmpty()) positions.add(BlockPoz);
                }
            }

            return positions.isEmpty();
    }

    public static ArrayList<BlockPoz> getSurroundBlocks(PlayerEntity player) {
        ArrayList<BlockPoz> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPoz BlockPoz : getSphere(new BlockPoz(player.getBlockPos()), 3, 1)) {
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(BlockPoz), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(BlockPoz.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(BlockPoz);
            }
        }

        return positions;
    }

    public static boolean hasEntity(Box box) {
        return hasEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity || entity instanceof TntEntity);
    }

    public static boolean hasEntity(Box box, Predicate<Entity> predicate) {
        return !mc.world.getOtherEntities(null, box, predicate).isEmpty();
    }
}