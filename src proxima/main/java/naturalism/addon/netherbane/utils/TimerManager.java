package naturalism.addon.netherbane.utils;


import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MinecraftClientMixin;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.mixins.ITimer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.MinecraftClientGame;
import net.minecraft.client.RunArgs;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TimerManager {
    private Module currentModule;
    private int priority;
    private float timerSpeed;
    private boolean active = false;
    private boolean tpsSync = false;

    public void updateTimer(Module module, int priority, float timerSpeed) {
        if (module == currentModule) {
            this.priority = priority;
            this.timerSpeed = timerSpeed;
            this.active = true;
        } else if (priority > this.priority || !this.active) {
            this.currentModule = module;
            this.priority = priority;
            this.timerSpeed = timerSpeed;
            this.active = true;
        }
    }

    public void resetTimer(Module module) {
        if (this.currentModule == module) {
            active = false;
        }
    }

    @EventHandler
    public void onUpdate(TickEvent.Pre event) {
        try {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            assert playerListEntry != null;

            RenderTickCounter renderTickCounter = new RenderTickCounter(20, playerListEntry.getLatency());
            if (mc.world == null || mc.player == null) {

                ((ITimer) (renderTickCounter)).setTickLength(50);
                return;
            }
            if (tpsSync && TickRateUtil.INSTANCE.getLatestTickRate() > 0.125D) { // 0.125D check is nessasary to avoid 0tps when joining server
                ((ITimer)(renderTickCounter)).setTickLength(Math.min(500, 50F * (20F / TickRateUtil.INSTANCE.getLatestTickRate())));
            } else {
                ((ITimer) (renderTickCounter)).setTickLength(active ? (50.0f / timerSpeed) : 50.0f);
            }
        }catch (NullPointerException ignored){}

    }

    public boolean isTpsSync() {
        return tpsSync;
    }

    public void setTpsSync(boolean tpsSync) {
        this.tpsSync = tpsSync;
    }
}
