package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import meteordevelopment.meteorclient.events.entity.player.PlaceBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoSponge extends Module {
    public AutoSponge(){
        super(Genesis.Px, "auto-sponge", "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Boolean> checkDamage = sgGeneral.add(new BoolSetting.Builder().name("check-damage").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Boolean> placeUnderObby = sgGeneral.add(new BoolSetting.Builder().name("place-under-obby").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Boolean> aroundObby = sgGeneral.add(new BoolSetting.Builder().name("around-obby").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Double> maxDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-damage").description("The radius in which players get targeted.").defaultValue(6).min(0).sliderMax(36).visible(checkDamage::get).build());

    @EventHandler
    private void onPlaceObby(PlaceBlockEvent event){
        if (event.block.equals(Blocks.OBSIDIAN) && ((checkDamage.get() && DamageUtils.crystalDamage(mc.player, Utils.vec3d(event.blockPos.up())) < maxDamage.get()) || !checkDamage.get()) && BlockUtil.getBlock(event.blockPos.up()).equals(Blocks.WATER)){
            BlockUtils.place(getBlockPos(event.blockPos), InvUtils.findInHotbar(Items.SPONGE), rotate.get(), 50);
        }
    }

    private BlockPos getBlockPos(BlockPos pos){
       List<BlockPos> array = new ArrayList<>(){{
            if (placeUnderObby.get()){
                if (BlockUtil.isAir(pos.down()) || BlockUtil.getBlock(pos.down()) == Blocks.WATER) add(pos.down());
            }
            if (aroundObby.get()){
                if (BlockUtil.isAir(pos.east()) || BlockUtil.getBlock(pos.east()) == Blocks.WATER) add(pos.east());
                if (BlockUtil.isAir(pos.west()) || BlockUtil.getBlock(pos.west()) == Blocks.WATER) add(pos.west());
                if (BlockUtil.isAir(pos.south()) || BlockUtil.getBlock(pos.south()) == Blocks.WATER) add(pos.south());
                if (BlockUtil.isAir(pos.north()) || BlockUtil.getBlock(pos.north()) == Blocks.WATER) add(pos.north());
            }
        }};
       if (array.isEmpty()) return null;
        array.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));

        return array.get(0);
    }
}
