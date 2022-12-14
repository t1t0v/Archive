package genesis.team.addon.modules.misc;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Collections;
import java.util.List;

public class BlockBlackList extends Module {
    public BlockBlackList(){
        super(Genesis.Misc, "block-black-list", "Black List");
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<List<Block>> blackList = sgDefault.add(new BlockListSetting.Builder().name("black-list").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.SPAWNER)).build());

    @EventHandler
    private void onMine(StartBreakingBlockEvent event){
        if (blackList.get().contains(BlockUtil.getBlock(event.blockPos))){
            event.cancel();
        }
    }
}
