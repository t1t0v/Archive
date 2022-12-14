package genesis.team.addon.modules.movement.SpeedPlus;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class Matrix6_7_0 extends SpeedMode {
	public Matrix6_7_0() {
		super(SpeedModes.Matrix6_7_0);
	}

	private int noVelocityY = 0;

	@Override
	public void onDeactivate() {
		mc.player.airStrafingSpeed = 0.02f;
	}

	@Override
	public void onTickEventPre(TickEvent.Pre event) {
		work();
	}
	@Override
	public void onTickEventPost(TickEvent.Post event) {
		//work();
	}

	public void onReceivePacket(PacketEvent.Receive event) {
		if (event.packet instanceof EntityVelocityUpdateS2CPacket velocity) {
			if (mc.player != null && mc.world != null && mc.world.getEntityById(velocity.getId()) != null) {
				if (mc.player == mc.world.getEntityById(velocity.getId()))
					noVelocityY = 10;
			}
		}
	}

	private void work() {
		if (!mc.player.isOnGround() && noVelocityY <= 0) {
			if (mc.player.getVelocity().y > 0) {
				mc.player.getVelocity().add(0, -0.0005, 0);
			}
			mc.player.getVelocity().add(0, -0.0094001145141919810, 0);
		}
		if (!mc.player.isOnGround() && noVelocityY < 8) {
			if (MovementUtils.getSpeed() < 0.2177 && noVelocityY < 8) {
				MovementUtils.strafe(0.2177f);
			}
		}
		if (Math.abs(mc.player.airStrafingSpeed) < 0.1) {
			mc.player.airStrafingSpeed = 0.026f;
		}
		else {
			mc.player.airStrafingSpeed = 0.0247f;
		}
		if (mc.player.isOnGround() && PlayerUtils.isMoving()) {
			mc.options.jumpKey.setPressed(false);
			mc.player.jump();
			IVec3d v = (IVec3d) mc.player.getVelocity();
			v.setY(0.41050001145141919810);
			if (Math.abs(mc.player.airStrafingSpeed) < 0.1) {
				MovementUtils.strafe(MovementUtils.getSpeed());
			}
		}
		if (!PlayerUtils.isMoving()) {
			IVec3d v = (IVec3d) mc.player.getVelocity();
			v.setXZ(0, 0);
		}
	}
}
