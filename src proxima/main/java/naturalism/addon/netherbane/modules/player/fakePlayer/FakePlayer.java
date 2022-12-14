package naturalism.addon.netherbane.modules.player.fakePlayer;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;

public class FakePlayer extends Module {
    public FakePlayer(){
        super(NetherBane.MISCPLUS, "fake-vimer", "-");

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the fake player.")
        .defaultValue("seasnail8169")
        .build()
    );

    public final Setting<Boolean> copyInv = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-inv")
        .description("Copies your exact inventory to the fake player.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("The fake player's default health.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private boolean startRecord;
    private boolean startLoad;

    @Override
    public void onActivate() {
        FakePlayerManager.clear();
    }

    @Override
    public void onDeactivate() {
        FakePlayerManager.clear();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList w = theme.horizontalList();

        WButton spawn = w.add(theme.button("Spawn")).widget();
        WButton record = w.add(theme.button("Record")).widget();
        WButton stop = w.add(theme.button("Stop")).widget();
        WButton load = w.add(theme.button("Load")).widget();
        spawn.action = () -> {
            if (isActive()) FakePlayerManager.add(name.get(), health.get(), copyInv.get());
        };

        record.action = () -> startRecord = true;
        stop.action = () -> startRecord = false;
        load.action = () -> {
            startLoad = true;
            startRecord = false;
        };

        WButton clear = w.add(theme.button("Clear")).widget();
        clear.action = () -> {
            if (isActive()) FakePlayerManager.clear();
            startLoad = false;
        };

        return w;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (startRecord){
            FakePlayerManager.record();
        }
        if (!startRecord){
            FakePlayerManager.stop();
        }
        if (startLoad){
            FakePlayerManager.load();
        }
    }

    @Override
    public String getInfoString() {
        if (FakePlayerManager.getPlayers() != null) return String.valueOf(FakePlayerManager.getPlayers().size());
        return null;
    }
}
