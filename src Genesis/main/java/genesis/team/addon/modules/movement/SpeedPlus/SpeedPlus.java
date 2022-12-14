package genesis.team.addon.modules.movement.SpeedPlus;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class SpeedPlus extends Module {
	private final SettingGroup sgGeneral = settings.getDefaultGroup();

	public SpeedPlus() {
		super(Genesis.Move, "speed-plus", "Bypass speed");
		onSpeedModeChanged(speedMode.get());
	}

	public final Setting<SpeedModes> speedMode = sgGeneral.add(new EnumSetting.Builder<SpeedModes>()
		.name("mode")
		.description("The method of applying speed.")
		.defaultValue(SpeedModes.MatrixExploit)
		.onModuleActivated(spiderModesSetting -> onSpeedModeChanged(spiderModesSetting.get()))
		.onChanged(this::onSpeedModeChanged)
		.build()
	);

	public final Setting<Double> speedMatrix = sgGeneral.add(new DoubleSetting.Builder()
		.name("Speed")
		.description("Speed.")
		.defaultValue(4)
		.visible(() -> speedMode.get() == SpeedModes.MatrixExploit || speedMode.get() == SpeedModes.MatrixExploit2)
		.build()
	);

	public final Setting<Double> speedVulcanef2 = sgGeneral.add(new DoubleSetting.Builder()
		.name("Speed-effect-2")
		.description("Speed 2 effect.")
		.defaultValue(45)
		.max(75)
		.sliderRange(0, 75)
		.visible(() -> speedMode.get() == SpeedModes.Vulcan)
		.build()
	);

	public final Setting<Double> speedVulcanef1 = sgGeneral.add(new DoubleSetting.Builder()
		.name("Speed-effect-1")
		.description("Speed 1 effect.")
		.defaultValue(45)
		.max(75)
		.sliderRange(0, 75)
		.visible(() -> speedMode.get() == SpeedModes.Vulcan)
		.build()
	);

	public final Setting<Double> speedVulcanef0 = sgGeneral.add(new DoubleSetting.Builder()
		.name("Speed-effect-0")
		.description("Speed 0 effect.")
		.defaultValue(35)
		.max(75)
		.sliderRange(0, 75)
		.visible(() -> speedMode.get() == SpeedModes.Vulcan)
		.build()
	);


	public final Setting<Boolean> autoSwapVulcan = sgGeneral.add(new BoolSetting.Builder()
		.name("auto-swap")
		.description("Auto swap.")
		.defaultValue(true)
		.visible(() -> speedMode.get() == SpeedModes.Vulcan)
		.build()
	);

	private SpeedMode currentMode;

	@Override
	public void onActivate() {
		currentMode.onActivate();
	}

	@Override
	public void onDeactivate() {
		currentMode.onDeactivate();
	}

	@EventHandler
	private void onPreTick(TickEvent.Pre event) {
		currentMode.onTickEventPre(event);
	}

	@EventHandler
	private void onPostTick(TickEvent.Post event) {
		currentMode.onTickEventPost(event);
	}
	@EventHandler
	public void onSendPacket(PacketEvent.Send event) {
		currentMode.onSendPacket(event);
	}
	@EventHandler
	public void onSentPacket(PacketEvent.Sent event) {
		currentMode.onSentPacket(event);
	}

	@EventHandler
	private void onPlayerMoveEvent(PlayerMoveEvent event) {
		currentMode.onPlayerMoveEvent(event);
	}


	private void onSpeedModeChanged(SpeedModes mode) {
		switch (mode) {
			case MatrixExploit2 -> currentMode = new MatrixExploit2();
			case MatrixExploit -> currentMode = new MatrixExploit();
			case Matrix6_7_0 -> currentMode = new Matrix6_7_0();
			case Matrix -> currentMode = new Matrix();
			case AACHop438 -> currentMode = new AACHop438();
			case Vulcan -> currentMode = new Vulcan();
			case NCPHop -> currentMode = new NCPHop();
		}
	}
}
