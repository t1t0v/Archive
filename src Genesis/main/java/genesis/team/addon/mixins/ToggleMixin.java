package genesis.team.addon.mixins;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Modules.class)
public class ToggleMixin {

    @Inject(method = "addActive", at = @At(value = "INVOKE", target = "Lmeteordevelopment/orbit/IEventBus;post(Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private void addActive(Module module, CallbackInfo ci) {
        }


    @Inject(method = "removeActive", at = @At(value = "INVOKE", target = "Lmeteordevelopment/orbit/IEventBus;post(Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private void removeActive(Module module, CallbackInfo ci) {
        }
    }

