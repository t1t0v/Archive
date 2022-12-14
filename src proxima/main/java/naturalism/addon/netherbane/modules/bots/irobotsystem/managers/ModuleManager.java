package naturalism.addon.netherbane.modules.bots.irobotsystem.managers;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class ModuleManager {
    public static void onModule(String name){
        if (!Modules.get().get(name).isActive()) {
            Modules.get().get(name).toggle();
        }
    }

    public static void offModule(String name){
        if (Modules.get().get(name).isActive()) {
            Modules.get().get(name).toggle();
        }
    }

    public static void offAllModules(){
        for (Module module : Modules.get().getActive()){
            module.toggle();
        }
    }
}
