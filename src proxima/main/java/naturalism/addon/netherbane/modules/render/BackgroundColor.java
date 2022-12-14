package naturalism.addon.netherbane.modules.render;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import naturalism.addon.netherbane.NetherBane;


public class BackgroundColor extends Module {
    public BackgroundColor(){
        super(NetherBane.RENDERPLUS, "background-color", "------------");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<SettingColor> Color1 = sgGeneral.add(new ColorSetting.Builder().name("line-color-1").description("").defaultValue(new SettingColor(255, 0, 190, 50)).build());
    public final Setting<SettingColor> Color2 = sgGeneral.add(new ColorSetting.Builder().name("line-color-2").description("").defaultValue(new SettingColor(130, 5, 185, 50)).build());
    public final Setting<Boolean> displayItem = sgGeneral.add(new BoolSetting.Builder().name("display-item").description(".").defaultValue(true).build());


}
