package naturalism.addon.netherbane.modules.render.hud;

import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.Identifier;

public class LogoHud extends HudElement {
    private static final Identifier LOGO_FLAT = new Identifier("addon", "logo/logo_flat.png");

    private static final RainbowColor RAINBOW = new RainbowColor();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("scale").description("The scale.").defaultValue(2).min(1).sliderMin(1).sliderMax(5).build());
    public final Setting<Boolean> rainbow = sgGeneral.add(new BoolSetting.Builder().name("rainbow").description("Rainbow logo animation.").defaultValue(false).build());
    private final Setting<Double> rainbowSpeed = sgGeneral.add(new DoubleSetting.Builder().name("rainbow-speed").description("Speed of the chroma animation.").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder().name("background-color").description("Color of the background.").defaultValue(new SettingColor(255, 255, 255)).build());

    public LogoHud(HUD hud) {
        super(hud, "NetherBane-logo", "Displays the NetherBane logo.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(100 * scale.get(), 100 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;
        double x = box.getX();
        double y = box.getY();
        int w = (int) box.width;
        int h = (int) box.height;
        GL.bindTexture(LOGO_FLAT);
        Renderer2D.TEXTURE.begin();
        if (rainbow.get()) {
            RAINBOW.setSpeed(rainbowSpeed.get() / 100);
            Renderer2D.TEXTURE.texQuad(x, y, w, h, RAINBOW.getNext());
        } else {
            Renderer2D.TEXTURE.texQuad(x, y, w, h, color.get());
        }
        Renderer2D.TEXTURE.render(null);
    }
}
