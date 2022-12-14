package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;

public class AntiPistonPush extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> spoof = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof")
        .description("Spoofs your position to bypass horizontal piston pushing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sendTeleport = sgGeneral.add(new BoolSetting.Builder()
        .name("send-teleport")
        .description("Sends a teleport confirm packet when spoofing.")
        .defaultValue(true)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> invalidPacket = sgGeneral.add(new BoolSetting.Builder()
        .name("send-teleport")
        .description("Sends invalid position packets to bypass some anti cheats.")
        .defaultValue(true)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> updatePosition = sgGeneral.add(new BoolSetting.Builder()
        .name("update-Position")
        .description("")
        .defaultValue(true)
        .visible(spoof::get)
        .build()
    );


    private final HashSet<PlayerMoveC2SPacket> packets = new HashSet<>();
    private int teleportID = 0;

    public AntiPistonPush() {
        super(Genesis.Combat, "anti-piston-push", "Prevents you from being pushed by pistons.");
    }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event) {
        if (spoof.get()) {
            Vec3d velocity = mc.player.getVelocity();

            if (updatePosition.get()) {
                mc.player.setVelocity(0.0D, mc.player.getVelocity().getY(), 0.0D);
                velocity = new Vec3d(0.0D, mc.player.getVelocity().getY(), 0.0D);
            }

            sendPackets(velocity.x, velocity.y, velocity.z, sendTeleport.get());
        }
    }

    private void sendPackets(double x, double y, double z, boolean teleport) {
        Vec3d pos = mc.player.getPos().add(x, y, z);

        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, mc.player.isOnGround()));

        mc.player.setPos(pos.x, pos.y, pos.z);
        if (teleport) mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportID++));
    }


    private void sendPacket(PlayerMoveC2SPacket packet) {
        packets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }
}