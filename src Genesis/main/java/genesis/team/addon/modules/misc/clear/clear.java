package genesis.team.addon.modules.misc.clear;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;

public class clear extends Module {
   private final SettingGroup g = settings.getDefaultGroup();
   private final Setting<List<Item>> items = g.add((((new ItemListSetting.Builder()).name("items")).defaultValue(new ArrayList(0))).build());

   public clear() {
      super(Genesis.Misc, "Clear", "Automaticly removes choosing items.");
   }

   @EventHandler
   private void a(TickEvent.Pre e) {
      int sync = -1;
      if (mc.currentScreen == null) {
         sync = 0;
      }

      if (sync == -1) {
         Screen var4 = mc.currentScreen;
         if (var4 instanceof HandledScreen) {
            HandledScreen hs = (HandledScreen)var4;
            sync = hs.getScreenHandler().syncId;
         }
      }

      if (sync == 0) {
         FindItemResult item = InvUtils.find((a) -> (items.get()).contains(a.getItem()));
         if (item.found()) {
            mc.interactionManager.clickSlot(0, Ezz.invIndexToSlotId(item.slot()), 300, SlotActionType.SWAP, mc.player);
         }
      }
   }
}
