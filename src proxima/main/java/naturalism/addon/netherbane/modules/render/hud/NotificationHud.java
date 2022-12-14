package naturalism.addon.netherbane.modules.render.hud;


import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.BoundingBox;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import static meteordevelopment.meteorclient.utils.Utils.WHITE;

public class NotificationHud extends HudElement {

    public NotificationHud(HUD hud){
        super(hud, "uwu-notification-hud", "hud for notification system", false);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> maxCount = sgGeneral.add(new IntSetting.Builder().name("max-count").description("The delay to remove messages").defaultValue(6).min(1).sliderMax(20).build());
    private final Setting<Integer> removeDelay = sgGeneral.add(new IntSetting.Builder().name("update-delay").description("The delay to remove messages").defaultValue(300).min(1).sliderMax(2000).build());
    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder().name("text-color").description("text-color").defaultValue(new SettingColor(255, 255, 255, 255)).build());

    public static ArrayList<Notification> notifications = new ArrayList<>();

    public enum NotificationType{
        Info,
        Warning,
        Totem,
        Death,
        View,
        Mine,
        IronMine,
        Helmet,
        Chestplate,
        Legging,
        Boots
    }

    int timer;
    int i;

    @Override
    public void update(HudRenderer renderer) {
        messageHelper();
        double width = 0;
        double height = 0;
        if (notifications.isEmpty()) {
            String t = "Notifications";
            width = Math.max(width, renderer.textWidth(t));
            height = renderer.textHeight();
        } else {
            try {
                for (Notification mes :  notifications) {
                    width = 261;
                    height = renderer.textHeight() * 2 + 25;
                }
            }catch (ConcurrentModificationException ignored){}

        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        int w = (int) box.width + 118;
        int h = (int) box.height;
        i = 0;

        if (isInEditor()) {
            renderer.text("Notifications", x, y , textColor.get());
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x, y, w, h, new Color(15, 15, 17, 80));
            Renderer2D.COLOR.render(null);
            return;
        }

        if (notifications.isEmpty()) {
            String t = "";
            renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, textColor.get());
        }else {
            try {
                for (Notification notification : notifications){
                    notification.render(renderer, x, y, w, h, textColor.get(), box);
                    y -= renderer.textHeight() * 2 + 25;
                    if (i >= 0) y -= 20;
                    i++;
                }
            }catch (ConcurrentModificationException ignored){}

        }

    }

    public static class Notification{
        public String mes;
        public NotificationType type;


        public Notification(String mes, NotificationType type){
            this.mes = mes;
            this.type = type;
        }

        public void render(HudRenderer renderer, double x, double y, int w, int h, Color textColor, BoundingBox box){
            if (!mes.isEmpty()){
                Renderer2D.COLOR.begin();
                Renderer2D.COLOR.quad(x, y, w, h, new Color(15, 15, 17, 220));
                switch (type){
                    case Info -> {
                        Renderer2D.COLOR.boxLines(x + 0.5, y + 0.5, box.width + 118, box.height, new Color(87, 204, 79, 255));
                        Renderer2D.COLOR.boxLines(x + 1, y + 1, box.width + 118, box.height, new Color(87, 204, 79, 255));
                        Renderer2D.COLOR.boxLines(x + 1.5, y + 1.5, box.width + 118, box.height, new Color(87, 204, 79, 255));
                    }
                    case Warning -> {
                        Renderer2D.COLOR.boxLines(x + 0.5, y + 0.5, box.width + 118, box.height, new Color(249, 235, 70, 255));
                        Renderer2D.COLOR.boxLines(x + 1, y + 1, box.width + 118, box.height, new Color(249, 235, 70, 255));
                        Renderer2D.COLOR.boxLines(x + 1.5, y + 1.5, box.width + 118, box.height, new Color(249, 235, 70, 255));
                    }
                    case View -> {
                        Renderer2D.COLOR.boxLines(x + 0.5, y + 0.5, box.width + 118, box.height, new Color(0, 255, 171, 255));
                        Renderer2D.COLOR.boxLines(x + 1, y + 1, box.width + 118, box.height, new Color(0, 255, 171, 255));
                        Renderer2D.COLOR.boxLines(x + 1.5, y + 1.5, box.width + 118, box.height, new Color(0, 255, 171, 2555));
                    }
                    case Totem -> {
                        Renderer2D.COLOR.boxLines(x + 0.5, y + 0.5, box.width + 118, box.height, new Color(251, 178, 0, 255));
                        Renderer2D.COLOR.boxLines(x + 1, y + 1, box.width + 118, box.height, new Color(251, 178, 0, 255));
                        Renderer2D.COLOR.boxLines(x + 1.5, y + 1.5, box.width + 118, box.height, new Color(251, 178, 0, 255));
                    }
                    case Death -> {
                        Renderer2D.COLOR.boxLines(x + 0.5, y + 0.5, box.width + 118, box.height, new Color(120, 120, 120, 255));
                        Renderer2D.COLOR.boxLines(x + 1, y + 1, box.width + 118, box.height, new Color(120, 120, 120, 255));
                        Renderer2D.COLOR.boxLines(x + 1.5, y + 1.5, box.width + 118, box.height, new Color(120, 120, 120, 255));
                    }
                    case Mine -> {
                        Renderer2D.COLOR.boxLines(x + 0.5, y + 0.5, box.width + 118, box.height, new Color(255, 255, 87, 255));
                        Renderer2D.COLOR.boxLines(x + 1, y + 1, box.width + 118, box.height, new Color(255, 255, 87, 255));
                        Renderer2D.COLOR.boxLines(x + 1.5, y + 1.5, box.width + 118, box.height, new Color(255, 255, 87, 255));
                    }
                    case IronMine -> {
                        Renderer2D.COLOR.boxLines(x + 0.5, y + 0.5, box.width + 118, box.height, new Color(255, 255, 87, 255));
                        Renderer2D.COLOR.boxLines(x + 1, y + 1, box.width + 118, box.height, new Color(255, 255, 87, 255));
                        Renderer2D.COLOR.boxLines(x + 1.5, y + 1.5, box.width + 118, box.height, new Color(255, 255, 87, 255));
                    }
                }
                Renderer2D.COLOR.render(null);
                Identifier TEXTURE = new Identifier("addon", "notification/info.png");
                if (type == NotificationType.Info) {
                    TEXTURE = new Identifier("addon", "notification/info.png");
                    renderer.text("Info", x + box.width / 4.411, y + box.height / 6.1, new Color(87, 204, 79, 255));
                }
                else if (type == NotificationType.Warning) {
                    TEXTURE = new Identifier("addon", "notification/warning.png");
                    renderer.text("Warning", x + box.width / 4.411, y + box.height / 6.1, new Color(249, 235, 70, 255));
                }
                else if (type == NotificationType.Totem) {
                    TEXTURE = new Identifier("addon", "notification/totempop.png");
                    renderer.text("Totem Pop", x + box.width / 4.411, y + box.height / 6.1, new Color(251, 178, 0, 255));
                }
                else if (type == NotificationType.Death) {
                    TEXTURE = new Identifier("addon", "notification/death.png");
                    renderer.text("Death", x + box.width / 4.411, y + box.height / 6.1, new Color(120, 120, 120, 255));
                }
                else if (type == NotificationType.View) {
                    TEXTURE = new Identifier("addon", "notification/view.png");
                    renderer.text("View", x + box.width / 4.411, y + box.height / 6.1, new Color(0, 255, 171, 255));
                }
                else if (type == NotificationType.Mine) {
                    TEXTURE = new Identifier("addon", "notification/mine.png");
                    renderer.text("Mining", x + box.width / 4.411, y + box.height / 6.1, new Color(255, 255, 87, 255));
                }
                else if (type == NotificationType.IronMine) {
                    TEXTURE = new Identifier("addon", "notification/ironmine.png");
                    renderer.text("Iron Mining", x + box.width / 4.411, y + box.height / 6.1, new Color(255, 255, 87, 255));
                }
                GL.bindTexture(TEXTURE);
                Renderer2D.TEXTURE.begin();
                Renderer2D.TEXTURE.texQuad(x + box.width / 26.4699, y + box.width / 26.4699, box.width / 6.0296, box.height / 1.3895, WHITE);
                Renderer2D.TEXTURE.render(null);
                renderer.text(mes, x + box.width / 4.411, y + renderer.textHeight() + box.height / 6.1, textColor);
            }
        }
    }

    public void messageHelper() {
        if (i > maxCount.get() && !notifications.isEmpty()) notifications.remove(0);
        if (timer >= removeDelay.get() && !notifications.isEmpty()){
            notifications.remove(0);
            timer = 0;
        } else if (!notifications.isEmpty()) timer++;
    }

}
