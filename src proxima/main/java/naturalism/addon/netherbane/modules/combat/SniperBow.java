package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;
import naturalism.addon.netherbane.utils.Timer;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SniperBow extends Module {
    public SniperBow(){
        super(NetherBane.COMBATPLUS, "sniper-bow", "");
    }

    public enum TargetMode{
        Nearest,
        Crosshair
    }

    public enum RotateMode{
        Vanilla,
        Packet
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<TargetMode> targetMode = sgDefault.add(new EnumSetting.Builder<TargetMode>().name("target-mode").description("Block breaking method").defaultValue(TargetMode.Nearest).build());
    private final Setting<RotateMode> rotateMode = sgDefault.add(new EnumSetting.Builder<RotateMode>().name("rotate-mode").description("Block breaking method").defaultValue(RotateMode.Packet).build());
    private final Setting<Boolean> checkOpen = sgDefault.add(new BoolSetting.Builder().name("check-open").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Boolean> autoShot = sgDefault.add(new BoolSetting.Builder().name("auto-shot").description("Hold city pos.").defaultValue(true).build());

    @EventHandler
    private void onRender(Render3DEvent event){
        FindItemResult bow = InvUtils.findInHotbar(Items.BOW);
        FindItemResult arrows = InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem);
        if (!bow.found() || !arrows.found()) return;
        //getTarget
        List<PlayerEntity> targets = EntityUtil.getTargetsInRange(256D);
        if (checkOpen.get()){
            targets = targets.stream().filter(PlayerUtils::canSeeEntity).collect(Collectors.toList());
        }
        switch (targetMode.get()){
            case Nearest ->  targets.sort(Comparator.comparing(player -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), player.getBlockPos())));
            case Crosshair -> {
                targets.sort(Comparator.comparing(player -> {
                    double yaw = mc.player.getYaw();
                    double yawToPlayer = Rotations.getYaw(player);
                    return Math.abs(yaw - yawToPlayer);
                }));
            }
        }

        if (targets.isEmpty()) return;

        PlayerEntity target = targets.get(0);

        if (autoShot.get()){
            switch (rotateMode.get()){
                case Packet -> {
                    aim(event.tickDelta, target);
                }
                case Vanilla -> {
                    mc.player.setYaw((float) Rotations.getYaw(target.getEyePos()));
                    mc.player.setPitch((float) Rotations.getPitch(target.getEyePos()));
                }
            }

            if (!(mc.player.getMainHandStack().getItem() instanceof BowItem)){
                InvUtils.swap(bow.slot(), false);
            }

            if (mc.player.getItemUseTime() >= 25) {
                mc.player.stopUsingItem();
                mc.interactionManager.stopUsingItem(mc.player);
            } else {
                setPressed(true);
            }
        }else {
            if (mc.player.isUsingItem() && mc.player.getMainHandStack().getItem() instanceof BowItem && mc.options.useKey.isPressed()){
                switch (rotateMode.get()){
                    case Packet -> {
                        aim(event.tickDelta, target);
                    }
                    case Vanilla -> {
                        mc.player.setYaw((float) Rotations.getYaw(target.getEyePos()));
                        mc.player.setPitch((float) Rotations.getPitch(target.getEyePos()));
                    }
                }
                if (mc.player.getItemUseTime() >= 25) {
                    mc.player.stopUsingItem();
                    mc.interactionManager.stopUsingItem(mc.player);
                } else {
                    setPressed(true);
                }
            }
        }
    }

    private void setPressed(boolean pressed) {
        mc.options.useKey.setPressed(pressed);
    }

    private void aim(double tickDelta, PlayerEntity target) {
        // Velocity based on bow charge.
        float velocity = (mc.player.getItemUseTime() - mc.player.getItemUseTimeLeft()) / 20f;
        velocity = (velocity * velocity + velocity * 2) / 3;
        if (velocity > 1) velocity = 1;

        // Positions
        double posX = target.getPos().getX() + (target.getPos().getX() - target.prevX) * tickDelta;
        double posY = target.getPos().getY() + (target.getPos().getY() - target.prevY) * tickDelta;
        double posZ = target.getPos().getZ() + (target.getPos().getZ() - target.prevZ) * tickDelta;

        // Adjusting for hitbox heights
        posY -= 1.9f - target.getHeight();

        double relativeX = posX - mc.player.getX();
        double relativeY = posY - mc.player.getY();
        double relativeZ = posZ - mc.player.getZ();

        // Calculate the pitch
        double hDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
        double hDistanceSq = hDistance * hDistance;
        float g = 0.006f;
        float velocitySq = velocity * velocity;
        float pitch = (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - g * (g * hDistanceSq + 2 * relativeY * velocitySq))) / (g * hDistance)));

        // Set player rotation
        if (Float.isNaN(pitch)) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
        } else {
            Rotations.rotate(Rotations.getYaw(new Vec3d(posX, posY, posZ)), pitch);
        }
    }
}
