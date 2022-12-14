package com.example.addon.modules.combat;

import com.example.addon.Addon;
import com.example.addon.events.InteractEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class MultiTask extends Module {
    public MultiTask() {
        super(Addon.Alliance, "multi-task", "Allows you to eat while mining a block.");
    }

    @EventHandler
    public void onInteractEvent(InteractEvent event) {
        event.usingItem = false;
    }
}
