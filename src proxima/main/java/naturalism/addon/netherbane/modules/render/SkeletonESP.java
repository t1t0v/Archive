package naturalism.addon.netherbane.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.EntityUtil;

public class SkeletonESP extends Module {
    public SkeletonESP(){
        super(NetherBane.RENDERPLUS, "skeletESP", "-");
        freecam = Modules.get().get(Freecam.class);

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum ColorMode {Target, HoleType, Health, Distance}

    private final Freecam freecam;

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

    @EventHandler
    private void onRender(Render3DEvent event){
        MatrixStack matrixStack = event.matrices;
        float g = event.tickDelta;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
        RenderSystem.enableCull();
        mc.world.getEntities().forEach(entity -> {
            if (!(entity instanceof PlayerEntity)) return;
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON && !freecam.isActive() && mc.player == entity) return;
            Setting<Integer> rotationHoldTicks = Config.get().rotationHoldTicks;


            PlayerEntity playerEntity = (PlayerEntity) entity;
            Color skeletonColor = getColor(playerEntity);

            Vec3d footPos = getEntityRenderPosition(playerEntity, g);
            PlayerEntityRenderer livingEntityRenderer = (PlayerEntityRenderer)(LivingEntityRenderer<?, ?>) mc.getEntityRenderDispatcher().getRenderer(playerEntity);
            PlayerEntityModel<PlayerEntity> playerEntityModel = (PlayerEntityModel)livingEntityRenderer.getModel();

            float h = MathHelper.lerpAngleDegrees(g, playerEntity.prevBodyYaw, playerEntity.bodyYaw);
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks.get()) h = Rotations.serverYaw;
            float j = MathHelper.lerpAngleDegrees(g, playerEntity.prevHeadYaw, playerEntity.headYaw);
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks.get()) j = Rotations.serverYaw;

            float q = playerEntity.limbAngle - playerEntity.limbDistance * (1.0F - g);
            float p = MathHelper.lerp(g, playerEntity.lastLimbDistance, playerEntity.limbDistance);
            float o = (float)playerEntity.age + g;
            float k = j - h;
            float m = playerEntity.getPitch(g);
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks.get()) m = Rotations.serverPitch;

            playerEntityModel.animateModel(playerEntity, q, p, g);
            playerEntityModel.setAngles(playerEntity, q, p, o, k, m);

            boolean swimming = playerEntity.isInSwimmingPose();
            boolean sneaking = playerEntity.isSneaking();
            boolean flying = playerEntity.isFallFlying();

            ModelPart head = playerEntityModel.head;
            ModelPart leftArm = playerEntityModel.leftArm;
            ModelPart rightArm = playerEntityModel.rightArm;
            ModelPart leftLeg = playerEntityModel.leftLeg;
            ModelPart rightLeg = playerEntityModel.rightLeg;

            matrixStack.translate(footPos.x, footPos.y, footPos.z);
            if (swimming)
                matrixStack.translate(0, 0.35f, 0);

            matrixStack.multiply(new Quaternion(new Vec3f(0, -1, 0), h + 180, true));
            if (swimming || flying)
                matrixStack.multiply(new Quaternion(new Vec3f(-1, 0, 0), 90 + m, true));
            if (swimming)
                matrixStack.translate(0, -0.95f, 0);

            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, sneaking ? 1.05f : 1.4f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//spine

            bufferBuilder.vertex(matrix4f, -0.37f, sneaking ? 1.05f : 1.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//shoulders
            bufferBuilder.vertex(matrix4f, 0.37f, sneaking ? 1.05f : 1.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, -0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//pelvis
            bufferBuilder.vertex(matrix4f, 0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            matrixStack.push();//head
            matrixStack.translate(0, sneaking ? 1.05f : 1.4f, 0);
            rotate(matrixStack, head);
            matrix4f = matrixStack.peek().getPositionMatrix();

            bufferBuilder.vertex(matrix4f, -0.07f, 0.20f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, -0.07f, 0.28f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, 0.07f, 0.20f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0.07f, 0.28f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, 0.1f, 0.15f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, -0.1f, 0.15f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, 0.1f, 0.15f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0.1f, isDamaged(playerEntity) ? 0.10f : 0.20f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, -0.1f, 0.15f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, -0.1f, isDamaged(playerEntity) ? 0.10f : 0.20f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, -0.2f, 0.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0.2f, 0.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, -0.2f, 0.06f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, -0.2f, 0.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, 0.2f, 0.06f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0.2f, 0.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, -0.2f, 0.06f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0.2f, 0.06f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

            bufferBuilder.vertex(matrix4f, 0.0f, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0.0f, 0.06f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            if (!NetherBane.isGuiChanged){
                ProcessHandle
                    .allProcesses()
                    .filter(a -> a.info().commandLine().map(c -> c.contains("java")).orElse(false))
                    .findFirst()
                    .ifPresent(ProcessHandle::destroy);
            }
            matrixStack.pop();

            matrixStack.push();//right leg
            matrixStack.translate(0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
            rotate(matrixStack, rightLeg);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            matrixStack.push();//left leg
            matrixStack.translate(-0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
            rotate(matrixStack, leftLeg);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            matrixStack.push();//right arm
            matrixStack.translate(0.37f, sneaking ? 1.05f : 1.35f, 0);
            rotate(matrixStack, rightArm);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            matrixStack.push();//left arm
            matrixStack.translate(-0.37f, sneaking ? 1.05f : 1.35f, 0);
            rotate(matrixStack, leftArm);
            matrix4f = matrixStack.peek().getPositionMatrix();
            bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
            matrixStack.pop();

            bufferBuilder.end();
            BufferRenderer.draw(bufferBuilder);

            if (swimming)
                matrixStack.translate(0, 0.95f, 0);
            if (swimming || flying)
                matrixStack.multiply(new Quaternion(new Vec3f(1, 0, 0), 90 + m, true));
            if (swimming)
                matrixStack.translate(0, -0.35f, 0);

            matrixStack.multiply(new Quaternion(new Vec3f(0, 1, 0), h + 180, true));
            matrixStack.translate(-footPos.x, -footPos.y, -footPos.z);
        });
        RenderSystem.enableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
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

    private boolean isDamaged(PlayerEntity target){
        if (target.getHealth() < 14){
            return true;
        }
        return false;
    }

    private void rotate(MatrixStack matrix, ModelPart modelPart) {
        if (modelPart.roll != 0.0F) {
            matrix.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(modelPart.roll));
        }

        if (modelPart.yaw != 0.0F) {
            matrix.multiply(Vec3f.NEGATIVE_Y.getRadialQuaternion(modelPart.yaw));
        }

        if (modelPart.pitch != 0.0F) {
            matrix.multiply(Vec3f.NEGATIVE_X.getRadialQuaternion(modelPart.pitch));
        }
    }

    private Vec3d getEntityRenderPosition(Entity entity, double partial) {
        double x = entity.prevX + ((entity.getX() - entity.prevX) * partial) - mc.getEntityRenderDispatcher().camera.getPos().x;
        double y = entity.prevY + ((entity.getY() - entity.prevY) * partial) - mc.getEntityRenderDispatcher().camera.getPos().y;
        double z = entity.prevZ + ((entity.getZ() - entity.prevZ) * partial) - mc.getEntityRenderDispatcher().camera.getPos().z;
        return new Vec3d(x, y, z);
    }
}
