package naturalism.addon.netherbane.utils;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.Util;

public class TimeUtil {
    public static long GetResponseTime() {
        return Util.getMeasuringTimeMs() + GetLatency3();
    }

    public static long GetLatency3() {
        return Math.max(LatencyUtil.GetRealLatency(), GetIntervalPerTick()) * 2L;
    }

    public static long GetCurTime() {
        return Util.getMeasuringTimeMs();
    }

    public static long GetIntervalPerTick() {
        return 50L;
    }

}
