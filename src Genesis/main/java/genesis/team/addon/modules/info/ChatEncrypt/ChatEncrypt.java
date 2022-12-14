package genesis.team.addon.modules.info.ChatEncrypt;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;

public class ChatEncrypt extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>().name("mode").description("The way to use Chat Encryptor.").defaultValue(Mode.Both).build());
    private final Setting<TextUtils.Color> nameColor = sgDefault.add(new EnumSetting.Builder<TextUtils.Color>().name("name-color").description("Sets the color of names in decrypted messages.").defaultValue(TextUtils.Color.DarkPurple).build());
    private final Setting<TextUtils.Color> messageColor = sgDefault.add(new EnumSetting.Builder<TextUtils.Color>().name("message-color").description("Sets the color of message in decrypted messages.").defaultValue(TextUtils.Color.Gray).build());
    private final Setting<Boolean> proximaDecrypt = sgDefault.add(new BoolSetting.Builder().name("proxima-decrypt").description("Decrypts messages from Proxima users.").defaultValue(false).build());

    public ChatEncrypt() {
        super(Genesis.Info, "chat-encrypt", "Encrypts and decrypts chat messages.");
    }

    public static String[] encryptors = {"[BT] ","[CE] "};

    private String getSender(String message) {
        ArrayList<PlayerListEntry> entry = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
        for (PlayerListEntry player : entry) {
            if (message.contains(player.getProfile().getName())) return player.getProfile().getName();
        }

        return "null";
    }

    @EventHandler
    public void onSendMessage(SendMessageEvent event) {
        if (mode.get() == Mode.Decrypt) return;

        if (!event.message.startsWith(String.valueOf(Config.get().prefix)) && !event.message.startsWith("/")) {
            event.message = "[BT] " + TextUtils.encrypt(event.message.toLowerCase());
        }
    }

    public enum Mode {
        Encrypt, Decrypt, Both
    }
}
