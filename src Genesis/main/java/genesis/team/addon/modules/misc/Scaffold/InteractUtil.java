package genesis.team.addon.modules.misc.Scaffold;

import genesis.team.addon.util.ProximaUtil.RotationUtil;

public class InteractUtil {

    public static boolean canPlaceNormally() {
        return !RotationUtil.isRotationsSet();
    }
    public static boolean canPlaceNormally(boolean rotate) {
        if (!rotate) return true;
        return !RotationUtil.isRotationsSet();
    }
}
