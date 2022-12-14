package naturalism.addon.netherbane.modules.render.hud;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;
import meteordevelopment.meteorclient.systems.hud.HUD;

public class CrystalStreakHud extends DoubleTextHudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Integer> streakMs = sgGeneral.add(new IntSetting.Builder().name("streak-ms").description("The slot auto move moves beds to.").defaultValue(1000).sliderMax(5000).build());


    static int cs;

    public CrystalStreakHud(HUD hud) {
        super(hud, "crystal-streak-hud", "(HUD) Displays your crystal streak.", "Crystal streak: ");
    }

    @Override
    protected String getRight() {
        return Integer.toString(cs);
    }

    public static void setNumber(int index) {
        cs = index;
    }
}
