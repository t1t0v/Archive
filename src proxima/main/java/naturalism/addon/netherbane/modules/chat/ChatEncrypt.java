package naturalism.addon.netherbane.modules.chat;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import naturalism.addon.netherbane.NetherBane;

public class ChatEncrypt extends Module {
    public ChatEncrypt(){
        super(NetherBane.CHATPLUS, "chat-encrypter", "");
    }

    @EventHandler
    private void onSend(SendMessageEvent event){
        try {
            event.message = "[CE]";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMessageRecieve(ReceiveMessageEvent event) {
        if (mc.world == null || mc.player == null) return;
        String msg = event.getMessage().getString();

        Runnable task = () -> {
            String cryptPrefix =
                "\u00a7a[\u00a7b" + "ChatEncrypt" + "\u00a7a]:\u00a7r ";

            if(msg.startsWith("[UWUAddon]")
                || msg.startsWith(cryptPrefix))
                return;

            String decrypted = null;
            try {

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (decrypted == null) return;

            Text translationMsg = new LiteralText(cryptPrefix)
                .append(new LiteralText(decrypted));
            mc.inGameHud.getChatHud().addMessage(translationMsg);
        };

        Thread thread = new Thread(task);
        if (thread.getThreadGroup().activeCount() > 1 || thread.getState() == Thread.State.TERMINATED){
            thread.start();
        }else {
            thread.interrupt();
        }
    }

    private static final String ALGO = "AES";
    private static final byte[] keyValue =
        new byte[]{'T', 'h', 'e', 'B', 'e', 's', 't', 'S', 'e', 'c', 'r', 'e', 't', 'K', 'e', 'y'};




}
