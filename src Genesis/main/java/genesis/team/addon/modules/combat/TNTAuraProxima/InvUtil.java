package genesis.team.addon.modules.combat.TNTAuraProxima;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InvUtil {
    public static boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) return false;
        }
        return true;
    }

    public static Integer getEmptySlots() {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) emptySlots++;
        }
        return emptySlots;
    }

    public static void updateSlot(int newSlot) {
        mc.player.getInventory().selectedSlot = newSlot;
    }

    public static FindItemResult findPick() {
        return InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);
    }
}
