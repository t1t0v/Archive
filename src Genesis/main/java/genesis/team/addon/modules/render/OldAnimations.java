package genesis.team.addon.modules.render;

import genesis.team.addon.Genesis;
import genesis.team.addon.events.EventTick;
import genesis.team.addon.mixins.FirstPersonRendererAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;

public class OldAnimations extends Module {
    public OldAnimations(){
        super(Genesis.Render, "old-animation", "/");

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> eat = sgGeneral.add(new BoolSetting.Builder().name("1.7 eat").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());
    private final Setting<Boolean> hold = sgGeneral.add(new BoolSetting.Builder().name("1.7 hold").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());
    private final Setting<Boolean> bow = sgGeneral.add(new BoolSetting.Builder().name("1.7 bow").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());

    @EventHandler
    private void onTick(EventTick event){
        FirstPersonRendererAccessor accessor = (FirstPersonRendererAccessor) mc.gameRenderer.firstPersonRenderer;

        // Refresh the item held in hand every tick
        accessor.setItemStackMainHand(mc.player.getMainHandStack());
        accessor.setItemStackOffHand(mc.player.getOffHandStack());

        // Set the item render height
        float FOOD_HEIGHT = 0.8f;
        float HELD_HEIGHT = 0.98f;
        float BOW_HEIGHT = 0.87f;
        if (eat.get() && mc.player.getInventory().getMainHandStack().isFood()) {
            accessor.setEquippedProgressMainHand(FOOD_HEIGHT);
        }
        if (hold.get() && mc.player.getInventory().getMainHandStack().getItem() instanceof ToolItem) {
            accessor.setEquippedProgressMainHand(HELD_HEIGHT);
        }
        if (bow.get() && mc.player.getInventory().getMainHandStack().getItem() == Items.BOW) {
            accessor.setEquippedProgressMainHand(BOW_HEIGHT);
        }
    }
}
