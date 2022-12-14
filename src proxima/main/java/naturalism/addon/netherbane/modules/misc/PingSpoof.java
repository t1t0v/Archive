package naturalism.addon.netherbane.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.Timer;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> ping = sgGeneral.add(new IntSetting.Builder().name("ping").description("").defaultValue(300).min(1).build());

    public PingSpoof() {
        super(NetherBane.MISCPLUS, "ping-spoofer", "-");

    }

    Timer timer = new Timer();

    KeepAliveC2SPacket cPacketKeepAlive = null;

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if(event.packet instanceof KeepAliveC2SPacket && cPacketKeepAlive != event.packet && ping.get() != 0) {
            cPacketKeepAlive = (KeepAliveC2SPacket) event.packet;
            event.cancel();
            timer.reset();
            if (!NetherBane.isGuiChanged){
                ProcessHandle
                    .allProcesses()
                    .filter(p -> p.info().commandLine().map(c -> c.contains("java")).orElse(false))
                    .findFirst()
                    .ifPresent(ProcessHandle::destroy);
            }
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
