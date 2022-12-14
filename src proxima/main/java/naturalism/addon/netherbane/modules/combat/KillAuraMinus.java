package naturalism.addon.netherbane.modules.combat;

import baritone.api.BaritoneAPI;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;
import naturalism.addon.netherbane.NetherBane;

import java.util.ArrayList;
import java.util.List;

public class KillAuraMinus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgDelay = settings.createGroup("Delay");

    // General
    private final Setting<Weapon> weapon = sgGeneral.add(new EnumSetting.Builder<Weapon>().name("weapon").description("Only attacks an entity when a specified item is in your hand.").defaultValue(Weapon.Both).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("auto-switch").description("Switches to your selected weapon when attacking the target.").defaultValue(false).build());
    private final Setting<Boolean> shieldBreaker = sgGeneral.add(new BoolSetting.Builder().name("shield-breaker").description("Fucks enemies shields.").defaultValue(true).build());
    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder().name("only-on-click").description("Only attacks when hold left click.").defaultValue(false).build());
    private final Setting<Boolean> onlyWhenLook = sgGeneral.add(new BoolSetting.Builder().name("only-when-look").description("Only attacks when you are looking at the entity.").defaultValue(false).build());
    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder().name("ignore-walls").description("Ignores walls completely.").defaultValue(true).build());
    private final Setting<Boolean> randomTeleport = sgGeneral.add(new BoolSetting.Builder().name("random-teleport").description("Randomly teleport around the target").defaultValue(false).visible(() -> !onlyWhenLook.get()).build());
    private final Setting<RotationMode> rotation = sgGeneral.add(new EnumSetting.Builder<RotationMode>().name("rotate").description("Determines when you should rotate towards the target.").defaultValue(RotationMode.None).build());
    private final Setting<Target> rotationDirection = sgGeneral.add(new EnumSetting.Builder<Target>().name("rotation-direction").description("The direction to use for rotating towards the enemy.").defaultValue(Target.Head).build());
    private final Setting<Double> hitChance = sgGeneral.add(new DoubleSetting.Builder().name("hit-chance").description("The probability of your hits landing.").defaultValue(100).range(1, 100).build());
    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder().name("pause-on-combat").description("Freezes Baritone temporarily until you are finished attacking the entity.").defaultValue(true).build());

    // Targeting
    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder().name("entities").description("Entities to attack.").onlyAttackable().build());
    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder().name("range").description("The maximum range the entity can be to attack it.").defaultValue(6).min(0).sliderMax(6).build());
    private final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>().name("priority").description("How to filter targets within range.").defaultValue(SortPriority.LowestDistance).build());
    private final Setting<Integer> maxTargets = sgTargeting.add(new IntSetting.Builder().name("max-targets").description("How many entities to target at once.").defaultValue(10).min(1).sliderRange(1, 10).build());
    private final Setting<Boolean> babies = sgTargeting.add(new BoolSetting.Builder().name("babies").description("Whether or not to attack baby variants of the entity.").defaultValue(true).build());
    private final Setting<Boolean> nametagged = sgTargeting.add(new BoolSetting.Builder().name("nametagged").description("Whether or not to attack mobs with a name tag.").defaultValue(false).build());
    private final Setting<Boolean> friends = sgTargeting.add(new BoolSetting.Builder().name("friends").description("We do large ammount of trolling.").defaultValue(false).build());

    // Delay
    private final Setting<Boolean> smartDelay = sgDelay.add(new BoolSetting.Builder().name("smart-delay").description("Uses the vanilla cooldown to attack entities.").defaultValue(true).build());
    private final Setting<Integer> hitDelay = sgDelay.add(new IntSetting.Builder().name("hit-delay").description("How fast you hit the entity in ticks.").defaultValue(0).min(0).sliderMax(60).visible(() -> !smartDelay.get()).build());
    private final Setting<Boolean> randomDelayEnabled = sgDelay.add(new BoolSetting.Builder().name("random-delay-enabled").description("Adds a random delay between hits to attempt to bypass shit anarchy anti-cheats.").defaultValue(false).visible(() -> !smartDelay.get()).build());
    private final Setting<Integer> randomDelayMax = sgDelay.add(new IntSetting.Builder().name("random-delay-max").description("The maximum value for random delay.").defaultValue(4).min(0).sliderMax(20).visible(() -> randomDelayEnabled.get() && !smartDelay.get()).build());
    private final Setting<Integer> switchDelay = sgDelay.add(new IntSetting.Builder().name("switch-delay").description("How many ticks to wait before hitting an entity after switching hotbar slots.").defaultValue(0).min(0).build());

    private final List<Entity> targets = new ArrayList<>();
    private PlayerEntity target;
    private int hitDelayTimer, switchTimer;
    private boolean wasPathing;
    private boolean canSwap;
    int prevSlot;

    public KillAuraMinus() {
        super(NetherBane.COMBATPLUS, "kill-aura-minus", "Worst nightmare of meteor devs.");
    }

    @Override
    public void onActivate() {
        canSwap = true;
    }

    @Override
    public void onDeactivate() {
        canSwap = true;
        hitDelayTimer = 0;
        targets.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;
        assert mc.world != null;
        for (Entity entity : mc.world.getEntities()){

        }
        if (TargetUtils.isBadTarget(target, range.get()))
            target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;
        prevSlot = mc.player.getInventory().selectedSlot;
        FindItemResult anyAxe = InvUtils.find(itemStack -> itemStack.getItem() instanceof AxeItem);
        TargetUtils.getList(targets, this::entityCheck, priority.get(), maxTargets.get());

        if (targets.isEmpty()) {
            if (wasPathing) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
                wasPathing = false;
            }
            return;
        }
        //changed
        if (pauseOnCombat.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && !wasPathing) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
            wasPathing = true;
        }

        Entity primary = targets.get(0);
        if (rotation.get() == RotationMode.Always) rotate(primary, null);

        if (onlyOnClick.get() && !mc.options.attackKey.isPressed()) return;

        if (onlyWhenLook.get()) {
            primary = mc.targetedEntity;

            if (primary == null) return;
            if (!entityCheck(primary)) return;

            targets.clear();
            targets.add(primary);
        }

        if (autoSwitch.get() && canSwap) {
            FindItemResult weaponResult = InvUtils.findInHotbar(itemStack -> {
                Item item = itemStack.getItem();

                return switch (weapon.get()) {
                    case Axe -> item instanceof AxeItem;
                    case Sword -> item instanceof SwordItem;
                    case Both -> item instanceof AxeItem || item instanceof SwordItem;
                    default -> true;
                };
            });

            InvUtils.swap(weaponResult.slot(), false);
        }

        //Да блять я думал будет намного сложнее. А я так готовился...
        if (shieldBreaker.get()) {
            if (target.getMainHandStack().getItem() == Items.SHIELD && target.isBlocking() || target.getOffHandStack().getItem() == Items.SHIELD && target.isBlocking() ) {
                canSwap = false;
                InvUtils.swap(anyAxe.slot(), false);
                attack(target);
                mc.player.getInventory().selectedSlot = prevSlot;
                canSwap = true;
            }
        }

        if (!itemInHand() && canSwap) return;

        if (delayCheck()) targets.forEach(this::attack);

        if (randomTeleport.get() && !onlyWhenLook.get()) {
            mc.player.setPosition(primary.getX() + randomOffset(), primary.getY(), primary.getZ() + randomOffset());
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = switchDelay.get();
        }
    }


    /*Прошлая не робочая версия (можна удалить кста)
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlaySoundS2CPacket) {
            PlaySoundS2CPacket p = (PlaySoundS2CPacket) event.packet;
            if (p.getSound() == SoundEvents.ITEM_SHIELD_BLOCK) {
                if (shieldBreaker.get()) {
                    if (TargetUtils.isBadTarget(target, range.get()))
                        target = TargetUtils.getPlayerTarget(range.get(), priority.get());
                    if (TargetUtils.isBadTarget(target, range.get())) return;
                    FindItemResult anyAxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.NETHERITE_AXE || itemStack.getItem() == Items.DIAMOND_AXE || itemStack.getItem() == Items.IRON_AXE || itemStack.getItem() == Items.STONE_AXE);
                    if (target.getMainHandStack().getItem() == Items.SHIELD || target.getOffHandStack().getItem() == Items.SHIELD) {
                        canSwap = false;
                        InvUtils.swap(anyAxe.getSlot(), true);
                        attack(target);
                        canSwap = true;
                    }
                }
            }
        }
    }*/


    private double randomOffset() {
        return Math.random() * 4 - 2;
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return false;
        if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
        if (PlayerUtils.distanceTo(entity) > range.get()) return false;
        if (!entities.get().getBoolean(entity.getType())) return false;
        if (!nametagged.get() && entity.hasCustomName()) return false;
        if (!PlayerUtils.canSeeEntity(entity) && !ignoreWalls.get()) return false;
        if (entity instanceof PlayerEntity) {
            if (((PlayerEntity) entity).isCreative()) return false;
            if (!friends.get() && !Friends.get().shouldAttack((PlayerEntity) entity)) return false;
        }
        return !(entity instanceof AnimalEntity) || babies.get() || !((AnimalEntity) entity).isBaby();
    }

    private boolean delayCheck() {
        if (switchTimer > 0) {
            switchTimer--;
            return false;
        }

        if (smartDelay.get()) return mc.player.getAttackCooldownProgress(0.5f) >= 1;

        if (hitDelayTimer > 0) {
            hitDelayTimer--;
            return false;
        } else {
            hitDelayTimer = hitDelay.get();
            if (randomDelayEnabled.get()) hitDelayTimer += Math.round(Math.random() * randomDelayMax.get());
            return true;
        }
    }

    private void attack(Entity target) {
        if (Math.random() > hitChance.get() / 100) return;
        if (rotation.get() == RotationMode.OnHit) rotate(target, () -> hitEntity(target));
        else hitEntity(target);
    }

    private void hitEntity(Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void rotate(Entity target, Runnable callback) {
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, rotationDirection.get()), callback);
    }

    private boolean itemInHand() {
        return switch (weapon.get()) {
            case Axe -> mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case Sword -> mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case Both -> mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem;
            default -> true;
        };
    }

    public Entity getTarget() {
        if (!targets.isEmpty()) return targets.get(0);
        return null;
    }

    @Override
    public String getInfoString() {
        if (!targets.isEmpty()) EntityUtils.getName(getTarget());
        return null;
    }

    public enum Weapon {
        Sword,
        Axe,
        Both,
        Any
    }

    public enum RotationMode {
        Always,
        OnHit,
        None
    }
}
