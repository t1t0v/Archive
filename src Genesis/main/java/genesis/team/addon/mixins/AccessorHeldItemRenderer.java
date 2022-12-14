package genesis.team.addon.mixins;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HeldItemRenderer.class)
public interface AccessorHeldItemRenderer {

	@Accessor
	void setMainHand(ItemStack value);

	@Accessor
	void setOffHand(ItemStack value);

	@Accessor
	float getEquipProgressMainHand();

	@Accessor
	void setEquipProgressMainHand(float value);

	@Accessor
	float getEquipProgressOffHand();

	@Accessor
	void setEquipProgressOffHand(float value);
}
