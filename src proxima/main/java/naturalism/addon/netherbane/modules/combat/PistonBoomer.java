package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.utils.ItemUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PistonBoomer extends Module {
    public PistonBoomer(){
        super(NetherBane.COMBATPLUS, "piston-boomer", "");
    }

    public enum TargetMode{
        Nearest,
        MinHealth
    }

    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final Setting<Double> enemyRange = sgTarget.add(new DoubleSetting.Builder().name("enemy-range").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<TargetMode> targetMode = sgTarget.add(new EnumSetting.Builder<TargetMode>().name("target-mode").description("Which way to swap.").defaultValue(TargetMode.Nearest).build());
    private final Setting<Boolean> ignoreBedrockBurrow = sgTarget.add(new BoolSetting.Builder().name("ignore-bedrock-burrow").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Boolean> y1Place = sgPlace.add(new BoolSetting.Builder().name("place-y-+1").description("Will not place and break beds if they will kill you.").defaultValue(true).build());
    private final Setting<Boolean> place112 = sgPlace.add(new BoolSetting.Builder().name("1.12-place").description("Will not place and break beds if they will kill you.").defaultValue(false).build());
    private final Setting<Boolean> support = sgPlace.add(new BoolSetting.Builder().name("1.12-place").description("Will not place and break beds if they will kill you.").defaultValue(false).build());

    private final SettingGroup sgSwitch = settings.createGroup("Switch");
    private final Setting<Boolean> silentSwitch = sgSwitch.add(new BoolSetting.Builder().name("silent-switch").description("Will not place and break beds if they will kill you.").defaultValue(true).build());


    @EventHandler
    private void onTick(TickEvent.Pre event){
        //Check items
        FindItemResult piston = InvUtils.findInHotbar(Items.PISTON);
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        FindItemResult button = InvUtils.findInHotbar(itemStack -> ItemUtil.buttons.contains(itemStack.getItem()));
        FindItemResult redstoneBlock = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
        FindItemResult redstoneTorch = InvUtils.findInHotbar(Items.REDSTONE_TORCH);
        FindItemResult obby = InvUtils.findInHotbar(Items.OBSIDIAN);

        if (!piston.found()) return;
        if (!crystal.found()) return;
        if (!button.found() && !redstoneBlock.found() && !redstoneTorch.found()) return;

        int[] indexArray = {redstoneBlock.slot(), redstoneTorch.slot(), button.slot()};
        indexArray = Arrays.stream(indexArray).filter(i -> i >= 0).toArray();
        Arrays.stream(indexArray).min().getAsInt();

        Item finalRedstoneSource = mc.player.getInventory().getStack(indexArray[0]).getItem();

        //Find target
        List<PlayerEntity> targets = EntityUtil.getTargetsInRange(enemyRange.get());

        switch (targetMode.get()){
            case Nearest -> targets.sort(Comparator.comparing(player -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), player.getBlockPos())));
            case MinHealth -> targets.sort(Comparator.comparing(LivingEntity::getHealth));
        }

        if (ignoreBedrockBurrow.get()) targets = targets.stream().filter(player -> BlockUtil.getBlock(player.getBlockPos()) != Blocks.BEDROCK).collect(Collectors.toList());
        if (targets.isEmpty()) return;

        PlayerEntity target = targets.get(0);
        BlockPos pos = target.getBlockPos();
        //Find Good Direction
        int i = 1;

        Direction bestDirection = findBestDirection(pos.up());
        if (bestDirection == null && y1Place.get()){
            bestDirection = findBestDirection(pos.up(2));
            if (bestDirection != null) i = 2;
        }
        if (bestDirection == null) return;

        //Rotate

        Rotations.rotate(bestDirection.asRotation(), 0);

        //Break
        for (Entity entity : mc.world.getEntities()){
            if (entity instanceof EndCrystalEntity && DamageUtils.crystalDamage(target, entity.getPos()) > 8){
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            }
        }

        //Place

        //Place Support
        if (BlockUtil.isAir(pos.offset(bestDirection))){
            placeBlock(pos.up(i - 1).offset(bestDirection), obby);
        }
        if (support.get() && BlockUtil.isAir(pos.offset(bestDirection, 2))){
            placeBlock(pos.up(i - 1).offset(bestDirection, 2), obby);
        }

        //Place piston
        Box pistonBox = new Box(pos.up(i).offset(bestDirection, 2));
        if (BlockUtil.isAir(pos.up(i).offset(bestDirection, 2)) && !EntityUtils.intersectsWithEntity(pistonBox, entity -> entity instanceof EndCrystalEntity || entity instanceof PlayerEntity)){
            placeBlock(pos.up(i).offset(bestDirection, 2), piston);
        }

        //Place crystal
        Box crystalBox = new Box(pos.up(i).offset(bestDirection));
        if (BlockUtil.isAir(pos.up(i).offset(bestDirection)) && BlockUtil.getBlock(pos.up(i).offset(bestDirection, 2)) == Blocks.PISTON && !EntityUtils.intersectsWithEntity(crystalBox, entity -> entity instanceof EndCrystalEntity || entity instanceof PlayerEntity)){
            placeCrystal(pos.up(i - 1).offset(bestDirection));
        }

        //Place Redstone
        BlockPos redPos = getRedstonePos(finalRedstoneSource, pos.up(i - 1), bestDirection);
        if (redPos == null) return;
        if (BlockUtil.isAir(redPos)){
            placeBlock(redPos, InvUtils.findInHotbar(finalRedstoneSource));
        }

        //Button active
    }

    private BlockPos getRedstonePos(Item item, BlockPos pos, Direction direction){
        if (item == Items.REDSTONE_BLOCK){
            List<BlockPos> array = new ArrayList<>(){{
                if (BlockUtil.isAir(pos.up().offset(direction, 3)) || BlockUtil.getBlock(pos.up().offset(direction, 3)) == Blocks.REDSTONE_BLOCK) add(pos.up().offset(direction, 3));
            }};
            if (array.isEmpty()) return null;
            return array.get(0);
        }
        if (item == Items.REDSTONE_TORCH){
            List<BlockPos> array = new ArrayList<>(){{
                if ((BlockUtil.isAir(pos.up().offset(direction, 3)) || BlockUtil.getBlock(pos.up().offset(direction, 3)) == Blocks.REDSTONE_TORCH) && BlockUtil.getBlock(pos.offset(direction, 3)) != null) add(pos.up().offset(direction, 3));
                if ((BlockUtil.isAir(pos.up().offset(getClockWise(direction), 2)) || BlockUtil.getBlock(pos.up().offset(getClockWise(direction), 2)) == Blocks.REDSTONE_TORCH) && BlockUtil.getBlock(pos.offset(getClockWise(direction), 2)) != null) add(pos.up().offset(getClockWise(direction), 2));
                if ((BlockUtil.isAir(pos.up().offset(getCounterClockWise(direction), 2)) || BlockUtil.getBlock(pos.up().offset(getCounterClockWise(direction), 2)) == Blocks.REDSTONE_TORCH) && BlockUtil.getBlock(pos.offset(getCounterClockWise(direction), 2)) != null) add(pos.up().offset(getCounterClockWise(direction), 2));
            }};
            array.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
            if (array.isEmpty()) return null;
            return array.get(0);
        }
        if (ItemUtil.buttons.contains(item)){
            List<BlockPos> array = new ArrayList<>(){{
                if (BlockUtil.isAir(pos.up().offset(direction, 3)) || BlockUtil.buttons.contains(BlockUtil.getBlock(pos.up().offset(direction, 3)))) add(pos.up().offset(direction, 3));
                if (BlockUtil.isAir(pos.up().offset(getClockWise(direction), 2)) || BlockUtil.buttons.contains(BlockUtil.getBlock(pos.up().offset(getClockWise(direction), 2)))) add(pos.up().offset(getClockWise(direction), 2));
                if (BlockUtil.isAir(pos.up().offset(getCounterClockWise(direction), 2)) || BlockUtil.buttons.contains(BlockUtil.getBlock(pos.up().offset(getCounterClockWise(direction), 2)))) add(pos.up().offset(getCounterClockWise(direction), 2));
                if (BlockUtil.isAir(pos.up().offset(direction, 2).up()) || BlockUtil.buttons.contains(BlockUtil.getBlock(pos.up().offset(direction, 2).up()))) add(pos.up().offset(direction, 2).up());
                if (BlockUtil.isAir(pos.up().offset(direction, 2).down()) || BlockUtil.buttons.contains(BlockUtil.getBlock(pos.up().offset(direction, 2).down()))) add(pos.up().offset(direction, 2).down());
            }};
            array.sort(Comparator.comparing(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos)));
            if (array.isEmpty()) return null;
            return array.get(0);
        }
        return null;
    }

    private void placeCrystal(BlockPos pos){
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL){
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
        }else {
            int prev = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = InvUtils.findInHotbar(Items.END_CRYSTAL).slot();
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
            if (silentSwitch.get()) mc.player.getInventory().selectedSlot = prev;
        }
    }

    private void placeBlock(BlockPos pos, FindItemResult block){
        int prev = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = block.slot();
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
        if (silentSwitch.get()) mc.player.getInventory().selectedSlot = prev;
    }

    private Direction findBestDirection(BlockPos up){
        List<Direction> validDirections = new ArrayList<>();

        for (CardinalDirection cardinalDirection : CardinalDirection.values()){
            if (isValidDirection(cardinalDirection.toDirection(), up)){
                validDirections.add(cardinalDirection.toDirection());
            }
        }

        validDirections.sort(Comparator.comparing(direction -> BlockUtil.distance(mc.player.getBlockPos(), up.offset(direction))));

        if (validDirections.isEmpty()) return null;
        return validDirections.get(0);
    }

    private boolean isValidDirection(Direction direction, BlockPos targetHead){
        if (BlockUtil.getBlock(targetHead.offset(direction, 2)) != Blocks.AIR && BlockUtil.getBlock(targetHead.offset(direction, 2)) != Blocks.PISTON && BlockUtil.getBlock(targetHead.offset(direction, 2)) != Blocks.MOVING_PISTON && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), targetHead.offset(direction, 2)) > placeRange.get()) return false;
        if (BlockUtil.getBlock(targetHead.offset(direction)) != Blocks.AIR && BlockUtil.getBlock(targetHead.offset(direction)) != Blocks.PISTON_HEAD && BlockUtil.getBlock(targetHead.offset(direction)) != Blocks.MOVING_PISTON && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), targetHead.offset(direction)) > placeRange.get()) return false;
        if (place112.get() && BlockUtil.isAir(targetHead.offset(direction, 2).down()) && !support.get()) return false;
        if ((BlockUtil.getBlock(targetHead.offset(direction, 3)) != Blocks.AIR && !BlockUtil.buttons.contains(BlockUtil.getBlock(targetHead.offset(direction, 3))) && BlockUtil.getBlock(targetHead.offset(direction, 3)) != Blocks.REDSTONE_BLOCK && BlockUtil.getBlock(targetHead.offset(direction, 3)) != Blocks.REDSTONE_TORCH) &&
            (BlockUtil.getBlock(targetHead.offset(direction, 2).up()) != Blocks.AIR && !BlockUtil.buttons.contains(BlockUtil.getBlock(targetHead.offset(direction, 2).up())) && BlockUtil.getBlock(targetHead.offset(direction, 2).up()) != Blocks.REDSTONE_BLOCK && BlockUtil.getBlock(targetHead.offset(direction, 2).up()) != Blocks.REDSTONE_TORCH) &&
            (BlockUtil.getBlock(targetHead.offset(direction, 2).down()) != Blocks.AIR && !BlockUtil.buttons.contains(BlockUtil.getBlock(targetHead.offset(direction, 2).down())) && BlockUtil.getBlock(targetHead.offset(direction, 2).down()) != Blocks.REDSTONE_BLOCK && BlockUtil.getBlock(targetHead.offset(direction, 2).down()) != Blocks.REDSTONE_TORCH) &&
            (BlockUtil.getBlock(targetHead.offset(direction, 2).offset(getClockWise(direction))) != Blocks.AIR && !BlockUtil.buttons.contains(BlockUtil.getBlock(targetHead.offset(direction, 2).offset(getClockWise(direction)))) && BlockUtil.getBlock(targetHead.offset(direction, 2).offset(getClockWise(direction))) != Blocks.REDSTONE_BLOCK && BlockUtil.getBlock(targetHead.offset(direction, 2).offset(getClockWise(direction))) != Blocks.REDSTONE_TORCH) &&
            (BlockUtil.getBlock(targetHead.offset(direction, 2).offset(getCounterClockWise(direction))) != Blocks.AIR && !BlockUtil.buttons.contains(BlockUtil.getBlock(targetHead.offset(direction, 2).offset(getClockWise(direction)))) && BlockUtil.getBlock(targetHead.offset(direction, 2).offset(getClockWise(direction))) != Blocks.REDSTONE_BLOCK && BlockUtil.getBlock(targetHead.offset(direction, 2).offset(getClockWise(direction))) != Blocks.REDSTONE_TORCH) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), targetHead.offset(direction, 2)) > placeRange.get()) return false;
        return true;
    }

    private Direction getClockWise(Direction direction){
        switch (direction){
            case WEST -> { return  Direction.SOUTH; }
            case EAST -> { return  Direction.NORTH; }
            case NORTH -> { return Direction.WEST; }
            case SOUTH -> { return Direction.EAST; }
        }
        return null;
    }

    private Direction getCounterClockWise(Direction direction){
        switch (direction){
            case WEST -> { return  Direction.NORTH; }
            case EAST -> { return  Direction.SOUTH; }
            case NORTH -> { return Direction.EAST; }
            case SOUTH -> { return Direction.WEST; }
        }
        return null;
    }
}
