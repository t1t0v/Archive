package naturalism.addon.netherbane.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
public class MixinTitleScreen {
    private MinecraftClient mc = MinecraftClient.getInstance();


    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
            //mc.setScreen(new MainMenu());
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        mc.textRenderer.drawWithShadow(matrices, Formatting.DARK_RED  + "NetherBane" + Formatting.WHITE + " v" + "0.1" + " made by " + Formatting.RED + "Naturalism", 5, 5, 0);
    }

}
