package genesis.team.addon.modules.misc;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.Timer;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> ping = sgGeneral.add(new IntSetting.Builder().name("ping").description("").defaultValue(300).min(1).build());

    public PingSpoof() {
        super(Genesis.Misc, "ping-spoofer", "-");

    }

    Timer timer = new Timer();

    KeepAliveC2SPacket cPacketKeepAlive = null;

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if(event.packet instanceof KeepAliveC2SPacket && cPacketKeepAlive != event.packet && ping.get() != 0) {
            cPacketKeepAlive = (KeepAliveC2SPacket) event.packet;
            event.cancel();
            timer.reset();
            }
        }

    @EventHandler
    public void onUpdate(Render3DEvent event) {
        if(timer.hasPassed(ping.get()) && cPacketKeepAlive != null) {
            mc.player.networkHandler.sendPacket(cPacketKeepAlive);
            cPacketKeepAlive = null;
        }
    }
}
