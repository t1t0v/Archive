package genesis.team.addon.modules.info;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;

import static genesis.team.addon.modules.info.Notifications.send;

public class AutoReKit extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> kName = sgGeneral.add(new StringSetting.Builder().name("name-of-kit").description("Name of kit that should be taken.").defaultValue("").build());
    private final Setting<String> kCommand = sgGeneral.add(new StringSetting.Builder().name("kit-command").description("Command to activate kit commands.").defaultValue("/kit").build());
    private final Setting<Notifications.Mode> notifications = sgGeneral.add(new EnumSetting.Builder<Notifications.Mode>().name("notifications").defaultValue(Notifications.Mode.Toast).build());

    private boolean lock = false;
    private int i = 40;

    public AutoReKit() {
        super(Genesis.Info, "Auto-ReKit", "Automatically takes specified kit after joining server/respawn.");
    }

    @EventHandler
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;
        lock = true;
        i = 40;
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        lock = true;
        i = 40;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen instanceof DeathScreen) return;
        if (lock) i--;
        if (lock && i <= 0) {
            send("Selected kit: " + kName.get() + "", notifications);
            ChatUtils.sendPlayerMsg(kCommand.get() + " " + kName.get());
            lock = false;
            i = 40;
            return;
        }
    }
}
