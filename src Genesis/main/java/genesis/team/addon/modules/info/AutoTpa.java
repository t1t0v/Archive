package genesis.team.addon.modules.info;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class AutoTpa extends Module {
    public enum Mode {
        All,
        Blacklist,
        Whitelist
    }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Enum<Mode>> mode = sgGeneral.add(new EnumSetting.Builder<Enum<Mode>>()
        .name("mode")
        .defaultValue(Mode.Whitelist)
        .build()
    );

    private final Setting<List<String>> list = sgGeneral.add(new StringListSetting.Builder()
        .name("list")
        .visible(() -> !mode.get().equals(Mode.All))
        .build()
    );

    public AutoTpa() {
        super(Genesis.Info, "auto-tpa", "Automatically accepts tpa requests.");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (event.isModified() || event.isCancelled() || mc.player == null) return;
        Text text = event.getMessage();
        //if (!text.getStyle().isEmpty()) return; todo fix
        String message = text.getString();
        Genesis.LOG.info(message);
        int idx = message.indexOf(' ');
        if (idx == -1) return;
        String name = message.substring(0, idx);
        message = message.substring(idx);
        Genesis.LOG.info(message);
        if (!message.equals(" has requested to teleport to you.")) return;
        if (!mode.get().equals(Mode.All)) {
            Genesis.LOG.info(name);
            if (list.get().contains(name)) {
                if (mode.get().equals(Mode.Whitelist)) {
                    ChatUtils.sendPlayerMsg("tpaccept");
                }
            }
        } else {
            ChatUtils.sendPlayerMsg("tpaccept");
        }
    }
}
