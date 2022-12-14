package naturalism.addon.netherbane.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import naturalism.addon.netherbane.NetherBane;

import java.util.*;

public class SexAura extends Module {
    public SexAura(){
        super(NetherBane.CHATPLUS, "sex-aura", "-------------------------");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> messageDelay = sgGeneral.add(new IntSetting.Builder().name("message-delay").description("The delay between breaks.").defaultValue(100).min(0).build());
    private final Setting<Boolean> customName = sgGeneral.add(new BoolSetting.Builder().name("custom-name").description("").defaultValue(false).build());
    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder().name("name").description("Name to be replaced with.").defaultValue("Naturalism").visible(()->customName.get()).build());
    private final Setting<Boolean> random = sgGeneral.add(new BoolSetting.Builder().name("random-player").description("").defaultValue(false).build());
    private final Setting<Boolean> changeName = sgGeneral.add(new BoolSetting.Builder().name("change-name").description("").defaultValue(true).visible(()->random.get()).build());
    private final Setting<Integer> changeNameDelay = sgGeneral.add(new IntSetting.Builder().name("change-name-delay").description("The delay between breaks.").defaultValue(400).min(0).visible(()->random.get() && changeName.get()).build());
    private final Setting<Boolean> pm = sgGeneral.add(new BoolSetting.Builder().name("pm").description("").defaultValue(true).build());
    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder().name("messages").description("T.").defaultValue(Collections.emptyList()).build());

    private final ArrayList<String> texts = new ArrayList<String>() {{
        add("I love the things you do with your tongue");
        add("Honey, you’re the sexiest thing I’ve ever seen!");
        add("I want you so bad");
        add("Your wish is my command");
        add("I want to rub my pussy all over your face");
        add("Spray your juice all over my tits");
        add("Put your big fat baby maker inside my pussy and fuck my brain out. Right now!");
        add("This is how a real man fucks!");
        add("You look so sexy when you do that");
        add("You taste sooo good");
        add("Oh, that feels so good! Do that again!");
        add("No one has ever made me come as hard as you can");
        add("Tell me all the dirty little things you do when you masturbate, you naughty boy/girl. Tell me everything, baby. Tell me how you play with yourself.");
        add("Kiss me there… Lick every inch of me.");
        add("Come over here and ride me hard!");
        add("Fuck me. Right now!");
        add("It drives me crazy when you look at me that way.");
        add("Shut the fuck up! I’ll fuck you wherever I want, naughty little whore!");
        add("Your biceps look so muscular in that shirt…");
        add("Your ass looks hot in those jeans…");
        add("I bet you can’t guess what incredibly hot thoughts I’m having about you right now.");
        add("If you could do anything you want to me, what would you do?");
        add("Could you please come at my place and unhook my bra for me?");
        add("I can't stop thinking about your ass in those tight boxers you were wearing this morning.");
        add("I know exactly what you want, but I am going to make you beg for it before I’ll give it to you.");
        add("I want to show you how naughty I can be. ");
        add("I can’t stop thinking about some very dirty things… please, can you help me?");
        add("Do you want to see what a naughty girl I can be for you? Look at me and wink if so.");
        add("Just thinking of you gets my pussy wet.");
        add("I love you when you moan in excitement taking my name….but tell me what will it take to see you sweating in intense pleasure!");
        add("I want to see how good your tongue can play between my gaps.");
        add("Your clothes are coming off the moment get through the door.");
        add("I want you to boss me around tonight.");
        add("Get out of the gym sweetheart and save some energy for the Bang-Bang. I am going to make love to you and drive you wild between my legs tonight.");
        add("Oh, Baby! Even a thought of you makes me wet below my waist.");
        add("When I am with you my whole body tightens up in ecstasy with the sensation of your touch.");
        add("I love the way you get naughty in bed it keeps me seduced and excited.");
        add("I can feel your masculine power when I touch you down there. It’s really hot, heavy and hard like a rod.");
        add("Oh, Baby! Even a thought of you makes me wet below my waist.");
        add("Reveal four parts of my body you admire the most? I promise to give you at least two tonight for being honest.....deal!");
        add("I love the way you kiss me, especially the part where I moan and scream the most.");
        add("Tell me how desperate you are for me and why? If your reply is satisfactory we will move on to the next step to turn your desperation into reality.");
        add("I am planning to wear my favorite sexy red thong tonight. What about you?");
        add("I can’t wait, I’m dying to hug you because the way you hug makes me feel so hot and turned on.");
        add("It makes me feel amazingly hot when you touch me down there.");
        add("Tonight I want to enjoy the taste, touch, scent and feel of your skin next to mine.");
        add("Shut the fuck up! I’ll fuck you wherever I want, naughty little whore!");
        add("I am missing your toy so much that I started searching for sex toys on the internet.");
    }};

    private String playerName;
    private int i = 0;
    private int messageTick;
    private int changeTick;
    private Collection<PlayerListEntry> entry;

    @EventHandler
    private void onTick(TickEvent.Pre event){

        if (changeName.get()){
            if (changeTick >= changeNameDelay.get()){
                playerName = null;
                changeTick = 0;
            }else changeTick++;
        }
        if (playerName == null) {
            playerName = name.get();
            if (random.get()) playerName = getName();
        }
        if (playerName == null){
            return;
        }
        if (messageTick >= messageDelay.get()){
            texts.addAll(messages.get());
            int randomIndex = new Random().nextInt(texts.size());
            if (!pm.get()) mc.player.sendChatMessage(playerName + " " + texts.get(randomIndex));
            else mc.player.sendChatMessage("/msg " + playerName + " " + texts.get(randomIndex));
            messageTick = 0;
        }else messageTick++;

    }

    private String getName(){
        entry = mc.getNetworkHandler().getPlayerList();

        if (entry.isEmpty()) return null;

        i = Utils.random(0, entry.size());

        if (entry.stream().toList().get(i).getProfile().getName() != mc.player.getGameProfile().getName()){
            return entry.stream().toList().get(i).getProfile().getName();
        }
        return null;
    }

}
