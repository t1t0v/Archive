package genesis.team.addon.modules.misc;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;

public class AntiNarrator extends Module {
    public AntiNarrator(){
        super(Genesis.Info, "anti-narrator", "Anti narrator");
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
        isPressedCtrl = Keybind.fromKey(345).isPressed() || Keybind.fromKey(341).isPressed();
    }
}
