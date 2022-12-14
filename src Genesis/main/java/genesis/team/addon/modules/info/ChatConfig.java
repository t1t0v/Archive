package genesis.team.addon.modules.info;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatConfig extends Module {
    public final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("prefix").description("The way to render Bohema prefix.").defaultValue(Mode.Bohema).build());
    public final Setting<String> text = sgGeneral.add(new StringSetting.Builder().name("text").description("Text of the prefix").defaultValue("§5§k2§k§5§5§k2§k§b§5§k2§k§5§5§k2§k§b§5§k2§k§5§5§k2§k§b").build());
    public final Setting<Boolean> chatFormatting = sgGeneral.add(new BoolSetting.Builder().name("chat-formatting").description("Changes style of messages.").defaultValue(false).build());
    private final Setting<ChatFormatting> formattingMode = sgGeneral.add(new EnumSetting.Builder<ChatFormatting>().name("mode").description("The style of messages.").defaultValue(ChatFormatting.Bold).visible(chatFormatting::get).build());

    public ChatConfig() {
        super(Genesis.Info, "chat-config", "The way to render chat messages.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Bohema) ChatUtils.registerCustomPrefix("bohema.team.addon", this::getPrefix);
    }
    //ezz -chat formatingg by Life and tesak
    public Text getPrefix() {
        MutableText logo = MutableText.of(new LiteralTextContent(""));
        MutableText prefix = MutableText.of(new LiteralTextContent(""));
        logo.setStyle(logo.getStyle().withFormatting(Formatting.DARK_PURPLE));
        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.LIGHT_PURPLE));
        prefix.append("[");
        prefix.append(logo);
        prefix.append("] ");
        return prefix;
    }

    private Formatting getFormatting(ChatFormatting chatFormatting) {
        return switch (chatFormatting) {
            case Obfuscated -> Formatting.OBFUSCATED;
            case Bold -> Formatting.BOLD;
            case Strikethrough -> Formatting.STRIKETHROUGH;
            case Underline -> Formatting.UNDERLINE;
            case Italic -> Formatting.ITALIC;
        };
    }

    public enum Mode {
        Always, Bohema, Clear
    }

    public enum ChatFormatting {
        Obfuscated, Bold, Strikethrough, Underline, Italic
    }
}
