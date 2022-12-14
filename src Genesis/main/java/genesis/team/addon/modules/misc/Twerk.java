package genesis.team.addon.modules.misc;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.advanced.TimerUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Twerk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    // General
    private final Setting<Double> twerkDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("Twerk Delay")
            .description("In Millis.")
            .defaultValue(4)
            .min(2)
            .sliderRange(2,100)
            .build()
    );


    public Twerk() {
        super(Genesis.Misc, "Tverk", "FUUUUKICNG TWEEEERK");
    }


    private boolean upp = false;
    private final TimerUtils onTwerk = new TimerUtils();


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.options.sneakKey.setPressed(upp);

        if (onTwerk.passedMillis(twerkDelay.get().longValue()) && !upp) {
            onTwerk.reset();
            upp = true;
        }

        if (onTwerk.passedMillis(twerkDelay.get().longValue()) && upp) {
            onTwerk.reset();
            upp = false;
        }

    }

    @Override
    public void onDeactivate() {
        upp = false;
        mc.options.sneakKey.setPressed(false);
        onTwerk.reset();
    }
}
