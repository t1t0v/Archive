package naturalism.addon.netherbane.mixins;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.config.Config;
import naturalism.addon.netherbane.events.InteractEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Redirect(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    public boolean breakBlockCheck(ClientPlayerEntity clientPlayerEntity) {
        return MeteorClient.EVENT_BUS.post(new InteractEvent(clientPlayerEntity.isUsingItem())).usingItem;
    }

    @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    public boolean useItemBreakCheck(ClientPlayerInteractionManager clientPlayerInteractionManager) {
        return MeteorClient.EVENT_BUS.post(new InteractEvent(clientPlayerInteractionManager.isBreakingBlock())).usingItem;
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;printCrashReport(Lnet/minecraft/util/crash/CrashReport;)V"))
    public void crash(CallbackInfo info) {
        Config.get().save();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stop(CallbackInfo info) {
        Config.get().save();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void close(CallbackInfo info) {
        Config.get().save();
    }
}
