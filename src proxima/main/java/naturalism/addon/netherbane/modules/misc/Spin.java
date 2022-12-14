package naturalism.addon.netherbane.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;

public class Spin extends Module {
    public Spin(){
        super(NetherBane.MISCPLUS, "spin", "");
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Integer> yawDegree = sgDefault.add(new IntSetting.Builder().name("degree").description("The delay between placing beds in ticks.").defaultValue(1).min(0).sliderMax(20).build());
    private final Setting<Integer> pitchDegree = sgDefault.add(new IntSetting.Builder().name("degree").description("The delay between placing beds in ticks.").defaultValue(1).min(0).sliderMax(20).build());

    int yaw, pitch;

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (yaw > 180){
            yaw = -180;
        }
        else {
            yaw = yaw + yawDegree.get();
        }
        if (pitch > 180){
            pitch = -180;
        }
        else {
            pitch =  pitch + pitchDegree.get();
        }

        Rotations.rotate(yaw, pitch);
    }
}
