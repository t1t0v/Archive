package genesis.team.addon.modules.info.Translator;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.text.Text;

import java.util.Objects;

public class Translator extends Module {
    public Translator(){
        super(Genesis.Info, "translator", "translator");
    }

    private static final GoogleTranslate googleTranslate =
        new GoogleTranslate();


    private final SettingGroup sgOthers = settings.createGroup("Other`s messages");

    private final Setting<Boolean> others = sgOthers.add(new BoolSetting.Builder()
        .name("others")
        .description(".")
        .defaultValue(true)
        .build()
    );

    private final Setting<FromLanguage> fromChat = sgOthers.add(new EnumSetting.Builder<FromLanguage>()
        .name("from-language")
        .description(".")
        .defaultValue(FromLanguage.ENGLISH)
        .build()
    );

    private final Setting<ToLanguage> toChat = sgOthers.add(new EnumSetting.Builder<ToLanguage>()
        .name("to-language")
        .description(".")
        .defaultValue(ToLanguage.RUSSIAN)
        .build()
    );

    private final SettingGroup sgYour = settings.createGroup("Your Messages");

    private final Setting<Boolean> your = sgYour.add(new BoolSetting.Builder()
        .name("your")
        .description(".")
        .defaultValue(false)
        .build()
    );

    private final Setting<FromLanguage> fromMy = sgYour.add(new EnumSetting.Builder<FromLanguage>()
        .name("from-language")
        .description(".")
        .defaultValue(FromLanguage.ENGLISH)
        .build()
    );

    private final Setting<ToLanguage> toMy = sgYour.add(new EnumSetting.Builder<ToLanguage>()
        .name("to-language")
        .description(".")
        .defaultValue(ToLanguage.RUSSIAN)
        .build()
    );


    @EventHandler
    private void onSend(SendMessageEvent event){
        if (!your.get()) return;
        event.message = Objects.requireNonNull(translateMy(event.message));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMessageRecieve(ReceiveMessageEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (!others.get()) return;
        String msg = event.getMessage().getString();

        Runnable task = () -> translateOthers(msg);

        Thread thread = new Thread(task);
        if (thread.getThreadGroup().activeCount() > 1 || thread.getState() == Thread.State.TERMINATED){
            thread.start();
        }else {
            thread.interrupt();
        }
    }



    private String translateMy(String text)
    {
        String incomingMsg = text;

        String translatorPrefix =
            "\u00a7a[\u00a7b" + toMy.get().name + "\u00a7a]:\u00a7r ";

        if(incomingMsg.startsWith("[UWUAddon]")
            || incomingMsg.startsWith(translatorPrefix))
            return null;

        if (fromMy.get().value == toMy.get().value){
            return text;
        }

        String translated = googleTranslate.translate(incomingMsg,
            fromMy.get().value, toMy.get().value);

        return translated;
    }

    private void translateOthers(String text)
    {
        String incomingMsg = text;

        String translatorPrefix =
            "\u00a7a[\u00a7b" + toChat.get().name + "\u00a7a]:\u00a7r ";

        if(incomingMsg.startsWith("[UWUAddon]")
            || incomingMsg.startsWith(translatorPrefix))
            return;

        if (Objects.equals(fromChat.get().value, toChat.get().value)){
            return;
        }

        String translated = googleTranslate.translate(incomingMsg,
            fromChat.get().value, toChat.get().value);

        if(translated == null)
            return;

        Text translationMsg = Text.literal(translatorPrefix + translated);

        mc.inGameHud.getChatHud().addMessage(translationMsg);
    }

    public enum FromLanguage
    {
        AUTO_DETECT("Detect Language", "auto"),
        ARABIC("Arabic", "ar"),
        CHINESE_SIMPLIFIED("Chinese (simplified)", "zh-CN"),
        CHINESE_TRADITIONAL("Chinese (traditional)", "zh-TW"),
        ENGLISH("English", "en"),
        FRENCH("French", "fr"),
        GERMAN("Deutsch!", "de"),
        ITALIAN("Italian", "it"),
        JAPANESE("Japanese", "ja"),
        KOREAN("Korean", "ko"),
        POLISH("Polish", "pl"),
        PORTUGUESE("Portugese", "pt"),
        RUSSIAN("Russian", "ru"),
        TURKISH("Turkish", "tr");

        private final String name;
        private final String value;

        FromLanguage(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    public enum ToLanguage
    {
        ARABIC("Arabic", "ar"),
        CHINESE_SIMPLIFIED("Chinese (simplified)", "zh-CN"),
        CHINESE_TRADITIONAL("Chinese (traditional)", "zh-TW"),
        ENGLISH("English", "en"),
        FRENCH("French", "fr"),
        GERMAN("Deutsch!", "de"),
        ITALIAN("Italian", "it"),
        JAPANESE("Japanese", "ja"),
        KOREAN("Korean", "ko"),
        POLISH("Polish", "pl"),
        PORTUGUESE("Portugese", "pt"),
        RUSSIAN("Russian", "ru"),
        TURKISH("Turkish", "tr");

        private final String name;
        private final String value;

        ToLanguage(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}
