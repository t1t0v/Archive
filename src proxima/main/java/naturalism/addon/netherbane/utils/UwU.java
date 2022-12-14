package naturalism.addon.netherbane.utils;

import java.awt.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class UwU {
    public static double getMouseX() {
        return mc.mouse.getX() / mc.getWindow().getScaleFactor();
    }

    public static double getMouseY() {
        return mc.mouse.getY() / mc.getWindow().getScaleFactor();
    }

    public static Color getCurrentRGB() {
        return new Color(Color.HSBtoRGB((System.currentTimeMillis() % 4750) / 4750f, 0.7f, 1));
    }


}
