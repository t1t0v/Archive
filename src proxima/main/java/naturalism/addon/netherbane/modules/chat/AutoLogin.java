package naturalism.addon.netherbane.modules.chat;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import naturalism.addon.netherbane.NetherBane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AutoLogin extends Module {
    public AutoLogin(){
        super(NetherBane.CHATPLUS, "auto-log-in", "-");

    }

    private String currMsgK = "", currMsgV = "", currMsgH = "";
    private boolean sended;
    private boolean need;
    private String password;
    List<Line> lines = new ArrayList<>();

    @EventHandler
    private void onLeft(GameLeftEvent event){
        sended = false;
    }

    @EventHandler
    private void onSend(SendMessageEvent event){
        if (Objects.equals(event.message, "/login " + password)){
            sended = true;
            need = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (!sended && need){
            for (Line line : lines) {
                if (!Utils.getWorldName().equals(line.ip)) continue;
                else {
                    if (!mc.getSession().getUsername().equals(line.nickname)) continue;
                    else {
                        mc.player.sendChatMessage("/login " + line.password);
                        password = line.password;
                        sended = true;
                        need = false;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMessageRecieve(ReceiveMessageEvent event) {
        if (mc.world == null || mc.player == null) return;
        String msg = event.getMessage().getString();

        if (msg.contains("/login ")){
            need = true;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        fillTable(theme, table);
        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        table.clear();
        lines.forEach((key) -> {
            table.add(theme.label(key.ip)).expandCellX();
            table.add(theme.label(key.nickname)).expandCellX();
            table.add(theme.label(key.password)).expandCellX();
            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                lines.remove(key);
                fillTable(theme,table);
            };
            table.row();
        });
        WTextBox textBoxK = table.add(theme.textBox(currMsgK)).minWidth(200).expandX().widget();
        textBoxK.action = () -> {
            currMsgK = textBoxK.get();
        };
        WTextBox textBoxV = table.add(theme.textBox(currMsgV)).minWidth(200).expandX().widget();
        textBoxV.action = () -> {
            currMsgV = textBoxV.get();
        };
        WTextBox textBoxH = table.add(theme.textBox(currMsgH)).minWidth(200).expandX().widget();
        textBoxH.action = () -> {
            currMsgH = textBoxH.get();
        };
        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            if (currMsgK != ""  && currMsgV != "") {
                lines.add(new Line(currMsgK, currMsgV, currMsgH));
                currMsgK = ""; currMsgV = ""; currMsgH = "";
                fillTable(theme,table);
            }
            Config.get().save();
        };
        table.row();
    }

    public class Line{
        public final String ip;
        public final String nickname;
        public final String password;

        public Line(String ip, String nickname, String password){
            this.ip = ip;
            this.nickname = nickname;
            this.password = password;
        }
    }
}
