package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SurroundBuster extends Module {
    public SurroundBuster() {
        super(NetherBane.COMBATPLUS, "surround-buster", "");
    }

    public enum Mode {
        Default,
        Full,
        AntiCity,
        Square
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> enemyRange = sgGeneral.add(new DoubleSetting.Builder().name("enemy-range").description("The range at which players can be targeted.").defaultValue(5).min(0.0).sliderMax(15).build());
    private final Setting<Double> cityRange = sgGeneral.add(new DoubleSetting.Builder().name("city-range").description("The range at which players can be targeted.").defaultValue(5).min(0.0).sliderMax(15).build());

    private final Setting<Boolean> antiCity = sgGeneral.add(new BoolSetting.Builder().name("anti-city").description("Checks if obsidian in hotbar. If not, toggles.").defaultValue(false).build());
    private final Setting<Boolean> square = sgGeneral.add(new BoolSetting.Builder().name("square").description("Checks if obsidian in hotbar. If not, toggles.").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotating towards blocks being placed.").defaultValue(false).build());
    private final Setting<List<Block>> blocksList = sgGeneral.add(new BlockListSetting.Builder().name("block-list").description("List of ignored blocks.").defaultValue(Collections.singletonList(Blocks.STONE_BUTTON)).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").description("").defaultValue(true).build());

    private final List<BlockPos> defaultList = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
    }};

    private final List<BlockPos> antiCityList = new ArrayList<>() {{
        add(new BlockPos(2, 0, 0));
        add(new BlockPos(-2, 0, 0));
        add(new BlockPos(0, 0, 2));
        add(new BlockPos(0, 0, -2));
    }};

    private final List<BlockPos> squareList = new ArrayList<>() {{
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(1, 0, -1));
        add(new BlockPos(-1, 0, -1));
    }};

    @EventHandler
    private void onTick(TickEvent.Pre event){
        List<PlayerEntity> players = EntityUtil.getTargetsInRange(enemyRange.get());
        if (players.isEmpty()) return;

        players.sort(Comparator.comparing(player -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), player.getBlockPos())));

        PlayerEntity player = players.get(0);
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocksList.get().contains(Block.getBlockFromItem(itemStack.getItem())));

        List<BlockPos> array =  getPoses(player.getBlockPos());
        array = array.stream().filter(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos) <= cityRange.get()).collect(Collectors.toList());

        if (array.isEmpty()) return;

        for (BlockPos pos : array){
            if (BlockUtil.isAir(pos)){
                BlockUtils.place(pos, block, rotate.get(), 50, swing.get());
            }
        }
    }

    private List<BlockPos> getPoses(BlockPos center){
        List<BlockPos> finalArray = new ArrayList<>();
        List<BlockPos> array = defaultList;
        if (antiCity.get()) array.addAll(antiCityList);
        if (square.get()) array.addAll(squareList);

        for (BlockPos pos : array){
            BlockPos p = center.add(pos.getX(), pos.getY(), pos.getZ());
            finalArray.add(p);
        }
        return finalArray;
    }

}
