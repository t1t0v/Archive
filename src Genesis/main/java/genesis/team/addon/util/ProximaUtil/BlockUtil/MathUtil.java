package genesis.team.addon.util.ProximaUtil.BlockUtil;

import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class MathUtil {
    private static final Random random = new Random();

    public static double squaredDistanceBetween(Vec3d pos1, Vec3d pos2) {
        double
            x = pos1.x - pos2.x,
            y = pos1.y - pos2.y,
            z = pos1.z - pos2.z;

        return x * x + y * y + z * z;
    }

    public static float getRandomFloat(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
    public static float getRandomFloat() {
        return getRandomFloat(0, 1);
    }
}
