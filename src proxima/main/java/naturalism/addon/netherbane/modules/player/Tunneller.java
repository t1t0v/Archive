package naturalism.addon.netherbane.modules.player;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.player.AutoTool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Tunneller extends Module {
    public Tunneller(){
        super(NetherBane.PLAYERPLUS, "infinity-tunneller", "");
    }

    private final SettingGroup sgTool = settings.createGroup("Tool");
    private final Setting<Integer> minDuration = sgTool.add(new IntSetting.Builder().name("min-duration").description("The delay between breaks.").defaultValue(100).min(0).sliderMax(500).build());

    public final Setting<List<Block>> repairBlocks = sgTool.add(new BlockListSetting.Builder()
        .name("repair-blocks")
        .description("The repair blocks to mine.")
        .defaultValue(Blocks.COAL_ORE, Blocks.REDSTONE_ORE, Blocks.NETHER_QUARTZ_ORE)
        .filter(this::filter)
        .build()
    );

    private boolean filter(Block block) {
        return block != Blocks.AIR && block.getDefaultState().getHardness(mc.world, null) != -1 && !(block instanceof FluidBlock);
    }

    List<BlockPos> destroyArray = new ArrayList<>();
    BlockPos currentMinePos;

    @EventHandler
    private void onTick(TickEvent.Pre event){

        FindItemResult tool = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);

        //GetForwardBlockState
        if (currentMinePos != null){
            tool = InvUtils.findFastestTool(BlockUtil.getState(currentMinePos));
        }

        boolean isMendByBaritone = false;
        boolean isNeedGoBack = false;
        boolean isMineTunnel = true;
        BlockPos prevPos = null;

        if (!tool.found()) return;

        //CheckItems

        ItemStack itemStack = mc.player.getInventory().getStack(tool.slot());
        if (itemStack.getMaxDamage() - itemStack.getDamage() <= minDuration.get()){
            isMineTunnel = false;
            isMendByBaritone = true;
            prevPos = mc.player.getBlockPos();
        }
        else if (!isMineTunnel){
            isNeedGoBack = true;
            isMendByBaritone = false;
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }

        //BaritoneMend
        if (isMendByBaritone){
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(getRepairBlocks());
        }

        //Go back to prev pos

        if (itemStack.getMaxDamage() - itemStack.getDamage() > minDuration.get() && isNeedGoBack && prevPos != null){
            Goal goal = new GoalBlock(prevPos);
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
        }

        if (mc.player.getBlockPos() == prevPos && !isMineTunnel){
            prevPos = null;
        }
        if (prevPos != null) return;

        if (isMendByBaritone) return;

        //Direction
        Direction forward = mc.player.getHorizontalFacing();
        mc.player.setYaw(forward.asRotation());
        BlockPos playerPos = mc.player.getBlockPos();

        //set center
        double x = mc.player.getX();
        double z = mc.player.getZ();

        switch (forward){
            case EAST, WEST -> {
                z = MathHelper.floor(z) + 0.5;
            }
            case SOUTH, NORTH -> {
                x = MathHelper.floor(mc.player.getX()) + 0.5;
            }
        }
        mc.player.setPosition(x, mc.player.getY(), z);

        //AutoWalk
        if (BlockUtil.isAir(playerPos.offset(forward)) && BlockUtil.isAir(playerPos.offset(forward).up())){
            mc.options.forwardKey.setPressed(true);
        }
        else {
            mc.options.forwardKey.setPressed(false);
        }

        //Mine

        if (!BlockUtil.isAir(playerPos.offset(forward)) && BlockUtils.canBreak(playerPos.offset(forward))){
            destroyArray.add(playerPos.offset(forward));
        }
        if (!BlockUtil.isAir(playerPos.offset(forward).up()) && BlockUtils.canBreak(playerPos.offset(forward).up())){
            destroyArray.add(playerPos.offset(forward).up());
        }

        if (!destroyArray.isEmpty()){
            destroyArray.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
            currentMinePos = destroyArray.get(0);
        }

        if (currentMinePos != null){
            if (BlockUtil.isAir(currentMinePos)){
                destroyArray.remove(currentMinePos);
            }
            else {
                InvUtils.swap(tool.slot(), false);
                mine(currentMinePos);
            }
        }



        //CheckAround

        //AutoWalk

        //SafeWalk

        //Scaffold

        //BreakScaffold

        //PlaceBehind

    }

    private Block[] getRepairBlocks() {
        Block[] array = new Block[repairBlocks.get().size()];
        return repairBlocks.get().toArray(array);
    }

    private void mine(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

}
