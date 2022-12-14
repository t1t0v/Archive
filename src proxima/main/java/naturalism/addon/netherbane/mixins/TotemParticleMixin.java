package naturalism.addon.netherbane.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.particle.AnimatedParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TotemParticle.class)
public abstract class TotemParticleMixin extends AnimatedParticle {
    //Thank you Walaryne for cool module

    protected TotemParticleMixin(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider, float upwardsAcceleration) {
        super(world, x, y, z, spriteProvider, upwardsAcceleration);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ontotemParticle1Constructor(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider, CallbackInfo ci) {
        TotemParticle totemParticle = ((TotemParticle)(Object) this);
        if(Modules.get().isActive(naturalism.addon.netherbane.modules.render.TotemParticle.class)) {
            naturalism.addon.netherbane.modules.render.TotemParticle totemParticle1 = new naturalism.addon.netherbane.modules.render.TotemParticle();
            Vec3d rainbowColor = totemParticle1.getRainbowColor();
            Vec3d colorOne = totemParticle1.getColorOne();
            Vec3d colorTwo = totemParticle1.getColorTwo();

            if (this.random.nextInt(4) == 0) {
                totemParticle.setColor(
                    totemParticle1.isRainbow() ? (float) rainbowColor.x : (float) colorOne.x,
                    totemParticle1.isRainbow() ? (float) rainbowColor.y : (float) colorOne.y,
                    totemParticle1.isRainbow() ? (float) rainbowColor.x : (float) colorOne.z
                );
            } else {
                totemParticle.setColor(
                    totemParticle1.isRainbow() ? (float) rainbowColor.x : (float) colorTwo.x,
                    totemParticle1.isRainbow() ? (float) rainbowColor.y : (float) colorTwo.y,
                    totemParticle1.isRainbow() ? (float) rainbowColor.z : (float) colorTwo.z
                );
            }
        }
    }

}
