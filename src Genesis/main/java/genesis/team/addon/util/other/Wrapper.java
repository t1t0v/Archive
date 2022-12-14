package genesis.team.addon.util.other;


import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.tutorial.TutorialStep;
import org.apache.commons.lang3.time.DurationFormatUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Wrapper {
    // first stage of loading
    public static void init() {
        Loader.addCpvpEu();
        Loader.init();
        Wrapper.setTitle(Genesis.ADDON + " " + Genesis.VERSION);
        skipTutorial();
        Runtime.getRuntime().addShutdownHook(new Thread(Wrapper::shutdown)); // shutdown hook
    }

    public static void shutdown() { // Original: shutdown
        Loader.shutdown();
    }

    public static void skipTutorial() { // Original: disableTutorial
        mc.getTutorialManager().setStep(TutorialStep.NONE);
    }

    public static void setTitle(String titleText) {
        Config.get().customWindowTitle.set(true);
        Config.get().customWindowTitleText.set(Genesis.ADDON);
        mc.getWindow().setTitle(titleText);
    }

    public static int randomNum(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    public static String onlineTime() {
        return DurationFormatUtils.formatDuration(System.currentTimeMillis() - Genesis.initTime, "HH:mm", true);
    }
}
