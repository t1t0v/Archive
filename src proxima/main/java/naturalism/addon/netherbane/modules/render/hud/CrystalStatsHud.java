package naturalism.addon.netherbane.modules.render.hud;

import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;
import meteordevelopment.meteorclient.systems.hud.HUD;

public class CrystalStatsHud extends DoubleTextHudElement {
    static int cps;

    public CrystalStatsHud(HUD hud) {
        super(hud, "crystal-stats-hud", "(HUD) Displays your crystals per second.", "C/s: ");
    }

    @Override
    protected String getRight() {
        return Integer.toString(cps);
    }

    public static void setNumber(int index) {
        cps = index;
    }
}
