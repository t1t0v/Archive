package naturalism.addon.netherbane.modules.misc;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;

public class AntiNarrator extends Module {
    public AntiNarrator(){
        super(NetherBane.MISCPLUS, "anti-narrator", "");
    }

    boolean isPressedCtrl;

    @EventHandler
    private void keyPressCtrl(KeyEvent event){
        if (event.action == KeyAction.Press && event.key == 66 && isPressedCtrl){
            event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (Keybind.fromKey(345).isPressed() || Keybind.fromKey(341).isPressed()){
            isPressedCtrl = true;
        }
        else {
            isPressedCtrl = false;
        }
    }
}
