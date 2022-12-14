package genesis.team.addon.util.other;

import genesis.team.addon.Genesis;
import genesis.team.addon.commands.MoneyMod;
import genesis.team.addon.commands.Move;
import genesis.team.addon.modules.bbc.AutoCity;
import genesis.team.addon.modules.bbc.BogeyMan.BogeyMan;
import genesis.team.addon.modules.bbc.DeathCrystal.DeathCrystal;
import genesis.team.addon.modules.bbc.PacketMine;
import genesis.team.addon.modules.bbc.QuickMend;
import genesis.team.addon.modules.combat.*;
import genesis.team.addon.modules.combat.AutoCrystal.AutoCrystal;
import genesis.team.addon.modules.combat.AutoCrystal.CrystalUtils;
import genesis.team.addon.modules.combat.AutoCrystalRewrite.AutoCrystalRewrite;
import genesis.team.addon.modules.combat.BedBomb.BedBomb;
import genesis.team.addon.modules.combat.BedBomb.BedUtils;
import genesis.team.addon.modules.combat.BedBoomer.BedBoomer;
import genesis.team.addon.modules.combat.CrystalBoomer.CrystalBoomer;
import genesis.team.addon.modules.combat.FeetTrap.FeetTrap;
import genesis.team.addon.modules.combat.MinecartAura.MinecartAura;
import genesis.team.addon.modules.combat.MiningTS.MiningTS;
import genesis.team.addon.modules.combat.PistonCrystal.PistonCrystal;
import genesis.team.addon.modules.combat.PostTickKA.PostTickKA;
import genesis.team.addon.modules.combat.VenomCrystal.VenomCrystal;
import genesis.team.addon.modules.haha.*;
import genesis.team.addon.modules.info.AutoEz.DeathUtils;
import genesis.team.addon.modules.info.*;
import genesis.team.addon.modules.info.Translator.Translator;
import genesis.team.addon.modules.misc.*;
import genesis.team.addon.modules.misc.AutoCraft;
import genesis.team.addon.modules.misc.BedCrafter.BedCrafter;
import genesis.team.addon.modules.misc.PacketFly.PacketFly;
import genesis.team.addon.modules.misc.Scaffold.Scaffold;
import genesis.team.addon.modules.misc.clear.clear;
import genesis.team.addon.modules.movement.*;
import genesis.team.addon.modules.movement.ElytraFly.ElytraFly;
import genesis.team.addon.modules.movement.SpeedPlus.SpeedPlus;
import genesis.team.addon.modules.pvp.PeeVeePee;
import genesis.team.addon.modules.render.*;
import genesis.team.addon.modules.totem.AutoTotem;
import genesis.team.addon.modules.totem.Offhand;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.commands.Commands;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Loader {
    public static ExecutorService executor = Executors.newSingleThreadExecutor();
    public static ExecutorService moduleExecutor = Executors.newFixedThreadPool(5);

    public static void init() {


        MeteorClient.EVENT_BUS.registerLambdaFactory("genesis.team.addon", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())); // event handler


        BedUtils.init();
        CrystalUtils.init();
        DeathUtils.init();
        postInit();
    }

    public static void postInit() {
        //Modules
        Genesis.addModules(
                //Totem
                new AutoTotem(),
                new Offhand(),                     new PeeVeePee(),

                // Info
                new AutoTesak(),
                new SexAura(),
                new Translator(),
                new ChatConfig(),
                new AutoReKit(),
                new AntiNarrator(),
                new NotifierPlus(),

                // Render
                new BackgroundColor(),
                new Breadcrumbs(),
                new PenisESP(),
                new CircleESP(),
                new BlockSelectionV3(),
                new CoolRenderPlus(),
                new HoleESP(),
                new LogOutSpotsPlus(),
                new NoSwing(),
                new OldAnimations(),

                //Proxima
                new CrystalBoomer(),
                new CevBreakerPx(),
                new VenomCrystal(),
                new SniperBow(),
                new BedPlacer(),
                new BedBoomer(),
                new MinecartAura(),
                new OffhandV2(),
                new OneTap(),
                new AutoXP(),
                new PistonCrystal(),
                new SilentPickaxe(),
                new AutoCrystalRewrite(),
                new SelfTrapPx(),
                new SurroundV2Px(),
                new SelfProtect(),
                new AutoSponge(),
                new CityDestroyer(),

                // Combat
                new AutoCraftPlus(),
                new AnchorBoomer(),
                new MiningTS(),
                new AutoTrapPlus(),
                new AutoCrystal(),
                new AntiSurroundBlocks(),
                new PostTickKA(),
                new AutoCrystalRewrite(),
                new BowBomb(),
                new FeetTrap(),
                new cuboid(),
                new PistonAura(),
                new AntiPistonPush(),
                new BedBomb(),
                new CevBreaker(),
                new HoleFill(),
                new HeadProtect(),
                new SelfTrap(),
                new Surround(),
                new TNTAura(),
                new PistonPush(),

                //Movement
                new TickShift(),
                new SpeedPlus(),
                new ElytraRepl(),
                new ElytraFly(),
                new ElytraHelper(),
                new AirStrafe(),
                new TimerFall(),
                new EFly(),
                new Strafe(),

                //BBC
                new BogeyMan(),
                new DeathCrystal(),
                new genesis.team.addon.modules.bbc.Surround.Surround(),
                new AutoCity(),
                new QuickMend(),
                new PacketMine(),

                //new
                new More_tracers(),
                new Nav_tracer(),
                new NoRotate(),
                new Villager_Aura(),
                new Prefix(),
                new SCAFFOLD(),
                new SilentItem(),
                new ViewModel(),


                // Misc
                new rangechecker(),
                new OneClickEat(),
                new ArmorMessages(),
                new clear(),
                new Twerk(),
                new AntiGlitchBlock(),
                new InstaMineBypass(),
                new NewChunks(),
                new InstaMinePlus(),
                new PingSpoof(),
                new PacketFly(),
                new AutoCraft(),
                new AntiRespawnLose(),
                new AntiRespawnLose(),
                new AutoBuild(),
                new AutoTunnel(),
                new LogOut(),
                new MultiTask(),
                new Phase(),
                new Scaffold(),
                new BedCrafter(),
                new BlockBlackList()

        );

        // Hud

        // Commands
        Commands.get().add(new MoneyMod());
        Commands.get().add(new Move());
    }

    public static void addCpvpEu(){
        ServerList servers = new ServerList(mc);
        servers.loadFile();

        boolean b = false;
        for (int i = 0; i < servers.size(); i++) {
            ServerInfo server = servers.get(i);

            if (server.address.contains("play.new-places.ru")) {
                b = true;
                break;
            }
        }

        if (!b) {
            servers.saveFile();
        }
    }
    // Shutdown background auth threads
    public static void shutdown() {
        executor.shutdown();
        moduleExecutor.shutdown();
    }
}

//Info
//new AutoExcuse(),
//new AutoEz(),
//new KillFx(),
//new Notifications(),
//new SpamPlus(),
//new AutoTpa(),
//new RPC(),
//Render
//new CustomCrosshair(),
//new SkeletonESP(),
//new HandViewV2(),
//Combat
//new TNTAuraProxima(),
//new SelfProtect(),
//new Burrow(),
//new AutoMinecart(),
//new QQuiver(),
//Movement
//new JumpFlight(),
//new SpiderPlus(),
//Misc
//new OffHando(),
//new AntiLay(),
//new EyeFinder(),
//new AutoSell(),
//new Sevila(),
//new ChorusPredict(),
//new AutoFloor(),
//new AutoBedTrap()