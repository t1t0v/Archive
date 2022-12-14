package genesis.team.addon.util.InfoUtil;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;

import static genesis.team.addon.util.CombatUtil.RenderUtils.RenderMode;

public class RenderInfo {
    public Render3DEvent event;
    public RenderMode renderMode;
    public ShapeMode shapeMode;

    // Основной рендер
    public RenderInfo(Render3DEvent event, RenderMode renderMode, ShapeMode shapeMode) {
        this.event = event;
        this.renderMode = renderMode;
        this.shapeMode = shapeMode;
    }

    // Рендер в случае если нет RenderMode
    public RenderInfo(Render3DEvent event, RenderMode renderMode) {
        this.event = event;
        this.renderMode = renderMode;
    }
}
