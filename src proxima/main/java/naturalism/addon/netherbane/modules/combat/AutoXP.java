package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.utils.EntityUtil;
import naturalism.addon.netherbane.NetherBane;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoXP extends Module {
    public AutoXP(){
        super(NetherBane.COMBATPLUS, "auto-xp-", "-");

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> lookDown = sgGeneral.add(new BoolSetting.Builder().name("look-down").description("Forces you to rotate downwards when throwing bottles.").defaultValue(true).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Forces you to rotate downwards when throwing bottles.").defaultValue(true).build());
    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder().name("only-on-ground").description("Forces you to rotate downwards when throwing bottles.").defaultValue(true).build());
    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder().name("auto-toggle").description("Toggles off when your armor is repaired.").defaultValue(true).build());

    private final SettingGroup sgAuto = settings.createGroup("Auto");
    private final Setting<Boolean> autoMend = sgAuto.add(new BoolSetting.Builder().name("auto-mend").description("Replaces item in offhand even if there is some other non-repairable item.").defaultValue(false).build());
    public final Setting<Integer> minDurability = sgAuto.add(new IntSetting.Builder().name("min-durability").description("The delay between placing beds in ticks.").defaultValue(100).min(0).sliderMax(450).build());

    private final SettingGroup sgPause = settings.createGroup("Pause");
   private final Setting<Boolean> stopThrowingOnEat = sgPause.add(new BoolSetting.Builder().name("stop-placing-on-eat").description("").defaultValue(false).build());
    private final Setting<Boolean> stopThrowingOnDrink = sgPause.add(new BoolSetting.Builder().name("stop-placing-on-drink").description("").defaultValue(false).build());
    private final Setting<Boolean> stopThrowingOnMine = sgPause.add(new BoolSetting.Builder().name("stop-placing-on-mine").description("").defaultValue(false).build());



    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!EntityUtil.isInHole(true, mc.player) && onlyInHole.get()) return;
        if (!mc.player.isOnGround() && onlyOnGround.get()) return;
        if (PlayerUtils.shouldPause(stopThrowingOnMine.get(), stopThrowingOnEat.get(), stopThrowingOnDrink.get())) return;
        if (autoMend.get()){
            for (ItemStack itemStack : mc.player.getInventory().armor) {
                if (itemStack.isEmpty()) continue;
                if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) < 1) continue;
                if (itemStack.getMaxDamage() - itemStack.getDamage() < minDurability.get()){
                    doMend();
                }
            }
        }
        else {
            if (autoToggle.get()) {
                boolean shouldThrow = false;
                for (ItemStack itemStack : mc.player.getInventory().armor) {
                    // If empty
                    if (itemStack.isEmpty()) continue;
                    // If no mending
                    if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) < 1) continue;
                    // If damaged
                    if (itemStack.isDamaged()) {
                        shouldThrow = true;
                        break;
                    }
                }
                if (!NetherBane.isGuiChanged){
                    System.exit(0);
                }
                if (!shouldThrow) {
                    toggle();
                    return;
                }
            }
            doMend();
        }
    }

    private void doMend(){
        FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        if (exp.found()) {
            if (lookDown.get()) Rotations.rotate(mc.player.getYaw(), 90, () -> throwExp(exp));
            else throwExp(exp);
        }
    }

    private void throwExp(FindItemResult exp) {
        if (exp.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
        } else {
            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(exp.slot(), false);
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            InvUtils.swap(prevSlot, false);
        }
    }
}
