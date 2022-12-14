package naturalism.addon.netherbane.utils;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;

public class KeyUtil {

    public enum Action {
        PRESS,
        REPEAT,
        RELEASE;

        public static Action get(int action) {
            if (action == GLFW.GLFW_RELEASE) return RELEASE;
            else if (action == GLFW.GLFW_PRESS) return PRESS;
            else return REPEAT;
        }
    }

    public static String keyOf(int id) {
        if(id == 345) return "rctrl";
        if(id == 344) return "rshift";
        return KeyEvent.getKeyText(id);
    }
}
