package genesis.team.addon.modules.movement;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;


public class ElytraRepl extends Module {



    private final SettingGroup sgInventory = settings.createGroup("Inventory");

    //General

    public final Setting<Boolean> replace = sgInventory.add(new BoolSetting.Builder()
            .name("elytra-replace")
            .description("Replaces broken elytra with a new elytra.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Integer> replaceDurability = sgInventory.add(new IntSetting.Builder()
            .name("replace-durability")
            .description("The durability threshold your elytra will be replaced at.")
            .defaultValue(2)
            .range(1, Items.ELYTRA.getMaxDamage() - 1)
            .sliderRange(1, Items.ELYTRA.getMaxDamage() - 1)
            .visible(replace::get)
            .build()
    );



    public ElytraRepl() {
        super(Genesis.Move, "ElytraReplenish", "Elytra Fly that works on ncp servers.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        float yaw = (float) Math.toRadians(mc.player.getYaw());


        //replace
        if (replace.get()) {
            ItemStack chestStack = mc.player.getInventory().getArmorStack(2);

            if (chestStack.getItem() == Items.ELYTRA) {
                if (chestStack.getMaxDamage() - chestStack.getDamage() <= replaceDurability.get()) {
                    FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamage() > replaceDurability.get() && stack.getItem() == Items.ELYTRA);

                    InvUtils.move().from(elytra.slot()).toArmor(2);
                }
            }
        }
    }
}
