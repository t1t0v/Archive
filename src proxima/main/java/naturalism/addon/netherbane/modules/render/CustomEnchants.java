package naturalism.addon.netherbane.modules.render;

import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import naturalism.addon.netherbane.NetherBane;

public class CustomEnchants extends Module {
    public CustomEnchants(){
        super(NetherBane.RENDERPLUS, "custom-enchants", "");
    }
    private final SettingGroup sgDefault = settings.createGroup("Default");
    public final Setting<SettingColor> color = sgDefault.add(new ColorSetting.Builder().name("color-1").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
}
