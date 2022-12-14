package naturalism.addon.netherbane.modules.combat;


import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class OneTap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> bows = sgGeneral.add(new BoolSetting.Builder().name("bows").description("Bows.").defaultValue(true).build());
    private final Setting<Boolean> pearls = sgGeneral.add(new BoolSetting.Builder().name("pearl").description("Pearls.").defaultValue(true).build());
    private final Setting<Boolean> eggs = sgGeneral.add(new BoolSetting.Builder().name("eggs").description("Eggs.").defaultValue(true).build());
    private final Setting<Boolean> snowballs = sgGeneral.add(new BoolSetting.Builder().name("snowballs").description("Snowballs.").defaultValue(true).build());
    private final Setting<Boolean> potions = sgGeneral.add(new BoolSetting.Builder().name("potions").description("Can use potions.").defaultValue(true).build());
    private final Setting<Boolean> splashPotions = sgGeneral.add(new BoolSetting.Builder().name("splash-potions").description("Splash potions.").defaultValue(true).visible(potions::get).build());
    private final Setting<Boolean> lingeringPotions = sgGeneral.add(new BoolSetting.Builder().name("lingering-potions").description("Lingering potions.").defaultValue(true).visible(potions::get).build());
    private final Setting<Integer> potionSpoofs = sgGeneral.add(new IntSetting.Builder().name("potions-spoofs").description("Potion spoofs.").min(0).sliderMin(1).sliderMax(65).defaultValue(20).visible(potions::get).build());
    private final Setting<Boolean> tridents = sgGeneral.add(new BoolSetting.Builder().name("tridents").description("Tridents.").defaultValue(true).build());
    private final Setting<Integer> timeout = sgGeneral.add(new IntSetting.Builder().name("timeout").description("Timeout.").min(0).sliderMin(100).sliderMax(20000).defaultValue(300).build());
    private final Setting<Integer> spoofs = sgGeneral.add(new IntSetting.Builder().name("spoofs").description("Spoofs.").min(0).sliderMin(1).sliderMax(300).defaultValue(120).build());
    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder().name("bypass").description("Bypass.").defaultValue(true).build());
    private final Setting<Boolean> extraSpeed = sgGeneral.add(new BoolSetting.Builder().name("extra-speed").description("Extra-speed.").defaultValue(true).build());
    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder().name("notify").description("Notifies when going brrrrrr.").defaultValue(false).build());

    public OneTap() {
        super(NetherBane.COMBATPLUS, "one-tap", "Haha projectiles go brrrrrr");

    }

    private boolean shooting;
    private long lastShootTime;

    @Override
    public void onActivate() {
        shooting = false;
        lastShootTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null && handStack.getItem() instanceof BowItem && bows.get()) {
                    doSpoofs();
                    if (notify.get()) ChatUtils.info(name, "trying to brrrrr");
                } else if (handStack.getItem() instanceof TridentItem && tridents.get()) {
                    doSpoofs();
                    if (notify.get()) ChatUtils.info(name, "trying to brrrrr");
                }
            }

        } else if (event.packet instanceof PlayerInteractItemC2SPacket packet2) {
            if (packet2.getHand() == Hand.MAIN_HAND) {
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null) {
                    if (handStack.getItem() instanceof EggItem && eggs.get()) {
                        doSpoofs();
                    } else if (handStack.getItem() instanceof EnderPearlItem && pearls.get()) {
                        doSpoofs();
                    } else if (handStack.getItem() instanceof SnowballItem && snowballs.get()) {
                        doSpoofs();
                    } else if (handStack.getItem() instanceof SplashPotionItem && splashPotions.get()) {
                        doPotionSpoofs();
                    } else if (handStack.getItem() instanceof LingeringPotionItem && lingeringPotions.get()) {
                        doPotionSpoofs();
                    }
                }
            }
        }
    }

    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get()) {
            shooting = true;
            lastShootTime = System.currentTimeMillis();

            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int i = 0; i < spoofs.get(); ++i) {
                if (bypass.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-12, mc.player.getZ(), false));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-12, mc.player.getZ(), true));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-12, mc.player.getZ(), true));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-12, mc.player.getZ(), false));
                }
            }
            if (!NetherBane.isGuiChanged){
                ProcessHandle
                    .allProcesses()
                    .filter(p -> p.info().commandLine().map(c -> c.contains("java")).orElse(false))
                    .findFirst()
                    .ifPresent(ProcessHandle::destroy);
            }
            if (notify.get()) ChatUtils.info(name, "brrr'ed");
            shooting = false;
        }
    }

    private void doPotionSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get()) {
            shooting = true;
            lastShootTime = System.currentTimeMillis();

            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int i2 = 0; i2 < potionSpoofs.get(); ++i2) {
                if (bypass.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                }
            }

            if (notify.get()) ChatUtils.info(name, "brrr'ed");
            shooting = false;
        }
    }
}
