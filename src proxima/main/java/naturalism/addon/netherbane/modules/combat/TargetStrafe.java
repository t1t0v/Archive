package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;

public class TargetStrafe extends Module {
    public TargetStrafe(){
        super(NetherBane.COMBATPLUS, "target-strafe", "");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){

    }
}
