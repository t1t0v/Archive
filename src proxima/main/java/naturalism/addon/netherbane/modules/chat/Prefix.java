package naturalism.addon.netherbane.modules.chat;

import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import naturalism.addon.netherbane.NetherBane;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class Prefix extends Module {
    public Prefix(){
        super(NetherBane.CHATPLUS, "bane-prefix", "-");

    }

    public enum PrefixMode {All, Proxima}

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<PrefixMode> prefixMode = sgDefault.add(new EnumSetting.Builder<PrefixMode>().name("mining").description("Block breaking method").defaultValue(PrefixMode.Proxima).build());
    private final Setting<SettingColor> prefixColors = sgDefault.add(new ColorSetting.Builder().name("prefix-color").description("Color").defaultValue(new SettingColor(255,255,255,255)).build());

    @Override
    public void onActivate() {
        if (prefixMode.get().equals(PrefixMode.Proxima)){
            ChatUtils.registerCustomPrefix("uwu.naturalism.netherbane.modules", this::getPrefix);
            ChatUtils.registerCustomPrefix("meteordevelopment.meteorclient.systems.modules", this::getPrefix2);
        }
        else {
            ChatUtils.registerCustomPrefix("meteordevelopment.meteorclient.systems.modules", this::getPrefix);
            ChatUtils.registerCustomPrefix("uwu.naturalism.netherbane.modules", this::getPrefix);
        }
    }

    @Override
    public void onDeactivate() {
        ChatUtils.registerCustomPrefix("uwu.naturalism.netherbane.modules", this::getPrefix2);
        ChatUtils.registerCustomPrefix("meteordevelopment.meteorclient.systems.modules", this::getPrefix2);
    }

    public LiteralText getPrefix2() {
        BaseText logo = new LiteralText("Meteor");
        LiteralText prefix = new LiteralText("");
        logo.setStyle(logo.getStyle().withFormatting(Formatting.DARK_PURPLE));
        LiteralText left = new LiteralText("[");
        left.setStyle(left.getStyle().withFormatting(Formatting.GRAY));
        prefix.append(left);
        prefix.append(logo);
        LiteralText right = new LiteralText("]");
        right.setStyle(right.getStyle().withFormatting(Formatting.GRAY));
        prefix.append(right);
        return prefix;
    }

    public LiteralText getPrefix() {
        BaseText logo = new LiteralText("NetherBane");
        LiteralText prefix = new LiteralText("");
        logo.setStyle(logo.getStyle().withColor(TextColor.fromRgb(prefixColors.get().getPacked())));
        LiteralText left = new LiteralText("[");
        left.setStyle(left.getStyle().withFormatting(Formatting.GRAY));
        prefix.append(left);
        prefix.append(logo);
        LiteralText right = new LiteralText("]");
        right.setStyle(right.getStyle().withFormatting(Formatting.GRAY));
        prefix.append(right);
        return prefix;
    }
}
