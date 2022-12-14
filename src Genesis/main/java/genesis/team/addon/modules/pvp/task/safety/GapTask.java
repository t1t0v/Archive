package genesis.team.addon.modules.pvp.task.safety;

import genesis.team.addon.modules.pvp.PeeVeePee;
import genesis.team.addon.modules.pvp.utils.Helper;
import genesis.team.addon.modules.pvp.utils.HoleUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class GapTask extends SafetyTask {


    public GapTask() {
        super(Modules.get().get(AutoGap.class));
    }

    @Override
    public boolean check() {
        PeeVeePee p = Helper.getAutoPVP();
        return HoleUtils.isPlayerSafe(mc.player) && !(PlayerUtils.getTotalHealth() > p.autogapHealth.get());
    }
}
