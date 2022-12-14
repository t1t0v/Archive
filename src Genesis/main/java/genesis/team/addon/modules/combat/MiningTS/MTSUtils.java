package genesis.team.addon.modules.combat.MiningTS;

import genesis.team.addon.modules.combat.AutoCrystalRewrite.BlockPoz;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MTSUtils {
    public static double progress = 0;

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

    public static double distanceTo(BlockPoz blockPoz) {
        return distanceTo(blockPoz.getX(), blockPoz.getY(), blockPoz.getZ());
    }

    public static double distanceTo(Vec3d vec3d) {
        return distanceTo(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public static double distanceTo(double x, double y, double z) {
        Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        Vec3d vec3d = closestVec3d(Box.from(new Vec3d(x, y, z)));

        float f = (float) (eyePos.getX() - vec3d.x);
        float g = (float) (eyePos.getY() - vec3d.y);
        float h = (float) (eyePos.getZ() - vec3d.z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public static double distanceTo(BlockPoz blockPoz, BlockPoz BlockPoz2) {
        double d = blockPoz.getX() - BlockPoz2.getX();
        double e = blockPoz.getY() - BlockPoz2.getY();
        double f = blockPoz.getZ() - BlockPoz2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static double distanceTo(BlockPoz blockPoz, PlayerEntity player) {
        if (blockPoz == null || player == null) return 99;

        double d = blockPoz.getX() - player.getX();
        double e = blockPoz.getY() - player.getY();
        double f = blockPoz.getZ() - player.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static boolean isPlayerNear(BlockPoz blockPoz) {
        if (blockPoz == null) return false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (distanceTo(blockPoz, player) > 5) continue;

            if (player.isDead()) continue;
            if (player == mc.player) continue;
            if (Friends.get().isFriend(player)) continue;

            if (getBlocksAround(player).contains(blockPoz)) return true;
        }

        return false;
    }

    public static List<BlockPoz> getBlocksAround(PlayerEntity player) {
        List<BlockPoz> positions = new ArrayList<>();
        List<Entity> getEntityBoxes;

        for (BlockPoz BlockPoz : getSphere(new BlockPoz(player.getBlockPos()), 3, 1)) {
            getEntityBoxes = mc.world.getOtherEntities(null, new Box(BlockPoz), entity -> entity == player);
            if (!getEntityBoxes.isEmpty()) continue;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;

                getEntityBoxes = mc.world.getOtherEntities(null, new Box(BlockPoz.offset(direction)), entity -> entity == player);
                if (!getEntityBoxes.isEmpty()) positions.add(new BlockPoz(BlockPoz));
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

    public static EndCrystalEntity getEntity(BlockPoz blockPoz) {
        if (blockPoz == null) return null;

        return hasEntity(new Box(blockPoz), entity -> entity instanceof EndCrystalEntity) ?
                (EndCrystalEntity) mc.world.getOtherEntities(null, new Box(blockPoz), entity -> entity instanceof EndCrystalEntity).get(0) : null;
    }

    public static Vec3d closestVec3d(Box box) {
        if (box == null) return new Vec3d(0.0, 0.0, 0.0);
        Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        double x = MathHelper.clamp(eyePos.getX(), box.minX, box.maxX);
        double y = MathHelper.clamp(eyePos.getY(), box.minY, box.maxY);
        double z = MathHelper.clamp(eyePos.getZ(), box.minZ, box.maxZ);

        return new Vec3d(x, y, z);
    }

    // Progress
    public static boolean canBreak(int slot, BlockPoz blockPoz) {
        if (progress >= 1) return true;
        BlockState blockState = mc.world.getBlockState(blockPoz);

        if (progress < 1)
            progress += getBreakDelta(slot != 420 ? slot : mc.player.getInventory().selectedSlot, blockState);
        return false;
    }

    private static double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getHardness(null, null);
        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.isToolRequired() || mc.player.getInventory().main.get(slot).isSuitableFor(state) ? 30 : 100);
        }
    }

    private static double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(block);

        if (speed > 1) {
            ItemStack tool = mc.player.getInventory().getStack(slot);

            int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, tool);

            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }

        if (StatusEffectUtil.hasHaste(mc.player)) {
            speed *= 1 + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
        }

        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= k;
        }

        if (mc.player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(mc.player)) {
            speed /= 5.0F;
        }

        if (!mc.player.isOnGround()) {
            speed /= 5.0F;
        }

        return speed;
    }
}