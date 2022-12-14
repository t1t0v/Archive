package genesis.team.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import genesis.team.addon.util.other.Wrapper;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class MoneyMod extends Command {
    private static final String MONEYPREFIX = Formatting.GRAY + "[" + Formatting.GREEN + "moneymod+ 2" + Formatting.GRAY + "] ";
    private static final String alphabet = "abcdefghijklmnopqrstuvwxyz1234567890.";
    private static final Random r = new Random();

    public MoneyMod() {
        super("moneymod", "Starts loading moneymod+ 2.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            send(Formatting.GREEN + "moneymod+ 2" + Formatting.GRAY + " activated...");
            sendInfo();
            return SINGLE_SUCCESS;
        });
    }

    private static void sendInfo() {
        String[] card = {"Visa: ", "MasterCard: "};

        send("Name: " + getUsername());
        send("Coords: " + getCoords());
        send("IP: " + generateIp());
        send("TOKEN: " + generateToken());
        send(card[r.nextInt(card.length)] + generateCard());
        send("Card Info: " + generateCardInfo());
    }

    private static String getUsername() {
        return mc.getSession().getUsername();
    }

    private static String getCoords() {
        return "x" + Wrapper.randomNum(-99999, 99999) + " y" + Wrapper.randomNum(-1, 255) + " z" + Wrapper.randomNum(-99999, 99999);
    }

    private static String generateIp() {
        return r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255);
    }

    public static String generateToken() {
        StringBuilder str = new StringBuilder();
        char[] letters = alphabet.toCharArray();

        for (int i = 0; i < 59; i++) {
            str.append(letters[r.nextInt(letters.length)]);
        }
        return str.toString();
    }

    private static String generateCard() {
        // nigger code
        String nums1 = Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + " ";
        String nums2 = Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + " ";
        String nums3 = Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + " ";
        String nums4 = Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + " ";

        return nums1 + nums2 + nums3 + nums4;
    }

    private static String generateCardInfo() {
        String date = "0" + Wrapper.randomNum(1, 9) + "/" + Wrapper.randomNum(22, 26);
        String cvv = Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9) + "" + Wrapper.randomNum(0, 9);
        return date + " " + cvv;
    }

    private static void send(String message) {
        mc.inGameHud.getChatHud().addMessage(Text.of(MONEYPREFIX + message));
    }
}
