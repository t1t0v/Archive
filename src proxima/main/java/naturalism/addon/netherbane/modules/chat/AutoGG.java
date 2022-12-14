package naturalism.addon.netherbane.modules.chat;

import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.modules.render.hud.EZHud;
import naturalism.addon.netherbane.utils.Timer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.*;


public class AutoGG extends Module {
    public AutoGG(){
        super(NetherBane.CHATPLUS, "auto-Ez", "");

    }

    private ArrayList<String> popMessageEN = new ArrayList<>(){{
        add("{player} pop totem");
        add("easily poped totem from a {player}");
        add("{player} lost his totem");
        add("{player} senpai, you must try harder");
        add("{player} better log out or you will die");
        add("{player} senpai, so you won`t have totems left");
        add("This is time to stock up on new totems");
        add("{player} senpai, how do you feel");
        add("Ehh, it was a good battle, but you continue to lose totems");
        add("{player}, good game bro");
        add("{player}, my bloodlust is getting stronger");
        add("poped totem easily by BaneForce");
    }};

    private ArrayList<String> popMessageRU = new ArrayList<>(){{
        add("Сэр {player}, вы потеряли еще один тотем");
        add("{player}у попнули тотем");
        add("Легко попнул тотем {player}");
        add("Извините, но вы кажется дропнули тотем");
        add("Эхх, битва с {player} движется к концу");
        add("{player}, старайтесь больше");
        add("{player}, скоро вы умрете");
        add("Сэр {player}, я рекомендую вам покинуть игру, а то вы умрете");
        add("Сэр {player}, вас уже попнули {pops} раз");
    }};

    private ArrayList<String> popMessageJAP = new ArrayList<>(){{

    }};

    private ArrayList<String> killMessagesEN = new ArrayList<>(){{
        add("{player} senpai, I think I accidentally killed you:(");
        add("{player} died after {pops} pop");
        add("{player}, you were a good competitor");
        add("A terrible power of NetherBane killed you");
        add("Don`t mess with Bane");
        add("{player} are too weak for power of NetherBane");
        add("Ouch");
    }};

    private ArrayList<String> killMessagesRU = new ArrayList<>(){{
        add("Вы были убиты с помощью силы проклятия пустоты");
        add("{player} вас убила сила пустоты");
        add("{player} вы были убиты после попинга {pops} тотемов");
        add("Лучше не шутить с проклятием");
        add("Кажется я случайно убил {player} не заметив его");
    }};

    private ArrayList<String> logMessagesEN = new ArrayList<>(){{
        add("{player} log out");
        add("{player} log out after popping {pops} totem");
    }};

    private ArrayList<String> logMessagesRU = new ArrayList<>(){{
        add("{player} вышел из игры");
        add("player} ливнул после того, как попнул {pops} тотемов");
    }};

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Boolean> ignoreFriends = sgDefault.add(new BoolSetting.Builder().name("ignore-friends").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Boolean> ignoreNoobs = sgDefault.add(new BoolSetting.Builder().name("ignore-noobs").description("Automatically rotates you towards the city block.").defaultValue(true).build());
    private final Setting<Double> range = sgDefault.add(new DoubleSetting.Builder().name("range").description("The range a player has to be in with to detect a pop.").defaultValue(7).min(0).max(50).build());

    private final SettingGroup sgTotemPop = settings.createGroup("Totem Pop");
    private final Setting<Boolean> totemPop = sgTotemPop.add(new BoolSetting.Builder().name("totem pop").description(".").defaultValue(false).build());
    private final Setting<Boolean>  useDefaultPopPhrases = sgTotemPop.add(new BoolSetting.Builder().name("use-default-phrases").description(".").defaultValue(true).visible(totemPop::get).build());
    private final Setting<Boolean> engPop = sgTotemPop.add(new BoolSetting.Builder().name("eng-phrases").description(".").defaultValue(true).visible(() -> totemPop.get() && useDefaultPopPhrases.get()).build());
    private final Setting<Boolean> rusPop = sgTotemPop.add(new BoolSetting.Builder().name("rus-phrases").description(".").defaultValue(false).visible(() -> totemPop.get() && useDefaultPopPhrases.get()).build());
    private final Setting<List<String>> popMessages = sgTotemPop.add(new StringListSetting.Builder().name("pop-messages").description("T.").defaultValue(Collections.emptyList()).visible(() -> totemPop.get() && !useDefaultPopPhrases.get()).build());
    private final Setting<Boolean>  randomPopMsg = sgTotemPop.add(new BoolSetting.Builder().name("random-msg").description(".").defaultValue(true).visible(totemPop::get).build());
    private final Setting<Integer> popDelay = sgTotemPop.add(new IntSetting.Builder().name("pop-delay").description("How long to wait in ticks before sending a kill message again.").defaultValue(5).min(0).visible(totemPop::get).build());

    private final SettingGroup sgKill = settings.createGroup("Kill");
    private final Setting<Boolean> kill = sgKill.add(new BoolSetting.Builder().name("kill").description(".").defaultValue(true).build());
    private final Setting<Boolean>  useDefaultKillPhrases = sgKill.add(new BoolSetting.Builder().name("use-default-phrases").description(".").defaultValue(true).visible(kill::get).build());
    private final Setting<Boolean> engKill = sgKill.add(new BoolSetting.Builder().name("eng-phrases").description(".").defaultValue(true).visible(() -> kill.get() && useDefaultKillPhrases.get()).build());
    private final Setting<Boolean> rusKill = sgKill.add(new BoolSetting.Builder().name("rus-phrases").description(".").defaultValue(false).visible(() -> kill.get() && useDefaultKillPhrases.get()).build());
    private final Setting<List<String>> killMessages = sgKill.add(new StringListSetting.Builder().name("kill-messages").description("T.").defaultValue(Collections.emptyList()).visible(() -> kill.get() && !useDefaultKillPhrases.get()).build());
    private final Setting<Boolean>  randomKillMsg = sgKill.add(new BoolSetting.Builder().name("random-msg").description(".").defaultValue(true).visible(kill::get).build());
    private final Setting<Integer> killDelay = sgKill.add(new IntSetting.Builder().name("kill-delay").description("How long to wait in ticks before sending a kill message again.").defaultValue(5).min(0).visible(kill::get).build());

    private final SettingGroup sgLog = settings.createGroup("Log");
    private final Setting<Boolean> log =  sgLog.add(new BoolSetting.Builder().name("log").description(".").defaultValue(false).build());
    private final Setting<Boolean>  useDefaultLogPhrases = sgLog.add(new BoolSetting.Builder().name("use-default-phrases").description(".").defaultValue(true).visible(kill::get).build());
    private final Setting<Boolean> engLog =  sgLog.add(new BoolSetting.Builder().name("eng-phrases").description(".").defaultValue(true).visible(() -> kill.get() && useDefaultKillPhrases.get()).build());
    private final Setting<Boolean> rusLog =  sgLog.add(new BoolSetting.Builder().name("rus-phrases").description(".").defaultValue(true).visible(() -> kill.get() && useDefaultKillPhrases.get()).build());
    private final Setting<List<String>> logMessages =  sgLog.add(new StringListSetting.Builder().name("log-messages").description("T.").defaultValue(Collections.emptyList()).visible(() -> kill.get() && !useDefaultKillPhrases.get()).build());
    private final Setting<Boolean>  randomLogMsg =  sgLog.add(new BoolSetting.Builder().name("random-msg").description(".").defaultValue(true).visible(kill::get).build());
    private final Setting<Integer> logDelay =  sgLog.add(new IntSetting.Builder().name("log-delay").description("How long to wait in ticks before sending a kill message again.").defaultValue(5).min(0).visible(kill::get).build());


    private HashMap<UUID, Integer> kills = new HashMap<>();
    private HashMap<UUID, Integer> pops = new HashMap<>();

    private Timer popTimer = new Timer();
    private Timer killTimer = new Timer();
    private Timer logTimer = new Timer();

    private Random random = new Random();
    private int i;


    @EventHandler
    private void onLog(EntityRemovedEvent event){
        if (pops.containsKey(event.entity.getUuid())){
            if (log.get() && logTimer.hasPassed(logDelay.get() * 50F)){
                sendLog((PlayerEntity) event.entity);
            }
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event){
        if (event.packet instanceof EntityStatusS2CPacket entityStatusS2CPacket){
            Entity entity = entityStatusS2CPacket.getEntity(mc.world);
            if (!(entity instanceof PlayerEntity)) return;
            PlayerEntity player = (PlayerEntity) entity;
            int status = entityStatusS2CPacket.getStatus();
            if (status != 35 && status != 3) return;


            if (!isValidTarget(player)) return;

            if (status == 35) {
                pops.putIfAbsent(player.getUuid(), 0);
                pops.replace(player.getUuid(), pops.get(player.getUuid()) + 1);

                if (totemPop.get() && popTimer.hasPassed(popDelay.get() * 50F)){
                    if (i >= popMessages.get().size()) i = 0;
                    else i++;
                    sendPop(player);
                    popTimer.reset();
                    return;
                }
            }
            if (status == 3){
                kills.putIfAbsent(player.getUuid(), 0);
                kills.replace(player.getUuid(), kills.get(player.getUuid()) + 1);

                if (kill.get() && killTimer.hasPassed(killDelay.get() * 50F)){
                    sendKill(player);
                    killTimer.reset();
                }

                pops.remove(player);
            }
        }
    }

    private void sendPop(PlayerEntity player){
        String string = getPopMessage(player);
        if (!string.equals("")) mc.player.sendChatMessage(string);
    }

    private String getPopMessage(PlayerEntity player){
        String mes = "";
        try {
            if (useDefaultPopPhrases.get()){

                if (engPop.get()) mes = randomPopMsg.get() ? popMessageEN.get(random.nextInt(popMessageEN.size())) : popMessageEN.get(i);
                else if (rusPop.get()) mes = randomPopMsg.get() ? popMessageRU.get(random.nextInt(popMessageRU.size())) : popMessageRU.get(i);
            }else {
                mes = randomPopMsg.get() ? popMessages.get().get(random.nextInt(popMessages.get().size() - 1)) : popMessages.get().get(i);
            }
        } catch (IllegalArgumentException ignored){}
        return setValues(mes, player);
    }

    private int timer;

    private void sendKill(PlayerEntity player){
        String string = getKillMessage(player);
        EZHud.isEz = true;
        timer = EZHud.tick;
        if (!string.equals("")) mc.player.sendChatMessage(string);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (timer <= 0){
            EZHud.isEz = false;
        }else timer--;
    }

    private String getKillMessage(PlayerEntity player){
        String mes = "";

        try {
            if (useDefaultKillPhrases.get()){
                if (engKill.get()) mes = randomKillMsg.get() ? killMessagesEN.get(random.nextInt(killMessagesEN.size())) : killMessagesEN.get(i);
                else if (rusKill.get()) mes = randomKillMsg.get() ? killMessagesRU.get(random.nextInt(killMessagesRU.size())) : killMessagesRU.get(i);
            }else {
                mes = randomKillMsg.get() ? killMessages.get().get(random.nextInt(killMessages.get().size())) : killMessages.get().get(i);
            }
        }catch (IllegalArgumentException ignored){}

        return setValues(mes, player);
    }

    private void sendLog(PlayerEntity player){
        String string = getLogMessage(player);
        if (!string.equals("")) mc.player.sendChatMessage(string);
    }

    private String getLogMessage(PlayerEntity player){
        String mes = "";
        try {
            if (useDefaultLogPhrases.get()){
                if (engLog.get()) mes = randomLogMsg.get() ? logMessagesEN.get(random.nextInt(logMessagesEN.size())) : logMessagesEN.get(i);
                else if (rusLog.get()) mes = randomLogMsg.get() ? logMessagesRU.get(random.nextInt(logMessagesRU.size())) : logMessagesRU.get(i);
            }else {
                mes = randomLogMsg.get() ? logMessages.get().get(random.nextInt(logMessages.get().size())) : logMessages.get().get(i);
            }
        }catch (IllegalArgumentException ignored){}

        return setValues(mes, player);
    }

    private String setValues(String string, PlayerEntity player){
        String finalString = string.replace("{player}", player.getEntityName());
        if (!pops.isEmpty()){
            finalString = finalString.replace("{pops}", String.valueOf(pops.get(player.getUuid())));
            if (pops.get(player.getUuid()) > 1) finalString = finalString.replace("totem", "totems");
            if (pops.get(player.getUuid()) <= 1) finalString = finalString.replace("totems", "totem");
        }

        return finalString;
    }

    private boolean isValidTarget(PlayerEntity player){
        if (player == mc.player) return false;
        if (ignoreFriends.get() && Friends.get().isFriend(player)) return false;
        if (ignoreNoobs.get() && player.getInventory().armor.isEmpty()) return false;
        if (mc.player.distanceTo(player) > range.get()) return false;

        return true;
    }


}
