package naturalism.addon.netherbane.modules.misc;

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
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RPC extends Module {
    public RPC(){
        super(NetherBane.MISCPLUS ,"NetherBane RPC", "");
    }

    public enum ImageMode{
        NetherBane,
        NetherBaneRainbow,
        UwU
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<ImageMode> imageMode = sgDefault.add(new EnumSetting.Builder<ImageMode>().name("image-mode").description("Block breaking method").defaultValue(ImageMode.NetherBane).build());
    private final Setting<Integer> tickDelay = sgDefault.add(new IntSetting.Builder().name("tick-delay").description("The delay between breaks.").defaultValue(20).min(0).sliderMax(20).build());

    private final Setting<List<String>> line1Strings = sgDefault.add(new StringListSetting.Builder().name("line-1-messages").description("Messages used for the first line.").defaultValue("Good game. bro").onChanged(strings -> recompileLine1()).build());
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
        DiscordIPC.start(952062620800798781L, null);

        rpc.setStart(System.currentTimeMillis() / 1000L);

        String largeText = "NetherBane";
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
            case NetherBane -> {
                rpc.setLargeImage("logo1", "NetherBane");
            }
            case UwU -> {
                if(imageTimer.hasPassed(tickDelay.get() * 50F)) rpc.setLargeImage("av1", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 2 * 50F)) rpc.setLargeImage("av2", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 3 * 50F)) rpc.setLargeImage("av3", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 4 * 50F)) rpc.setLargeImage("av4", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 5 * 50F)) rpc.setLargeImage("av5", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 6 * 50F)) rpc.setLargeImage("av6", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 7 * 50F)) rpc.setLargeImage("av7", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 8 * 50F)) rpc.setLargeImage("av8", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 9 * 50F)) rpc.setLargeImage("av9", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 10 * 50F)) rpc.setLargeImage("av10", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 11 * 50F)) imageTimer.reset();
            }
            case NetherBaneRainbow -> {
                if(imageTimer.hasPassed(tickDelay.get() * 50F)) rpc.setLargeImage("logo1", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 2 * 50F)) rpc.setLargeImage("logo2", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 3 * 50F)) rpc.setLargeImage("logo3", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 4 * 50F)) rpc.setLargeImage("logo4", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 5 * 50F)) rpc.setLargeImage("logo5", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 6 * 50F)) rpc.setLargeImage("logo6", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 7 * 50F)) rpc.setLargeImage("logo7", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 8 * 50F)) rpc.setLargeImage("logo8", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 9 * 50F)) rpc.setLargeImage("logo9", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 10 * 50F)) rpc.setLargeImage("logo10", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 11 * 50F)) rpc.setLargeImage("logo11", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 12 * 50F)) rpc.setLargeImage("logo12", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 13 * 50F)) rpc.setLargeImage("logo13", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 14 * 50F)) rpc.setLargeImage("logo14", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 15 * 50F)) rpc.setLargeImage("logo15", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 16 * 50F)) rpc.setLargeImage("logo16", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 17 * 50F)) rpc.setLargeImage("logo17", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 18 * 50F)) rpc.setLargeImage("logo18", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 19 * 50F)) rpc.setLargeImage("logo19", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 20 * 50F)) rpc.setLargeImage("logo20", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 21 * 50F)) rpc.setLargeImage("logo21", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 22 * 50F)) rpc.setLargeImage("logo22", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 23 * 50F)) rpc.setLargeImage("logo23", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 24 * 50F)) rpc.setLargeImage("logo24", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 25 * 50F)) rpc.setLargeImage("logo25", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 26 * 50F)) rpc.setLargeImage("logo26", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 27 * 50F)) rpc.setLargeImage("logo27", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 28 * 50F)) rpc.setLargeImage("logo28", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 29 * 50F)) rpc.setLargeImage("logo29", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 30 * 50F)) rpc.setLargeImage("logo30", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 31 * 50F)) rpc.setLargeImage("logo31", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 32 * 50F)) rpc.setLargeImage("logo32", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 33 * 50F)) rpc.setLargeImage("logo33", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 34 * 50F)) rpc.setLargeImage("logo34", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 35 * 50F)) rpc.setLargeImage("logo35", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 36 * 50F)) rpc.setLargeImage("logo36", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 37 * 50F)) rpc.setLargeImage("logo37", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 38 * 50F)) rpc.setLargeImage("logo38", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 39 * 50F)) rpc.setLargeImage("logo39", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 40 * 50F)) rpc.setLargeImage("logo40", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 41 * 50F)) rpc.setLargeImage("logo41", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 42 * 50F)) rpc.setLargeImage("logo42", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 43 * 50F)) rpc.setLargeImage("logo43", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 44 * 50F)) rpc.setLargeImage("logo44", "NetherBane");
                if(imageTimer.hasPassed(tickDelay.get() * 45 * 50F)) imageTimer.reset();
            }
        }

        if (lineTimer.hasPassed(line1UpdateDelay.get() * 50F)){
            try {
                rpc.setDetails(MeteorStarscript.ss.run(line1Scripts.get(new Random().nextInt(line1Scripts.size()))));
            } catch (StarscriptError | IndexOutOfBoundsException e) {
                ChatUtils.error("Starscript", e.getMessage());
            }
            lineTimer.reset();
        }

        // Update
        DiscordIPC.setActivity(rpc);
    }
}
