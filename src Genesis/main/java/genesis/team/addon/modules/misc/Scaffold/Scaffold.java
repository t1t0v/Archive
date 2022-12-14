package genesis.team.addon.modules.misc.Scaffold;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.PlayerUtil;
import genesis.team.addon.util.ProximaUtil.RotationUtil;
import genesis.team.addon.util.ProximaUtil.Timer;
import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Arrays;
import java.util.List;


public class Scaffold extends Module {
    public Scaffold(){
        super(Genesis.Misc, "scaffoldV2", "Scaffold");

    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<List<Block>> customBlocks = sgGeneral.add(new BlockListSetting.Builder().name("customBlocks").description("Selected blocks.").build());
    public final Setting<FilterMode> filterMode = sgGeneral.add(new EnumSetting.Builder<FilterMode>().name("filterMode").description(".").defaultValue(FilterMode.NONE).build());
    public final Setting<Double> expand = sgGeneral.add(new DoubleSetting.Builder().name("expand").description("The range at which players can be targeted.").defaultValue(1).min(0).sliderMax(6).build());
    public final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder().name("delay").description("The range at which players can be targeted.").defaultValue(3.5).min(0).sliderMax(10).build());
    public final Setting<Boolean> Switch = sgGeneral.add(new BoolSetting.Builder().name("switch").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    private final Setting<TowerMode> tower = sgGeneral.add(new EnumSetting.Builder<TowerMode>().name("tower").description("How to select the player to target.").defaultValue(TowerMode.NORMAL).build());
    public final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder().name("center").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> safe = sgGeneral.add(new BoolSetting.Builder().name("safe").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> keepY = sgGeneral.add(new BoolSetting.Builder().name("keepY").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder().name("sprint").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> down = sgGeneral.add(new BoolSetting.Builder().name("down").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    public final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").description("Only places beds in the direction you are facing.").defaultValue(true).build());
    private final Setting<Keybind> downBind = sgGeneral.add(new KeybindSetting.Builder().name("downBind").description("Starts face place when this button is pressed.").defaultValue(Keybind.none()).build());

    public enum TowerMode {
        NONE, NORMAL, FAST
    }
    private final List<Block> invalid = Arrays.asList(Blocks.AIR, Blocks.COBWEB, Blocks.WATER, Blocks.FIRE, Blocks.WATER, Blocks.LAVA, Blocks.TRAPPED_CHEST,
        Blocks.GRAVEL, Blocks.LADDER, Blocks.VINE, Blocks.JUKEBOX, Blocks.ACACIA_DOOR, Blocks.BIRCH_DOOR, Blocks.DARK_OAK_DOOR, Blocks.IRON_DOOR,
        Blocks.JUNGLE_DOOR, Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.IRON_TRAPDOOR, Blocks.ACACIA_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.CRIMSON_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR, Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.WARPED_TRAPDOOR, Blocks.BLACK_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.WHITE_SHULKER_BOX,
        Blocks.YELLOW_SHULKER_BOX);

    private final Timer timerMotion = new Timer();

    public enum FilterMode {
        NONE, WHITELIST, BLACKLIST
    }

    private int lastY;

    private final Timer lastTimer = new Timer();
    private float lastYaw;
    private float lastPitch;

    private BlockPos pos;

    private boolean teleported;

    private boolean isValid(Block block) {
        if (invalid.contains(block)) return false;

        if (filterMode.get() == FilterMode.BLACKLIST) {
            return !customBlocks.get().contains(block);
        } else if (filterMode.get() == FilterMode.WHITELIST) {
            return customBlocks.get().contains(block);
        }


        return true;
    }

    public void onEnable() {
        if (mc.world != null) {
            this.timerMotion.reset();
            this.lastY = MathHelper.floor(mc.player.getY());
        }
    }


    @EventHandler(priority = 3)
    public void onUpdateLessPriorty(TickEvent.Pre event) {
        if (!lastTimer.hasPassed(100D * delay.get()) && InteractUtil.canPlaceNormally()) {
            RotationUtil rotationUtil = new RotationUtil();
            rotationUtil.setRotations(lastYaw, lastPitch);
        }
    }


    @EventHandler
    public void onRender(TickEvent.Pre event) {
        if (this.pos != null) {
        }
    }

    @EventHandler
    public void onClipAtLedge(ClipAtLedgeEvent event) {
        //if (down.get() && InputUtil.isKeyPressed(mc.getWindow().getHandle(), downBind.get().getValue()))
        //return;

        if (safe.get()) event.reset();
    }

    /*@EventHandler(priority = 3)
    public void onUpdateLessPriorty(UpdateEvent.Pre event) {
        if (!lastTimer.hasPassed(100D * delay.get()) && InteractionUtil.canPlaceNormally()) {
            KonasGlobals.INSTANCE.rotationManager.setRotations((float) lastYaw, (float) lastPitch);
        }
    }*/

    @EventHandler(priority = 20)
    public void onUpdate(TickEvent.Pre event) {
        int downDistance;
        if (!Modules.get().isActive(Sprint.class) && ((
            down.get() && InputUtil.isKeyPressed(mc.getWindow().getHandle(), downBind.get().getValue())) || !sprint.get()))
            mc.player.setSprinting(false);

        boolean doDown = down.get() && InputUtil.isKeyPressed(mc.getWindow().getHandle(), downBind.get().getValue());
        downDistance = doDown ? 2 : 1;

        if (keepY.get()) {
            if ((!PlayerUtil.isPlayerMoving() && mc.options.jumpKey.isPressed()) || mc.player.verticalCollision || mc.player.isOnGround())
                this.lastY = MathHelper.floor(mc.player.getY());
        } else {
            this.lastY = MathHelper.floor(mc.player.getY());
        }
        this.pos = null;
        double x = mc.player.getX();
        double z = mc.player.getZ();
        double y = keepY.get() ? this.lastY : mc.player.getY();
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();
        if (!mc.player.horizontalCollision && expand.get() > 0D) {
            double[] coords = getExpandCoords(x, z, forward, strafe, yaw);
            x = coords[0];
            z = coords[1];
        }
        if (canPlace(mc.world.getBlockState(new BlockPos(mc.player.getX(), mc.player.getY() - downDistance, mc.player.getZ())).getBlock())) {
            x = mc.player.getX();
            z = mc.player.getZ();
        }
        BlockPos blockBelow = new BlockPos(x, Math.floor(y) - downDistance, z);
        if (mc.world.getBlockState(blockBelow).getMaterial().isReplaceable()) {
            this.pos = blockBelow;
        }
        if (this.pos != null) {
            int slot = -1;
            final ItemStack mainhandStack = mc.player.getMainHandStack();
            if (mainhandStack != ItemStack.EMPTY && mainhandStack.getItem() instanceof BlockItem) {
                final Block blockFromMainhandItem = ((BlockItem) mainhandStack.getItem()).getBlock();
                if (isValid(blockFromMainhandItem)) {
                    slot = mc.player.getInventory().selectedSlot;
                }
            }

            final ItemStack offhandStack = mc.player.getOffHandStack();
            if (offhandStack != ItemStack.EMPTY && offhandStack.getItem() instanceof BlockItem) {
                final Block blockFromOffhandItem = ((BlockItem) offhandStack.getItem()).getBlock();
                if (isValid(blockFromOffhandItem)) {
                    slot = -2;
                }
            }

            if (Switch.get()) {
                if (slot == -1) {
                    for (int i = 0; i < 9; i++) {
                        final ItemStack stack = mc.player.getInventory().getStack(i);
                        if (stack != ItemStack.EMPTY && stack.getItem() instanceof BlockItem) {
                            final Block blockFromItem = ((BlockItem) stack.getItem()).getBlock();
                            if (isValid(blockFromItem)) {
                                slot = i;
                                break;
                            }
                        }
                    }
                }
            }

            if (slot != -1) {
                if (tower.get() != TowerMode.NONE) {
                    if (mc.options.jumpKey.isPressed() && mc.player.forwardSpeed == 0.0F && mc.player.forwardSpeed * 1.6 == 0.0F && tower.get() != TowerMode.NONE && !mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                        if (!this.teleported && center.get()) {
                            this.teleported = true;
                            BlockPos pos = new BlockPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                            mc.player.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                        }
                        if (center.get() && !this.teleported)
                            return;
                        if (tower.get() == TowerMode.FAST) {
                            Timer.updateTimer(this, 25, mc.player.age % 10 == 0 ? 1F : 1.5782F);
                        }
                        mc.player.setVelocity(0D,0.41999998688697815D, 0D);
                        if (this.timerMotion.hasPassed(1500L)) {
                            Timer.resetTimer(this);
                            timerMotion.reset();
                            mc.player.setVelocity(mc.player.getVelocity().x, -0.28D, mc.player.getVelocity().z);
                        }
                    } else {
                        Timer.resetTimer(this);
                        this.timerMotion.reset();
                        if (this.teleported && this.center.get())
                            this.teleported = false;
                    }
                } else {
                    Timer.resetTimer(this);
                }

                mc.player.getInventory().selectedSlot = slot;
                PlayerUtil.placeBlock(pos, slot == -2 ? Hand.OFF_HAND : Hand.MAIN_HAND, swing.get(), true);
            }
        }
    }

    private int getBlockCount() {
        int blockCount = 0;
        for (int i = 0; i < 45; i++) {
            if (mc.player.getInventory().getStack(i).isStackable()) {
                ItemStack is = mc.player.getInventory().getStack(i);
                Item item = is.getItem();
                if (is.getItem() instanceof BlockItem &&
                    !this.invalid.contains(((BlockItem) item).getBlock()))
                    blockCount += is.getCount();
            }
        }
        return blockCount;
    }

    public double[] getExpandCoords(double x, double z, double forward, double strafe, float yaw) {
        BlockPos underPos = new BlockPos(x, mc.player.getY() - ((InputUtil.isKeyPressed(mc.getWindow().getHandle(), downBind.get().getValue()) && down.get()) ? 2 : 1), z);
        Block underBlock = mc.world.getBlockState(underPos).getBlock();
        double xCalc = -999.0D, zCalc = -999.0D;
        double dist = 0.0D;
        double expandDist = expand.get() * 2.0D;
        while (!canPlace(underBlock)) {
            xCalc = x;
            zCalc = z;
            dist++;
            if (dist > expandDist)
                dist = expandDist;
            xCalc += (forward * 0.45D * Math.cos(Math.toRadians((yaw + 90.0F))) + strafe * 0.45D * Math.sin(Math.toRadians((yaw + 90.0F)))) * dist;
            zCalc += (forward * 0.45D * Math.sin(Math.toRadians((yaw + 90.0F))) - strafe * 0.45D * Math.cos(Math.toRadians((yaw + 90.0F)))) * dist;
            if (dist == expandDist)
                break;
            underPos = new BlockPos(xCalc, mc.player.getY() - ((InputUtil.isKeyPressed(mc.getWindow().getHandle(), downBind.get().getValue()) && down.get()) ? 2 : 1), zCalc);
            underBlock = mc.world.getBlockState(underPos).getBlock();
        }
        return new double[]{xCalc, zCalc};
    }

    public boolean canPlace(Block block) {
        //think
        return ((block instanceof AirBlock || block instanceof FluidBlock) && mc.world != null && mc.player != null && this.pos != null && !mc.world.doesNotIntersectEntities(mc.player, VoxelShapes.cuboid(new Box(this.pos))));
    }

    private int getBlockCountHotbar() {
        int blockCount = 0;
        for (int i = 36; i < 45; i++) {
            if (mc.player.getInventory().getStack(i).isStackable()) {
                ItemStack is = mc.player.getInventory().getStack(i);
                Item item = is.getItem();
                if (is.getItem() instanceof BlockItem &&
                    !this.invalid.contains(((BlockItem) item).getBlock()))
                    blockCount += is.getCount();
            }
        }
        return blockCount;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        double x = event.movement.getX();
        double z = event.movement.getZ();

        if (mc.player.isOnGround() && !mc.player.noClip && safe.get() && !InputUtil.isKeyPressed(mc.getWindow().getHandle(), downBind.get().getValue())) {
            double i;

            for (i = 0.05D; x != 0.0D && !mc.world.doesNotIntersectEntities(mc.player, VoxelShapes.cuboid(mc.player.getBoundingBox().offset(x, -1.0f, 0.0D))); ) {
                if (x < i && x >= -i) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= i;
                } else {
                    x += i;
                }
            }

            while (z != 0.0D && !mc.world.doesNotIntersectEntities(mc.player, VoxelShapes.cuboid(mc.player.getBoundingBox().offset(0.0D, -1.0f, z)))) {
                if (z < i && z >= -i) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= i;
                } else {
                    z += i;
                }
            }

            while (x != 0.0D && z != 0.0D && !mc.world.doesNotIntersectEntities(mc.player, VoxelShapes.cuboid(mc.player.getBoundingBox().offset(x, -1.0f, z)))) {
                if (x < i && x >= -i) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= i;
                } else {
                    x += i;
                }
                if (z < i && z >= -i) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= i;
                } else {
                    z += i;
                }
            }
        }

        ((IVec3d) event.movement).setXZ(x, z);
    }
}
