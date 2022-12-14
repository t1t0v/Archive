package naturalism.addon.netherbane.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import naturalism.addon.netherbane.NetherBane;

import java.util.UUID;

public class AutoLog extends Module {
    public AutoLog(){
        super(NetherBane.MISCPLUS, "AutoLog", "------------------");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Keybind> disconnect = sgGeneral.add(new KeybindSetting.Builder().name("disconnect").description(".").defaultValue(Keybind.fromKey(8)).build());
    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder().name("auto-disable").description(".").defaultValue(true).build());

    private final SettingGroup sgTotemHave = settings.createGroup("Totem Have");
    private final Setting<Boolean> totemHave = sgTotemHave.add(new BoolSetting.Builder().name("min-totem-count").description(".").defaultValue(false).build());
    private final Setting<Integer> minTotems = sgTotemHave.add(new IntSetting.Builder().name("min-totems-count").description(".").defaultValue(2).min(0).sliderMin(0).max(50).sliderMax(50).visible(totemHave::get).build());
    private final Setting<Boolean> chatTotemCount = sgTotemHave.add(new BoolSetting.Builder().name("chat").description(".").defaultValue(false).visible(totemHave::get).build());
    private final Setting<String> chatTextTotemCount = sgTotemHave.add(new StringSetting.Builder().name("chat-text").description(".").defaultValue("Oh no, I have only {totem} totems").visible(()->chatTotemCount.get() && totemHave.get()).build());
    private final Setting<Boolean> clientTotemCount = sgTotemHave.add(new BoolSetting.Builder().name("client").description(".").defaultValue(true).visible(totemHave::get).build());
    private final Setting<String> clientTextTotemCount = sgTotemHave.add(new StringSetting.Builder().name("client-text").description(".").defaultValue("Oh no, I have only {totem} totems").visible(()->clientTotemCount.get() && totemHave.get()).build());

    private final SettingGroup sgTotemPop = settings.createGroup("Totem Pop");
    private final Setting<Boolean> totemPop = sgTotemPop.add(new BoolSetting.Builder().name("on-totem-pop").description(".").defaultValue(true).build());
    private final Setting<Integer> popTotems = sgTotemPop.add(new IntSetting.Builder().name("pop-totems").description(".").defaultValue(2).min(0).sliderMin(0).max(50).sliderMax(50).visible(()->totemPop.get()).build());
    private final Setting<Boolean> chatTotem = sgTotemPop.add(new BoolSetting.Builder().name("chat").description(".").defaultValue(false).visible(totemPop::get).build());
    private final Setting<String> chatTextTotem = sgTotemPop.add(new StringSetting.Builder().name("chat-text").description(".").defaultValue("Oh no, I have popped {totem} totems").visible(()->chatTotem.get() && totemPop.get()).build());
    private final Setting<Boolean> clientTotem = sgTotemPop.add(new BoolSetting.Builder().name("client").description(".").defaultValue(true).visible(totemPop::get).build());
    private final Setting<String> clientTextTotem = sgTotemPop.add(new StringSetting.Builder().name("client-text").description(".").defaultValue("Oh no, I have popped {totem} totems").visible(()->clientTotem.get() && totemPop.get()).build());

    private final SettingGroup sgPing = settings.createGroup("Ping");
    private final Setting<Boolean> onPing = sgTotemPop.add(new BoolSetting.Builder().name("on-ping").description(".").defaultValue(true).build());
    private final Setting<Integer> maxPing = sgPing.add(new IntSetting.Builder().name("max-ping").description(".").defaultValue(700).min(0).sliderMin(0).max(10000).sliderMax(10000).visible(onPing::get).build());
    private final Setting<Boolean> chatPing = sgPing.add(new BoolSetting.Builder().name("chat").description(".").defaultValue(false).visible(onPing::get).build());
    private final Setting<String> chatTextPing = sgPing.add(new StringSetting.Builder().name("chat-text").description(".").defaultValue("Oh no, I have {ping} ping").visible(()->chatPing.get() && onPing.get()).build());
    private final Setting<Boolean> clientPing = sgPing.add(new BoolSetting.Builder().name("client").description(".").defaultValue(true).visible(onPing::get).build());
    private final Setting<String> clientTextPing = sgPing.add(new StringSetting.Builder().name("client-text").description(".").defaultValue("Oh no, I have {ping} ping").visible(()->clientPing.get() && onPing.get()).build());

    private final Object2IntMap<UUID> totemPops = new Object2IntOpenHashMap<>();
    private int pops;
    private int ping;

    @EventHandler
    private void onLeave(GameLeftEvent event){
        pops = 0;
        ping = 0;
    }

    @EventHandler
    private void onJoin(GameJoinedEvent event){
        ping = 0;
        pops = 0;
    }

    @EventHandler
    private void onMinTotemCount(TickEvent.Pre event){
        if (totemHave.get()){
            int count = InvUtils.find(Items.TOTEM_OF_UNDYING).count();
            if (count < minTotems.get()){
                Text client = new LiteralText(clientTextTotemCount.get().replace("{totem}", Integer.toString(count)));
                Text chat = new LiteralText(chatTextTotemCount.get().replace("{totem}", Integer.toString(count)));
                if (clientTotemCount.get()) {
                    mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(client));
                    if (autoDisable.get()) toggle();
                } else if (chatTotemCount.get()) {
                    mc.player.sendChatMessage(chat.toString());
                    mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(client));
                    if (autoDisable.get()) toggle();
                }
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (totemPop.get()){
            if (!(event.packet instanceof EntityStatusS2CPacket)) return;

            EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
            if (p.getStatus() != 35) return;

            Entity entity = p.getEntity(mc.world);

            if (entity == null
            ) return;

            if (entity == mc.player){
                synchronized (totemPops) {
                    pops = totemPops.getOrDefault(entity.getUuid(), 0);
                    totemPops.put(entity.getUuid(), ++pops);

                    if (pops >= popTotems.get()){
                        disconnectOnTotemPop(pops);
                        pops = 0;
                    }
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (disconnect.get().isPressed()){
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("Disconnect")));
        }

        if (onPing.get()) {
            try {
                PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                assert playerListEntry != null;
                if (playerListEntry.getLatency() >= maxPing.get()) {
                    ping = playerListEntry.getLatency();
                    disconnectOnHighPing(ping);
                }
            }catch (NullPointerException ignored){}

        }
    }

    private void disconnectOnTotemPop(int pops){
        Text client = new LiteralText(clientTextTotem.get().replace("{totem}", Integer.toString(pops)));
        Text chat = new LiteralText(chatTextTotem.get().replace("{totem}", Integer.toString(pops)));
        if (clientTotem.get()) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(client));
            if (autoDisable.get()) toggle();
        } else if (chatTotem.get()) {
            mc.player.sendChatMessage(chat.toString());
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(client));
            if (autoDisable.get()) toggle();
        }
    }

    private void disconnectOnHighPing(int ping){
        Text client = new LiteralText(clientTextPing.get().replace("{ping}", Integer.toString(ping)));
        Text chat = new LiteralText(chatTextPing.get().replace("{ping}", Integer.toString(ping)));
        if (clientPing.get()) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(client));
            if (autoDisable.get()) toggle();
        } else if (chatPing.get()) {
            mc.player.sendChatMessage(chat.toString());
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(client));
            if (autoDisable.get()) toggle();
        }
    }
}
