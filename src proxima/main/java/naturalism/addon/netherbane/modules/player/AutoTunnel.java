package naturalism.addon.netherbane.modules.player;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.SafeWalk;
import meteordevelopment.meteorclient.systems.modules.movement.Scaffold;
import meteordevelopment.meteorclient.systems.modules.player.AutoTool;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SandBlock;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import naturalism.addon.netherbane.NetherBane;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.PlayerUtil;

import java.util.ArrayList;
import java.util.List;

import static naturalism.addon.netherbane.utils.RenderUtil.*;


public class AutoTunnel extends Module {
    public AutoTunnel(){
        super(NetherBane.PLAYERPLUS, "auto-tunnel", "-");
    }

    public enum FilterMode {
        NONE, WHITELIST, BLACKLIST
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgModules = settings.createGroup("Modules");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> buildUp = sgGeneral.add(new BoolSetting.Builder().name("build-up").description("Faces the blocks being mined server side.").defaultValue(false).build());
    private final Setting<Boolean> breakScaffold = sgGeneral.add(new BoolSetting.Builder().name("break-scaffold").description("Faces the blocks being mined server side.").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Faces the blocks being mined server side.").defaultValue(false).build());
    private final Setting<List<Block>> customBlocks = sgGeneral.add(new BlockListSetting.Builder().name("customBlocks").description("Selected blocks.").build());
    public final Setting<FilterMode> filterMode = sgGeneral.add(new EnumSetting.Builder<FilterMode>().name("filterMode").description(".").defaultValue(FilterMode.NONE).build());

    private final Setting<Boolean> autoTool = sgModules.add(new BoolSetting.Builder().name("auto-tool").description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.").defaultValue(true).build());
    private final Setting<Boolean> scaffold = sgModules.add(new BoolSetting.Builder().name("scaffold").description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.").defaultValue(true).build());
    private final Setting<Boolean> safeWalk = sgModules.add(new BoolSetting.Builder().name("safe-walk").description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.").defaultValue(true).build());
    private final Setting<Boolean> liquidFiller = sgModules.add(new BoolSetting.Builder().name("liquid-filler").description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.").defaultValue(true).build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders where the surround will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());

    private BlockPos playerPos;
    private ArrayList<BlockPos> blockPoses = new ArrayList<>();

    @Override
    public void onDeactivate() {
        mc.options.forwardKey.setPressed(false);
        if (Modules.get().get(AutoTool.class).isActive()){
            Modules.get().get(AutoTool.class).toggle();
        }
        if (Modules.get().get(Scaffold.class).isActive()){
            Modules.get().get(Scaffold.class).toggle();
        }
        if (Modules.get().get(SafeWalk.class).isActive()){
            Modules.get().get(SafeWalk.class).toggle();
        }
        if (Modules.get().get(LiquidFiller.class).isActive()){
            Modules.get().get(LiquidFiller.class).toggle();
        }
    }

    private boolean isValid(Block block) {
        if (filterMode.get() == FilterMode.BLACKLIST) {
            return !customBlocks.get().contains(block);
        } else if (filterMode.get() == FilterMode.WHITELIST) {
            return customBlocks.get().contains(block);
        }
        return true;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        mc.player.setYaw(mc.player.getHorizontalFacing().asRotation());

        if (!Modules.get().get(AutoTool.class).isActive() && autoTool.get()){
            Modules.get().get(AutoTool.class).toggle();
        }
        if (!Modules.get().get(Scaffold.class).isActive() && scaffold.get()){
            Modules.get().get(Scaffold.class).toggle();
        }
        if (!Modules.get().get(SafeWalk.class).isActive() && safeWalk.get()){
            Modules.get().get(SafeWalk.class).toggle();
        }
        if (!Modules.get().get(LiquidFiller.class).isActive() && liquidFiller.get()){
            Modules.get().get(LiquidFiller.class).toggle();
        }

        BlockPos playerPos = mc.player.getBlockPos();
        if (!mc.world.getBlockState(playerPos.down()).isFullCube((BlockView) mc.world, playerPos.down())) {
            playerPos = playerPos.up();
        }
        blockPoses.clear();

        switch (mc.player.getHorizontalFacing()){
            case EAST:
                double z = MathHelper.floor(mc.player.getZ()) + 0.5;
                mc.player.setPosition(mc.player.getX(), mc.player.getY(), z);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                if (isTunnel(playerPos.west(), playerPos.west().up())){
                    if (doBuildUp(playerPos.west(), playerPos.west().up())){
                        tunnel(playerPos.east(), playerPos.east().up(), playerPos.east(2), playerPos.east(2).up(), playerPos.east(3), playerPos.east(3).up());
                    }
                }else {
                    if (doBreak(playerPos.west().down(), playerPos.west(2).down())){
                        tunnel(playerPos.east(), playerPos.east().up(), playerPos.east(2), playerPos.east(2).up(), playerPos.east(3), playerPos.east(3).up());
                    }
                }

                break;
            case WEST:
                z = MathHelper.floor(mc.player.getZ()) + 0.5;
                mc.player.setPosition(mc.player.getX(), mc.player.getY(), z);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                if (isTunnel(playerPos.east(), playerPos.east().up())){
                    if (doBuildUp(playerPos.east(), playerPos.east().up())){
                        tunnel(playerPos.west(), playerPos.west().up(), playerPos.west(2), playerPos.west(2).up(), playerPos.west(3), playerPos.west(3).up());
                    }
                }else {
                    if (doBreak(playerPos.east().down(), playerPos.east(2).down())){
                        tunnel(playerPos.west(), playerPos.west().up(), playerPos.west(2), playerPos.west(2).up(), playerPos.west(3), playerPos.west(3).up());
                    }
                }

                break;
            case SOUTH:
                double x = MathHelper.floor(mc.player.getX()) + 0.5;
                mc.player.setPosition(x, mc.player.getY(), mc.player.getZ());
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                if (isTunnel(playerPos.north(), playerPos.north().up())){
                    if (doBuildUp(playerPos.north(), playerPos.north().up())){
                        tunnel(playerPos.south(), playerPos.south().up(), playerPos.south(2), playerPos.south(2).up(), playerPos.south(3), playerPos.south(3).up());
                    }
                }else {
                    if (doBreak(playerPos.north().down(), playerPos.north(2).down())){
                        tunnel(playerPos.south(), playerPos.south().up(), playerPos.south(2), playerPos.south(2).up(), playerPos.south(3), playerPos.south(3).up());
                    }
                }

                break;
            case NORTH:
                x = MathHelper.floor(mc.player.getX()) + 0.5;
                mc.player.setPosition(x, mc.player.getY(), mc.player.getZ());
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                if (isTunnel(playerPos.south(), playerPos.south().up())){
                    if (doBuildUp(playerPos.south(), playerPos.south().up())){
                        tunnel(playerPos.north(), playerPos.north().up(), playerPos.north(2), playerPos.north(2).up(), playerPos.north(3), playerPos.north(3).up());
                    }
                }else {
                    if (doBreak(playerPos.south().down(), playerPos.south(2).down())){
                        tunnel(playerPos.north(), playerPos.north().up(), playerPos.north(2), playerPos.north(2).up(), playerPos.north(3), playerPos.north(3).up());
                    }
                }

                break;
        }
    }

    private boolean isTunnel(BlockPos b1, BlockPos b2){
        if (!BlockUtil.isAir(b1.down()) && !BlockUtil.isAir(b2.up()) && ((!BlockUtil.isAir(b1.east()) && !BlockUtil.isAir(b1.west()) && !BlockUtil.isAir(b2.east()) && !BlockUtil.isAir(b2.west())) || (!BlockUtil.isAir(b1.south()) && !BlockUtil.isAir(b1.north()) && !BlockUtil.isAir(b2.south()) && !BlockUtil.isAir(b2.north())))){
            return true;
        }
        return false;
    }

    private boolean doBreak(BlockPos b1, BlockPos b2){
        if (breakScaffold.get()){
            FindItemResult pickAxe = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);
            if (!BlockUtil.isAir(b1)){
                InvUtils.swap(pickAxe.slot(), false);
                BlockUtils.breakBlock(b1, true);
            }
            if (!BlockUtil.isAir(b2)){
                InvUtils.swap(pickAxe.slot(), false);
                BlockUtils.breakBlock(b2, true);
            }
            if (BlockUtil.isAir(b1) && BlockUtil.isAir(b2)) return true;
        }else return true;
        return false;
    }

    private boolean doBuildUp(BlockPos b1, BlockPos b2){
        if (buildUp.get()){
            if (BlockUtil.isAir(b1)){
                FindItemResult itemResult = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && isValid(Block.getBlockFromItem(itemStack.getItem())));
                InvUtils.swap(itemResult.slot(), false);
                PlayerUtil.placeBlock(b1, Hand.MAIN_HAND, true, true);
            }
            if (BlockUtil.isAir(b2)){
                FindItemResult itemResult = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && isValid(Block.getBlockFromItem(itemStack.getItem())));
                InvUtils.swap(itemResult.slot(), false);
                PlayerUtil.placeBlock(b2, Hand.MAIN_HAND, true, true);
            }
            if (!BlockUtil.isAir(b1) && !BlockUtil.isAir(b2)){
                return true;
            }
        }else return true;
        return false;
    }

    private void tunnel(BlockPos b1, BlockPos b2, BlockPos b3, BlockPos b4, BlockPos b5, BlockPos b6){

        if (BlockUtil.isAir(b1) && (BlockUtil.isAir(b2) && (BlockUtil.isAir(b3) && (BlockUtil.isAir(b4)) && (BlockUtil.isAir(b5) && (BlockUtil.isAir(b6)))))){
            mc.options.forwardKey.setPressed(true);
        }
        else {
            mc.options.forwardKey.setPressed(false);
            blockPoses.add(b1);
            blockPoses.add(b2);
            blockPoses.add(b3);
            blockPoses.add(b4);
            blockPoses.add(b5);
            blockPoses.add(b6);
            if (!BlockUtil.isAir(b1)){
                swap(b1);
                mine(b1);
            }
            if (!BlockUtil.isAir(b2)){
                swap(b2);
                mine(b2);
            }
            if (!BlockUtil.isAir(b3)){
                swap(b3);
                mine(b3);
            }
            if (!BlockUtil.isAir(b4)){
                swap(b4);
                mine(b4);
            }
            if (!BlockUtil.isAir(b5)){
                swap(b5);
                mine(b5);
            }
            if (!BlockUtil.isAir(b6)){
                swap(b6);
                mine(b6);
            }
        }


    }

    private void swap(BlockPos pos){
        if (BlockUtil.getBlock(pos) instanceof SoulSandBlock || BlockUtil.getBlock(pos) instanceof SandBlock || BlockUtil.getBlock(pos) == Blocks.SOUL_SOIL || BlockUtil.getBlock(pos) == Blocks.GRASS_BLOCK || BlockUtil.getBlock(pos) == Blocks.DIRT){
            InvUtils.swap(InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.DIAMOND_SHOVEL || itemStack.getItem() == Items.NETHERITE_SHOVEL).slot(),false);
        }
        else {
            InvUtils.swap(InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE).slot(), true);
        }
    }

    private void mine(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            for (BlockPos pos : blockPoses) {
                if (BlockUtil.distance(mc.player.getBlockPos(), pos) <= 4.5) {
                    if (!mc.world.getBlockState(pos).isAir()) {
                        if (shapeMode.get().equals(ShapeMode.Lines) || shapeMode.get().equals(ShapeMode.Both)){
                            S(event, pos, 0.99,0, 0.01, lineColor.get(), lineColor2.get());
                            TAB(event, pos, 0.99, 0.01,true, true, lineColor.get(), lineColor2.get());

                            if (width.get() == 2){
                                S(event, pos, 0.98,0, 0.02, lineColor.get(), lineColor2.get());
                                TAB(event, pos, 0.98, 0.02,true, true, lineColor.get(), lineColor2.get());
                            }
                            if (width.get() == 3){
                                S(event, pos, 0.97,0, 0.03, lineColor.get(), lineColor2.get());
                                TAB(event, pos, 0.97, 0.03,true, true, lineColor.get(), lineColor2.get());
                            }
                            if (width.get() == 4){
                                S(event, pos, 0.96,0, 0.04, lineColor.get(), lineColor2.get());
                                TAB(event, pos, 0.96, 0.04,true, true, lineColor.get(), lineColor2.get());
                            }
                        }
                        if (shapeMode.get().equals(ShapeMode.Sides) || shapeMode.get().equals(ShapeMode.Both)){
                            FS(event, pos,0, true, true, sideColor.get(), sideColor2.get());
                        }
                    }
                }
            }
        }
    }
}
