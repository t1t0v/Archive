package genesis.team.addon.modules.movement;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.InfoUtil.EntityInfo;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;

public class TimerFall extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public static TimerFall INSTANCE;
    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder().name("timer-speed").defaultValue(5).min(1.2).sliderRange(1.2, 15).build());
    private final Setting<Integer> shiftTicks = sgGeneral.add(new IntSetting.Builder().name("shift-ticks").defaultValue(2).min(0).sliderRange(0, 5).build());
    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder().name("fall-distance").defaultValue(2).min(0).build());


    int ticks;
    boolean jump;
    public static boolean shouldFreeze;

    public TimerFall() {
        super(Genesis.Move, "timer-fall", "Falling faster with timer.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        ticks = 0;
        jump = false;
        shouldFreeze = false;
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (shouldFreeze) mc.player.setVelocity(0, mc.player.getVelocity().getY(), 0);

        if (mc.options.jumpKey.isPressed())
            jump = true;
        else if (mc.player.isOnGround()) jump = false;

        if (!EntityInfo.isOnGround(mc.player)
                && !mc.player.isSubmergedInWater()
                && !mc.player.isInLava()
                && !mc.player.noClip
                && !jump
                && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, (float) -(fallDistance.get() + 0.01), 0.0))) {
            Modules.get().get(Timer.class).setOverride(this.timer.get());
            ticks = shiftTicks.get();
            shouldFreeze = true;
            return;
        } else
            Modules.get().get(Timer.class).setOverride(Timer.OFF);

        if (ticks != 0){
            shouldFreeze = true;
            ticks--;
        } else shouldFreeze = false;
    }
}
