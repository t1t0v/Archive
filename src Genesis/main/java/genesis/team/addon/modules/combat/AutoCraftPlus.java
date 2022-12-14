package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;


public class AutoCraftPlus extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<Boolean> count = sgGeneral.add(new BoolSetting.Builder()
            .name("count")
            .description("mine-cart-count")
            .defaultValue(true)
            .build());


    private final Setting<Integer> counts = sgGeneral.add(new IntSetting.Builder()
            .name("mine-cart-count")
            .description("mine-cart-count")
            .defaultValue(6)
            .min(1)
            .max(100)
            .sliderMin(1)
            .sliderMax(100)
            .visible(count::get)
            .build());


    private final Setting<List<Item>> first_item = sgGeneral.add(new ItemListSetting.Builder()
            .name("First item")
            .description("Items that are considered trash and can be thrown out.")
            .defaultValue(Items.MINECART)
            .build()
    );
    private final Setting<List<Item>> second_item = sgGeneral.add(new ItemListSetting.Builder()
            .name("second item")
            .description("Items that are considered trash and can be thrown out.")
            .defaultValue(Items.TNT_MINECART)
            .build()
    );
    private final Setting<Boolean> disableAfter = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-after")
            .description("Toggle off after filling your inv with beds.")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> closeAfter = sgGeneral.add(new BoolSetting.Builder()
            .name("close-after")
            .description("Close the crafting GUI after filling.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> antiDesync = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-desync")
            .description("Try to prevent inventory desync.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> craftAll = sgGeneral.add(new BoolSetting.Builder()
            .name("craft-all")
            .description("Crafts maximum possible amount amount per craft (shift-clicking)")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> drop = sgGeneral.add(new BoolSetting.Builder()
            .name("drop")
            .description("Automatically drops crafted items (useful for when not enough inventory space)")
            .defaultValue(false)
            .build()
    );

    public AutoCraftPlus() {
        super(Genesis.Misc, "craft", "Automatically crafts items.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (mc.interactionManager == null) return;
        if (first_item.get().isEmpty()) return;

        if (!(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) return;


        if (antiDesync.get())
            mc.player.getInventory().updateItems();
        CraftingScreenHandler currentScreenHandler = (CraftingScreenHandler) mc.player.currentScreenHandler;
        List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getOrderedResults();
        for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
            for (Recipe<?> recipe : recipeResultCollection.getRecipes(true)) {
                if (!first_item.get().contains(recipe.getOutput().getItem())) continue;
                mc.interactionManager.clickRecipe(currentScreenHandler.syncId, recipe, craftAll.get());
                mc.interactionManager.clickSlot(currentScreenHandler.syncId, 0, 1,
                        drop.get() ? SlotActionType.THROW : SlotActionType.QUICK_MOVE, mc.player);


            }

        }
    }
    @EventHandler
    private void onTicks (TickEvent.Post event){
        if (CraftUtils.isInventoryFull())
        if (second_item.get().isEmpty()) CraftUtils.closeCraftingTable();
        if (!(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) return;

        CraftingScreenHandler currentScreenHandler = (CraftingScreenHandler) mc.player.currentScreenHandler;
        List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getOrderedResults();

        for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
            for (Recipe<?> recipe : recipeResultCollection.getRecipes(true)) {
                if (!second_item.get().contains(recipe.getOutput().getItem())) continue;
                mc.interactionManager.clickRecipe(currentScreenHandler.syncId, recipe, craftAll.get());
                mc.interactionManager.clickSlot(currentScreenHandler.syncId, 0, 1,
                        drop.get() ? SlotActionType.THROW : SlotActionType.QUICK_MOVE, mc.player);
            }
        }


    }

}





