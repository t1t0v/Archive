package genesis.team.addon.modules.pvp.task.combat;

import genesis.team.addon.modules.pvp.utils.BlockUtils;
import genesis.team.addon.modules.pvp.utils.Helper;
import genesis.team.addon.modules.pvp.utils.HoleUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.player.PlayerEntity;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CATask extends CombatTask{

    public CATask() {
        super(Modules.get().get(CrystalAura.class));
    }

    @Override
    public boolean selfCheck() {
        if (!Helper.getAutoPVP().useCrystalAura.get()) return false;
        if (PlayerUtils.getTotalHealth() <= Helper.getMinHealth()) return false;
        return HoleUtils.isPlayerSafe(mc.player);
    }

    @Override
    public boolean targetCheck() {
        if (!Helper.getAutoPVP().useCrystalAura.get()) return false;
        PlayerEntity target = Helper.getCurrentTarget();
        if (TargetUtils.isBadTarget(target, Helper.getTargetRange())) return false;
        return !BlockUtils.isSafePos(target.getBlockPos());
    }
}
