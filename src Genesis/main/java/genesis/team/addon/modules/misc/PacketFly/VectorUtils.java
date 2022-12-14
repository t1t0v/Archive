package genesis.team.addon.modules.misc.PacketFly;

import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.function.Predicate;

public class VectorUtils {
    public static MinecraftClient mc;
    public static Screen screen;
    public static int previousSlot = -1;

    public static void init() {
        mc = MinecraftClient.getInstance();

        screen = null;
    }



    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distance(Vec3d vec1, Vec3d vec2) {
        double dX = vec2.x - vec1.x;
        double dY = vec2.y - vec1.y;
        double dZ = vec2.z - vec1.z;

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distance(BlockPos pos1, BlockPos pos2) {
        double dX = pos2.getX() - pos1.getX();
        double dY = pos2.getY() - pos1.getY();
        double dZ = pos2.getZ() - pos1.getZ();

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distanceXZ(Vec3d pos1, Vec3d pos2) {
        double dX = pos1.getX() - pos2.getX();
        double dZ = pos1.getZ() - pos2.getZ();

        return MathHelper.sqrt((float) (dX * dX + dZ * dZ));
    }

    public static double distanceXZ(double x1, double x2, double z1, double z2) {
        double dX = x1 - x2;
        double dZ = z1 - z2;

        return MathHelper.sqrt((float) (dX * dX + dZ * dZ));
    }

    public static double distanceY(Vec3d pos1, Vec3d pos2) {
        return Math.abs(pos2.y - pos1.y);
    }

    public static double distanceY(double y1, double y2) {
        return Math.abs(y1 - y2);
    }

    public static Vec3d getHitPos(BlockPos pos) {
        Direction side = getPlaceSide(pos);
        Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        if (side != null) {
            side = side.getOpposite();

            hitPos = hitPos.add(
                    side.getOffsetX() == 0 ? 0 : (side.getOffsetX() > 0 ? 0.5 : -0.5),
                    side.getOffsetY() == 0 ? 0 : (side.getOffsetY() > 0 ? 0.5 : -0.5),
                    side.getOffsetZ() == 0 ? 0 : (side.getOffsetZ() > 0 ? 0.5 : -0.5)
            );
        }

        return hitPos;
    }

    public static BlockPos getNeighbourPos(BlockPos pos) {
        Direction side = getPlaceSide(pos);
        BlockPos neighbour;

        if (side == null) {
            neighbour = pos;
        } else {
            neighbour = pos.offset(side.getOpposite());
        }

        return neighbour;
    }

    public static Direction getSide(BlockPos pos) {
        Direction side = getPlaceSide(pos);

        return side == null ? Direction.UP : side;
    }

    public static Direction getPlaceSide(BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.offset(side);
            Direction direction = side.getOpposite();
            BlockState state = mc.world.getBlockState(neighbor);

            if (!state.getMaterial().isReplaceable() && state.getFluidState().isEmpty() && !VectorUtils.isClickable(mc.world.getBlockState(pos.offset(direction)).getBlock())) {
                return direction;
            }
        }

        return null;
    }

    public static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof AbstractButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }

    public static boolean canPlace(BlockPos pos, boolean checkEntities) {
        return canPlace(pos, Blocks.OBSIDIAN.getDefaultState(), checkEntities);
    }

    public static boolean canPlace(BlockPos pos, BlockState state, boolean checkEntities) {
        if (pos == null || mc.world == null) return false;
        if (!World.isValid(pos)) return false;
        if (!mc.world.getBlockState(pos).getMaterial().isReplaceable()) return false;

        return !checkEntities || mc.world.canPlace(state, pos, ShapeContext.absent());
    }

    public static ArrayList<BlockPos> getPositionsAroundPlayer(double range) {
        return getPositionsAroundEntity(mc.player, range);
    }

    public static ArrayList<BlockPos> getPositionsAroundEntity(Entity entity, double range) {
        double pX = entity.getX() - 0.5;
        double pY = entity.getY();
        double pZ = entity.getZ() - 0.5;

        int minX = (int) Math.floor(pX - range);
        int minY = (int) Math.floor(pY - range);
        int minZ = (int) Math.floor(pZ - range);

        int maxX = (int) Math.floor(pX + range);
        int maxY = (int) Math.floor(pY + range);
        int maxZ = (int) Math.floor(pZ + range);

        double rangeSq = Math.pow(range, 2);

        ArrayList<BlockPos> positions = new ArrayList<>();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (VectorUtils.distance(pX, pY, pZ, x, y, z) <= rangeSq) {
                        BlockPos pos = new BlockPos(x, y, z);

                        if (World.isValid(pos)) positions.add(pos);
                    }
                }
            }
        }

        return positions;
    }

    public static double[] directionSpeed(float speed) {
        float forward = mc.player.input.movementForward;
        float side = mc.player.input.movementSideways;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);

        if (forward != 0.0F) {
            if (side > 0.0F) {
                yaw += ((forward > 0.0F) ? -45 : 45);
            } else if (side < 0.0F) {
                yaw += ((forward > 0.0F) ? 45 : -45);
            }

            side = 0.0F;

            if (forward > 0.0F) {
                forward = 1.0F;
            } else if (forward < 0.0F) {
                forward = -1.0F;
            }
        }

        double sin = Math.sin(Math.toRadians(yaw + 90.0F));
        double cos = Math.cos(Math.toRadians(yaw + 90.0F));
        double dx = forward * speed * cos + side * speed * sin;
        double dz = forward * speed * sin - side * speed * cos;

        return new double[] { dx, dz };
    }

    // Finding Items

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (isGood.test(mc.player.getOffHandStack())) {
            return new FindItemResult(SlotUtils.OFFHAND);
        }

        if (isGood.test(mc.player.getMainHandStack())) {
            return new FindItemResult(mc.player.getInventory().selectedSlot);
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        return find(isGood, 0, mc.player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        int slot = -1;

        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
            }
        }

        return new FindItemResult(slot);
    }

    public static boolean swap(int slot, boolean swapBack) {
        if (slot < 0 || slot > 8) return false;
        if (swapBack && previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;

        mc.player.getInventory().selectedSlot = slot;
        ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
        return true;
    }

    public static boolean swapBack() {
        if (previousSlot == -1) return false;

        boolean return_ = swap(previousSlot, false);
        previousSlot = -1;
        return return_;
    }


}
