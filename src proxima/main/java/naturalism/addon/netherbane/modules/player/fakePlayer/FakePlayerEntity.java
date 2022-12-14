package naturalism.addon.netherbane.modules.player.fakePlayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FakePlayerEntity extends OtherClientPlayerEntity {
    public FakePlayerEntity(PlayerEntity player, String name, float health, boolean copyInv) {
        super(mc.world, new GameProfile(UUID.randomUUID(), name));

        copyPositionAndRotation(player);

        prevYaw = getYaw();
        prevPitch = getPitch();
        headYaw = player.headYaw;
        prevHeadYaw = headYaw;
        bodyYaw = player.bodyYaw;
        prevBodyYaw = bodyYaw;

        Byte playerModel = player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
        dataTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);

        getAttributes().setFrom(player.getAttributes());
        setPose(player.getPose());

        capeX = getX();
        capeY = getY();
        capeZ = getZ();

        if (health <= 20) {
            setHealth(health);
        } else {
            setHealth(health);
            setAbsorptionAmount(health - 20);
        }

        if (copyInv) getInventory().clone(player.getInventory());
    }

    public void updatePosAndLook(float yaw, float pitch, float headYaw, float bodyYaw, Vec3d vec3d, double x, double y, double z, boolean sneaking, boolean swimming){
        this.updatePosition(vec3d.add(x, y, z).x,vec3d.add(x, y, z).y, vec3d.add(x, y, z).z);
        this.setHeadYaw(headYaw);
        this.setBodyYaw(bodyYaw);
        this.setSneaking(sneaking);
        this.setSwimming(swimming);
        this.setYaw(yaw);
        this.setPitch(pitch);
    }

    public void spawn() {
        unsetRemoved();
        mc.world.addEntity(getId(), this);
    }

    public void despawn() {
        mc.world.removeEntity(getId(), RemovalReason.DISCARDED);
        setRemoved(RemovalReason.DISCARDED);
    }

}
