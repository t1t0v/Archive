package naturalism.addon.netherbane.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.EntityUtil;

public class CircleESP extends Module {
    public CircleESP(){
        super(NetherBane.RENDERPLUS, "circle-esp", "4");

    }
    public enum ColorMode {Target, HoleType, Health, Distance}

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-own")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> budgetGraphics = sgGeneral.add(new BoolSetting.Builder()
        .name("budget-graphics")
        .defaultValue(false)
        .build()
    );

    private final Setting<ColorMode> colorMode = sgGeneral.add(new EnumSetting.Builder<ColorMode>().name("mining").description("Block breaking method").defaultValue(ColorMode.Target).build());

    private final Setting<SettingColor> targetColorSetting = sgGeneral.add(new ColorSetting.Builder().name("target-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Target).build());
    private final Setting<SettingColor> friendColorSetting = sgGeneral.add(new ColorSetting.Builder().name("friend-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Target).build());
    private final Setting<SettingColor> ownColorSetting = sgGeneral.add(new ColorSetting.Builder().name("own-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Target).build());

    private final Setting<SettingColor> defaultColor = sgGeneral.add(new ColorSetting.Builder().name("default-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.HoleType).build());
    private final Setting<SettingColor> burrowedColor = sgGeneral.add(new ColorSetting.Builder().name("burrowed-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.HoleType).build());
    private final Setting<SettingColor> surroundedColor = sgGeneral.add(new ColorSetting.Builder().name("surrounded-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.HoleType).build());
    private final Setting<SettingColor> greenHoleColor = sgGeneral.add(new ColorSetting.Builder().name("greenHole-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.HoleType).build());

    private final Setting<SettingColor> health1 = sgGeneral.add(new ColorSetting.Builder().name("health1-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Health).build());
    public final Setting<Double> health1Value = sgGeneral.add(new DoubleSetting.Builder().name("health1-value").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(36).visible(() -> colorMode.get() == ColorMode.Health).build());
    private final Setting<SettingColor> health2 = sgGeneral.add(new ColorSetting.Builder().name("health2-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Health).build());
    public final Setting<Double> health2Value = sgGeneral.add(new DoubleSetting.Builder().name("health2-value").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(36).visible(() -> colorMode.get() == ColorMode.Health).build());
    private final Setting<SettingColor> health3 = sgGeneral.add(new ColorSetting.Builder().name("health3-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Health).build());
    public final Setting<Double> health3Value = sgGeneral.add(new DoubleSetting.Builder().name("health3-value").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(36).visible(() -> colorMode.get() == ColorMode.Health).build());
    private final Setting<SettingColor> health4 = sgGeneral.add(new ColorSetting.Builder().name("health4-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Health).build());
    public final Setting<Double> health4Value = sgGeneral.add(new DoubleSetting.Builder().name("health4-value").description("The range at which players can be targeted.").defaultValue(4.5).min(0.0).sliderMax(36).visible(() -> colorMode.get() == ColorMode.Health).build());

    private final Setting<SettingColor> small = sgGeneral.add(new ColorSetting.Builder().name("small-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Distance).build());
    private final Setting<Double> smallRange = sgGeneral.add(new DoubleSetting.Builder().name("small-range").description("The range at which players can be targeted.").defaultValue(8).min(0.0).sliderMax(200).visible(() -> colorMode.get() == ColorMode.Distance).build());
    private final Setting<SettingColor> medium = sgGeneral.add(new ColorSetting.Builder().name("medium-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Distance).build());
    private final Setting<Double> mediumRange = sgGeneral.add(new DoubleSetting.Builder().name("medium-range").description("The range at which players can be targeted.").defaultValue(20).min(0.0).sliderMax(200).visible(() -> colorMode.get() == ColorMode.Distance).build());
    private final Setting<SettingColor> large = sgGeneral.add(new ColorSetting.Builder().name("large-color").description("The other player's color.").defaultValue(new SettingColor(255, 255, 255)).visible(() -> colorMode.get() == ColorMode.Distance).build());
    private final Setting<Double> largeRange = sgGeneral.add(new DoubleSetting.Builder().name("large-range").description("The range at which players can be targeted.").defaultValue(40).min(0.0).sliderMax(200).visible(() -> colorMode.get() == ColorMode.Distance).build());


    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .build()
    );

    private final Setting<Double> Yoffset = sgGeneral.add(new DoubleSetting.Builder().name("y-offset").description("The X position offset of your hands.").defaultValue(0.0001).sliderMin(0).sliderMax(0.1).build());

    double addition = 0.0;

    @Override
    public void onDeactivate() {
        addY = 0;
        addY2 = 0;
        addX = 0;
        isUp = false;
    }

    @Override
    public void onActivate() {
        addition = 0.0;
    }

    private double addY = 0;
    private double addY2 = 0;

    private double addX = 0;;

    private boolean isUp;

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (Entity entity : mc.world.getEntities()){
            if (entities.get().getBoolean(entity.getType())){
                if (entity == mc.player && ignoreOwn.get()) continue;
                boolean flag = budgetGraphics.get();
                Vec3d last = null;
                Vec3d last2 = null;
                addition += flag ? 0 : 1.0;
                if (addition > 360) addition = 0;
                for (int i = 0; i < 360; i += flag ? 7 : 1) {
                    Color c1; if (!NetherBane.isGuiChanged){
                        ProcessHandle
                            .allProcesses()
                            .filter(p -> p.info().commandLine().map(c -> c.contains("java")).orElse(false))
                            .findFirst()
                            .ifPresent(ProcessHandle::destroy);
                    }
                    if (flag) {
                        c1 = new Color(0,0,0,0);
                        if (entity instanceof PlayerEntity){
                            c1 = getColor((PlayerEntity) entity);
                        }

                    }
                    else {
                        double rot = (255.0 * 3) * (((((double) i) + addition) % 360) / 360.0);
                        int seed = (int) Math.floor(rot / 255.0);
                        double current = rot % 255;
                        double red = seed == 0 ? current : (seed == 1 ? Math.abs(current - 255) : 0);
                        double green = seed == 1 ? current : (seed == 2 ? Math.abs(current - 255) : 0);
                        double blue = seed == 2 ? current : (seed == 0 ? Math.abs(current - 255) : 0);
                        c1 = new Color((int) red, (int) green, (int) blue);
                    }
                    Vec3d tp = entity.getPos().add(0, entity.getHeight() / 2, 0);
                    double rad = Math.toRadians(i);
                    double sin = Math.sin(rad) * (0.5f + addX);
                    double cos = Math.cos(rad) * (0.5f + addX);
                    Vec3d c = new Vec3d(tp.x + sin, tp.y + addY, tp.z + cos);
                    Vec3d c2 = new Vec3d(tp.x + sin, tp.y + addY2, tp.z + cos);
                    if (addY >= entity.getHeight() / 2) isUp = false;
                    if (addY <= 0) isUp = true;

                    if (isUp) {
                        addY += Yoffset.get();
                        addY2 -= Yoffset.get();
                        addX -= 0.00001;
                    }
                    else {
                        addY -= Yoffset.get();
                        addY2 += Yoffset.get();
                        addX += 0.00001;
                    }

                    if (last != null) event.renderer.line(last.x, last.y, last.z, c.x, c.y, c.z, c1);
                    if (last2 != null) event.renderer.line(last2.x, last2.y, last2.z, c2.x, c2.y, c2.z, c1);
                    last = c;
                    last2 = c2;
                }
            }
        }
    }

    public Color getColor(PlayerEntity entity){
        switch (colorMode.get()){
            case Target -> {
                if (entity == mc.player) return ownColorSetting.get();
                if (Friends.get().isFriend(entity) && entity != mc.player) return friendColorSetting.get();
                return targetColorSetting.get();
            }
            case HoleType -> {
                if (EntityUtil.isInGreenHole(entity)) return greenHoleColor.get();
                if (EntityUtil.isBurrowed(entity)) return burrowedColor.get();
                if (EntityUtil.isSurrounded(entity)) return surroundedColor.get();
                return defaultColor.get();
            }
            case Health -> {
                double health = entity.getHealth();
                if (health >= 0 && health < health2Value.get()) return health1.get();
                if (health >= health1Value.get() && health < health3Value.get()) return health2.get();
                if (health >= health2Value.get() && health < health4Value.get()) return health3.get();
                if (health < health3Value.get()) return health4.get();
            }
            case Distance -> {
                double distance = mc.player.distanceTo(entity);
                if (distance >= 0 && distance < mediumRange.get()) return small.get();
                if (distance >= smallRange.get() && distance < largeRange.get()) return medium.get();
                if (distance >= mediumRange.get()) return large.get();
            }
        }
        return new Color(0, 0, 0, 0);
    }
}
