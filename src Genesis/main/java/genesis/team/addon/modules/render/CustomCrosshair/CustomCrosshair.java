package genesis.team.addon.modules.render.CustomCrosshair;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import java.awt.*;

public class CustomCrosshair extends Module {
    public CustomCrosshair(){
        super(Genesis.Render, "custom-crosshair", "");
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Boolean> line = sgDefault.add(new BoolSetting.Builder().name("line").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Double> lenght = sgDefault.add(new DoubleSetting.Builder().name("lenght").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(100).visible(line::get).build());
    private final Setting<Double> offset = sgDefault.add(new DoubleSetting.Builder().name("offset").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(100).visible(line::get).build());

    private final Setting<SettingColor> lineColor = sgDefault.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> lineColor2 = sgDefault.add(new ColorSetting.Builder().name("line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());

    @EventHandler
    private void onRender(Render2DEvent event){
        if (!mc.options.getPerspective().isFirstPerson()) return;
        int centerX = mc.getWindow().getScaledWidth() / 2;
        int centerY = mc.getWindow().getScaledHeight() / 2;

        if (line.get()){
            Renderer.gradientLineScreen(new Color(lineColor.get().r, lineColor.get().g, lineColor.get().b, lineColor.get().a), new Color(lineColor2.get().r, lineColor2.get().g, lineColor2.get().b, lineColor2.get().a), centerX + offset.get(), centerY, centerX + lenght.get(), centerY);
            Renderer.gradientLineScreen(new Color(lineColor.get().r, lineColor.get().g, lineColor.get().b, lineColor.get().a), new Color(lineColor2.get().r, lineColor2.get().g, lineColor2.get().b, lineColor2.get().a), centerX - offset.get(), centerY, centerX - lenght.get(), centerY);
            Renderer.gradientLineScreen(new Color(lineColor.get().r, lineColor.get().g, lineColor.get().b, lineColor.get().a), new Color(lineColor2.get().r, lineColor2.get().g, lineColor2.get().b, lineColor2.get().a), centerX, centerY + offset.get(), centerX, centerY + lenght.get());
            Renderer.gradientLineScreen(new Color(lineColor.get().r, lineColor.get().g, lineColor.get().b, lineColor.get().a), new Color(lineColor2.get().r, lineColor2.get().g, lineColor2.get().b, lineColor2.get().a), centerX, centerY - offset.get(), centerX, centerY - lenght.get());
        }
    }
}
