package com.example.addon;

import com.example.addon.commands.Move;
import com.example.addon.hud.HudExample;
import com.example.addon.modules.combat.*;
import com.example.addon.modules.info.FakePops;
import com.example.addon.modules.info.KillEffect;;
import com.example.addon.modules.info.Notifications;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category Alliance = new Category("Alliance", Items.END_CRYSTAL.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("Худа не будет потому что я так сказал");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon");

        // Modules
        Modules.get().add(new AutoCrystalRewrite());
        Modules.get().add(new PistonAura());
        Modules.get().add(new Notifications());
        Modules.get().add(new SurroundPlus());
        Modules.get().add(new BedBomb());
        Modules.get().add(new FakePops());
        Modules.get().add(new MultiTask());
        Modules.get().add(new KillEffect());
        Modules.get().add(new BedCrafter());
        Modules.get().add(new LogOutSpots());
        Modules.get().add(new CityBreaker());
        Modules.get().add(new HeadProtect());
        Modules.get().add(new AutoTrapPlus());
        Modules.get().add(new AntiSurroundBlock());
        Modules.get().add(new CevBreaker());
        Modules.get().add(new SelfTrapPlus());
        Modules.get().add(new EFly());
        Modules.get().add(new Surround());
        Modules.get().add(new Prefix());
        Modules.get().add(new PistonPush());
        // Commands
        Commands.get().add(new Move());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Alliance);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
