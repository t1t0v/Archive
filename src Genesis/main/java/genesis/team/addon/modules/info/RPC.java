package genesis.team.addon.modules.info;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.Timer;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.StarscriptError;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RPC extends Module {
    public RPC(){
        super(Genesis.Info ,"Ezra RPC", "");
    }

    public enum ImageMode{
        Girl,
        GirlFull,
        Girls
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<ImageMode> imageMode = sgDefault.add(new EnumSetting.Builder<ImageMode>().name("image-mode").description("Block breaking method").defaultValue(ImageMode.Girl).build());
    private final Setting<Integer> tickDelay = sgDefault.add(new IntSetting.Builder().name("tick-delay").description("The delay between breaks.").defaultValue(20).min(0).sliderMax(20).build());

    private final Setting<List<String>> line1Strings = sgDefault.add(new StringListSetting.Builder().name("line-1-messages").description("Messages used for the first line.").defaultValue(":З").onChanged(strings -> recompileLine1()).build());
    private final Setting<Integer> line1UpdateDelay = sgDefault.add(new IntSetting.Builder().name("line-1-update-delay").description("How fast to update the first line in ticks.").defaultValue(200).min(10).sliderRange(10, 200).build());

    private static final RichPresence rpc = new RichPresence();

    private void recompile(List<String> messages, List<Script> scripts) {
        scripts.clear();

        for (int i = 0; i < messages.size(); i++) {
            Parser.Result result = Parser.parse(messages.get(i));

            if (result.hasErrors()) {
                if (Utils.canUpdate()) {
                    MeteorStarscript.printChatError(i, result.errors.get(0));
                }

                continue;
            }

            scripts.add(Compiler.compile(result));
        }
    }

    private void recompileLine1() {
        recompile(line1Strings.get(), line1Scripts);
    }

    @Override
    public void onActivate() {
        DiscordIPC.start(997893583778627725L, null);

        rpc.setStart(System.currentTimeMillis() / 1000L);

        String largeText = "uwu";
        String largeText1 = ":З";
        String largeText2 = "ohh";
        String largeText3 = "omg";
        String largeText4 = "good";
        String largeText6 = "just do it";
        String largeText7 = "I really love you";
        String largeText8 = "cool";
        String largeText9 = "***";
        String largeText10 = "Stay With Me";
        rpc.setLargeImage("logo1", largeText);
    }

    private final Timer imageTimer = new Timer();
    private final Timer lineTimer = new Timer();

    private final List<Script> line1Scripts = new ArrayList<>();


    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        // Image

        switch (imageMode.get()){
            case Girl -> {
                rpc.setLargeImage("logo1", "Stay With Me");
            }
            case Girls -> {
                if(imageTimer.hasPassed(tickDelay.get() * 50F)) rpc.setLargeImage("logo", "ughh");
                if(imageTimer.hasPassed(tickDelay.get() * 2 * 50F)) rpc.setLargeImage("logo2", ":З");
                if(imageTimer.hasPassed(tickDelay.get() * 3 * 50F)) rpc.setLargeImage("logo2", ":З");
                if(imageTimer.hasPassed(tickDelay.get() * 4 * 50F)) rpc.setLargeImage("logo3", "ohh");
                if(imageTimer.hasPassed(tickDelay.get() * 5 * 50F)) rpc.setLargeImage("logo4", "omg");
                if(imageTimer.hasPassed(tickDelay.get() * 6 * 50F)) rpc.setLargeImage("logo5", "good");
                if(imageTimer.hasPassed(tickDelay.get() * 7 * 50F)) rpc.setLargeImage("logo6", "just do it");
                if(imageTimer.hasPassed(tickDelay.get() * 8 * 50F)) rpc.setLargeImage("logo7", "I really love you");
                if(imageTimer.hasPassed(tickDelay.get() * 9 * 50F)) rpc.setLargeImage("logo8", "cool");
                if(imageTimer.hasPassed(tickDelay.get() * 10 * 50F)) rpc.setLargeImage("logo9", "***");
                if(imageTimer.hasPassed(tickDelay.get() * 11 * 50F)) rpc.setLargeImage("logo10", "Stay With Me");
                if(imageTimer.hasPassed(tickDelay.get() * 12 * 50F)) imageTimer.reset();
            }
            case GirlFull -> {
                if(imageTimer.hasPassed(tickDelay.get() * 50F)) rpc.setLargeImage("logo1", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 2 * 50F)) rpc.setLargeImage("logo2", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 3 * 50F)) rpc.setLargeImage("logo3", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 4 * 50F)) rpc.setLargeImage("logo4", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 5 * 50F)) rpc.setLargeImage("logo5", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 6 * 50F)) rpc.setLargeImage("logo6", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 7 * 50F)) rpc.setLargeImage("logo7", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 8 * 50F)) rpc.setLargeImage("logo8", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 9 * 50F)) rpc.setLargeImage("logo9", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 10 * 50F)) rpc.setLargeImage("logo10", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 11 * 50F)) rpc.setLargeImage("logo11", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 12 * 50F)) rpc.setLargeImage("logo12", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 13 * 50F)) rpc.setLargeImage("logo13", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 14 * 50F)) rpc.setLargeImage("logo14", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 15 * 50F)) rpc.setLargeImage("logo15", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 16 * 50F)) rpc.setLargeImage("logo16", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 17 * 50F)) rpc.setLargeImage("logo17", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 18 * 50F)) rpc.setLargeImage("logo18", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 19 * 50F)) rpc.setLargeImage("logo19", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 20 * 50F)) rpc.setLargeImage("logo20", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 21 * 50F)) rpc.setLargeImage("logo21", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 22 * 50F)) rpc.setLargeImage("logo22", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 23 * 50F)) rpc.setLargeImage("logo23", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 24 * 50F)) rpc.setLargeImage("logo24", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 25 * 50F)) rpc.setLargeImage("logo25", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 26 * 50F)) rpc.setLargeImage("logo26", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 27 * 50F)) rpc.setLargeImage("logo27", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 28 * 50F)) rpc.setLargeImage("logo28", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 29 * 50F)) rpc.setLargeImage("logo29", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 30 * 50F)) rpc.setLargeImage("logo30", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 31 * 50F)) rpc.setLargeImage("logo31", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 32 * 50F)) rpc.setLargeImage("logo32", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 33 * 50F)) rpc.setLargeImage("logo33", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 34 * 50F)) rpc.setLargeImage("logo34", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 35 * 50F)) rpc.setLargeImage("logo35", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 36 * 50F)) rpc.setLargeImage("logo36", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 37 * 50F)) rpc.setLargeImage("logo37", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 38 * 50F)) rpc.setLargeImage("logo38", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 39 * 50F)) rpc.setLargeImage("logo39", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 40 * 50F)) rpc.setLargeImage("logo40", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 41 * 50F)) rpc.setLargeImage("logo41", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 42 * 50F)) rpc.setLargeImage("logo42", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 43 * 50F)) rpc.setLargeImage("logo43", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 44 * 50F)) rpc.setLargeImage("logo44", "Who are you?");
                if(imageTimer.hasPassed(tickDelay.get() * 45 * 50F)) imageTimer.reset();
            }
        }

        if (lineTimer.hasPassed(line1UpdateDelay.get() * 50F)){
            try {
                rpc.setDetails(String.valueOf(MeteorStarscript.ss.run(line1Scripts.get(new Random().nextInt(line1Scripts.size())))));
            } catch (StarscriptError | IndexOutOfBoundsException e) {
                ChatUtils.error("Starscript", e.getMessage());
            }
            lineTimer.reset();
        }

        // Update
        DiscordIPC.setActivity(rpc);
    }
}
