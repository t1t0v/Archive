package naturalism.addon.netherbane.utils;

public class ColorUtil {
    public static int modify(ColorMutable original, int redOverwrite, int greenOverwrite, int blueOverwrite, int alphaOverwrite) {
        return (((alphaOverwrite == -1 ? original.getAlpha() : alphaOverwrite) & 0xFF) << 24) |
            (((redOverwrite == -1 ? original.getRed() : redOverwrite) & 0xFF) << 16) |
            (((greenOverwrite == -1 ? original.getGreen() : greenOverwrite) & 0xFF) << 8) |
            (((blueOverwrite == -1 ? original.getBlue() : blueOverwrite) & 0xFF));
    }
}
