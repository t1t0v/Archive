package naturalism.addon.netherbane.mixins;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTickCounter.class)
public interface ITimer {
    @Accessor(value = "tickDelta")
    void setTickLength(float tickLength);
}
