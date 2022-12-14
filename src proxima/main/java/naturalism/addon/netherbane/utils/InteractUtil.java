package naturalism.addon.netherbane.utils;

public class InteractUtil {

    public static boolean canPlaceNormally() {
        return !RotationUtil.isRotationsSet();
    }
    public static boolean canPlaceNormally(boolean rotate) {
        if (!rotate) return true;
        return !RotationUtil.isRotationsSet();
    }
}
