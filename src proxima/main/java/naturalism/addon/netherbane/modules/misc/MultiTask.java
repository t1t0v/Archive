package naturalism.addon.netherbane.modules.misc;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.events.InteractEvent;


public class MultiTask extends Module {
    public MultiTask(){
        super(NetherBane.MISCPLUS, "MultiTask", "");

    }

    @EventHandler
    private void onPacket(InteractEvent event){
        event.usingItem = false;
    }

}
