package naturalism.addon.netherbane.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.events.PlayerMoveEvent;
import naturalism.addon.netherbane.events.UpdateWalkingPlayerEvent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.shape.VoxelShape;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.PlayerUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


public class AirStrafe extends Module {
    public AirStrafe(){
        super(NetherBane.MOVEMENTPLUS, "air-strafe", "");
    }

    private double currentSpeed = 0.0D;
    private double prevMotion = 0.0D;
    private boolean oddStage = false;
    private int state = 4;

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (mc.player.isOnGround() || mc.player.isFallFlying() || mc.player.getAbilities().flying) return;
        if (state != 1 || (mc.player.forwardSpeed == 0.0f || mc.player.sidewaysSpeed == 0.0f)) {
            if (state == 2 && (mc.player.forwardSpeed != 0.0f || mc.player.sidewaysSpeed != 0.0f)) {
                currentSpeed *= oddStage ? 1.6835D : 1.395D;
            } else if (state == 3) {
                double adjustedMotion = 0.66D * (prevMotion - getBaseMotionSpeed());
                currentSpeed = prevMotion - adjustedMotion;
                oddStage = !oddStage;
            } else {
                List<VoxelShape> collisionBoxes = mc.world.getEntityCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0));
                if ((collisionBoxes.size() > 0 || mc.player.verticalCollision) && state > 0) {
                    state = mc.player.forwardSpeed == 0.0f && mc.player.sidewaysSpeed == 0.0f ? 0 : 1;
                }
                currentSpeed = prevMotion - prevMotion / 159.0;
            }
        } else {
            currentSpeed = 1.35D * getBaseMotionSpeed() - 0.01D;
        }

        currentSpeed = Math.max(currentSpeed, getBaseMotionSpeed());

        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0.0D && strafe == 0.0D) {
            event.setX(0.0D);
            event.setZ(0.0D);
        } else {
            if (forward != 0.0D) {

                if (strafe > 0.0D) {
                    yaw += (float)(forward > 0.0D ? -45 : 45);
                } else if (strafe < 0.0D) {
                    yaw += (float)(forward > 0.0D ? 45 : -45);
                }

                strafe = 0.0D;

                if (forward > 0.0D) {
                    forward = 1.0D;
                } else if (forward < 0.0D) {
                    forward = -1.0D;
                }
            }

            event.setX(forward * currentSpeed * Math.cos(Math.toRadians(yaw + 90.0F)) + strafe * currentSpeed * Math.sin(Math.toRadians(yaw + 90.0F)));
            event.setZ(forward * currentSpeed * Math.sin(Math.toRadians(yaw + 90.0F)) - strafe * currentSpeed * Math.cos(Math.toRadians(yaw + 90.0F)));
        }


        if (mc.player.forwardSpeed == 0.0f && mc.player.sidewaysSpeed == 0.0f) {
            return;
        }

        state++;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerMoveC2SPacket.LookAndOnGround) {
            //KonasGlobals.INSTANCE.timerManager.resetTimer(this);
            currentSpeed = 0.0D;
            state = 4;
            prevMotion = 0;
        }
    }

    private double getBaseMotionSpeed() {
        double baseSpeed = 0.2873D;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0D + 0.2D * ((double) amplifier + 1);
        }
        return baseSpeed;
    }

    private double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        state = 4;
        currentSpeed = getBaseMotionSpeed();
        prevMotion = 0;
    }

    @EventHandler
    public void onPlayerWalkingUpdate(UpdateWalkingPlayerEvent.Pre event) {
        if (!PlayerUtil.isPlayerMoving()) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            currentSpeed = 0.0;
            return;
        }
        double dX = mc.player.getX() - mc.player.prevX;
        double dZ = mc.player.getZ() - mc.player.prevZ;
        prevMotion = Math.sqrt(dX * dX + dZ * dZ);
    }
}
