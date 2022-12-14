package naturalism.addon.netherbane.modules.bots.irobotsystem;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import naturalism.addon.netherbane.modules.bots.irobotsystem.managers.FindTarget;
import naturalism.addon.netherbane.modules.bots.irobotsystem.managers.ModuleManager;
import naturalism.addon.netherbane.modules.bots.irobotsystem.managers.PathBuilder;
import naturalism.addon.netherbane.utils.BlockUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;


import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class IroBotSystem{
    public static void init(FindTarget.TargetMode targetMode, String name, boolean ignoreNoob, PathBuilder.HoleMode holeMode, PathBuilder.HoleChooseMode chooseMode, boolean doubleHole, double range, List<Module> modules, double xz, double y){
        //Find target
        PlayerEntity player = FindTarget.init(targetMode, name, ignoreNoob);
        if (player == null) return;

        ChatUtils.info(player.getEntityName());
        //Find hole
        PathBuilder.init(player, holeMode, chooseMode, doubleHole, xz, y);

        //ToggleModule

        if (BlockUtil.distance(new BlockPos(mc.player.getEyePos()), player.getBlockPos()) < range){
            for (Module module : modules){
                ModuleManager.onModule(module.name);
            }
        }else {
            for (Module module : modules){
                ModuleManager.offModule(module.name);
            }
        }

        //DrinkPotion
    }


}
