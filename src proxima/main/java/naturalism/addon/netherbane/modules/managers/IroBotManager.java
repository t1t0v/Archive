package naturalism.addon.netherbane.modules.managers;

import baritone.api.BaritoneAPI;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.modules.bots.irobotsystem.IroBotSystem;
import naturalism.addon.netherbane.modules.bots.irobotsystem.managers.FindTarget;
import naturalism.addon.netherbane.modules.bots.irobotsystem.managers.ModuleManager;
import naturalism.addon.netherbane.modules.bots.irobotsystem.managers.PathBuilder;

import java.util.Collections;
import java.util.List;

public class IroBotManager extends Module {
    public IroBotManager(){
        super(NetherBane.MANAGERS, "iro-bot-manager", "");
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<FindTarget.TargetMode> targetMode = sgDefault.add(new EnumSetting.Builder<FindTarget.TargetMode>().name("target-mode").description("Block breaking method").defaultValue(FindTarget.TargetMode.Nearest).build());
    private final Setting<String> name = sgDefault.add(new StringSetting.Builder().name("name").description("Name to be replaced with.").defaultValue("WomanAreObject").visible(() -> targetMode.get() == FindTarget.TargetMode.Name).build());
    private final Setting<Boolean> ignoreNoob = sgDefault.add(new BoolSetting.Builder().name("ignore-noob").description("Instant break").defaultValue(true).build());
    private final Setting<PathBuilder.HoleMode> holeMode = sgDefault.add(new EnumSetting.Builder<PathBuilder.HoleMode>().name("hole-mode").description("Block breaking method").defaultValue(PathBuilder.HoleMode.BothHole).build());
    private final Setting<PathBuilder.HoleChooseMode> holeChooseMode = sgDefault.add(new EnumSetting.Builder<PathBuilder.HoleChooseMode>().name("hole-choose-mode").description("Block breaking method").defaultValue(PathBuilder.HoleChooseMode.Free).build());
    private final Setting<Double> XZRange = sgDefault.add(new DoubleSetting.Builder().name("XZ-range").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Double> YRange = sgDefault.add(new DoubleSetting.Builder().name("Y-range").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Boolean> doubleHole = sgDefault.add(new BoolSetting.Builder().name("double-hole").description("Instant break").defaultValue(true).build());

    private final Setting<Double> moduleToggleRange = sgDefault.add(new DoubleSetting.Builder().name("module-toggle-range").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    public final Setting<List<Module>> modules = sgDefault.add(new ModuleListSetting.Builder().name("crystal-modules").description("Modules at which the crystal will be taken").defaultValue(Collections.singletonList(Modules.get().get(KillAura.class))).build());

    @Override
    public void onDeactivate() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        for (Module module : modules.get()){
            ModuleManager.offModule(module.name);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        IroBotSystem.init(targetMode.get(), name.get(), ignoreNoob.get(), holeMode.get(), holeChooseMode.get(), doubleHole.get(), moduleToggleRange.get(), modules.get(), XZRange.get(), YRange.get());
    }
}
