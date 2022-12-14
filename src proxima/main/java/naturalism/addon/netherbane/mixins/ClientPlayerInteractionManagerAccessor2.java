package naturalism.addon.netherbane.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor2 {
    @Accessor("currentBreakingPos")
    BlockPos getCurrentBreakingPos();

    @Accessor("blockBreakingCooldown")
    int getBlockBreakingCooldown();

    @Accessor("blockBreakingCooldown")
    void setBlockBreakingCooldown(int blockBreakingCooldown);

    @Accessor("currentBreakingProgress")
    float getCurrentBreakingProgress();

    @Accessor("currentBreakingProgress")
    void setCurrentBreakingProgress(float currentBreakingProgress);

    @Accessor("blockBreakingSoundCooldown")
    float getBlockBreakingSoundCooldown();

    @Accessor("blockBreakingSoundCooldown")
    void setBlockBreakingSoundCooldown(float blockBreakingSoundCooldown);

    @Accessor("breakingBlock")
    void setBreakingBlock(boolean breakingBlock);

    @Accessor("lastSelectedSlot")
    int getLastSelectedSlot();

    @Accessor("lastSelectedSlot")
    void setLastSelectedSlot(int lastSelectedSlot);

    @Invoker("isCurrentlyBreaking")
    boolean getIsCurrentlyBreaking(BlockPos pos);
}
