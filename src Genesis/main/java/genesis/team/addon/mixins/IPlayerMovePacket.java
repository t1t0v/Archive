package genesis.team.addon.mixins;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface IPlayerMovePacket {
    @Accessor(value = "y")
    void setY(double y);

    @Accessor(value = "y")
    double getY();
}
