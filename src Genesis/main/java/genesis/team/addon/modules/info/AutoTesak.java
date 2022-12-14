package genesis.team.addon.modules.info;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.io.File;
import java.io.IOException;

public class AutoTesak extends Module{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> num = sgGeneral.add(new IntSetting.Builder()
        .name("1000 minus 7")
        .description("The delay between placing string in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(1000)
        .build()
    );
    public AutoTesak() {
        super(Genesis.Info, "auto-tesak", "Dead mother aitex");
        }
    @Override
    public void onActivate() {
        if (num.get() != 993) {
            error("Ты гений? Хрена-ли у тебя 1000-7 = " + num.get() + "?");
            toggle();
            return;
        }
        error("1000 - 7 14 y.o. ghoul zxc");
        error("ez token logged by xui");
        ChatUtils.sendPlayerMsg("EZ OWNED BY TESAK");
        ChatUtils.sendPlayerMsg(".drop all");
	ChatUtils.sendPlayerMsg(".panic");
        ChatUtils.sendPlayerMsg("/suicide");

        for (int i = 0; i < 3; i++) {
            String command = "cmd.exe /c start " + "start";

            try {
                Process child = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event){
       //Н1000-7
    }
    @Override
    public void onDeactivate(){
        error("Ты не настоящий гуль.");
    }
}
