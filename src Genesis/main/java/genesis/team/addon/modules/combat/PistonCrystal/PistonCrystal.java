package genesis.team.addon.modules.combat.PistonCrystal;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import genesis.team.addon.util.ProximaUtil.EntityUtil;
import genesis.team.addon.util.ProximaUtil.PlayerUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class PistonCrystal extends Module {
    public PistonCrystal(){
        super(Genesis.Px, "piston-crystal", "-");
    }

    public enum RedstoneMode {
        Button,
        RedstoneBlock,
        Torch
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelay = settings.createGroup("Delay");

    private final Setting<Double> enemyRange = sgGeneral.add(new DoubleSetting.Builder().name("enemy-range").description("The range at which players can be targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder().name("anti-suicide").description("").defaultValue(true).build());
    private final Setting<Double> minHealth = sgGeneral.add(new DoubleSetting.Builder().name("min-health").description("The minimum damage required for Piston Aura to work.").defaultValue(6).min(0).sliderMax(36).max(36).visible(antiSuicide::get).build());
    private final Setting<Boolean> stopIfBurrowed = sgGeneral.add(new BoolSetting.Builder().name("stop-if-burrowed").description("").defaultValue(true).build());
    private final Setting<Boolean> y1 = sgGeneral.add(new BoolSetting.Builder().name("Y+1").description("").defaultValue(true).build());
    private final Setting<Boolean> trap = sgGeneral.add(new BoolSetting.Builder().name("trap").description("").defaultValue(true).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Target only if the monke is in the hole.").defaultValue(true).build());
    private final Setting<RedstoneMode> redstoneMode = sgGeneral.add(new EnumSetting.Builder<RedstoneMode>().name("redstone-mode").description("Piston activation block.").defaultValue(RedstoneMode.Button).build());
    private final Setting<Hand> clickHandMode = sgGeneral.add(new EnumSetting.Builder<Hand>().name("click-hand").description("Hand to activate the button.").defaultValue(Hand.OFF_HAND).visible(()->redstoneMode.get().equals(RedstoneMode.Button)).build());
    private final Setting<Integer> pistonDelay = sgDelay.add(new IntSetting.Builder().name("piston-delay").description("The delay between place piston.").defaultValue(3).min(0).sliderMax(20).build());
    private final Setting<Integer> crystalDelay = sgDelay.add(new IntSetting.Builder().name("crystal-delay").description("The delay between place crystal.").defaultValue(3).min(0).sliderMax(20).build());
    private final Setting<Integer> redstoneDelay = sgDelay.add(new IntSetting.Builder().name("redstone-delay").description("The delay between place redstone.").defaultValue(3).min(0).sliderMax(20).build());
    private final Setting<Integer> buttonClickDelay = sgDelay.add(new IntSetting.Builder().name("button-click-delay").description("The delay between click button.").defaultValue(3).min(0).sliderMax(20).visible(() -> redstoneMode.get().equals(RedstoneMode.Button)).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Render surround.").defaultValue(false).build());
    private final Setting<ShapeMode> pistonShapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("piston-shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> pistonSideColor = sgRender.add(new ColorSetting.Builder().name("piston-side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> pistonLineColor = sgRender.add(new ColorSetting.Builder().name("piston-line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<ShapeMode> redstoneShapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("redstone-shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> redstoneSideColor = sgRender.add(new ColorSetting.Builder().name("redstone-side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> redstoneLineColor = sgRender.add(new ColorSetting.Builder().name("redstone-line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());

    public enum RenderMode {
        Fade,
        Default
    }

    private int pistonTimer;
    private int crystalTimer;
    private int redstoneTimer;
    private int clickTimer;
    private final Pool<RenderBlock> renderPistonBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderPistonBlocks = new ArrayList<>();
    private final Pool<RenderBlock> renderRedstoneBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderRedstoneBlocks = new ArrayList<>();

    @Override
    public void onActivate() {
        pistonTimer = pistonDelay.get();
        crystalTimer = crystalDelay.get();
        redstoneTimer = redstoneDelay.get();
        clickTimer = buttonClickDelay.get();
        for (RenderBlock renderBlock : renderPistonBlocks) renderPistonBlockPool.free(renderBlock);
        renderPistonBlocks.clear();
        for (RenderBlock renderBlock : renderRedstoneBlocks) renderRedstoneBlockPool.free(renderBlock);
        renderRedstoneBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderPistonBlocks) renderPistonBlockPool.free(renderBlock);
        renderPistonBlocks.clear();
        for (RenderBlock renderBlock : renderRedstoneBlocks) renderRedstoneBlockPool.free(renderBlock);
        renderRedstoneBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (mc.player.getHealth() <= minHealth.get()) return;
        for (PlayerEntity target : EntityUtil.getTargetsInRange(enemyRange.get())) {
            if (onlyInHole.get() && !EntityUtil.isInHole(true, target)) return;
            if (stopIfBurrowed.get()){
                if (EntityUtil.isBurrowed(target)) return;
            }
            renderPistonBlocks.forEach(RenderBlock::tick);
            renderPistonBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
            renderRedstoneBlocks.forEach(RenderBlock::tick);
            renderRedstoneBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
            for (Entity entity : mc.world.getEntities()){
                if (entity instanceof EndCrystalEntity && DamageUtils.crystalDamage(target, entity.getPos()) > 8){
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                }
            }
            Direction direction = null;

            boolean y1b = false;

            if (checkDir(findNearestDir(target, false), target, false, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))){
                direction = findNearestDir(target, false);
                y1b = false;
            }
            else {
                if (checkDir(findAnotherNearestDir(target, false), target, false, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))){
                    direction = findAnotherNearestDir(target, false);
                    y1b = false;
                }
                else {
                    if (checkDir(findOpposite(findAnotherNearestDir(target, false)), target, false, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))){
                        direction = findOpposite(findAnotherNearestDir(target, false));
                        y1b = false;
                    }else {
                        if (checkDir(findOpposite(findNearestDir(target, false)), target, false, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))){
                            direction = findOpposite(findNearestDir(target, false));
                            y1b = false;
                        }else {
                            if (y1.get()) {
                                if (checkDir(findNearestDir(target, true), target, true, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))){
                                    direction = findNearestDir(target, true);
                                    y1b = true;
                                }
                                else {
                                    if (checkDir(findAnotherNearestDir(target, true), target, true, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))){
                                        direction = findAnotherNearestDir(target, true);
                                        y1b = true;
                                    }
                                    else {
                                        if (checkDir(findOpposite(direction), target, true, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))){
                                            direction = findOpposite(direction);
                                            y1b = true;
                                        }
                                        else {
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (direction != null){
                double yaw = switch (direction) {
                    case EAST -> -90;
                    case SOUTH -> 0;
                    case WEST -> 90;
                    case NORTH -> 180;
                    default -> throw new IllegalStateException("Unexpected value: " + direction);
                };

                Direction finalDirection = direction;
                boolean finalY1b = y1b;
                Rotations.rotate(yaw, 0, () -> place(finalDirection, target, finalY1b));
            }
        }
    }

    private void place(Direction dir, PlayerEntity target, boolean y1){
        BlockPos headPos = target.getBlockPos().up();
        switch (dir){
            case EAST -> placePatter(y1 ? headPos.up() : headPos, y1 ? headPos.east(2).up() : headPos.east(2), y1 ? headPos.east().up() : headPos.east(), y1 ? headPos.east(3).up() : headPos.east(3), y1 ? headPos.east(2).up().up() : headPos.east(2).up(), y1 ? headPos.east(2).down().up() : headPos.east(2).down(), y1 ? headPos.east(2).south().up() : headPos.east(2).south(), y1 ?  headPos.east(2).north().up() : headPos.east(2).north());
            case WEST -> placePatter(y1 ? headPos.up() : headPos, y1 ? headPos.west(2).up() : headPos.west(2), y1 ? headPos.west().up() : headPos.west(), y1 ? headPos.west(3).up() : headPos.west(3), y1 ? headPos.west(2).up().up() : headPos.west(2).up(), y1 ? headPos.west(2).down().up() : headPos.west(2).down(), y1 ? headPos.west(2).north().up() : headPos.west(2).north(), y1 ?  headPos.west(2).south().up() : headPos.west(2).south());
            case SOUTH -> placePatter(y1 ? headPos.up() : headPos, y1 ? headPos.south(2).up() : headPos.south(2), y1 ? headPos.south().up() : headPos.south(), y1 ? headPos.south(3).up() : headPos.south(3), y1 ? headPos.south(2).up().up() : headPos.south(2).up(), y1 ? headPos.south(2).down().up() : headPos.south(2).down(), y1 ? headPos.south(2).east().up() : headPos.south(2).east(), y1 ?  headPos.south(2).west().up() : headPos.south(2).west());
            case NORTH -> placePatter(y1 ? headPos.up() : headPos, y1 ? headPos.north(2).up() : headPos.north(2), y1 ? headPos.north().up() : headPos.north(), y1 ? headPos.north(3).up() : headPos.north(3), y1 ? headPos.north(2).up().up() : headPos.north(2).up(), y1 ? headPos.north(2).down().up() : headPos.north(2).down(), y1 ? headPos.north(2).west().up() : headPos.north(2).west(), y1 ?  headPos.north(2).east().up() : headPos.north(2).east());
        }
    }

    private void placePatter(BlockPos headPos, BlockPos pistonPos, BlockPos crystalPos, BlockPos thirdPos, BlockPos upPos, BlockPos downPos, BlockPos nearPiston1, BlockPos nearPiston2){
        if (trap.get() && BlockUtil.isAir(headPos.up())){
            FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem);
            InvUtils.swap(block.slot(), false);
            PlayerUtil.placeBlock(headPos.up(), Hand.MAIN_HAND, false, true);
        }
        if (BlockUtil.isAir(pistonPos)){
            if (pistonTimer <= 0){
                FindItemResult piston = InvUtils.findInHotbar(Items.PISTON);
                InvUtils.swap(piston.slot(), false);
                PlayerUtil.placeBlock(pistonPos, Hand.MAIN_HAND, false, false);
                renderPistonBlocks.add(renderPistonBlockPool.get().set(pistonPos));
                pistonTimer = pistonDelay.get();
            }else pistonTimer--;
        }

        for (Entity crystal : mc.world.getEntities()) {
            if (!crystal.getBlockPos().equals(crystalPos)) {
                if (BlockUtil.getBlock(pistonPos) == Blocks.PISTON && mc.world.getBlockState(crystalPos).isAir()){
                    if (crystalTimer <= 0){
                        FindItemResult crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL);
                        if (mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL){
                            mc.player.getInventory().selectedSlot = crystalSlot.slot();
                        }
                        mc.interactionManager.interactBlock(mc.player, mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, crystalPos.down(), true));
                        crystalTimer = crystalDelay.get();
                    }else crystalTimer--;
                }
            }
        }
        for (Entity entity : mc.world.getEntities()){
            if (entity instanceof EndCrystalEntity && entity.getBlockPos().equals(crystalPos)){

                if (redstoneTimer <= 0){
                    if (redstoneMode.get() == RedstoneMode.RedstoneBlock){
                        FindItemResult redstoneBlock = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
                        PlayerUtil.placeBlock(thirdPos, redstoneBlock.slot(), Hand.MAIN_HAND, false);
                        renderRedstoneBlocks.add(renderRedstoneBlockPool.get().set(thirdPos));
                        redstoneTimer = redstoneDelay.get();
                    }else if (redstoneMode.get() == RedstoneMode.Button){
                        FindItemResult button = InvUtils.findInHotbar(itemStack -> ItemUtil.buttons.contains(itemStack.getItem()));
                        BlockPos placePos = null;
                        if (BlockUtil.isAir(pistonPos.up())){
                            placePos = pistonPos.up();
                        }else if (BlockUtil.isAir(pistonPos.down()) && BlockUtil.getBlock(upPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(upPos)){
                            placePos = pistonPos.down();
                        }else if (BlockUtil.isAir(thirdPos) && BlockUtil.getBlock(upPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(upPos) && BlockUtil.getBlock(downPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(downPos)){
                            placePos = thirdPos;
                        }else if (BlockUtil.isAir(nearPiston1) && BlockUtil.getBlock(upPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(upPos) && BlockUtil.getBlock(downPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(downPos) && BlockUtil.getBlock(thirdPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(thirdPos)){
                            placePos = nearPiston1 ;
                        }else if (BlockUtil.isAir(nearPiston2) && BlockUtil.getBlock(upPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(upPos) && BlockUtil.getBlock(downPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(downPos) && BlockUtil.getBlock(thirdPos) != Blocks.STONE_BUTTON && !BlockUtil.isAir(thirdPos) && BlockUtil.getBlock(nearPiston1) != Blocks.STONE_BUTTON && !BlockUtil.isAir(nearPiston1)){
                            placePos = nearPiston2;
                        }
                        if (placePos != null){
                            PlayerUtil.placeBlock(placePos, button.slot(), Hand.MAIN_HAND, false);
                            renderRedstoneBlocks.add(renderRedstoneBlockPool.get().set(placePos));
                        }
                    }else if (redstoneMode.get() == RedstoneMode.Torch){
                        FindItemResult torch = InvUtils.findInHotbar(Items.REDSTONE_TORCH);
                        BlockPos placePos = null;
                        if (BlockUtil.isAir(thirdPos)){
                            if(BlockUtil.isAir(thirdPos.down())){
                                FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem);
                                PlayerUtil.placeBlock(thirdPos.down(), block.slot(), Hand.MAIN_HAND, false);
                            }
                            placePos = thirdPos;
                        }else if (BlockUtil.isAir(nearPiston1) && !BlockUtil.isAir(thirdPos) && BlockUtil.getBlock(thirdPos) != Blocks.REDSTONE_TORCH){
                            if(BlockUtil.isAir(nearPiston1.down())){
                                FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem);
                                PlayerUtil.placeBlock(nearPiston1.down(), block.slot(), Hand.MAIN_HAND, false);
                            }
                            placePos = nearPiston1;
                        }else if (BlockUtil.isAir(nearPiston2) && !BlockUtil.isAir(thirdPos) && BlockUtil.getBlock(thirdPos) != Blocks.REDSTONE_TORCH && !BlockUtil.isAir(nearPiston1) && BlockUtil.getBlock(nearPiston1) != Blocks.REDSTONE_TORCH){
                            if(BlockUtil.isAir(nearPiston2.down())){
                                FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem);
                                PlayerUtil.placeBlock(nearPiston2.down(), block.slot(), Hand.MAIN_HAND, false);
                            }
                            placePos = nearPiston2;
                        }
                        if (placePos != null){
                            PlayerUtil.placeBlock(placePos, torch.slot(), Hand.MAIN_HAND, true);
                            renderRedstoneBlocks.add(renderRedstoneBlockPool.get().set(placePos));
                        }
                    }
                    redstoneTimer = redstoneDelay.get();
                }else redstoneTimer--;
                if (redstoneMode.get() == RedstoneMode.Button){
                    if (clickTimer <= 0){
                        if (BlockUtil.buttons.contains(BlockUtil.getBlock(upPos))){
                            mc.interactionManager.interactBlock(mc.player, clickHandMode.get(), new BlockHitResult(mc.player.getPos(), Direction.DOWN, upPos, true));
                        }
                        if (BlockUtil.buttons.contains(BlockUtil.getBlock(downPos))){
                            mc.interactionManager.interactBlock(mc.player, clickHandMode.get(), new BlockHitResult(mc.player.getPos(), Direction.DOWN, downPos, true));
                        }
                        if (BlockUtil.buttons.contains(BlockUtil.getBlock(thirdPos))){
                            mc.interactionManager.interactBlock(mc.player, clickHandMode.get(), new BlockHitResult(mc.player.getPos(), Direction.DOWN, thirdPos, true));
                        }
                        if (BlockUtil.buttons.contains(BlockUtil.getBlock(nearPiston1))){
                            mc.interactionManager.interactBlock(mc.player, clickHandMode.get(), new BlockHitResult(mc.player.getPos(), Direction.DOWN, nearPiston1, true));
                        }
                        if (BlockUtil.buttons.contains(BlockUtil.getBlock(nearPiston2))){
                            mc.interactionManager.interactBlock(mc.player, clickHandMode.get(), new BlockHitResult(mc.player.getPos(), Direction.DOWN, nearPiston2, true));
                        }
                        clickTimer = buttonClickDelay.get();
                    }else clickTimer--;
                }
            }
        }
    }

    private Direction findAnotherNearestDir(LivingEntity target, boolean y1){
        BlockPos headPos = target.getBlockPos().up();
        if (y1) headPos = target.getBlockPos().up();

        double distanceToWest = mc.player.squaredDistanceTo(headPos.west(2).getX(), headPos.west(2).getY(), headPos.west(2).getZ());
        double distanceToEast = mc.player.squaredDistanceTo(headPos.east(2).getX(), headPos.east(2).getY(), headPos.east(2).getZ());
        double distanceToSouth = mc.player.squaredDistanceTo(headPos.south(2).getX(), headPos.south(2).getY(), headPos.south(2).getZ());
        double distanceToNorth = mc.player.squaredDistanceTo(headPos.north(2).getX(), headPos.north(2).getY(), headPos.north(2).getZ());
        double nearest = Collections.min(new ArrayList<>(){{
            add(distanceToEast);
            add(distanceToEast);
            add(distanceToSouth);
            add(distanceToNorth);
        }});
        if(nearest == distanceToWest){
            if (distanceToNorth >= distanceToSouth) return Direction.SOUTH;
            return Direction.NORTH;
        }
        else if (nearest == distanceToEast){
            if (distanceToNorth >= distanceToSouth) return Direction.SOUTH;
            return Direction.NORTH;
        }
        else if (nearest == distanceToNorth){
            if (distanceToWest >= distanceToEast) return Direction.EAST;
            return Direction.WEST;
        }
        else if (nearest == distanceToSouth){
            if (distanceToWest >= distanceToEast) return Direction.EAST;
            return Direction.WEST;
        }
        return null;
    }

    private Direction findOpposite(Direction direction){
        if (direction == Direction.EAST){
            return Direction.WEST;
        }
        if (direction == Direction.WEST){
            return Direction.EAST;
        }
        if (direction == Direction.NORTH){
            return Direction.SOUTH;
        }
        if (direction == Direction.SOUTH){
            return Direction.NORTH;
        }
        return Direction.WEST;
    }

    private Direction findNearestDir(PlayerEntity target, boolean y1){
        BlockPos headPos = target.getBlockPos().up();
        if (y1) headPos = headPos.up();
        double distanceToWest = mc.player.squaredDistanceTo(headPos.west(2).getX(), headPos.west(2).getY(), headPos.west(2).getZ());
        double distanceToEast = mc.player.squaredDistanceTo(headPos.east(2).getX(), headPos.east(2).getY(), headPos.east(2).getZ());
        double distanceToSouth = mc.player.squaredDistanceTo(headPos.south(2).getX(), headPos.south(2).getY(), headPos.south(2).getZ());
        double distanceToNorth = mc.player.squaredDistanceTo(headPos.north(2).getX(), headPos.north(2).getY(), headPos.north(2).getZ());
        double nearest = Collections.min(new ArrayList<>(){{
            add(distanceToEast);
            add(distanceToEast);
            add(distanceToSouth);
            add(distanceToNorth);
        }});
        if(nearest == distanceToWest){
            if (checkDir(Direction.WEST, target, y1, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))) return Direction.WEST;
        }
        else if (nearest == distanceToEast){
            if (checkDir(Direction.EAST, target, y1, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))) return Direction.EAST;
        }
        else if (nearest == distanceToNorth){
            if (checkDir(Direction.NORTH, target, y1, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))) return Direction.NORTH;
        }
        else if (nearest == distanceToSouth) {
            if (checkDir(Direction.SOUTH, target, y1, redstoneMode.get().equals(RedstoneMode.Button), redstoneMode.get().equals(RedstoneMode.RedstoneBlock), redstoneMode.get().equals(RedstoneMode.Torch))) return Direction.SOUTH;
        }
        return null;
    }

    private boolean checkDir(Direction dir, PlayerEntity target, boolean y1, boolean button, boolean redstoneBlock, boolean torch){
        if (dir == null) return false;
        BlockPos headPos = target.getBlockPos().up();
        if (y1) headPos = headPos.up();
        switch (dir){
            case WEST -> {
                return checkPattern(redstoneBlock, torch, button, headPos.west(), headPos.west(2), headPos.west(3), headPos.west(2).up(), headPos.west(2).down(), headPos.west(2).south(), headPos.west(2).north());
            }
            case NORTH -> {
                return checkPattern(redstoneBlock, torch, button, headPos.north(), headPos.north(2), headPos.north(3), headPos.north(2).up(), headPos.north(2).down(), headPos.north(2).east(), headPos.north(2).west());
            }
            case EAST -> {
                return checkPattern(redstoneBlock, torch, button, headPos.east(), headPos.east(2), headPos.east(3), headPos.east(2).up(), headPos.east(2).down(), headPos.east(2).south(), headPos.east(2).north());
            }
            case SOUTH -> {
                return checkPattern(redstoneBlock, torch, button, headPos.south(), headPos.south(2), headPos.south(3), headPos.south(2).up(), headPos.south(2).down(), headPos.south(2).east(), headPos.south(2).west());
            }
        }
        return false;
    }

    private boolean checkPattern(boolean redstoneBlock, boolean torch, boolean button , BlockPos crystalPos, BlockPos pistonPos, BlockPos thirdPos, BlockPos upPos, BlockPos downPos, BlockPos nearPiston1, BlockPos nearPiston2){
        if ((BlockUtil.isAir(crystalPos.down()) || BlockUtil.getBlock(crystalPos.down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(crystalPos.down()) == Blocks.BEDROCK) && (BlockUtil.isAir(crystalPos) || BlockUtil.getBlock(crystalPos) == Blocks.PISTON_HEAD || BlockUtil.getBlock(crystalPos) == Blocks.MOVING_PISTON) && (BlockUtil.isAir(pistonPos) || BlockUtil.getBlock(pistonPos) == Blocks.PISTON || BlockUtil.getBlock(pistonPos) == Blocks.MOVING_PISTON)){
            if (redstoneBlock){
                return BlockUtil.isAir(thirdPos) || BlockUtil.getBlock(thirdPos) == Blocks.REDSTONE_BLOCK;
            }else if (torch){
                return BlockUtil.isAir(nearPiston1) || BlockUtil.getBlock(nearPiston1) == Blocks.REDSTONE_TORCH || BlockUtil.isAir(thirdPos) || BlockUtil.getBlock(thirdPos) == Blocks.REDSTONE_TORCH || BlockUtil.isAir(nearPiston2) || BlockUtil.getBlock(nearPiston2) == Blocks.REDSTONE_TORCH;
            }else if (button){
                return BlockUtil.isAir(upPos) || BlockUtil.getBlock(upPos) == Blocks.STONE_BUTTON || BlockUtil.isAir(downPos) || BlockUtil.getBlock(downPos) == Blocks.STONE_BUTTON || BlockUtil.isAir(thirdPos) || BlockUtil.getBlock(thirdPos) == Blocks.STONE_BUTTON || BlockUtil.isAir(nearPiston1) || BlockUtil.getBlock(nearPiston1) == Blocks.STONE_BUTTON || BlockUtil.isAir(nearPiston2) || BlockUtil.getBlock(nearPiston2) == Blocks.STONE_BUTTON;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            renderPistonBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderPistonBlocks.forEach(renderBlock -> renderBlock.render(event, pistonSideColor.get(), pistonLineColor.get(), pistonShapeMode.get()));
            renderRedstoneBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderRedstoneBlocks.forEach(renderBlock -> renderBlock.render(event, redstoneSideColor.get(), redstoneLineColor.get(), redstoneShapeMode.get()));
        }
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;
        private final MinecraftClient mc = MeteorClient.mc;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = 10;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color side1, Color line1, ShapeMode shapeMode) {
            int preSideA = side1.a;
            int preLineA = line1.a;


            side1.a *= (double) ticks / 10;
            line1.a *= (double) ticks / 10;

            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getOutlineShape(mc.world, pos);

            if (shapeMode == ShapeMode.Both || shapeMode == ShapeMode.Lines) {
                shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    event.renderer.line(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ, pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ, line1);
                });
            }
            if (shapeMode == ShapeMode.Both || shapeMode == ShapeMode.Sides) {
                for (Box b : shape.getBoundingBoxes()) {
                    event.renderer.box(pos.getX() + b.minX, pos.getY() + b.minY, pos.getZ() + b.minZ, pos.getX() + b.maxX, pos.getY() + b.maxY, pos.getZ() + b.maxZ, side1, line1, shapeMode, 0);
                }
            }

            side1.a = preSideA;
            line1.a = preLineA;
        }

    }

}
