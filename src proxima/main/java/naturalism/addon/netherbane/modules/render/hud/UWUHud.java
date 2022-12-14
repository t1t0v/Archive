package naturalism.addon.netherbane.modules.render.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import naturalism.addon.netherbane.utils.Timer;

public class UWUHud extends HudElement {
    public UWUHud(HUD hud) {
        super(hud, "netherbane-hud", "NetherBane", false);
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> updateDelay = sgGeneral.add(new IntSetting.Builder().name("update-delay").description("The delay to remove messages").defaultValue(200).min(1).sliderMax(2000).build());
    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder().name("text-color").description("text-color").defaultValue(new SettingColor(255, 255, 255, 255)).build());
    private final Setting<Boolean> write = sgGeneral.add(new BoolSetting.Builder().name("write").description("Hold city pos.").defaultValue(true).build());

    public StringBuilder mes = new StringBuilder();
    private Timer timer = new Timer();

    @Override
    public void update(HudRenderer renderer) {
        if (write.get()){
            if (timer.hasPassed(updateDelay.get())) mes = new StringBuilder("N_");
            if (timer.hasPassed(updateDelay.get() * 2)) mes = new StringBuilder("Ne_");
            if (timer.hasPassed(updateDelay.get() * 3)) mes = new StringBuilder("Net_");
            if (timer.hasPassed(updateDelay.get() * 4)) mes = new StringBuilder("Neth_");
            if (timer.hasPassed(updateDelay.get() * 5)) mes = new StringBuilder("Nethe_");
            if (timer.hasPassed(updateDelay.get() * 6)) mes = new StringBuilder("Nether_");
            if (timer.hasPassed(updateDelay.get() * 7)) mes = new StringBuilder("NetherB_");
            if (timer.hasPassed(updateDelay.get() * 8)) mes = new StringBuilder("NetherBa_");
            if (timer.hasPassed(updateDelay.get() * 8)) mes = new StringBuilder("NetherBan_");
            if (timer.hasPassed(updateDelay.get() * 8)) mes = new StringBuilder("NetherBane");
            if (timer.hasPassed(updateDelay.get() * 9)) timer.reset();
        }else {
            mes = new StringBuilder("NetherBane");
        }
        box.setSize(70, 20);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mes != null){
            renderer.text(mes.toString(), box.getX(), box.getY(), textColor.get());
        }
    }
}
