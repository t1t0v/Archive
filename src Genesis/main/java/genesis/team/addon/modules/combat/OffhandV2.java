package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.modules.combat.CrystalBoomer.CrystalBoomer;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class OffhandV2 extends Module {
    public OffhandV2(){
        super(Genesis.Px, "offhandV2", "Refill totems in the offhand slot.");

    }

    public enum CrystalDamage{
        Lethal,
        Custom
    }

    public enum Mode{
        Strict,
        Offhand
    }

    public enum ChooseItem {
        Crystal(Items.END_CRYSTAL),
        EGApple(Items.ENCHANTED_GOLDEN_APPLE);

        public Item item;

        ChooseItem(Item item) {
            this.item = item;
        }
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    public final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>().name("mode").description("Offhand mode").defaultValue(Mode.Strict).build());
    public final Setting<Boolean> antiDesync = sgDefault.add(new BoolSetting.Builder().name("anti-desync").description("Try to prevent inventory desync.").defaultValue(true).build());
    private final Setting<Integer> delay = sgDefault.add(new IntSetting.Builder().name("delay").description("The delay between totem move.").defaultValue(0).min(0).sliderMax(1).build());
    public final Setting<ChooseItem> item = sgDefault.add(new EnumSetting.Builder<ChooseItem>().name("item").description("Offhand item").defaultValue(ChooseItem.EGApple).visible(() -> mode.get().equals(Mode.Offhand)).build());

    private final SettingGroup sgDamage = settings.createGroup("Damage");
    public final Setting<CrystalDamage> damageCalc = sgDamage.add(new EnumSetting.Builder<CrystalDamage>().name("damage-calc").description("The method of calculating the damage.").defaultValue(CrystalDamage.Custom).visible(() -> mode.get().equals(Mode.Offhand)).build());
    private final Setting<Integer> crystalDamage = sgDamage.add(new IntSetting.Builder().name("crystal-damage").description("Damage from the crystal.").defaultValue(10).min(0).sliderMax(36).visible(() -> mode.get().equals(Mode.Offhand) && damageCalc.get().equals(CrystalDamage.Custom)).build());
    private final Setting<Integer> range = sgDamage.add(new IntSetting.Builder().name("range").description("Range to crystal.").defaultValue(10).min(0).sliderMax(36).visible(() -> mode.get().equals(Mode.Offhand)).build());
    private final Setting<Integer> minHealth = sgDamage.add(new IntSetting.Builder().name("min-health").description("The minimum health at which the totem will be taken in offhand.").defaultValue(8).min(0).sliderMax(36).visible(() -> mode.get().equals(Mode.Offhand)).build());
    public final Setting<Boolean> fall = sgDamage.add(new BoolSetting.Builder().name("fall").description("Take a totem when you fall.").defaultValue(true).visible(() -> mode.get().equals(Mode.Offhand)).build());
    private final Setting<Integer> fallDistance = sgDamage.add(new IntSetting.Builder().name("fall-distance").description("Distance.").defaultValue(10).min(0).sliderMax(36).visible(() -> mode.get().equals(Mode.Offhand) && fall.get()).build());
    public final Setting<Boolean> elytra = sgDamage.add(new BoolSetting.Builder().name("elytra").description("Take a totem when you fly on elytra.").defaultValue(true).visible(() -> mode.get().equals(Mode.Offhand)).build());

    private final SettingGroup sgIntegration = settings.createGroup("Integration");
    public final Setting<Boolean> integration = sgIntegration.add(new BoolSetting.Builder().name("integration").description("Take the selected item in offhand if the module is enabled.").defaultValue(false).visible(() -> mode.get().equals(Mode.Offhand)).build());
    public final Setting<Boolean> modules = sgIntegration.add(new BoolSetting.Builder().name("modules").description("Take the crystal item in offhand if the player is mining.").defaultValue(false).visible(() -> integration.get()  && mode.get().equals(Mode.Offhand)).build());
    public final Setting<List<Module>> Cmodules = sgIntegration.add(new ModuleListSetting.Builder().name("crystal-modules").description("Modules at which the crystal will be taken").defaultValue(Collections.singletonList(Modules.get().get(CrystalBoomer.class))).visible(() -> integration.get() && modules.get() && mode.get().equals(Mode.Offhand)).build());
    public final Setting<List<Module>> Emodules = sgIntegration.add(new ModuleListSetting.Builder().name("egapple-modules").description("Modules at which the enchanted golden apple will be taken").defaultValue(Collections.singletonList(Modules.get().get(CrystalBoomer.class))).visible(() -> integration.get() && modules.get() && mode.get().equals(Mode.Offhand)).build());
    public final Setting<Boolean> crystalWhenMining = sgIntegration.add(new BoolSetting.Builder().name("crystal-when-mining").description("Take the crystal item in offhand if the player is mining.").defaultValue(false).visible(() -> integration.get() && mode.get().equals(Mode.Offhand)).build());
    public final Setting<Boolean> egappleWhenSword = sgIntegration.add(new BoolSetting.Builder().name("eggaple-when-sword").description("Take the egapple item in offhand if the player take a sword.").defaultValue(false).visible(() -> integration.get() && mode.get().equals(Mode.Offhand)).build());

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event){
        doIt();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Post event){
        doIt();
    }

    private void doIt(){
        if (antiDesync.get()) mc.player.getInventory().updateItems();
        switch (mode.get()){
            case Strict -> doItemMove(Items.TOTEM_OF_UNDYING);
            case Offhand -> {
                if (isThereDangerousCrystal()) doItemMove(Items.TOTEM_OF_UNDYING);
                if (isThereDangerousBlock()) doItemMove(Items.TOTEM_OF_UNDYING);
                else if (mc.player.getHealth() <= minHealth.get()) doItemMove(Items.TOTEM_OF_UNDYING);
                else if (!mc.player.isOnGround() && mc.player.fallDistance >= fallDistance.get() && fall.get()) doItemMove(Items.TOTEM_OF_UNDYING);
                else if (mc.player.isFallFlying() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem && elytra.get()) doItemMove(Items.TOTEM_OF_UNDYING);
                else if (integration.get()){
                    boolean on = false;
                    if (modules.get()){
                        for (Module module : Cmodules.get()){
                            if (module.isActive()) {
                                doItemMove(Items.END_CRYSTAL);
                                on = true;
                            }

                        }
                        for (Module module : Emodules.get()){
                            if (module.isActive()) {
                                doItemMove(Items.ENCHANTED_GOLDEN_APPLE);
                                on = true;
                            }
                        }
                    }
                    if (crystalWhenMining.get() && mc.interactionManager.isBreakingBlock()) {
                        doItemMove(Items.END_CRYSTAL);
                        on = true;
                    }
                    if (egappleWhenSword.get() && mc.player.getMainHandStack().getItem() instanceof SwordItem) {
                        doItemMove(Items.ENCHANTED_GOLDEN_APPLE);
                        on = true;
                    }
                    if (!on) doItemMove(Items.TOTEM_OF_UNDYING);
                }
                else doItemMove(item.get().item);
            }
        }
    }

    private boolean isThereDangerousBlock(){
        ArrayList<BlockPos> bed = new ArrayList<>();
        AtomicBoolean isBed = new AtomicBoolean(false);
        BlockUtil.getSphere(mc.player.getBlockPos(), range.get(), range.get()).stream().filter(blockPos -> (!mc.world.getDimension().bedWorks() && BlockUtil.getBlock(blockPos) instanceof BedBlock) || (!mc.world.getDimension().respawnAnchorWorks() && BlockUtil.getBlock(blockPos) == Blocks.RESPAWN_ANCHOR)).forEach(blockPos -> {
            switch (damageCalc.get()){
                case Custom -> {
                    if (DamageUtils.bedDamage(mc.player, Utils.vec3d(blockPos)) > crystalDamage.get()) isBed.set(true);
                }
                case Lethal -> {
                    double health = mc.player.getHealth();
                    if (DamageUtils.bedDamage(mc.player, Utils.vec3d(blockPos)) > health && health - DamageUtils.crystalDamage(mc.player, Utils.vec3d(blockPos)) < minHealth.get()){
                        isBed.set(true);
                    }
                }
            }
        });
        return isBed.get();
    }

    private boolean isThereDangerousCrystal(){
        ArrayList<EndCrystalEntity> crystals = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()){
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) < range.get()){
                crystals.add((EndCrystalEntity) entity);
            }
        }
        crystals.sort(Comparator.comparing(entity -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), entity.getBlockPos())));
        for (EndCrystalEntity crystalEntity : crystals){
            switch (damageCalc.get()){
                case Custom -> {
                    if (DamageUtils.crystalDamage(mc.player, crystalEntity.getPos()) > crystalDamage.get()) return true;
                }
                case Lethal -> {
                    double health = mc.player.getHealth();
                    if (DamageUtils.crystalDamage(mc.player, crystalEntity.getPos()) > health && health - DamageUtils.crystalDamage(mc.player, crystalEntity.getPos()) < minHealth.get()){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean locked;
    private int totems, ticks;

    private void doItemMove(Item item){
        FindItemResult result = InvUtils.find(item);
        totems = result.count();

        locked = totems > 0;
        if (ticks >= delay.get()) {
            if (mc.player.getOffHandStack().getItem() != item) {
                InvUtils.move().from(result.slot()).toOffhand();
            }

            ticks = 0;
            return;
        }

        ticks++;
    }



}
