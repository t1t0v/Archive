package genesis.team.addon.modules.haha;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

public class ViewModel extends Module {
    public ViewModel() {
        super(Genesis.Misc, "View-model", " ");
    }

    private final SettingGroup mainHandPage = settings.createGroup("MainHand");
    private final Setting<Double> scaralMain = mainHandPage.add(new DoubleSetting.Builder().name("brightness").defaultValue(1).sliderMax(2).build());
    private final Setting<Double> scaleMainX = mainHandPage.add(new DoubleSetting.Builder().name("scale-main-x").defaultValue(1).sliderMax(2).build());
    private final Setting<Double> scaleMainY = mainHandPage.add(new DoubleSetting.Builder().name("scale-main-y").defaultValue(1).sliderMax(2).build());
    private final Setting<Double> scaleMainZ = mainHandPage.add(new DoubleSetting.Builder().name("scale-main-z").defaultValue(1).sliderMax(2).build());
    private final Setting<Double> posX = mainHandPage.add(new DoubleSetting.Builder().name("main-pos-x").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> posY = mainHandPage.add(new DoubleSetting.Builder().name("main-pos-y").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> posZ = mainHandPage.add(new DoubleSetting.Builder().name("main-pos-z").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> rotationX = mainHandPage.add(new DoubleSetting.Builder().name("main-rotation-x").defaultValue(1).sliderRange(-180, 180).build());
    private final Setting<Double> rotationY = mainHandPage.add(new DoubleSetting.Builder().name("main-rotation-y").defaultValue(1).sliderRange(-180, 180).build());
    private final Setting<Double> rotationZ = mainHandPage.add(new DoubleSetting.Builder().name("main-rotation-z").defaultValue(1).sliderRange(-180, 180).build());
    private final Setting<Double> useXMain = mainHandPage.add(new DoubleSetting.Builder().name("main-use-x").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> useZMain = mainHandPage.add(new DoubleSetting.Builder().name("main-use-y").defaultValue(1).sliderRange(-1, 2).build());

    private final SettingGroup offHandPage = settings.createGroup("OffHand");
    private final Setting<Double> scaralOff = offHandPage.add(new DoubleSetting.Builder().name("brightness").defaultValue(1).sliderMax(2).build());
    private final Setting<Double> scaleOffX = offHandPage.add(new DoubleSetting.Builder().name("scale-off-x").defaultValue(1).sliderMax(2).build());
    private final Setting<Double> scaleOffY = offHandPage.add(new DoubleSetting.Builder().name("scale-off-y").defaultValue(1).sliderMax(2).build());
    private final Setting<Double> scaleOffZ = offHandPage.add(new DoubleSetting.Builder().name("scale-off-z").defaultValue(1).sliderMax(2).build());
    private final Setting<Double> posXoff = offHandPage.add(new DoubleSetting.Builder().name("off-pos-x").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> posYoff = offHandPage.add(new DoubleSetting.Builder().name("off-pos-y").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> posZoff = offHandPage.add(new DoubleSetting.Builder().name("off-pos-z").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> rotationXoff = offHandPage.add(new DoubleSetting.Builder().name("off-rotation-x").defaultValue(1).sliderRange(-180, 180).build());
    private final Setting<Double> rotationYoff = offHandPage.add(new DoubleSetting.Builder().name("off-rotation-y").defaultValue(1).sliderRange(-180, 180).build());
    private final Setting<Double> rotationZoff = offHandPage.add(new DoubleSetting.Builder().name("off-rotation-z").defaultValue(1).sliderRange(-180, 180).build());
    private final Setting<Double> useXOff = offHandPage.add(new DoubleSetting.Builder().name("off-use-x").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> useZOff = offHandPage.add(new DoubleSetting.Builder().name("off-use-y").defaultValue(1).sliderRange(-1, 2).build());

    private final SettingGroup handAnimation = settings.createGroup("Hand Animation");
    private final Setting<Boolean> mainHand = handAnimation.add(new BoolSetting.Builder().name("main-animation").defaultValue(true).build());
    private final Setting<Double> mainHandXAnimation = handAnimation.add(new DoubleSetting.Builder().name("main-x-speed").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> mainHandYAnimation = handAnimation.add(new DoubleSetting.Builder().name("main-y-speed").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> mainHandZAnimation = handAnimation.add(new DoubleSetting.Builder().name("main-z-speed").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Boolean> offHand = handAnimation.add(new BoolSetting.Builder().name("off-animation").defaultValue(true).build());
    private final Setting<Double> offHandXAnimation = handAnimation.add(new DoubleSetting.Builder().name("off-x-speed").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> offHandYAnimation = handAnimation.add(new DoubleSetting.Builder().name("off-y-speed").defaultValue(1).sliderRange(-1, 2).build());
    private final Setting<Double> offHandZAnimation = handAnimation.add(new DoubleSetting.Builder().name("off-z-speed").defaultValue(1).sliderRange(-1, 2).build());

    private float rotationXMain, rotationZMain, rotationYMain;
    private float rotationXOff, rotationZOff, rotationYOff;

    @Override
    public void onActivate(){
        rotationZMain = mc.getItemRenderer().getModels().getModel(mc.player.getMainHandStack()).getTransformation().firstPersonRightHand.rotation.getZ();
        rotationXMain = mc.getItemRenderer().getModels().getModel(mc.player.getMainHandStack()).getTransformation().firstPersonRightHand.rotation.getX();
        rotationYMain = mc.getItemRenderer().getModels().getModel(mc.player.getMainHandStack()).getTransformation().firstPersonRightHand.rotation.getY();
        rotationZOff = mc.getItemRenderer().getModels().getModel(mc.player.getOffHandStack()).getTransformation().firstPersonLeftHand.rotation.getZ();
        rotationXOff = mc.getItemRenderer().getModels().getModel(mc.player.getOffHandStack()).getTransformation().firstPersonLeftHand.rotation.getX();
        rotationYOff = mc.getItemRenderer().getModels().getModel(mc.player.getOffHandStack()).getTransformation().firstPersonLeftHand.rotation.getY();
    }

    @Override
    public void onDeactivate(){
        mc.getItemRenderer().getModels().getModel(mc.player.getMainHandStack()).getTransformation().firstPersonRightHand.rotation.add(rotationXMain, rotationYMain, rotationZMain);
        mc.getItemRenderer().getModels().getModel(mc.player.getOffHandStack()).getTransformation().firstPersonLeftHand.rotation.add(rotationXOff, rotationYOff, rotationZOff);
    }

    @EventHandler
    private void onHand(HeldItemRendererEvent event) {
        if (event.hand == Hand.MAIN_HAND) {
            event.matrix.peek().getNormalMatrix().multiply(scaralMain.get().floatValue());
            event.matrix.multiply(Vec3f.NEGATIVE_X.getDegreesQuaternion(rotationX.get().floatValue()));
            event.matrix.multiply(Vec3f.NEGATIVE_Y.getDegreesQuaternion(rotationY.get().floatValue()));
            event.matrix.multiply(Vec3f.NEGATIVE_Z.getDegreesQuaternion(rotationZ.get().floatValue()));
            event.matrix.scale(scaleMainX.get().floatValue(), scaleMainY.get().floatValue(), scaleMainZ.get().floatValue());
            if (mainHand.get())mc.getItemRenderer().getModels().getModel(mc.player.getMainHandStack()).getTransformation().firstPersonRightHand.rotation.add(mainHandXAnimation.get().floatValue(), mainHandYAnimation.get().floatValue(), mainHandZAnimation.get().floatValue());
            if (mc.player.isUsingItem() && mc.player.getMainHandStack().getItem().isFood()) {
                event.matrix.translate(posX.get().floatValue() - posX.get().floatValue() + useXMain.get().floatValue(), posY.get().floatValue() - 0.6D, posZ.get().floatValue() - posZ.get().floatValue() + useZMain.get().floatValue());
            } else {
                event.matrix.translate(posX.get().floatValue(), posY.get().floatValue(), posZ.get().floatValue());
            }
        } else {
            event.matrix.peek().getNormalMatrix().multiply(scaralOff.get().floatValue());
            event.matrix.multiply(Vec3f.NEGATIVE_X.getDegreesQuaternion(rotationXoff.get().floatValue()));
            event.matrix.multiply(Vec3f.NEGATIVE_Y.getDegreesQuaternion(rotationYoff.get().floatValue()));
            event.matrix.multiply(Vec3f.NEGATIVE_Z.getDegreesQuaternion(rotationZoff.get().floatValue()));
            event.matrix.scale(scaleOffX.get().floatValue(), scaleOffY.get().floatValue(), scaleOffZ.get().floatValue());
            if (offHand.get())mc.getItemRenderer().getModels().getModel(mc.player.getOffHandStack()).getTransformation().firstPersonLeftHand.rotation.add(offHandXAnimation.get().floatValue(), offHandYAnimation.get().floatValue(), offHandZAnimation.get().floatValue());
            if (mc.player.isUsingItem() && mc.player.getOffHandStack().getItem().isFood()) {
                event.matrix.translate(-posXoff.get().floatValue() - useXOff.get().floatValue(), posYoff.get().floatValue() - 0.6D, posZoff.get().floatValue() - posZ.get().floatValue() - useZOff.get().floatValue());
            } else {
                event.matrix.translate(posXoff.get().floatValue(), posYoff.get().floatValue(), posZoff.get().floatValue());
            }
        }
    }
}
