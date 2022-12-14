package naturalism.addon.netherbane.modules.bots.irobotsystem.managers;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import naturalism.addon.netherbane.utils.BlockUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PathBuilder {
    public static void init(PlayerEntity player, HoleMode holeMode, HoleChooseMode chooseMode, boolean doubleHole, double xz, double y){
        BlockPos pos = getBestHole(player, holeMode, doubleHole, chooseMode, xz, y);
        if (pos == null) return;
        buildPathTo(pos);
    }

    public static void buildPathTo(BlockPos blockPos){
        Goal goal = new GoalBlock(blockPos);
        assert mc.player != null;
        if (mc.player.getBlockPos() == blockPos){
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }else{
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
        }
    }

    public static BlockPos getBestHole(PlayerEntity player, HoleMode holeMode, boolean doubleHole, HoleChooseMode mode, double xz, double y){
        switch (mode){
            case Nearest -> {
                List<BlockPos> holes = getHoles(player, holeMode, doubleHole, xz, y);
                if (holes.isEmpty()) return null;
                holes.sort(Comparator.comparing(blockPos -> BlockUtil.distance(mc.player.getBlockPos(), blockPos)));
                return holes.get(0);
            }
            case Free -> {
                List<BlockPos> holes = getHoles(player, holeMode, doubleHole, xz, y);
                if (holes.isEmpty()) return null;

                holes = holes.stream().filter(blockPos -> {
                    Box box = new Box(blockPos);
                    if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity)){
                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());
                holes.sort(Comparator.comparing(blockPos -> BlockUtil.distance(mc.player.getBlockPos(), blockPos)));
                return holes.get(0);
            }
        }
        return player.getBlockPos();
    }

    private static List<BlockPos> getHoles(PlayerEntity player, HoleMode holeMode, boolean doubleHole, double xz, double y){
        assert player != null;
        List<BlockPos> holes = BlockUtil.getSphere(player.getBlockPos(), (int) xz, (int) y).stream().filter(block -> BlockUtil.isAir(block) && BlockUtil.isAir(block.up()) && BlockUtil.isAir(block.up(2)) && (BlockUtil.isHole(block) || (BlockUtil.isDoubleHole(block) && doubleHole))).collect(Collectors.toList());
        switch (holeMode){
            case GreenHole -> holes = holes.stream().filter(blockPos -> BlockUtil.getBlock(blockPos) == Blocks.BEDROCK).collect(Collectors.toList());
            case BothHole -> holes = holes.stream().filter(blockPos -> (BlockUtil.getBlock(blockPos) == Blocks.BEDROCK) || BlockUtil.getBlock(blockPos) == Blocks.OBSIDIAN).collect(Collectors.toList());
        }
        holes.sort(Comparator.comparing(block -> BlockUtil.distance(mc.player.getBlockPos(), block)));
        return holes;
    }

    public enum HoleChooseMode{
        Nearest,
        Free
    }

    public enum HoleMode{
        GreenHole,
        BothHole
    }
}
