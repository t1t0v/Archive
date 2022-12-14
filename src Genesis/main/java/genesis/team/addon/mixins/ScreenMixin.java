package genesis.team.addon.mixins;

import genesis.team.addon.modules.render.BackgroundColor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Screen.class)
public class ScreenMixin {

    @ModifyConstant(method = "renderBackground(Lnet/minecraft/client/util/math/MatrixStack;I)V", constant = @Constant(intValue = -1072689136))
    private int startColor(int c) {
        BackgroundColor BackgroundColorColor = Modules.get().get(BackgroundColor.class);
        if (Modules.get().get(BackgroundColor.class).isActive()){
            return toInt(BackgroundColorColor.Color1.get().r, BackgroundColorColor.Color1.get().g, BackgroundColorColor.Color1.get().b, BackgroundColorColor.Color1.get().a);
        }

        return 0;
    }

    @ModifyConstant(method = "renderBackground(Lnet/minecraft/client/util/math/MatrixStack;I)V", constant = @Constant(intValue = -804253680))
    private int endColor(int c) {
        BackgroundColor BackgroundColorColor = Modules.get().get(BackgroundColor.class);
        if (Modules.get().get(BackgroundColor.class).isActive()){
            return toInt(BackgroundColorColor.Color2.get().r, BackgroundColorColor.Color2.get().g, BackgroundColorColor.Color2.get().b, BackgroundColorColor.Color2.get().a);
        }
        return 0;
    }



    int toInt(int r, int g, int b, int a) {
        int A = (a << 24) & 0xFF000000;
        int R = (r << 16) & 0x00FF0000;
        int G = (g << 8) & 0x0000FF00;
        int B = (b) & 0x000000FF;

        return A | R | G | B;
    }
}
