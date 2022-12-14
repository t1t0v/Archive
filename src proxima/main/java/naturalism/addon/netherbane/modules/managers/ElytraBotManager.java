package naturalism.addon.netherbane.modules.managers;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.modules.bots.elytrabot.Flight;

public class ElytraBotManager extends Module {
    public ElytraBotManager(){
        super(NetherBane.MANAGERS, "elytra-bot-maanger", "");
    }



    private final SettingGroup sgFlight = settings.createGroup("Flight");


    private final Setting<Integer> selectedYaw= sgFlight.add(new IntSetting.Builder().name("selected-yaw").description("The delay between breaks.").defaultValue(0).min(-180).sliderMax(180).build());
    private final Setting<Flight.FlightMode> mode = sgFlight.add(new EnumSetting.Builder<Flight.FlightMode>().name("mode").description("Block breaking method").defaultValue(Flight.FlightMode.Pitch40).build());
}
