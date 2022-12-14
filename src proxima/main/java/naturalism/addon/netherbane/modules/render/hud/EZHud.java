package naturalism.addon.netherbane.modules.render.hud;

import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import net.minecraft.util.Identifier;
import naturalism.addon.netherbane.utils.Timer;

import static meteordevelopment.meteorclient.utils.Utils.WHITE;

public class EZHud extends HudElement {

    public EZHud(HUD hud) {
        super(hud, "netherbane-ez-hud", "-", false);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Integer> ticks = sgGeneral.add(new IntSetting.Builder().name("ticks").description("The delay to remove messages").defaultValue(60).min(1).sliderMax(2000).build());
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );
    private final Setting<Integer> updateDelay = sgGeneral.add(new IntSetting.Builder().name("update-delay").description("The delay to remove messages").defaultValue(200).min(1).sliderMax(2000).build());

    private Timer timer = new Timer();

    public static boolean isEz;
    public static int tick;

    Identifier TEXTURE = new Identifier("addon", "dancecat/0.png");

    @Override
    public void update(HudRenderer renderer) {
        tick = ticks.get();
        box.setSize(100 * scale.get(), 100 * scale.get());

        if (timer.hasPassed(updateDelay.get())) TEXTURE = new Identifier("addon", "dancecat/1.png");
        if (timer.hasPassed(updateDelay.get() * 2)) TEXTURE = new Identifier("addon", "dancecat/2.png");
            if (timer.hasPassed(updateDelay.get() * 3)) TEXTURE = new Identifier("addon", "dancecat/3.png");
            if (timer.hasPassed(updateDelay.get() * 4)) TEXTURE = new Identifier("addon", "dancecat/4.png");
            if (timer.hasPassed(updateDelay.get() * 5)) TEXTURE = new Identifier("addon", "dancecat/5.png");
            if (timer.hasPassed(updateDelay.get() * 6)) TEXTURE = new Identifier("addon", "dancecat/6.png");
            if (timer.hasPassed(updateDelay.get() * 7)) TEXTURE = new Identifier("addon", "dancecat/7.png");
            if (timer.hasPassed(updateDelay.get() * 8)) TEXTURE = new Identifier("addon", "dancecat/8.png");
            if (timer.hasPassed(updateDelay.get() * 9)) TEXTURE = new Identifier("addon", "dancecat/9.png");
            if (timer.hasPassed(updateDelay.get() * 10)) TEXTURE = new Identifier("addon", "dancecat/10.png");
            if (timer.hasPassed(updateDelay.get() * 11)) TEXTURE = new Identifier("addon", "dancecat/11.png");
            if (timer.hasPassed(updateDelay.get() * 12)) TEXTURE = new Identifier("addon", "dancecat/12.png");
            if (timer.hasPassed(updateDelay.get() * 13)) TEXTURE = new Identifier("addon", "dancecat/13.png");
            if (timer.hasPassed(updateDelay.get() * 14)) TEXTURE = new Identifier("addon", "dancecat/14.png");
            if (timer.hasPassed(updateDelay.get() * 15)) TEXTURE = new Identifier("addon", "dancecat/15.png");
            if (timer.hasPassed(updateDelay.get() * 16)) TEXTURE = new Identifier("addon", "dancecat/16.png");
            if (timer.hasPassed(updateDelay.get() * 17)) TEXTURE = new Identifier("addon", "dancecat/17.png");
            if (timer.hasPassed(updateDelay.get() * 18)) TEXTURE = new Identifier("addon", "dancecat/18.png");
            if (timer.hasPassed(updateDelay.get() * 19)) TEXTURE = new Identifier("addon", "dancecat/19.png");
            if (timer.hasPassed(updateDelay.get() * 20)) TEXTURE = new Identifier("addon", "dancecat/20.png");
            if (timer.hasPassed(updateDelay.get() * 21)) TEXTURE = new Identifier("addon", "dancecat/21.png");
            if (timer.hasPassed(updateDelay.get() * 22)) TEXTURE = new Identifier("addon", "dancecat/22.png");
            if (timer.hasPassed(updateDelay.get() * 23)) TEXTURE = new Identifier("addon", "dancecat/23.png");
            if (timer.hasPassed(updateDelay.get() * 24)) TEXTURE = new Identifier("addon", "dancecat/24.png");
            if (timer.hasPassed(updateDelay.get() * 25)) TEXTURE = new Identifier("addon", "dancecat/25.png");
            if (timer.hasPassed(updateDelay.get() * 26)) TEXTURE = new Identifier("addon", "dancecat/26.png");
            if (timer.hasPassed(updateDelay.get() * 27)) TEXTURE = new Identifier("addon", "dancecat/27.png");
            if (timer.hasPassed(updateDelay.get() * 28)) TEXTURE = new Identifier("addon", "dancecat/28.png");
            if (timer.hasPassed(updateDelay.get() * 29)) TEXTURE = new Identifier("addon", "dancecat/29.png");
            if (timer.hasPassed(updateDelay.get() * 30)) TEXTURE = new Identifier("addon", "dancecat/30.png");
            if (timer.hasPassed(updateDelay.get() * 31)) TEXTURE = new Identifier("addon", "dancecat/31.png");
            if (timer.hasPassed(updateDelay.get() * 32)) TEXTURE = new Identifier("addon", "dancecat/32.png");
            if (timer.hasPassed(updateDelay.get() * 33)) TEXTURE = new Identifier("addon", "dancecat/33.png");
            if (timer.hasPassed(updateDelay.get() * 34)) TEXTURE = new Identifier("addon", "dancecat/34.png");
            if (timer.hasPassed(updateDelay.get() * 35)) TEXTURE = new Identifier("addon", "dancecat/35.png");
            if (timer.hasPassed(updateDelay.get() * 36)) TEXTURE = new Identifier("addon", "dancecat/36.png");
            if (timer.hasPassed(updateDelay.get() * 37)) TEXTURE = new Identifier("addon", "dancecat/37.png");
            if (timer.hasPassed(updateDelay.get() * 38)) TEXTURE = new Identifier("addon", "dancecat/38.png");
            if (timer.hasPassed(updateDelay.get() * 39)) TEXTURE = new Identifier("addon", "dancecat/39.png");
            if (timer.hasPassed(updateDelay.get() * 40)) TEXTURE = new Identifier("addon", "dancecat/40.png");
            if (timer.hasPassed(updateDelay.get() * 41)) timer.reset();

    }

    @Override
    public void render(HudRenderer renderer) {
        if (isEz){
            GL.bindTexture(TEXTURE);
            Renderer2D.TEXTURE.begin();
            Renderer2D.TEXTURE.texQuad(box.getX(), box.getY(), box.width, box.height, WHITE);
            Renderer2D.TEXTURE.render(null);
        }
    }
}
