package naturalism.addon.netherbane.mixins;

import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.PositionHud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import naturalism.addon.netherbane.modules.misc.StreamerMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(PositionHud.class)
public class PositionHudMixin {
    @Shadow
    private String right1;

    @Shadow
    private String right2;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void render(HudRenderer renderer, CallbackInfo ci){
        if (Modules.get().isActive(StreamerMode.class)) {
            StreamerMode streamerMode = Modules.get().get(StreamerMode.class);

            switch (streamerMode.coordinatesMode.get()){
                case Zero -> {
                    right1 = "0 0 0";
                    right2 = "0 0 0";
                }
                case Custom -> {
                    if (mc.world.getDimension().isBedWorking() && !mc.world.getDimension().isRespawnAnchorWorking()){
                        right1 = streamerMode.x.get() + " " + streamerMode.y.get() + " " + streamerMode.z.get();
                        right2 = streamerMode.x.get() / 8 + " " + streamerMode.y.get() / 8 + " " + streamerMode.z.get() / 8;
                    }else if (mc.world.getDimension().isRespawnAnchorWorking() && !mc.world.getDimension().isBedWorking()){
                        right1 = streamerMode.x.get() + " " + streamerMode.y.get() + " " + streamerMode.z.get();
                        right2 = streamerMode.x.get() * 8 + " " + streamerMode.y.get() * 8 + " " + streamerMode.z.get() * 8;
                    }
                    else if (mc.world.getDimension().isRespawnAnchorWorking() && !mc.world.getDimension().isBedWorking()){
                        right1 = streamerMode.x.get() + " " + streamerMode.y.get() + " " + streamerMode.z.get();
                        right2 = streamerMode.x.get() + " " + streamerMode.y.get() + " " + streamerMode.z.get();
                    }
                }
                case Obf -> {
                    Random rng = new Random();
                    String characters = "0123456789";
                    char[] text = new char[7];
                    for (int i = 0; i < 6; i++)
                    {
                        text[i] = characters.charAt(rng.nextInt(characters.length()));
                    }
                    right1 = new String(text) + " " + new String(text) + " " + new String(text);
                    right2 = new String(text) + " " + new String(text) + " " + new String(text);
                }
            }

        }
    }
}
