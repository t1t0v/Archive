package genesis.team.addon.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import genesis.team.addon.modules.render.BackgroundColor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen {
    @Shadow
    @Nullable
    protected abstract Slot getSlotAt(double xPosition, double yPosition);

    @Shadow
    protected abstract void drawItem(ItemStack stack, int xPosition, int yPosition, String amountText);

    @Shadow
    protected int x;

    @Shadow protected int backgroundHeight;

    @Shadow @Final protected ScreenHandler handler;

    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void onRendered(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BackgroundColor customBackground = Modules.get().get(BackgroundColor.class);
        if (Modules.get().get(BackgroundColor.class).isActive() && customBackground.displayItem.get()) {
            ItemStack stack;

            Slot slot = getSlotAt(mouseX, mouseY);
            if (slot != null && !slot.getStack().isEmpty()) {
                stack = slot.getStack();
            } else {
                stack = handler.getCursorStack();
            }

            if (stack != null && !stack.isEmpty()) {
                float size = (float) Math.min(x * 0.8F, backgroundHeight * 0.8);
                float scale = size / 16;
                MatrixStack matrices_ = RenderSystem.getModelViewStack();
                matrices_.push();

                // For lightning to work correctly, the model needs to be scaled in z direction as well.
                // This causes problems when the model gets out of the rendering area and disappears partially or as a whole.
                // To fix this I manually fitted z scale and z translation for a bunch of values and did a linear regression on it.
                // The results look pretty promising.
                matrices_.translate((x - size) / 2F, (height - size) / 2F, -385F * scale + 955.5F);
                matrices_.scale(scale, scale, scale);

                drawItem(stack, 0, 0, "");
                matrices_.pop();

                RenderSystem.applyModelViewMatrix();
                RenderSystem.enableDepthTest();
            }
        }
    }
}
