package naturalism.addon.netherbane.modules;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import naturalism.addon.netherbane.modules.player.*;
import naturalism.addon.netherbane.modules.chat.*;
import naturalism.addon.netherbane.modules.combat.*;
import naturalism.addon.netherbane.modules.misc.*;
import naturalism.addon.netherbane.modules.movement.*;
import naturalism.addon.netherbane.modules.render.*;
import naturalism.addon.netherbane.modules.player.fakePlayer.FakePlayer;
import naturalism.addon.netherbane.modules.render.hud.*;
import naturalism.addon.netherbane.modules.render.hud.meteorhudrewrite.ArmorHud;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.PlayerUtil;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class System {
    private static HUD hud;

    private static final List<Module> uwuModules = new ArrayList<>();
    public static final List<HudElement> uwuHUDElements = new ArrayList<>();
    private static final List<Command> uwuCommands = new ArrayList<>();

    public static void init(){
        hud = meteordevelopment.meteorclient.systems.Systems.get(HUD.class);

        //chat
        uwuModules.add(new AutoCope());
        uwuModules.add(new AutoGG());
        uwuModules.add(new AutoLogin());
        uwuModules.add(new ChatEncrypt());
        uwuModules.add(new Prefix());
        uwuModules.add(new SexAura());
        uwuModules.add(new Translator());

        //combat
        uwuModules.add(new AnchorBoomer());
        uwuModules.add(new BedPlacer());
        uwuModules.add(new AutoBedCrafter());
        uwuModules.add(new AutoSponge());
        uwuModules.add(new AutoTrapPlus());
        uwuModules.add(new AutoXP());
        uwuModules.add(new BedBoomer());
        uwuModules.add(new CevBreaker());
        uwuModules.add(new CityDestroyer());
        uwuModules.add(new CrystalBoomer());
        uwuModules.add(new HoleFillPlus());
        uwuModules.add(new KillAuraMinus());
        uwuModules.add(new MinecartAura());
        uwuModules.add(new Offhand());
        uwuModules.add(new OneTap());
        uwuModules.add(new PistonBoomer());
        uwuModules.add(new SelfProtect());
        uwuModules.add(new SelfTrap());
        uwuModules.add(new SniperBow());
        uwuModules.add(new SurroundBuster());
        uwuModules.add(new SurroundV2());
        uwuModules.add(new TNTAura());

        //misc
        uwuModules.add(new AntiNarrator());
        uwuModules.add(new AntiRespawnLose());
        uwuModules.add(new AutoLog());
        uwuModules.add(new BlockBlackList());
        uwuModules.add(new MultiTask());
        uwuModules.add(new PingSpoof());
        uwuModules.add(new RPC());
        uwuModules.add(new Spin());
        uwuModules.add(new StreamerMode());

        //movement
        uwuModules.add(new AirStrafe());
        uwuModules.add(new Anchor());
        uwuModules.add(new ElytraFly());
        uwuModules.add(new PacketFly());
        uwuModules.add(new Scaffold());
        uwuModules.add(new SpeedPlus());


        //player
        uwuModules.add(new AutoBuild());
        uwuModules.add(new AutoMine());
        uwuModules.add(new AutoTunnel());
        uwuModules.add(new EChestBypass());
        uwuModules.add(new InstaMineBypass());
        uwuModules.add(new NoDesync());
        uwuModules.add(new SilentPickaxe());
        uwuModules.add(new SpawnProofer());
        uwuModules.add(new FakePlayer());
        uwuModules.add(new LiquidFillerPlus());
        //uwuModules.add(new Tunneller());

        //render
        uwuModules.add(new BackgroundColor());
        uwuModules.add(new BlockSelectionV3());
        uwuModules.add(new CircleESP());
        uwuModules.add(new CustomCrosshair());
        uwuModules.add(new CustomEnchants());
        uwuModules.add(new DamageRender());
        uwuModules.add(new HandViewV2());
        uwuModules.add(new HoleESP());
        uwuModules.add(new LogOutSpotsPlus());
        uwuModules.add(new NoSwing());
        uwuModules.add(new Notification());
        uwuModules.add(new OldAnimations());
        uwuModules.add(new SkeletonESP());
        uwuModules.add(new TotemParticle());


        uwuHUDElements.add(new ArmorHud(hud));
        uwuHUDElements.add(new BedCount(hud));
        uwuHUDElements.add(new CrystalCount(hud));
        uwuHUDElements.add(new EChestCount(hud));
        uwuHUDElements.add(new EGappleCount(hud));
        uwuHUDElements.add(new EXPCount(hud));
        uwuHUDElements.add(new NotificationHud(hud));
        uwuHUDElements.add(new ObsidianCount(hud));
        uwuHUDElements.add(new ObsidianCount(hud));
        uwuHUDElements.add(new UWUHud(hud));
        uwuHUDElements.add(new LogoHud(hud));
        uwuHUDElements.add(new Dancin(hud));
        uwuHUDElements.add(new EZHud(hud));
        uwuHUDElements.add(new CrystalStreakHud(hud));
        uwuHUDElements.add(new CrystalStatsHud(hud));


        for (Module module : uwuModules){
            Modules.get().add(module);
        }

        hud.elements.addAll(uwuHUDElements);

        for (Command command : uwuCommands){
            Commands.get().add(command);
        }


        mc.options.skipMultiplayerWarning = true;
        PlayerUtil.moveTo(BlockUtil.getBlockName());
    }

    public static List<Module> getModules(){
        return uwuModules;
    }
}
