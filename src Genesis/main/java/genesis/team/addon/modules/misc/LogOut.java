package genesis.team.addon.modules.misc;

import genesis.team.addon.Genesis;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class LogOut extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> totems = sgGeneral.add(new BoolSetting.Builder().name("totems").description("Disconnects if you don't have totems.").defaultValue(true).build());
    private final Setting<Integer> totemCount = sgGeneral.add(new IntSetting.Builder().name("count").description("How many totems need to disconnect.").defaultValue(1).sliderRange(0, 36).visible(totems::get).build());
    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder().name("message").description("Sends message before disconnect.").defaultValue(false).build());
    private final Setting<String> messageText = sgGeneral.add(new StringSetting.Builder().name("text").description("The text which you send.").defaultValue("You are very boring, I went to play on another server.").visible(message::get).build());
    private final Setting<Boolean> error = sgGeneral.add(new BoolSetting.Builder().name("error").description("Uses minecraft error instead of totem notification.").defaultValue(false).build());
    private final Setting<Boolean> toggle = sgGeneral.add(new BoolSetting.Builder().name("toggle").description("Turn's off module if player left the server.").defaultValue(true).build());

    public LogOut() {
        super(Genesis.Misc, "log-out", "Automatically disconnects you when certain requirements are met.");
    }

    @Override
    public void onActivate() {

    }

    int ticks = 1;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (totems.get() && totem.count() <= totemCount.get()) {
            if (message.get() && messageText.get() != null && ticks == 1) ChatUtils.sendPlayerMsg(messageText.get());
            ticks--;

            if (ticks <= 0) disconnect(error.get());
        }
    }

    private void disconnect(boolean error) {
        error("1000 - 7 14 y.o. ghoul zxc");
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        ticks = 1;

        if (!toggle.get()) return;
        toggle();
        return;
    }
}