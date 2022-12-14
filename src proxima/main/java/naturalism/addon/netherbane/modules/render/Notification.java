package naturalism.addon.netherbane.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Notifier;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.modules.render.hud.NotificationHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import static meteordevelopment.meteorclient.utils.player.ChatUtils.formatCoords;
import static naturalism.addon.netherbane.modules.render.hud.NotificationHud.notifications;


public class Notification extends Module {
    public Notification(){
        super(NetherBane.RENDERPLUS, "toast-notification", "*");

    }

    private final SettingGroup sgModules = settings.createGroup("Modules");
    private final Setting<Boolean> moduleToggle = sgModules.add(new BoolSetting.Builder().name("module-toggle").description("Notifies you when a player pops a totem.").defaultValue(true).build());
    private final Setting<Boolean> all = sgModules.add(new BoolSetting.Builder().name("all").description("Notifies you when a player pops a totem.").defaultValue(true).build());
    private final Setting<List<Module>> modules = sgModules.add(new ModuleListSetting.Builder().name("modules").description("What blocks to use for surround.").defaultValue(new ArrayList<>()).build());

    private final SettingGroup sgTotemPops = settings.createGroup("Totem Pops");

    private final Setting<Boolean> totemPops = sgTotemPops.add(new BoolSetting.Builder().name("totem-pops").description("Notifies you when a player pops a totem.").defaultValue(true).build());
    private final Setting<Boolean> totemsIgnoreOwn = sgTotemPops.add(new BoolSetting.Builder().name("ignore-own").description("Notifies you of your own totem pops.").defaultValue(false).build());
    private final Setting<Boolean> totemsIgnoreFriends = sgTotemPops.add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends totem pops.").defaultValue(false).build());
    private final Setting<Boolean> totemsIgnoreOthers = sgTotemPops.add(new BoolSetting.Builder().name("ignore-others").description("Ignores other players totem pops.").defaultValue(false).build());
    private final Setting<Boolean> notRepeat = sgTotemPops.add(new BoolSetting.Builder().name("not-repaet").description("Ignores other players totem pops.").defaultValue(false).build());


    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    // Visual Range
    private final Setting<Boolean> visualRange = sgVisualRange.add(new BoolSetting.Builder().name("visual-range").description("Notifies you when an entity enters your render distance.").defaultValue(false).build());
    private final Setting<Notifier.Event> event = sgVisualRange.add(new EnumSetting.Builder<Notifier.Event>().name("event").description("When to log the entities.").defaultValue(Notifier.Event.Both).build());
    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgVisualRange.add(new EntityTypeListSetting.Builder().name("entities").description("Which entities to nofity about.").defaultValue(EntityType.PLAYER).build());
    private final Setting<Boolean> visualRangeIgnoreFriends = sgVisualRange.add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends.").defaultValue(true).build());
    private final Setting<Boolean> visualRangeIgnoreFakes = sgVisualRange.add(new BoolSetting.Builder().name("ignore-fake-players").description("Ignores fake players.").defaultValue(true).build());

    private final SettingGroup sgBreakInfo = settings.createGroup("BreakInfo");
    private final Setting<Boolean> surrBreakInfo = sgBreakInfo.add(new BoolSetting.Builder().name("surround-break-info").description("Notifies you when an entity enters your render distance.").defaultValue(false).build());
    private final Setting<Boolean> burrowBreakInfo = sgBreakInfo.add(new BoolSetting.Builder().name("burrow-break-info").description("Notifies you when an entity enters your render distance.").defaultValue(false).build());
    private final Setting<Boolean> cevBreakInfo = sgBreakInfo.add(new BoolSetting.Builder().name("cev-break-info").description("Notifies you when an entity enters your render distance.").defaultValue(false).build());

    private final SettingGroup sgActions = settings.createGroup("Actions");
    private final Setting<Boolean> drinkStrenghtPotion = sgActions.add(new BoolSetting.Builder().name("drink-strenght-potion").description("Notifies you when a player pops a totem.").defaultValue(true).build());
    private final Setting<Boolean> drinkSpeedPotion = sgActions.add(new BoolSetting.Builder().name("drink-speed-potion").description("Notifies you when a player pops a totem.").defaultValue(true).build());
    private final Setting<Boolean> drinkTurtleShellPotion = sgActions.add(new BoolSetting.Builder().name("drink-turtle-shell-potion").description("Notifies you when a player pops a totem.").defaultValue(true).build());

    private final SettingGroup sgArmorBreak = settings.createGroup("Armor Break");
    private final Setting<Boolean> armorBreakInfo = sgArmorBreak.add(new BoolSetting.Builder().name("armor-break-notify").description("Notifies you when a player pops a totem").defaultValue(true).build());
    private final Setting<Integer> durability = sgArmorBreak.add(new IntSetting.Builder().name("durability").description("What damage should an armor item have to be notified.").defaultValue(100).visible(armorBreakInfo::get).build());

    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();
    private final Random random = new Random();
    private boolean bBoots = false;
    private boolean bLeggings = false;
    private boolean bArmor = false;
    private boolean bHead = false;

    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Repeat) return;
        onAction(true, event.key, event.action == KeyAction.Press);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Repeat) return;
        onAction(false, event.button, event.action == KeyAction.Press);
    }

    private void onAction(boolean isKey, int value, boolean isPress) {
        if (moduleToggle.get()){
            if (mc.currentScreen == null && !Input.isKeyPressed(GLFW.GLFW_KEY_F3)) {
                for (Module module : Modules.get().getList()) {
                    if (!all.get()){
                        if (modules.get().contains(module)) {
                            if (module.keybind.matches(isKey, value) && (isPress || module.toggleOnBindRelease)) {
                                notifications.add(new NotificationHud.Notification("Toggled " + module.name + (isActive() ? " on" : " off"), NotificationHud.NotificationType.Info));
                            }
                        }
                    }else {
                        if (module.keybind.matches(isKey, value) && (isPress || module.toggleOnBindRelease)) {
                            notifications.add(new NotificationHud.Notification("Toggled " + module.name + (isActive() ? " on" : " off"), NotificationHud.NotificationType.Info));
                        }
                    }

                }
            }
        }

    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity.getUuid().equals(mc.player.getUuid()) || !entities.get().getBoolean(event.entity.getType()) || !visualRange.get() || this.event.get() == Notifier.Event.Despawn) return;

        if (event.entity instanceof PlayerEntity) {
            if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((PlayerEntity) event.entity))) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                if (notifications.contains(new NotificationHud.Notification(event.entity.getEntityName() + " has entered your visual range!", NotificationHud.NotificationType.View))){
                    notifications.remove(new NotificationHud.Notification(event.entity.getEntityName() + " has entered your visual range!", NotificationHud.NotificationType.View));
                    notifications.add(new NotificationHud.Notification(event.entity.getEntityName() + " has entered your visual range!", NotificationHud.NotificationType.View));
                }
                else {
                    notifications.add(new NotificationHud.Notification(event.entity.getEntityName() + " has entered your visual range!", NotificationHud.NotificationType.View));
                }
            }
        }
        else {
            String text = event.entity.getType().toString();
            text += " has spawn at ";
            text += formatCoords(event.entity.getPos());
            text += ".";
            if (notifications.contains(new NotificationHud.Notification(text, NotificationHud.NotificationType.View))){
                notifications.remove(new NotificationHud.Notification(text, NotificationHud.NotificationType.View));
                notifications.add(new NotificationHud.Notification(text, NotificationHud.NotificationType.View));
            }
            else {
                notifications.add(new NotificationHud.Notification(text, NotificationHud.NotificationType.View));
            }
        }
    }

    @EventHandler
    private void onStartingBreak(PacketEvent.Receive event){
        assert mc.player != null;
        if (event.packet instanceof BlockBreakingProgressS2CPacket blockBreakingProgressS2CPacket){
            BlockPos breakPos = blockBreakingProgressS2CPacket.getPos();
            BlockPos center = mc.player.getBlockPos();
            if (blockBreakingProgressS2CPacket.getProgress() > 1 && mc.world.getEntityById(blockBreakingProgressS2CPacket.getEntityId()) != mc.player){
                if (surrBreakInfo.get()){
                    if (breakPos.equals(center.west()) || breakPos.equals(center.east()) ||
                        breakPos.equals(center.south()) || breakPos.equals(center.north())){
                        if (((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.DIAMOND_PICKAXE ||
                            ((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.NETHERITE_PICKAXE){
                            if (notifications.contains(new NotificationHud.Notification("Your surround is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine))){
                                notifications.remove(new NotificationHud.Notification("Your surround is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                                notifications.add(new NotificationHud.Notification("Your surround is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                            }
                            else {
                                notifications.add(new NotificationHud.Notification("Your surround is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                            }
                        }
                        if (((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.IRON_PICKAXE){
                            if (notifications.contains(new NotificationHud.Notification("Your surround is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine))){
                                notifications.remove(new NotificationHud.Notification("Your surround is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                                notifications.add(new NotificationHud.Notification("Your surround is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                            }
                            else {
                                notifications.add(new NotificationHud.Notification("Your surround is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                            }
                        }
                    }
                }
                if (burrowBreakInfo.get()){
                    if (breakPos.equals(mc.player.getBlockPos())){
                        if (((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.DIAMOND_PICKAXE ||
                            ((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.NETHERITE_PICKAXE){
                            if (notifications.contains(new NotificationHud.Notification("Your burrow is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine))){
                                notifications.remove(new NotificationHud.Notification("Your burrow is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                                notifications.add(new NotificationHud.Notification("Your burrow is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                            }
                            else {
                                notifications.add(new NotificationHud.Notification("Your burrow is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                            }
                        }
                        if (((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.IRON_PICKAXE){
                            if (notifications.contains(new NotificationHud.Notification("Your burrow is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine))){
                                notifications.remove(new NotificationHud.Notification("Your burrow is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                                notifications.add(new NotificationHud.Notification("Your burrow is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                            }
                            else {
                                notifications.add(new NotificationHud.Notification("Your burrow is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                            }
                        }
                    }
                }
                if (cevBreakInfo.get()){
                    if (breakPos.equals(mc.player.getBlockPos().up(2))){
                        if (((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.DIAMOND_PICKAXE ||
                            ((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.NETHERITE_PICKAXE){
                            if (notifications.contains(new NotificationHud.Notification("Your cev is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine))){
                                notifications.remove(new NotificationHud.Notification("Your cev is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                                notifications.add(new NotificationHud.Notification("Your cev is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                            }
                            else {
                                notifications.add(new NotificationHud.Notification("Your cev is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.Mine));
                            }
                        }
                        if (((PlayerEntity) mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId())).getMainHandStack().getItem() == Items.IRON_PICKAXE){
                            if (notifications.contains(new NotificationHud.Notification("Your cev is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine))){
                                notifications.remove(new NotificationHud.Notification("Your cev is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                                notifications.add(new NotificationHud.Notification("Your cev is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                            }
                            else {
                                notifications.add(new NotificationHud.Notification("Your cev is being broken by " + mc.world.getEntityById(((BlockBreakingProgressS2CPacket) event.packet).getEntityId()).getEntityName(), NotificationHud.NotificationType.IronMine));
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity.getUuid().equals(mc.player.getUuid()) || !entities.get().getBoolean(event.entity.getType()) || !visualRange.get() || this.event.get() == Notifier.Event.Spawn) return;

        if (event.entity instanceof PlayerEntity) {
            if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((PlayerEntity) event.entity))) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                if (notifications.contains(new NotificationHud.Notification(event.entity.getEntityName() + " has left your visual range!", NotificationHud.NotificationType.View))){
                    notifications.remove(new NotificationHud.Notification(event.entity.getEntityName() + " has left your visual range!", NotificationHud.NotificationType.View));
                    notifications.add(new NotificationHud.Notification(event.entity.getEntityName() + " has left your visual range!", NotificationHud.NotificationType.View));
                }
                else {
                    notifications.add(new NotificationHud.Notification(event.entity.getEntityName() + " has left your visual range!", NotificationHud.NotificationType.View));
                }
            }
        } else {
            String text = event.entity.getType().toString();
            text += " has despawned at ";
            text += formatCoords(event.entity.getPos());
            text += ".";
            if (notifications.contains(new NotificationHud.Notification(text, NotificationHud.NotificationType.View))){
                notifications.remove(new NotificationHud.Notification(text, NotificationHud.NotificationType.View));
                notifications.add(new NotificationHud.Notification(text, NotificationHud.NotificationType.View));
            }
            else {
                notifications.add(new NotificationHud.Notification(text, NotificationHud.NotificationType.View));
            }
        }
    }

    @Override
    public void onActivate() {
        totemPopMap.clear();
        chatIdMap.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        totemPopMap.clear();
        chatIdMap.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!totemPops.get()) return;
        if (!(event.packet instanceof EntityStatusS2CPacket)) return;

        EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);

        if (!(entity instanceof PlayerEntity)) return;

        if ((entity.equals(mc.player) && totemsIgnoreOwn.get())
            || (Friends.get().isFriend(((PlayerEntity) entity)) && totemsIgnoreOthers.get())
            || (!Friends.get().isFriend(((PlayerEntity) entity)) && totemsIgnoreFriends.get())
        ) return;

        synchronized (totemPopMap) {
            int pops = totemPopMap.getOrDefault(entity.getUuid(), 0);
            totemPopMap.put(entity.getUuid(), ++pops);

            if (notRepeat.get()){
                if (notifications.contains(new NotificationHud.Notification(entity.getEntityName() + " got " + (pops-1) + " pop's!", NotificationHud.NotificationType.Totem))){
                    notifications.remove(new NotificationHud.Notification(entity.getEntityName() + " got " + (pops-1) + " pop's!", NotificationHud.NotificationType.Totem));
                    notifications.add(new NotificationHud.Notification(entity.getEntityName() + " got " + pops + " pop's!", NotificationHud.NotificationType.Totem));
                }
                else {
                    notifications.add(new NotificationHud.Notification(entity.getEntityName() + " got " + pops + " pop's!", NotificationHud.NotificationType.Totem));
                }
            }else{
                notifications.add(new NotificationHud.Notification(entity.getEntityName() + " got " + pops + " pop's!", NotificationHud.NotificationType.Totem));
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!totemPops.get()) return;
        synchronized (totemPopMap) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPopMap.containsKey(player.getUuid())) continue;

                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPopMap.removeInt(player.getUuid());
                    notifications.add(new NotificationHud.Notification(player.getEntityName() + " died after " + pops + " pop's!", NotificationHud.NotificationType.Death));
                    chatIdMap.removeInt(player.getUuid());
                }
            }
        }

        assert mc.player != null;
        assert mc.world != null;
        ItemStack boots = mc.player.getInventory().getArmorStack(0);
        int durabilityBoots = boots.getMaxDamage() - boots.getDamage();
        ItemStack leggings = mc.player.getInventory().getArmorStack(1);
        int durabilityLeggings = leggings.getMaxDamage() - leggings.getDamage();
        ItemStack armour = mc.player.getInventory().getArmorStack(2);
        int durabilityArmor = armour.getMaxDamage() - armour.getDamage();
        ItemStack head = mc.player.getInventory().getArmorStack(3);
        int durabilityHead = head.getMaxDamage() - head.getDamage();

        if (boots.isEmpty() && leggings.isEmpty() && armour.isEmpty() && head.isEmpty()) return;

        if (durabilityBoots < durability.get() && !bBoots) {
            if (!notifications.contains(new NotificationHud.Notification("Your Boots is about to break.", NotificationHud.NotificationType.Boots))){
                notifications.add(new NotificationHud.Notification("Your Boots is about to break.", NotificationHud.NotificationType.Boots));
            }
            bBoots = true;
        } else if (durabilityBoots > durability.get() && bBoots) {
            bBoots = false;
        }
        if (durabilityLeggings < durability.get() && !bLeggings) {
            if (!notifications.contains(new NotificationHud.Notification("Your Leggings is about to break.", NotificationHud.NotificationType.Legging))){
                notifications.add(new NotificationHud.Notification("Your Leggings is about to break.", NotificationHud.NotificationType.Legging));
            }
            bLeggings = true;
        } else if (durabilityLeggings > durability.get() && bLeggings) {
            bLeggings = false;
        }
        if (durabilityArmor < durability.get() && !bArmor) {
            if (!notifications.contains(new NotificationHud.Notification("Your Chestplate is about to break.", NotificationHud.NotificationType.Chestplate))){
                notifications.add(new NotificationHud.Notification("Your Chestplate is about to break.", NotificationHud.NotificationType.Chestplate));
            }
            bArmor = true;
        } else if (durabilityArmor > durability.get() && bArmor) {
            bArmor = false;
        }
        if (durabilityHead < durability.get() && !bHead) {
            if (!notifications.contains(new NotificationHud.Notification("Your Helmet is about to break.", NotificationHud.NotificationType.Helmet))){
                notifications.add(new NotificationHud.Notification("Your Helmet is about to break.", NotificationHud.NotificationType.Helmet));
            }
            bHead = true;
        } else if (durabilityHead > durability.get() && bHead) {
            bHead = false;
        }
    }

    @EventHandler
    private void onPotionDrink(TickEvent.Pre event){

    }

    private int getChatId(Entity entity) {
        return chatIdMap.computeIntIfAbsent(entity.getUuid(), value -> random.nextInt());
    }

}
