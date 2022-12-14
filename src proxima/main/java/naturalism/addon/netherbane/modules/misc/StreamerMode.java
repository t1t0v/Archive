package naturalism.addon.netherbane.modules.misc;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;

public class StreamerMode extends Module {
    public StreamerMode(){
        super(NetherBane.MISCPLUS, "streamer-mode", "");
    }

    public enum CoordinatesMode{
        Obf,
        Zero,
        Custom
    }

    private final SettingGroup sgCoordinates = settings.createGroup("Coordinates");
    public final Setting<CoordinatesMode>  coordinatesMode = sgCoordinates.add(new EnumSetting.Builder<CoordinatesMode>().name("coordinates-mode").description("Block breaking method").defaultValue(CoordinatesMode.Zero).build());

    public final Setting<Integer> x = sgCoordinates.add(new IntSetting.Builder().name("x").description("The delay between breaks.").defaultValue(0).min(0).sliderMax(20).visible(() -> coordinatesMode.get() == CoordinatesMode.Custom).build());
    public final Setting<Integer> y = sgCoordinates.add(new IntSetting.Builder().name("y").description("The delay between breaks.").defaultValue(0).min(0).sliderMax(20).visible(() -> coordinatesMode.get() == CoordinatesMode.Custom).build());
    public final Setting<Integer> z = sgCoordinates.add(new IntSetting.Builder().name("z").description("The delay between breaks.").defaultValue(0).min(0).sliderMax(20).visible(() -> coordinatesMode.get() == CoordinatesMode.Custom).build());

}
