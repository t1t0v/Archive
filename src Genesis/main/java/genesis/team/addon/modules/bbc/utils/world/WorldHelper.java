package genesis.team.addon.modules.bbc.utils.world;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WorldHelper {
    public static boolean canUpdate() {
        return mc != null && mc.world != null && mc.player != null;
    }
}
