package naturalism.addon.netherbane.mixins;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import naturalism.addon.netherbane.events.*;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(value = ClientPlayerEntity.class, priority = Integer.MAX_VALUE)
public abstract class MixinClientPlayerEntity extends PlayerEntity {
    @Shadow
    private ClientPlayNetworkHandler networkHandler;

    public MixinClientPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void onMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        ci.cancel();

        PlayerMoveEvent event = MeteorClient.EVENT_BUS.post(PlayerMoveEvent.get(type, movement.x, movement.y, movement.z));

        if (!event.isCancelled()) {
            super.move(event.getType(), new Vec3d(event.getX(), event.getY(), event.getZ()));
        }
    }

    @Inject(method = "sendAbilitiesUpdate", at = @At("HEAD"))
    public void onPlayerUpdate(CallbackInfo info) {
        PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent();
        MeteorClient.EVENT_BUS.post(playerUpdateEvent);
    }

    /**
     * @author
     */
    @Overwrite
    public void swingHand(Hand hand) {
        SwingHandEvent event = new SwingHandEvent(hand);
        MeteorClient.EVENT_BUS.post(event);
        if (!event.isCancelled()) {
            super.swingHand(event.getHand());
        }
        networkHandler.sendPacket(new HandSwingC2SPacket(hand));
    }

    @Inject(at = @At("RETURN"), method = "tick()V", cancellable = true)
    public void tick(CallbackInfo info) {
        EventTick event = new EventTick();
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled())
            info.cancel();
    }
    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPackets(CallbackInfo ci) {
        UpdateWalkingPlayerEvent postEvent = UpdateWalkingPlayerEvent.Post.get(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
        MeteorClient.EVENT_BUS.post(postEvent);
    }
}
