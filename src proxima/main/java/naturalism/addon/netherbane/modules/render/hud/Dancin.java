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

public class Dancin extends HudElement {
    public Dancin(HUD hud) {
        super(hud, "dancin", "dance", true);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
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

    Identifier TEXTURE = new Identifier("addon", "danceanimation/frame1.png");

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(100 * scale.get(), 100 * scale.get());
        if (timer.hasPassed(updateDelay.get())) TEXTURE = new Identifier("addon", "danceanimation/frame1.png");
        if (timer.hasPassed(updateDelay.get() * 2)) TEXTURE = new Identifier("addon", "danceanimation/frame2.png");
        if (timer.hasPassed(updateDelay.get() * 3)) TEXTURE = new Identifier("addon", "danceanimation/frame3.png");
        if (timer.hasPassed(updateDelay.get() * 4)) TEXTURE = new Identifier("addon", "danceanimation/frame4.png");
        if (timer.hasPassed(updateDelay.get() * 5)) TEXTURE = new Identifier("addon", "danceanimation/frame5.png");
        if (timer.hasPassed(updateDelay.get() * 6)) TEXTURE = new Identifier("addon", "danceanimation/frame6.png");
        if (timer.hasPassed(updateDelay.get() * 7)) TEXTURE = new Identifier("addon", "danceanimation/frame7.png");
        if (timer.hasPassed(updateDelay.get() * 8)) TEXTURE = new Identifier("addon", "danceanimation/frame8.png");
        if (timer.hasPassed(updateDelay.get() * 9)) TEXTURE = new Identifier("addon", "danceanimation/frame9.png");
        if (timer.hasPassed(updateDelay.get() * 10)) TEXTURE = new Identifier("addon", "danceanimation/frame10.png");
        if (timer.hasPassed(updateDelay.get() * 11)) TEXTURE = new Identifier("addon", "danceanimation/frame11.png");
        if (timer.hasPassed(updateDelay.get() * 12)) TEXTURE = new Identifier("addon", "danceanimation/frame12.png");
        if (timer.hasPassed(updateDelay.get() * 13)) TEXTURE = new Identifier("addon", "danceanimation/frame13.png");
        if (timer.hasPassed(updateDelay.get() * 14)) timer.reset();
    }

    @Override
    public void render(HudRenderer renderer) {
        GL.bindTexture(TEXTURE);
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(box.getX(), box.getY(), box.width, box.height, WHITE);
        Renderer2D.TEXTURE.render(null);
    }
}
