package naturalism.addon.netherbane.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import java.awt.*;

public class Renderer {
    public static void gradientLineScreen(Color start, Color end, double x, double y, double x1, double y1) {
        float g = start.getRed() / 255f;
        float h = start.getGreen() / 255f;
        float k = start.getBlue() / 255f;
        float f = start.getAlpha() / 255f;
        float g1 = end.getRed() / 255f;
        float h1 = end.getGreen() / 255f;
        float k1 = end.getBlue() / 255f;
        float f1 = end.getAlpha() / 255f;
        Matrix4f m = new MatrixStack().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(m, (float) x, (float) y, 0f).color(g, h, k, f).next();
        bufferBuilder.vertex(m, (float) x1, (float) y1, 0f).color(g1, h1, k1, f1).next();
        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void fill(Color c, double x1, double y1, double x2, double y2) {
        fill(new MatrixStack(), c, x1, y1, x2, y2);
    }

    public static void fill(MatrixStack matrices, Color c, double x1, double y1, double x2, double y2) {
        int color = c.getRGB();
        double j;
        if (x1 < x2) {
            j = x1;
            x1 = x2;
            x2 = j;
        }

        if (y1 < y2) {
            j = y1;
            y1 = y2;
            y2 = j;
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float k = (float) (color & 255) / 255.0F;
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, (float) x1, (float) y2, 0.0F).color(g, h, k, f).next();
        bufferBuilder.vertex(matrix, (float) x2, (float) y2, 0.0F).color(g, h, k, f).next();
        bufferBuilder.vertex(matrix, (float) x2, (float) y1, 0.0F).color(g, h, k, f).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, 0.0F).color(g, h, k, f).next();
        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static Color modify(Color original, int redOverwrite, int greenOverwrite, int blueOverwrite, int alphaOverwrite) {
        return new Color(redOverwrite == -1 ? original.getRed() : redOverwrite, greenOverwrite == -1 ? original.getGreen() : greenOverwrite, blueOverwrite == -1 ? original.getBlue() : blueOverwrite, alphaOverwrite == -1 ? original.getAlpha() : alphaOverwrite);
    }
}
