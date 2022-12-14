package genesis.team.addon.modules.misc.clear;

import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Ezz {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static void clickSlot(int slot, int button, SlotActionType action) {
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, action, mc.player);
   }

   public static Modules get() {
      return Systems.get(Modules.class);
   }

   public static int invIndexToSlotId(int invIndex) {
      return invIndex < 9 && invIndex != -1 ? 44 - (8 - invIndex) : invIndex;
   }

   public static void swap(int slot) {
      if (slot != mc.player.getInventory().selectedSlot && slot >= 0 && slot < 9) {
         mc.player.getInventory().selectedSlot = slot;
      }
   }

   public static boolean equalsBlockPos(BlockPos p1, BlockPos p2) {
      if (p1 != null && p2 != null) {
          if (p1.getX() != p2.getX()) {
             return false;
          } else if (p1.getY() != p2.getY()) {
             return false;
          } else {
             return p1.getZ() == p2.getZ();
          }
      } else {
         return false;
      }
   }

   public static BlockPos SetRelative(int x, int y, int z) {
      return new BlockPos(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
   }

   public static boolean BlockPlace(int x, int y, int z, int HotbarSlot, boolean Rotate) {
      return BlockPlace(new BlockPos(x, y, z), HotbarSlot, Rotate);
   }

   public static boolean BlockPlace(BlockPos BlockPos, int HotbarSlot, boolean Rotate) {
      if (HotbarSlot == -1) {
         return false;
      } else if (!BlockUtils.canPlace(BlockPos, true)) {
         return false;
      } else {
         int PreSlot = mc.player.getInventory().selectedSlot;
         swap(HotbarSlot);
         if (Rotate) {
            Vec3d hitPos = new Vec3d(0.0D, 0.0D, 0.0D);
            ((IVec3d)hitPos).set(BlockPos.getX() + 0.5D, BlockPos.getY() + 0.5D, BlockPos.getZ() + 0.5D);
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos));
         }

         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, BlockPos, true));
         swap(PreSlot);
         return true;
      }
   }

   public static double DistanceTo(BlockPos pos) {
      return DistanceTo(pos.getX(), pos.getY(), pos.getZ());
   }

   public static double DistanceTo(int x, double y, double z) {
      double X = x;
      if (X >= 0.0D) {
         X += 0.5D;
      } else {
         X -= 0.5D;
      }

      double Y;
      if (y >= 0.0D) {
         Y = y + 0.5D;
      } else {
         Y = y - 0.5D;
      }

      double Z;
      if (z >= 0.0D) {
         Z = z + 0.5D;
      } else {
         Z = z - 0.5D;
      }

      double f = mc.player.getX() - X;
      double g = mc.player.getY() - Y;
      double h = mc.player.getZ() - Z;
      return Math.sqrt(f * f + g * g + h * h);
   }

   public static void interact(BlockPos pos, int HotbarSlot, Direction dir) {
      int PreSlot = mc.player.getInventory().selectedSlot;
      swap(HotbarSlot);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), dir, pos, true));
      swap(PreSlot);
   }

   public static void attackEntity(Entity entity) {
      mc.interactionManager.attackEntity(mc.player, entity);
   }

   public static boolean isFriend(PlayerEntity player) {
      return Friends.get().isFriend(player);
   }



   public static double distanceToBlockAnge(BlockPos pos) {
      double x1 = mc.player.getX();
      double y1 = mc.player.getY() + 1.0D;
      double z1 = mc.player.getZ();
      double x2 = pos.getX();
      double y2 = pos.getY();
      double z2 = pos.getZ();
      if (y2 == floor(y1)) {
         y2 = y1;
      }

      if (x2 > 0.0D && x2 == floor(x1)) {
         x2 = x1;
      }

      if (x2 < 0.0D && x2 + 1.0D == floor(x1)) {
         x2 = x1;
      }

      if (z2 > 0.0D && z2 == floor(z1)) {
         z2 = z1;
      }

      if (z2 < 0.0D && z2 + 1.0D == floor(z1)) {
         z2 = z1;
      }

      if (x2 < x1) {
         ++x2;
      }

      if (y2 < y1) {
         ++y2;
      }

      if (z2 < z1) {
         ++z2;
      }

      double dX = x2 - x1;
      double dY = y2 - y1;
      double dZ = z2 - z1;
      return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
   }

   public static double floor(double d) {
      return (d);
   }
}
