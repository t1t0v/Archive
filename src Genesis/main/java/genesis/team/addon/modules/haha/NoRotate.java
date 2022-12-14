package genesis.team.addon.modules.haha;


import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class NoRotate extends Module {
    public NoRotate() {
        super(Genesis.Misc, "no-rotate+", " ");
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> always = sgGeneral.add(new BoolSetting.Builder().name("always").defaultValue(false).build());
    public final Setting<Boolean> air = sgGeneral.add(new BoolSetting.Builder().name("fly").defaultValue(true).build());
    public final Setting<Boolean> inBlock = sgGeneral.add(new BoolSetting.Builder().name("in-block").defaultValue(false).build());


    boolean airS, alwaysS, inBlockS;

    @EventHandler
    private void onTickEvent(TickEvent.Pre event){
        alwaysS = always.get() && !mc.player.isFallFlying() && mc.world.getBlockState(mc.player.getBlockPos()).isAir();
        airS = air.get() && mc.player.isFallFlying();
        inBlockS = inBlock.get() && !mc.world.getBlockState(mc.player.getBlockPos()).isAir();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (airS) {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            }
            if (alwaysS) {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            }
            if (inBlockS) {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            }
        }
    }
}
