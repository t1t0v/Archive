package genesis.team.addon.modules.combat.AntiPiston;

import net.minecraft.screen.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;


public class SlotUtils {
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 8;

    public static final int OFFHAND = 45;

    public static final int MAIN_START = 9;
    public static final int MAIN_END = 35;

    public static final int ARMOR_START = 36;
    public static final int ARMOR_END = 39;

    public static int indexToId(int i) {
        if (mc.player == null) return -1;
        ScreenHandler handler = mc.player.currentScreenHandler;

        if (handler instanceof PlayerScreenHandler) return survivalInventory(i);
        else if (handler instanceof GenericContainerScreenHandler) return genericContainer(i, ((GenericContainerScreenHandler) handler).getRows());
        else if (handler instanceof CraftingScreenHandler) return craftingTable(i);
        else if (handler instanceof FurnaceScreenHandler) return furnace(i);
        else if (handler instanceof BlastFurnaceScreenHandler) return furnace(i);
        else if (handler instanceof SmokerScreenHandler) return furnace(i);
        else if (handler instanceof Generic3x3ContainerScreenHandler) return generic3x3(i);
        else if (handler instanceof EnchantmentScreenHandler) return enchantmentTable(i);
        else if (handler instanceof BrewingStandScreenHandler) return brewingStand(i);
        else if (handler instanceof MerchantScreenHandler) return villager(i);
        else if (handler instanceof BeaconScreenHandler) return beacon(i);
        else if (handler instanceof AnvilScreenHandler) return anvil(i);
        else if (handler instanceof HopperScreenHandler) return hopper(i);
        else if (handler instanceof ShulkerBoxScreenHandler) return genericContainer(i, 3);
        else if (handler instanceof CartographyTableScreenHandler) return cartographyTable(i);
        else if (handler instanceof GrindstoneScreenHandler) return grindstone(i);
        else if (handler instanceof LecternScreenHandler) return lectern();
        else if (handler instanceof LoomScreenHandler) return loom(i);
        else if (handler instanceof StonecutterScreenHandler) return stonecutter(i);

        return -1;
    }

    private static int survivalInventory(int i) {
        if (isHotbar(i)) return 36 + i;
        if (isArmor(i)) return 5 + (i - 36);
        return i;
    }

    private static int genericContainer(int i, int rows) {
        if (isHotbar(i)) return (rows + 3) * 9 + i;
        if (isMain(i)) return rows * 9 + (i - 9);
        return -1;
    }

    private static int craftingTable(int i) {
        if (isHotbar(i)) return 37 + i;
        if (isMain(i)) return i + 1;
        return -1;
    }

    private static int furnace(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int generic3x3(int i) {
        if (isHotbar(i)) return 36 + i;
        if (isMain(i)) return i;
        return -1;
    }

    private static int enchantmentTable(int i) {
        if (isHotbar(i)) return 29 + i;
        if (isMain(i)) return 2 + (i - 9);
        return -1;
    }

    private static int brewingStand(int i) {
        if (isHotbar(i)) return 32 + i;
        if (isMain(i)) return 5 + (i - 9);
        return -1;
    }

    private static int villager(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int beacon(int i) {
        if (isHotbar(i)) return 28 + i;
        if (isMain(i)) return 1 + (i - 9);
        return -1;
    }

    private static int anvil(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int hopper(int i) {
        if (isHotbar(i)) return 32 + i;
        if (isMain(i)) return 5 + (i - 9);
        return -1;
    }

    private static int cartographyTable(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int grindstone(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    private static int lectern() {
        return -1;
    }

    private static int loom(int i) {
        if (isHotbar(i)) return 31 + i;
        if (isMain(i)) return 4 + (i - 9);
        return -1;
    }

    private static int stonecutter(int i) {
        if (isHotbar(i)) return 29 + i;
        if (isMain(i)) return 2 + (i - 9);
        return -1;
    }

    // Utils

    public static boolean isHotbar(int i) {
        return i >= HOTBAR_START && i <= HOTBAR_END;
    }

    public static boolean isMain(int i) {
        return i >= MAIN_START && i <= MAIN_END;
    }

    public static boolean isArmor(int i) {
        return i >= ARMOR_START && i <= ARMOR_END;
    }
}

