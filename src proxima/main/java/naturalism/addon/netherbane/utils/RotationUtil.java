package naturalism.addon.netherbane.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class RotationUtil {
    public static boolean isRotationsSet() {
        return rotationsSet;
    }

    public void lookAtVec3d(Vec3d vec3d) {
        float[] angle = calculateAngle(mc.player.getEyePos(), new Vec3d(vec3d.x, vec3d.y, vec3d.z));
        setRotations(angle[0], angle[1]);
    }

    public void reset() {
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        rotationsSet = false;
    }


    public void setRotations(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        rotationsSet = true;
    }

    private float yaw;
    private float pitch;

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    private static boolean rotationsSet = false;

    public void lookAtXYZ(double x, double y, double z) {
        Vec3d vec3d = new Vec3d(x, y, z);
        lookAtVec3d(vec3d);
    }

    public static float[] calculateAngle(Vec3d from, Vec3d to) {
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));
        float yD = (float)MathHelper.wrapDegrees((double)(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0));
        float pD = (float)MathHelper.wrapDegrees((double)Math.toDegrees(Math.atan2(difY, dist)));
        if (pD > 90F) {
            pD = 90F;
        } else if (pD < -90F) {
            pD = -90F;
        }
        return new float[]{yD, pD};
    }

    public static double[] calculateLookAt(double px, double py, double pz, PlayerEntity me) {
        double dirx = me.getX() - px;
        double diry = me.getY() - py;
        double dirz = me.getZ() - pz;

        double len = Math.sqrt(dirx * dirx + diry * diry + dirz * dirz);

        dirx /= len;
        diry /= len;
        dirz /= len;

        double pitch = Math.asin(diry);
        double yaw = Math.atan2(dirz, dirx);

        //to degree
        pitch = pitch * 180.0d / Math.PI;
        yaw = yaw * 180.0d / Math.PI;

        yaw += 90f;

        return new double[]{yaw, pitch};
    }

    public static void update(float yaw, float pitch) {
        boolean flag = mc.player.isSprinting();

        if (flag != (mc.player.isSprinting())) {
            if (flag) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            } else {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }

            mc.player.setSprinting(flag);
        }

        boolean flag1 = mc.player.isSneaking();

        if (flag1 != mc.player.isSneaking()) {
            if (flag1) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            } else {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }

            mc.player.setSneaking(flag);
        }

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);


    }

    public static Direction getClockWise(Direction direction){
        switch (direction){
            case WEST -> { return  Direction.SOUTH; }
            case EAST -> { return  Direction.NORTH; }
            case NORTH -> { return Direction.WEST; }
            case SOUTH -> { return Direction.EAST; }
        }
        return null;
    }

    public static Direction getCounterClockWise(Direction direction){
        switch (direction){
            case WEST -> { return  Direction.NORTH; }
            case EAST -> { return  Direction.SOUTH; }
            case NORTH -> { return Direction.EAST; }
            case SOUTH -> { return Direction.WEST; }
        }
        return null;
    }
}
