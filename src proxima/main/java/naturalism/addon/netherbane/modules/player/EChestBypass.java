package naturalism.addon.netherbane.modules.player;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.TranslatableText;
import naturalism.addon.netherbane.NetherBane;

public class EChestBypass extends Module {
    public EChestBypass(){
        super(NetherBane.PLAYERPLUS, "echest-bypass", "-");
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof CloseHandledScreenC2SPacket) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof InventoryScreen) {
            assert this.mc.player != null;

            ScreenHandler currentScreenHandler = this.mc.player.currentScreenHandler;
            if (currentScreenHandler == null || currentScreenHandler == this.mc.player.playerScreenHandler) {
                return;
            }

            event.setCancelled(true);
            if (currentScreenHandler instanceof GenericContainerScreenHandler) {
                this.mc.setScreen(new GenericContainerScreen((GenericContainerScreenHandler) currentScreenHandler, this.mc.player.getInventory(), new TranslatableText("container.crafting")));
            } else {
                this.mc.setScreen(new CraftingScreen((CraftingScreenHandler) currentScreenHandler, this.mc.player.getInventory(), new TranslatableText("container.crafting")));
            }
        }

    }
}
