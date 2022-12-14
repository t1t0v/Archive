package genesis.team.addon.util.ProximaUtil;

import genesis.team.addon.modules.misc.Scaffold.Scaffold;
import net.minecraft.util.math.MathHelper;

public class Timer {
    private long time;

    public Timer() {
        time = System.currentTimeMillis();
    }

    public static float[] Field2558 = new float[20];

    public static float Method2190() {
        float f;
        float f2;
        float f3;
        try {
            f3 = Field2558[Field2558.length - 1];
            f2 = 0.0f;
            f = 20.0f;
        } catch (Exception exception) {
            exception.printStackTrace();
            return 20.0f;
        }
        return MathHelper.clamp(f3, f2, f);
    }

    public boolean hasPassed(double ms) {
        return System.currentTimeMillis() - time >= ms;
    }

    public void reset() {
        time = System.currentTimeMillis();
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    private static Scaffold currentModule;
    private static int priority;
    private static float timerSpeed;
    private static boolean active = false;
    private final boolean tpsSync = false;

    public static void updateTimer(Scaffold module, int prio, float speed) {
        if (module == currentModule) {
            priority = prio;
            timerSpeed = speed;
            active = true;
        } else if (priority > prio || !active) {
            currentModule = module;
            priority = prio;
            timerSpeed = speed;
            active = true;
        }
    }

    public static void resetTimer(Scaffold module) {
        if (currentModule == module) {
            active = false;
        }
    }
}
