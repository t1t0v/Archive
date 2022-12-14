package com.example.addon.util.autocrystalrewrite;

import com.example.addon.modules.combat.AutoCrystalRewrite;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class CrystalMap {
    private int crystalId;
    public int attacks;
    private int tick;

    public boolean shouldWait;

    public CrystalMap(int crystalId, int attacks) {
        this.crystalId = crystalId;
        this.attacks = attacks;
        this.shouldWait = false;
        this.tick = 0;
    }

    public int id() {
        return crystalId;
    }

    public void tick() {
        if (shouldWait) tick++;
    }

    public boolean hittable() {
        return tick > Modules.get().get(AutoCrystalRewrite.class).passedTicks.get();
    }
}
