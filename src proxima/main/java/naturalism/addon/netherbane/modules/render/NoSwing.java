package naturalism.addon.netherbane.modules.render;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.events.SwingHandEvent;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import naturalism.addon.netherbane.NetherBane;

public class NoSwing extends Module {
    public NoSwing(){
        super(NetherBane.RENDERPLUS, "no-swing", "*");
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Boolean> client = sgDefault.add(new BoolSetting.Builder().name("client").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> server = sgDefault.add(new BoolSetting.Builder().name("server").description("Automatically rotates you towards the city block.").defaultValue(false).build());

    @EventHandler
    private void onSwing(SwingHandEvent event){
        if (client.get()) event.setCancelled(true);
    }

    @EventHandler
    private void onPacket(PacketEvent.Send event){
        if (event.packet instanceof HandSwingC2SPacket && server.get()){
            event.setCancelled(true);
        }
    }

}
