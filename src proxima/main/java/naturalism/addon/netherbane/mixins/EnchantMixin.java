package naturalism.addon.netherbane.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import naturalism.addon.netherbane.modules.render.CustomEnchants;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class EnchantMixin <T extends LivingEntity, M extends BipedEntityModel<T>, A extends BipedEntityModel<T>>{
    @Shadow
    protected abstract Identifier getArmorTexture(ArmorItem item, boolean legs, @Nullable String overlay);

    @Inject(method = "renderArmorParts", at = @At("TAIL"))
    private void renderArmorEnchant(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ArmorItem item, boolean usesSecondLayer, A model, boolean legs, float red, float green, float blue, String overlay, CallbackInfo ci){
        CustomEnchants customEnchants = Modules.get().get(CustomEnchants.class);

        if (customEnchants.isActive()){
            VertexConsumer vertexConsumer = ItemRenderer.getArmorGlintConsumer(vertexConsumers, RenderLayer.getArmorCutoutNoCull(getArmorTexture(item, legs, overlay)), false, usesSecondLayer);
            model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, customEnchants.color.get().r, customEnchants.color.get().g, customEnchants.color.get().b, 1.0F);
        }
    }
}
