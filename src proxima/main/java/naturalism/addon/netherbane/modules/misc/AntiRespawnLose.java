package naturalism.addon.netherbane.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;

public class AntiRespawnLose extends Module {
    public AntiRespawnLose(){
        super(NetherBane.MISCPLUS, "anti-respawn-plus", "");
    }

    @EventHandler
    private void onPacket(PacketEvent.Send event){
        if (event.packet instanceof PlayerInteractBlockC2SPacket playerInteractBlockC2SPacket){
            if (mc.world.getDimension().isRespawnAnchorWorking() && BlockUtil.getBlock(playerInteractBlockC2SPacket.getBlockHitResult().getBlockPos()) == Blocks.RESPAWN_ANCHOR) event.cancel();
            if (mc.world.getDimension().isBedWorking() && BlockUtil.getBlock(playerInteractBlockC2SPacket.getBlockHitResult().getBlockPos()) instanceof BedBlock) event.cancel();
        }
    }
}
