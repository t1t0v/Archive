package genesis.team.addon.modules.movement.SpeedPlus;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;

public class NCPHop extends SpeedMode {
	public NCPHop() {
		super(SpeedModes.NCPHop);
	}

	@Override
	public void onActivate() {
		Modules.get().get(Timer.class).setOverride(1.0865f);
	}

	@Override
	public void onDeactivate() {
		Modules.get().get(Timer.class).setOverride(Timer.OFF);
		mc.player.airStrafingSpeed = 0.02f;
	}

	@Override
	public void onTickEventPre(TickEvent.Pre event) {
		if (mc.player.isTouchingWater() || mc.player.isInLava() ||
			mc.player.isClimbing() || mc.player.isRiding()) return;
		Timer timer = Modules.get().get(Timer.class);
		if (PlayerUtils.isMoving() && mc.player.isOnGround()) {
			mc.player.jump();
			mc.player.airStrafingSpeed = 0.0223f;
		}
		else {
			timer.setOverride(1);
		}
	}
}
