package naturalism.addon.netherbane.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;

import java.util.Collections;
import java.util.List;

public class AutoMine extends Module {
    public AutoMine(){
        super(NetherBane.PLAYERPLUS, "auto-mine", "");
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<List<Block>> blackList = sgDefault.add(new BlockListSetting.Builder().name("black-list").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.SPAWNER)).build());

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult result)) return;
        BlockPos pos = result.getBlockPos();

        if (pos == null) return;
        if (blackList.get().contains(BlockUtil.getBlock(pos))) return;
        if (BlockUtil.getState(pos).getHardness(mc.world, pos) == -1) return;

        mc.interactionManager.updateBlockBreakingProgress(pos, Direction.DOWN);
    }
}
