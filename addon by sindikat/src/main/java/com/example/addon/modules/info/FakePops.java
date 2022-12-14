package com.example.addon.modules.info;

import java.util.concurrent.ThreadLocalRandom;

import com.example.addon.Addon;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class FakePops extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<String> target;
    private final Setting<Integer> min;
    private final Setting<Integer> max;
    private final Setting<Integer> maxPops;
    private final Setting<String> popMessage;
    private int timer;
    private int i;

    public FakePops() {
        super(Addon.Alliance, "FakePops", "FakePops");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.target = this.sgGeneral.add((new Builder()).name("target").description("The player's name to fake the totem pops of").defaultValue("Griffer310").build());
        this.min = this.sgGeneral.add((new meteordevelopment.meteorclient.settings.IntSetting.Builder()).name("min-delay").description("The minimum delay between pop messages in ticks.").defaultValue(40).min(0).sliderMax(200).build());
        this.max = this.sgGeneral.add((new meteordevelopment.meteorclient.settings.IntSetting.Builder()).name("max-delay").description("The maximumdelay between pop messages in ticks.").defaultValue(80).min(0).sliderMax(200).build());
        this.maxPops = this.sgGeneral.add((new meteordevelopment.meteorclient.settings.IntSetting.Builder()).name("Max-amount-of-pops").description("Specify after how many pops it will stop.").defaultValue(12).min(1).sliderMax(37).build());
        this.popMessage = this.sgGeneral.add((new Builder()).name("pop-message").description("Chat alert to send when a player pops.").defaultValue("Bown down {player}! You popped {pops} {totems}!").build());
        this.i = 1;
    }

    public void onActivate() {
        this.timer = ThreadLocalRandom.current().nextInt((Integer)this.min.get(), (Integer)this.max.get() + 1);
    }

    @EventHandler
    private void onTick(Post event) {
        if (this.timer <= 0) {
            String tots = " totems";
            if (this.i == 1) {
                tots = " totem";
            }

            assert this.mc.player != null;

            String msg = ((String)this.popMessage.get()).replace("{player}", (CharSequence)this.target.get()).replace("{pops}", Integer.toString(this.i)).replace("{totems}", this.i == 1 ? "totem" : "totems");
            ++this.i;
            this.timer = ThreadLocalRandom.current().nextInt((Integer)this.min.get(), (Integer)this.max.get() + 1);
        } else {
            --this.timer;
        }

        if (this.i == (Integer)this.maxPops.get() + 1) {
            this.toggle();
        }

    }
}
