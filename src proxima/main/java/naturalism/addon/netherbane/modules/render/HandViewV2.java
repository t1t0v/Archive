package naturalism.addon.netherbane.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.events.EventTick;
import naturalism.addon.netherbane.events.RenderHeldItemEvent;
import naturalism.addon.netherbane.mixins.AccessorHeldItemRenderer;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Quaternion;

public class HandViewV2 extends Module {
    public HandViewV2(){
        super(NetherBane.RENDERPLUS, "hand-tweaks", "---------------------------------");

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTranslate = settings.createGroup("translate");
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgRotation = settings.createGroup("Rotation");
    private final Setting<Boolean> separate = sgGeneral.add(new BoolSetting.Builder().name("separate").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());
    // Scale
    private final Setting<Boolean> translate = sgTranslate.add(new BoolSetting.Builder().name("translate").description("Moves beds into a selected hotbar slot.").defaultValue(true).build());
    private final Setting<Double> translateMainX = sgTranslate.add(new DoubleSetting.Builder().name("translateMainX").description("The X scale of your hands.").defaultValue(0).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> translateMainY = sgTranslate.add(new DoubleSetting.Builder().name("translateMainY").description("The X scale of your hands.").defaultValue(-0.69).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> translateMainZ = sgTranslate.add(new DoubleSetting.Builder().name("translateMainZ").description("The X scale of your hands.").defaultValue(1.5).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> translateOffX = sgTranslate.add(new DoubleSetting.Builder().name("translateOffX").description("The X scale of your hands.").defaultValue(0).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> translateOffY = sgTranslate.add(new DoubleSetting.Builder().name("translateOffY").description("The X scale of your hands.").defaultValue(-0.69).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> translateOffZ = sgTranslate.add(new DoubleSetting.Builder().name("translateOffZ").description("The X scale of your hands.").defaultValue(1.5).sliderMin(-3).sliderMax(5).build());

    // Position
    private final Setting<Boolean> scale = sgScale.add(new BoolSetting.Builder().name("scale").description("Moves beds into a selected hotbar slot.").defaultValue(true).build());
    private final Setting<Double> scaleMainX = sgScale.add(new DoubleSetting.Builder().name("scaleMainX").description("The X position offset of your hands.").defaultValue(1.65).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> scaleMainY = sgScale.add(new DoubleSetting.Builder().name("scaleMainY").description("The X position offset of your hands.").defaultValue(1).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> scaleMainZ = sgScale.add(new DoubleSetting.Builder().name("scaleMainZ").description("The X position offset of your hands.").defaultValue(3).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> scaleOffX = sgScale.add(new DoubleSetting.Builder().name("scaleOffX").description("The X position offset of your hands.").defaultValue(1.65).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> scaleOffY = sgScale.add(new DoubleSetting.Builder().name("scaleOffY").description("The X position offset of your hands.").defaultValue(1).sliderMin(-3).sliderMax(5).build());
    private final Setting<Double> scaleOffZ = sgScale.add(new DoubleSetting.Builder().name("scaleMainX").description("The X position offset of your hands.").defaultValue(3).sliderMin(-3).sliderMax(5).build());

    // Rotation
    private final Setting<Boolean> rotation = sgRotation.add(new BoolSetting.Builder().name("rotation").description("Moves beds into a selected hotbar slot.").defaultValue(true).build());
    private final Setting<Integer> rotationMainX = sgRotation.add(new IntSetting.Builder().name("rotationMainX").description("The X orientation of your hands.").defaultValue(0).sliderMin(-180).sliderMax(180).build());
    private final Setting<Integer> rotationMainY = sgRotation.add(new IntSetting.Builder().name("rotationMainY").description("The X orientation of your hands.").defaultValue(0).sliderMin(-180).sliderMax(180).build());
    private final Setting<Integer> rotationMainZ = sgRotation.add(new IntSetting.Builder().name("rotationMainZ").description("The X orientation of your hands.").defaultValue(0).sliderMin(-180).sliderMax(180).build());
    private final Setting<Integer> rotationOffX = sgRotation.add(new IntSetting.Builder().name("rotationOffX").description("The X orientation of your hands.").defaultValue(0).sliderMin(-180).sliderMax(180).build());
    private final Setting<Integer> rotationOffY = sgRotation.add(new IntSetting.Builder().name("rotationOffY").description("The X orientation of your hands.").defaultValue(0).sliderMin(-180).sliderMax(180).build());
    private final Setting<Integer> rotationOffZ = sgRotation.add(new IntSetting.Builder().name("rotationOffZ").description("The X orientation of your hands.").defaultValue(0).sliderMin(-180).sliderMax(180).build());

    private final SettingGroup sgHand = settings.createGroup("Hand");
    private final Setting<Double> mainHandProgress = sgHand.add(new DoubleSetting.Builder().name("mainhand").description("The X position offset of your hands.").defaultValue(0.1).sliderMin(0).sliderMax(1).build());
    private final Setting<Double> offHandProgress = sgHand.add(new DoubleSetting.Builder().name("offhand").description("The X position offset of your hands.").defaultValue(0.1).sliderMin(0).sliderMax(1).build());
    private final Setting<Boolean> noAnimation = sgHand.add(new BoolSetting.Builder().name("no-animation").description(".").defaultValue(false).build());

    private final SettingGroup sgAnimation = settings.createGroup("Animation");
    private final Setting<Boolean> mainHandAnimationX = sgAnimation.add(new BoolSetting.Builder().name("main-hand-animation-x").description(".").defaultValue(false).build());
    private final Setting<Double> mainHandRotationXSpeed = sgAnimation.add(new DoubleSetting.Builder().name("main-hand-x-speed").description("The Y orientation of your hands.").defaultValue(0).sliderMin(-5).sliderMax(5).visible(mainHandAnimationX::get).build());
    private final Setting<Boolean> mainHandAnimationY = sgAnimation.add(new BoolSetting.Builder().name("main-hand-animation-y").description(".").defaultValue(false).build());
    private final Setting<Double> mainHandRotationYSpeed = sgAnimation.add(new DoubleSetting.Builder().name("main-hand-y-speed").description("The Y orientation of your hands.").defaultValue(0).sliderMin(-5).sliderMax(5).visible(mainHandAnimationY::get).build());

    private final Setting<Boolean> offHandAnimationX = sgAnimation.add(new BoolSetting.Builder().name("off-hand-animation-x").description(".").defaultValue(false).build());
    private final Setting<Double> offHandRotationXSpeed = sgAnimation.add(new DoubleSetting.Builder().name("off-hand-x-speed").description("The Y orientation of your hands.").defaultValue(0).sliderMin(-5).sliderMax(5).visible(offHandAnimationX::get).build());
    private final Setting<Boolean> offHandAnimationY = sgAnimation.add(new BoolSetting.Builder().name("off-hand-animation-y").description(".").defaultValue(false).build());
    private final Setting<Double> offHandRotationYSpeed = sgAnimation.add(new DoubleSetting.Builder().name("off-hand-y-speed").description("The Y orientation of your hands.").defaultValue(0).sliderMin(-5).sliderMax(5).visible(offHandAnimationY::get).build());

    private float mainxspeed;
    private float mainyspeed;

    private float offxspeed;
    private float offyspeed;

    @EventHandler
    public void onTick(EventTick event) {
        AccessorHeldItemRenderer accessor = (AccessorHeldItemRenderer) mc.gameRenderer.firstPersonRenderer;

        // Refresh the item held in hand every tick
        accessor.setMainHand(mc.player.getMainHandStack());
        accessor.setOffHand(mc.player.getOffHandStack());

        // Set the item render height
        float mainHand = noAnimation.get()
            ? mainHandProgress.get().floatValue()
            : Math.min(accessor.getEquipProgressMainHand(), mainHandProgress.get().floatValue());

        float offHand = noAnimation.get()
            ? offHandProgress.get().floatValue()
            : Math.min(accessor.getEquipProgressOffHand(), offHandProgress.get().floatValue());

        accessor.setEquipProgressMainHand(mainHand);
        accessor.setEquipProgressOffHand(offHand);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (mainHandAnimationX.get() || offHandAnimationX.get()) {
            mainxspeed = mainxspeed + mainHandRotationXSpeed.get().floatValue();
            offxspeed = offxspeed + offHandRotationXSpeed.get().floatValue();
            if (mainxspeed > 180) {
                mainxspeed = -180;
            }
            if (offxspeed > 180) {
                offxspeed = -180;
            }
        }
        if (mainHandAnimationY.get() || offHandAnimationY.get()) {
            mainyspeed = mainyspeed + mainHandRotationYSpeed.get().floatValue();
            offyspeed = offyspeed + offHandRotationYSpeed.get().floatValue();
            if (mainyspeed > 180) {
                mainyspeed = -180;
            }
            if (offyspeed > 180) {
                offyspeed = -180;
            }
        }
    }

    @EventHandler
    private void onInvoke(RenderHeldItemEvent.Invoke event){
        event.setCancelled(true);
    }
    @EventHandler
    private void onHand(RenderHeldItemEvent.Cancelled event){
        if(event.getHand() == Hand.MAIN_HAND) {
            if(translate.get()) event.getMatrices().translate(translateMainX.get(), translateMainY.get(), -translateMainZ.get());
            if(scale.get()) event.getMatrices().scale(scaleMainX.get().floatValue(), scaleMainY.get().floatValue(), scaleMainZ.get().floatValue());
            if(rotation.get()) event.getMatrices().multiply(new Quaternion(rotationMainX.get().floatValue(), rotationMainY.get(), rotationMainZ.get(), true));
            if (mainHandAnimationX.get()) event.getMatrices().multiply(Quaternion.fromEulerXyz(mainxspeed, 0, 0));
            if (mainHandAnimationY.get()) event.getMatrices().multiply(Quaternion.fromEulerYxz(0, mainyspeed, 0));
        }
        else {
            if(separate.get()) {
                if(translate.get()) event.getMatrices().translate(translateOffX.get(), translateOffY.get(), -translateOffZ.get());
                if(scale.get()) event.getMatrices().scale(scaleOffX.get().floatValue(), scaleOffY.get().floatValue(), scaleOffZ.get().floatValue());
                if(rotation.get()) event.getMatrices().multiply(new  Quaternion(rotationOffX.get(), rotationOffY.get(), rotationOffZ.get(), true));
                if (offHandAnimationX.get()) event.getMatrices().multiply(Quaternion.fromEulerXyz(offxspeed, 0, 0));
                if (offHandAnimationY.get()) event.getMatrices().multiply(Quaternion.fromEulerYxz(0, offyspeed, 0));
            }
            else {
                if(translate.get()) event.getMatrices().translate(translateMainX.get(), translateMainY.get(), -translateMainZ.get());
                if(scale.get()) event.getMatrices().scale(scaleMainX.get().floatValue(), scaleMainY.get().floatValue(), scaleMainZ.get().floatValue());
                if(rotation.get()) event.getMatrices().multiply(new Quaternion(rotationMainX.get(), rotationMainY.get(), rotationMainZ.get(), true));
                if (mainHandAnimationX.get() || offHandAnimationX.get()) event.getMatrices().multiply(Quaternion.fromEulerXyz(mainxspeed, 0, 0));
                if (mainHandAnimationY.get() || offHandAnimationY.get()) event.getMatrices().multiply(Quaternion.fromEulerYxz(0, mainyspeed, 0));
            }
        }
    }

}
