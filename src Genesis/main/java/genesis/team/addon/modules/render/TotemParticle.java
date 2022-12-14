package genesis.team.addon.modules.render;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.Vec3d;

public class TotemParticle extends Module {
    public TotemParticle(){
        super(Genesis.Combat, "totem-particle", "");
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");

    private final Setting<SettingColor> color1 = sgDefault.add(new ColorSetting.Builder().name("color-1").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> color2 = sgDefault.add(new ColorSetting.Builder().name("color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<Boolean> rainbow = sgDefault.add(new BoolSetting.Builder().name("rainbow").description("Automatically toggles off after activation.").defaultValue(true).build());

    public Vec3d getColorOne(){
        return getDoubleVectorColor(color1.get());
    }

    public Vec3d getColorTwo(){
        return getDoubleVectorColor(color2.get());
    }

    public Vec3d getRainbowColor(){
        return getDoubleVectorColor(new Color(rainbowColor()));
    }

    public static Vec3d getDoubleVectorColor(Color color) {
        return new Vec3d((double) color.r / 255, (double) color.g / 255, (double) color.b / 255);
    }

    public Boolean isRainbow(){
        return rainbow.get();
    }

    public static java.awt.Color rainbowColor(){
        return java.awt.Color.decode(String.valueOf(rainbowHEX()));
    }

    //ColorUtil.rainbowHEX(0.8f, 0.5f, 10, 0); <- use for hud
    public static int rainbowHEX(){
        double speed = 10;
        float saturation = 255;
        float brightness = 255;

        double rainbowState = Math.ceil((System.currentTimeMillis()) / speed);
        rainbowState %= 360.0;
        return java.awt.Color.HSBtoRGB((float) (rainbowState / 360.0), saturation,  brightness);
    }

    public static int RGBtoHEX(int r, int g, int b){
        java.awt.Color color = new java.awt.Color(r,g,b);
        String hex = Integer.toHexString(color.getRGB()).substring(2);
        return Integer.parseInt(hex,16);
    }
}
