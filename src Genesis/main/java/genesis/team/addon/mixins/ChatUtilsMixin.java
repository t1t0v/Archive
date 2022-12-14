package genesis.team.addon.mixins;

import genesis.team.addon.modules.info.ChatConfig;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatUtils.class)
public class ChatUtilsMixin {
    @Inject(method = "getPrefix", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getPrefix(CallbackInfoReturnable<Text> cir) {
        if (!Modules.get().isActive(ChatConfig.class)) return;
        if (Modules.get().get(ChatConfig.class).mode.get() != ChatConfig.Mode.Bohema) {
            MutableText PREFIX = Text.literal(Modules.get().get(ChatConfig.class).mode.get() == ChatConfig.Mode.Clear ? "" : Modules.get().get(ChatConfig.class).text.get());
            MutableText prefix = Text.literal("");
            PREFIX.setStyle(PREFIX.getStyle().withFormatting(Formatting.RED));
            prefix.setStyle(prefix.getStyle().withFormatting(Formatting.RED));
            prefix.append(Modules.get().get(ChatConfig.class).mode.get() == ChatConfig.Mode.Clear ? "" : "[");
            prefix.append(PREFIX);
            prefix.append(Modules.get().get(ChatConfig.class).mode.get() == ChatConfig.Mode.Clear ? "" : "] ");
            cir.setReturnValue(prefix);
        }
    }
}
