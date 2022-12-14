package genesis.team.addon.modules.bbc.BogeyMan;

import genesis.team.addon.modules.bbc.event.BlockUpdateEvent;
import genesis.team.addon.modules.bbc.event.BreakBlockEvent;
import genesis.team.addon.modules.bbc.BBCModule;
import genesis.team.addon.Genesis;
import genesis.team.addon.modules.bbc.BBCModule;
import genesis.team.addon.modules.bbc.utils.entity.EntityHelper;
import genesis.team.addon.modules.bbc.utils.entity.TargetHelper;
import genesis.team.addon.modules.bbc.utils.math.TimerUtils;
import genesis.team.addon.modules.bbc.utils.player.InvHelper;
import genesis.team.addon.modules.bbc.utils.player.PlayerHelper;
import genesis.team.addon.modules.bbc.utils.security.Initialization;
import genesis.team.addon.modules.bbc.utils.world.BlockHelper;
import genesis.team.addon.modules.bbc.utils.world.BlockPosX;
import genesis.team.addon.modules.bbc.utils.world.DamageHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BedItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BogeyMan extends BBCModule {
    public BogeyMan() {
        super(Genesis.Bbc, "Boogey Man", "Automatically place's and blows up beds.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacing = settings.createGroup("Placing");
    private final SettingGroup sgCalculating = settings.createGroup("Calculating");
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgBedCrafter = settings.createGroup("BedCrafter");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgDebug = settings.createGroup("Debug");

    private final Setting<Double> enemyRange = sgGeneral.add(new DoubleSetting.Builder().name("enemy-range").defaultValue(15).range(1, 30).build());

    private final Setting<Double> placeRange = sgPlacing.add(new DoubleSetting.Builder().name("place-range").description("The distance where you can place/break beds.").defaultValue(5).range(0, 7).sliderRange(0, 7).build());
    private final Setting<Integer> radius = sgPlacing.add(new IntSetting.Builder().name("radius").description("Delay between place.").defaultValue(5).range(0, 10).sliderRange(0, 10).build());
    private final Setting<Integer> height = sgPlacing.add(new IntSetting.Builder().name("height").description("Delay between place.").defaultValue(3).range(0, 7).sliderRange(0, 7).build());
    private final Setting<Integer> placeDelay = sgPlacing.add(new IntSetting.Builder().name("place-delay").description("Delay between place.").defaultValue(10).range(0, 20).sliderRange(0, 20).build());
    private final Setting<Boolean> oneDotTwelve = sgPlacing.add(new BoolSetting.Builder().name("1.12-place").description("Disabling air place and cannot be placed in the player.").defaultValue(false).build());
    private final Setting<Boolean> terrainIgnore = sgPlacing.add(new BoolSetting.Builder().name("terrain-ignore").description("Ignore blocks with blast resistane less than 600.").defaultValue(true).build());
    private final Setting<Boolean> quickPlace = sgPlacing.add(new BoolSetting.Builder().name("quick-place").description("Immediately places the bed after breaking the self-trap/surround.").defaultValue(true).build());
    private final Setting<Integer> allowedFails = sgPlacing.add(new IntSetting.Builder().name("allowed-fails").description("After N number of trouble attempts, the bed aura will switch to a place-break").defaultValue(3).range(0, 10).sliderRange(0, 10).build());
    private final Setting<Boolean> trap = sgPlacing.add(new BoolSetting.Builder().name("trap").defaultValue(true).build());


    private final Setting<Boolean> useThread = sgCalculating.add(new BoolSetting.Builder().name("use-thread").description("Calculate positions in a separate thread.").defaultValue(true).build());
    private final Setting<CalculatingMode> calculatingMode = sgCalculating.add(new EnumSetting.Builder<CalculatingMode>().name("calculating-mode").description("Position calculation method.").defaultValue(CalculatingMode.Normal).build());
    private final Setting<Double> minDistance = sgCalculating.add(new DoubleSetting.Builder().name("min-distance").description("Min distance to target.").defaultValue(3).range(1, 5).sliderRange(1, 5).visible(() -> calculatingMode.get() == CalculatingMode.ByDistance).build());
    private final Setting<Double> radiusFromTarget = sgCalculating.add(new DoubleSetting.Builder().name("radius-from-target").description("Radius around the target.").defaultValue(3).range(1, 5).sliderRange(1, 5).visible(() -> calculatingMode.get() == CalculatingMode.ByRadius).build());


    private final Setting<DamageMode> damageMode = sgDamage.add(new EnumSetting.Builder<DamageMode>().name("damage-mode").description("Damage calculation method.").defaultValue(DamageMode.BestDamage).build());
    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder().name("min-damage").description("Minimum damage for place.").defaultValue(7.0).range(0, 36).sliderRange(0, 36).build());
    private final Setting<Boolean> lethalDamage = sgDamage.add(new BoolSetting.Builder().name("lethal-damage").description("Keep placing beds ignoring damage if the target is low on HP.").defaultValue(true).build());
    private final Setting<Double> lethalHealth = sgDamage.add(new DoubleSetting.Builder().name("lethal-health").description("Health point at which the lethal damage function will turn on.").defaultValue(3).range(0, 36).sliderRange(0, 36).visible(lethalDamage::get).build());
    private final Setting<Double> safety = sgDamage.add(new DoubleSetting.Builder().name("safety").description("By what percentage should the target damage be greater than the self damage in order to continue to place.").defaultValue(25).range(0, 100).sliderRange(0, 100).build());
    private final Setting<Boolean> antiSelfPop = sgDamage.add(new BoolSetting.Builder().name("anti-self-pop").description("Try not to deal lethal damage to yourself.").defaultValue(true).build());
    private final Setting<Boolean> antiFriendPop = sgDamage.add(new BoolSetting.Builder().name("anti-friend-pop").description("Try not to deal lethal damage to friends.").defaultValue(true).build());
    private final Setting<Double> maxFriendDamage = sgDamage.add(new DoubleSetting.Builder().name("max-friend-damage").description("Maximum damage that can be dealt to a friend.").defaultValue(8).range(0, 36).sliderRange(0, 36).visible(antiFriendPop::get).build());


    private final Setting<SwapMode> swapMode = sgInventory.add(new EnumSetting.Builder<SwapMode>().name("swap-mode").description("Slot swap method.").defaultValue(SwapMode.Silent).build());
    private final Setting<Boolean> syncSlot = sgInventory.add(new BoolSetting.Builder().name("sync-slot").description("Synchronize the slot to get rid of fakes.").defaultValue(true).build());
    private final Setting<Boolean> refill = sgInventory.add(new BoolSetting.Builder().name("refill").description("Moves beds into a selected hotbar slot.").defaultValue(true).build());
    private final Setting<Integer> refillSlot = sgInventory.add(new IntSetting.Builder().name("refill-slot").description("The slot auto move moves beds to.").defaultValue(5).range(1, 9).sliderRange(1, 9).visible(refill::get).build());


    private final Setting<Boolean> bedCrafter = sgBedCrafter.add(new BoolSetting.Builder().name("bed-crafter").description("Craft beds if there are none/a workbench is open.").defaultValue(true).build());
    public final Setting<Double> craftRadius = sgBedCrafter.add(new DoubleSetting.Builder().name("craft-radius").description("Radius around the target.").defaultValue(3).range(1, 5).sliderRange(1, 5).visible(() -> calculatingMode.get() == CalculatingMode.ByRadius).build());
    public final Setting<Boolean> onlyOnHole = sgBedCrafter.add(new BoolSetting.Builder().name("only-on-hole").description(".").defaultValue(true).build());
    public final Setting<Boolean> whileMoving = sgBedCrafter.add(new BoolSetting.Builder().name("while-moving").description(".").defaultValue(true).build());


    private final Setting<Boolean> pauseOnCraft = sgPause.add(new BoolSetting.Builder().name("pause-on-craft").description("Pause placing while crafting.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pause placing while eating.").defaultValue(false).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pause placing while drinking.").defaultValue(false).build());
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pause placing while mining.").defaultValue(false).build());


    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Whether to swing hand client-side.").defaultValue(true).build());
    private final Setting<RenderMode> render = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render").description("Renders the block where it is placing a bed.").defaultValue(RenderMode.Smooth).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(() -> render.get() != RenderMode.Off).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).visible(() -> render.get() != RenderMode.Off).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).visible(() -> render.get() != RenderMode.Off).build());
    private final Setting<Integer> smoothFactor = sgRender.add(new IntSetting.Builder().name("smooth-factor").description("Speed of transition from position to another.").defaultValue(6).range(1, 30).sliderRange(1, 30).visible(() -> render.get() == RenderMode.Smooth).build());
    private final Setting<Integer> fadeTime = sgRender.add(new IntSetting.Builder().name("fade-time").description("Render fade time.").defaultValue(10).range(1, 30).sliderRange(1, 30).visible(() -> render.get() == RenderMode.Fade).build());


    private final Setting<Boolean> debugChat = sgDebug.add(new BoolSetting.Builder().name("debug-chat").description("Send information to the chat.").defaultValue(false).build());
    private final Setting<Boolean> debugRender = sgDebug.add(new BoolSetting.Builder().name("debug-render").description("Render information.").defaultValue(false).build());


    private final ExecutorService thread = Executors.newScheduledThreadPool(2);

    private List<PlayerEntity> targets = new ArrayList<>();
    private List<BlockPosX> renderSphere;

    private BlockPosX bestPos;
    private Direction bestOffset;
    private double bestDamage;

    private final TimerUtils placeTimer = new TimerUtils();

    private long lastTime;
    private int failTimes;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private Box renderBox;

    @Override
    public void onActivate() {


        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
    }

    @Override
    public void onDeactivate() {
        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
        failTimes = 0;
        targets.clear();
        bestPos = null;
        bestOffset = null;
        bestDamage = 0D;
        renderSphere = null;
        placeTimer.reset();
    }

    @EventHandler
    private void onTick(TickEvent.Post event){
        if (mc == null || mc.player == null || mc.world == null) return;

        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        if (!isValidDimension()) return;

        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!bed.found()){
            if (bedCrafter.get()) BedCrafter.startWork();
            return;
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler && bedCrafter.get()){
            if (!InvHelper.isInventoryFull()){
                BedCrafter.startWork();
                return;
            }
            else mc.player.closeHandledScreen();
        }

        if (refill.get() && swapMode.get() != SwapMode.Inventory) doRefill(bed);
        if (shouldPause()) return;

        targets = TargetHelper.getTargetsInRange(enemyRange.get());
        if (targets.isEmpty()) return;

        if (useThread.get()) thread.execute(this::doCalculate);
        else doCalculate();

        if (placeTimer.passedTicks(placeDelay.get())){
            placeTimer.reset();
            if (bestPos != null && bestOffset != null){
                doPlace();
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if (bestPos == null || bestOffset == null || render.get() == RenderMode.Off) return;

        try {
            if (debugRender.get() && renderSphere != null && !renderSphere.isEmpty()){
                renderSphere.forEach(pos -> {
                    event.renderer.box(pos, new Color().a(10), new Color().a(100), ShapeMode.Both, 0);
                });
            }
        }catch (ConcurrentModificationException ignored){
        }

        Box box = null;

        int x = bestPos.getX();
        int y = bestPos.getY();
        int z = bestPos.getZ();

        switch (bestOffset.getOpposite()){
            case EAST -> box = new Box(x - 1, y, z, x + 1, y + 0.6, z + 1);
            case WEST -> box = new Box(x, y, z, x + 2, y + 0.6, z + 1);
            case NORTH -> box = new Box(x, y, z, x + 1, y + 0.6, z + 2);
            case SOUTH -> box = new Box(x, y, z - 1, x + 1, y + 0.6, z + 1);
        }

        switch (render.get()){
            case Fade -> {
                if (!renderBlocks.isEmpty()) {
                    renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
                    renderBlocks.forEach(block -> block.renderBedForm(event, shapeMode.get()));
                }
            }
            case Smooth -> renderBox(event, box);
        }
    }


    @EventHandler
    private void onUpdate(BlockUpdateEvent event){
        if (!quickPlace.get()) return;
        BlockPosX pos = new BlockPosX(event.pos);
        if (isReplaceable(pos)){ // && (event.oldState.getBlock() instanceof BedBlock)){
            quickPlace(pos);
        }
    }

    @EventHandler
    private void onBreak(BreakBlockEvent event){
        if (!quickPlace.get()) return;
        quickPlace(new BlockPosX(event.getPos()));
    }

    // [Misc] //

    private boolean isValidDimension(){
        if (mc.world.getDimension().bedWorks()){
            warning("It is overworld...toggle");
            toggle();
            return false;
        }
        return true;
    }

    private boolean shouldPause(){
        if (pauseOnCraft.get() && mc.player.currentScreenHandler instanceof CraftingScreenHandler) return true;
        return PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get());
    }

    private boolean isOpenPos(BlockPosX pos){
        for (Direction direction : Direction.values()){
            if (direction == Direction.DOWN || direction == Direction.UP) continue;
            if (pos.offset(direction).distance() > placeRange.get() || EntityUtils.intersectsWithEntity(new Box(pos.offset(direction)), entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity)) continue;
            if (isReplaceable(pos.offset(direction)) || pos.offset(direction).of(BedBlock.class)) return true;
        }
        return false;
    }

    private boolean isReplaceable(BlockPosX pos){
        return pos.air() || BlockHelper.getState(pos).getFluidState().isStill() || pos.of(FireBlock.class) || pos.of(Blocks.GRASS) || pos.of(Blocks.TALL_GRASS) || pos.of(Blocks.SEAGRASS);
    }

    public boolean equals(BlockPos b1, BlockPos b2){
        return b1.getX() == b2.getX() && b1.getY() == b2.getY() && b1.getZ() == b2.getZ();
    }

    @Override
    public String getInfoString() {
        return "BestDmg: " + bestDamage + "; Time: " + lastTime;
    }

    // [Inv] //

    private void doRefill(FindItemResult bed){
        if (!bed.found() || bed.slot() == refillSlot.get() - 1) return;
        InvUtils.move().from(bed.slot()).toHotbar(refillSlot.get() - 1);
    }

    // [Target] //

    private BlockPosX getPartOfTarget(BlockPosX checkPos){
        for (PlayerEntity target : targets) {
            for (int j = 0; j < 3; j++) {
                BlockPosX pos = new BlockPosX(target.getBlockPos().add(0, j, 0));
                if (!isReplaceable(pos)) continue;
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.DOWN || dir == Direction.UP) continue;
                    BlockPosX offsetPos = pos.offset(dir);
                    if (equals(offsetPos, checkPos)) return pos;
                }
            }
        }
        return null;
    }

    private boolean shouldPress(){
        if (bestPos == null || bestOffset == null) return false;

        for (PlayerEntity player : targets){
            if (EntityHelper.getPlayerSpeed(player) > 1.5) continue;
            for (int i = 0; i < 3; i++){
                BlockPosX posX = new BlockPosX(player.getBlockPos().add(0, i, 0));
                if (equals(posX, bestPos)) return true;
            }
        }
        return false;
    }

    // [Place] //

    private void doPlace(){
        doPlace(bestPos, bestOffset, false);
    }

    private void doPlace(BlockPosX bestPos, Direction bestOffset, boolean onlyPlace){
        assert mc.interactionManager != null;
        assert mc.getNetworkHandler() != null;
        assert mc.world != null;

        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);

        Direction offset = bestOffset.getOpposite();
        BlockPosX offsetPos = bestPos.offset(bestOffset);

        if (!(mc.world.getBlockState(offsetPos).getBlock() instanceof BedBlock) && shouldPress()) failTimes++;

        double y = switch (offset) {
            case NORTH -> 180;
            case EAST -> -90;
            case WEST -> 90;
            case SOUTH -> 0;
            default -> throw new IllegalStateException("Unexpected value: " + offset);
        };

        double p = Rotations.getPitch(offsetPos);

        for (PlayerEntity player : targets){
            if (!trap.get() || !EntityHelper.isInHole(player)) continue;
            if (PlayerHelper.distanceTo(player) <= placeRange.get()){
                FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().getBlastResistance() >= 600);
                if (block.found()){
                    InvUtils.swap(block.slot(), true);
                    BlockHitResult placeResult = new BlockHitResult(BlockHelper.closestVec3d(player.getBlockPos().add(0, 2, 0)), Direction.DOWN, player.getBlockPos().add(0, 2, 0), false);
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, placeResult, 0));
                    mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    InvUtils.swapBack();
                }
            }
        }

        Rotations.rotate(y, p, () -> {
            if (swapMode.get() == SwapMode.Silent || swapMode.get() == SwapMode.Normal && !bed.isOffhand() && !bed.isMain()) InvUtils.swap(bed.slot(), true);

            boolean holdsBed = (bed.isOffhand() || bed.isMainHand()) || swapMode.get() == SwapMode.Inventory;

            if (holdsBed){
                mc.player.swingHand(mc.player.getActiveHand());
                if (!onlyPlace){
                    if (swapMode.get() == SwapMode.Inventory) {
                        move(bed, () -> place(offsetPos));
                    } else place(offsetPos);
                }
                else {
                    if (swapMode.get() == SwapMode.Inventory) {
                        move(bed, () -> onlyPlace(offsetPos));
                    } else onlyPlace(offsetPos);
                }

                renderBlocks.add(renderBlockPool.get().setBlock(bestPos, offsetPos, fadeTime.get()));
            }

            boolean canSilent = swapMode.get() == SwapMode.Silent || (bed.isHotbar() && swapMode.get() == SwapMode.Inventory);
            if (canSilent) {
                InvUtils.swapBack();
                if (syncSlot.get()) InvHelper.syncSlot();
            }
        });
    }

    private void place(BlockPosX offsetPos){
        BlockHitResult placeResult = new BlockHitResult(offsetPos.closestVec3d(), Direction.UP, offsetPos, false);
        BlockHitResult breakResult = new BlockHitResult(offsetPos.closestVec3d(), Direction.UP, offsetPos, false);

        if (failTimes > allowedFails.get() || !shouldPress()) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, placeResult, 0));
            mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, breakResult);

        } else if (failTimes <= allowedFails.get() && shouldPress()){
            mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, breakResult);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, placeResult, 0));
        }
    }

    private void onlyPlace(BlockPosX offsetPos){
        BlockHitResult placeResult = new BlockHitResult(offsetPos.closestVec3d(), Direction.UP, offsetPos, false);
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, placeResult, 0));
    }

    private void quickPlace(BlockPosX checkPos){
        BlockPosX bestPos = getPartOfTarget(checkPos);
        Direction bestOffset = Direction.DOWN;
        if (bestPos != null){
            if (bestPos.west().equals(checkPos)) bestOffset = Direction.WEST;
            if (bestPos.east().equals(checkPos)) bestOffset = Direction.EAST;
            if (bestPos.south().equals(checkPos)) bestOffset = Direction.SOUTH;
            if (bestPos.north().equals(checkPos)) bestOffset = Direction.NORTH;

            doPlace(bestPos, bestOffset, failTimes <= allowedFails.get());
            if (debugChat.get()) info("Placed on break block");
        }
    }

    public boolean move(FindItemResult itemResult, Runnable runnable) {
        if (itemResult.isOffhand()) {
            runnable.run();
            return true;
        }

        move(mc.player.getInventory().selectedSlot, itemResult.slot());
        runnable.run();
        move(mc.player.getInventory().selectedSlot, itemResult.slot());
        return true;
    }

    private void move(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, handler.getSlot(to).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), PlayerInventory.MAIN_SIZE + from, to, SlotActionType.SWAP, handler.getCursorStack().copy(), stack));
    }

    // [Calculating] //

    private void doCalculate(){
        long pre = System.currentTimeMillis();

        BlockPosX bestPos = null;
        Direction bestOffset = null;
        double bestDamage = 0.0;
        double safety = 0.0;
        int size = 0;

        if (debugRender.get()) renderSphere = new ArrayList<>();

        BlockPosX p = new BlockPosX(mc.player.getBlockPos());

        for (int i = p.getX() - radius.get(); i < p.getX() + radius.get(); i++) {
            for (int j = p.getY() - height.get(); j < p.getY() + height.get(); j++) {
                for (int k = p.getZ() - radius.get(); k < p.getZ() + radius.get(); k++) {
                    BlockPosX pos = new BlockPosX(i, j, k);
                    if (BlockHelper.distance(p, pos) <= radius.get()) {
                        if ((!isReplaceable(pos) && !(pos.of(BedBlock.class))) || !isOpenPos(pos) || (oneDotTwelve.get() && !pos.down().solid()) || EntityUtils.intersectsWithEntity(new Box(pos), entity -> entity instanceof EndCrystalEntity || (oneDotTwelve.get() && entity instanceof PlayerEntity))) continue;
                        boolean shouldSkip = true;

                        switch (calculatingMode.get()){
                            case Normal -> shouldSkip = false;
                            case ByRadius -> {
                                for (PlayerEntity target : targets){
                                    if (pos.distance(target) <= radiusFromTarget.get()) shouldSkip = false;
                                }
                            }
                            case ByDistance -> {
                                for (PlayerEntity target : targets){
                                    double distance = mc.player.distanceTo(target);
                                    if (distance < minDistance.get()) distance = minDistance.get();
                                    if (pos.distance(target) <= distance) shouldSkip = false;
                                }
                            }
                        }
                        if (shouldSkip) continue;

                        double targetDamage = getBestDamage(pos);
                        double selfDamage = DamageHelper.bedDamage(mc.player, pos, terrainIgnore.get());
                        safety = (targetDamage / 36 - selfDamage / 36) * 100;

                        if (safety < this.safety.get()
                            || antiSelfPop.get() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount())
                            continue;

                        boolean validPos = true;
                        if (antiFriendPop.get()) {
                            for (PlayerEntity friend : mc.world.getPlayers()) {
                                if (!Friends.get().isFriend(friend)) continue;

                                double friendDamage = DamageHelper.bedDamage(friend, pos, terrainIgnore.get());
                                if (friendDamage > maxFriendDamage.get() || EntityUtils.getTotalHealth(friend) - friendDamage <= 0) {
                                    validPos = false;
                                    break;
                                }
                            }
                        }

                        if (!validPos) continue;

                        if (debugChat.get()) size++;
                        if (debugRender.get()) renderSphere.add(pos);

                        if (targetDamage > bestDamage) {
                            bestDamage = targetDamage;
                            bestPos = pos;
                        }
                    }
                }
            }
        }

        if (bestPos != null){
            List<Direction> offsets = new ArrayList<>();
            int dirSize = Direction.values().length;
            for (int i = 0; i < dirSize; i++){
                Direction dir = Direction.values()[i];
                if (dir == Direction.DOWN || dir == Direction.UP) continue;
                BlockPosX offset = bestPos.offset(dir);

                if (bestPos.of(BedBlock.class)){
                    if (!(offset.of(BedBlock.class)) || EntityUtils.intersectsWithEntity(new Box(offset), entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity)) continue;
                    offsets.add(dir);
                }
                else {
                    if (!isReplaceable(offset) || EntityUtils.intersectsWithEntity(new Box(offset), entity -> entity instanceof PlayerEntity || entity instanceof EndCrystalEntity)) continue;
                    offsets.add(dir);
                }
            }

            if (offsets.isEmpty()) {
                return;
            }
            BlockPos finalBestPos = bestPos;
            offsets.sort(Comparator.comparing(direction -> PlayerUtils.distanceTo(finalBestPos.offset(direction))));
            bestOffset = offsets.get(0);
        }

        this.bestPos = bestPos;
        this.bestOffset = bestOffset;
        this.bestDamage = bestDamage;
        this.lastTime = System.currentTimeMillis() - pre;
        if (debugChat.get()) info(lastTime + ": " + size);
    }

    private double getBestDamage(BlockPos pos) {
        double highestDamage = 0;

        int size = targets.size();
        for (int i = 0; i < size; i++) {
            PlayerEntity player = targets.get(i);

            double targetDamage = DamageHelper.bedDamage(player, pos, terrainIgnore.get());
            double health = player.getHealth() + player.getAbsorptionAmount();

            if (targetDamage >= minDamage.get() || (lethalDamage.get() && health - targetDamage <= lethalHealth.get())){
                switch (damageMode.get()){
                    case BestDamage -> {
                        if (targetDamage > highestDamage) {
                            highestDamage = targetDamage;
                        }
                    }
                    case MostDamage -> highestDamage += targetDamage;
                }
            }
        }
        return highestDamage;
    }

    // [Render] //

    private void renderBox(Render3DEvent event, Box post){
        if (renderBox == null) renderBox = post;

        double minxX = (post.minX - renderBox.minX) / smoothFactor.get();
        double minxY = (post.minY - renderBox.minY) / smoothFactor.get();
        double minxZ = (post.minZ - renderBox.minZ) / smoothFactor.get();

        double maxX = (post.maxX - renderBox.maxX) / smoothFactor.get();
        double maxY = (post.maxY - renderBox.maxY) / smoothFactor.get();
        double maxZ = (post.maxZ - renderBox.maxZ) / smoothFactor.get();

        renderBox = new Box(renderBox.minX + minxX, renderBox.minY + minxY, renderBox.minZ + minxZ, renderBox.maxX + maxX, renderBox.maxY + maxY,  renderBox.maxZ + maxZ);

        event.renderer.box(renderBox, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    public class RenderBlock {
        public int ticks;
        public double offset;

        public BlockPos.Mutable head = new BlockPos.Mutable();
        public BlockPos.Mutable feet = new BlockPos.Mutable();
        private Color sidesTop;
        private Color sidesBottom;
        private Color linesTop;
        private Color linesBottom;


        public RenderBlock setBlock(BlockPos head, BlockPos feet, int tick) {
            this.head.set(head);
            this.feet.set(feet);
            ticks = tick;

            sidesTop = sideColor.get();
            sidesBottom = sideColor.get();
            linesTop = lineColor.get();
            linesBottom = lineColor.get();

            offset = 1;
            return this;
        }


        public void tick() {
            ticks--;
        }

        public void renderBedForm(Render3DEvent event, ShapeMode shapeMode) {
            if (sidesTop == null || sidesBottom == null || linesTop == null || linesBottom == null || head == null || feet == null) return;

            int preSideTopA = sidesTop.a;
            int preSideBottomA = sidesBottom.a;
            int preLineTopA = linesTop.a;
            int preLineBottomA = linesBottom.a;

            sidesTop.a *= (double) ticks / 8;
            sidesBottom.a *= (double) ticks / 8;
            linesTop.a *= (double) ticks / 8;
            linesBottom.a *= (double) ticks / 8;

            double x = head.getX();
            double y = head.getY();
            double z = head.getZ();

            double px3 = 1.875 / 10;
            double px8 = 5.62 / 10;

            double px16 = 1;
            double px32 = 2;

            Direction dir = Direction.EAST;

            if (feet.equals(head.west())) dir = Direction.WEST;
            if (feet.equals(head.east())) dir = Direction.EAST;
            if (feet.equals(head.south())) dir = Direction.SOUTH;
            if (feet.equals(head.north())) dir = Direction.NORTH;

            if (dir == Direction.NORTH) z -= 1;
            else if (dir == Direction.WEST) x -= 1;

            // Lines

            if (shapeMode.lines()) {
                if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                    // Edges

                    renderEdgeLines(x, y, z, px3, 1, event);
                    renderEdgeLines(x, y, z + px32 - px3, px3, 2, event);
                    renderEdgeLines(x + px16 - px3, y, z, px3, 3, event);
                    renderEdgeLines(x + px16 - px3, y, z + px32 - px3, px3, 4, event);

                    // High Lines

                    event.renderer.line(x, y + px3, z, x, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x, y + px3, z + px32, x, y + px8, z + px32, linesBottom, linesTop);
                    event.renderer.line(x + px16, y + px3, z, x + px16, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x + px16, y + px3, z + px32, x + px16, y + px8, z + px32, linesBottom, linesTop);

                    // Connections

                    event.renderer.line(x + px3, y + px3, z, x + px16 - px3, y + px3, z, linesBottom);
                    event.renderer.line(x + px3, y + px3, z + px32, x + px16 - px3, y + px3, z + px32, linesBottom);

                    event.renderer.line(x, y + px3, z + px3, x, y + px3, z + px32 - px3, linesBottom);
                    event.renderer.line(x + px16, y + px3, z + px3, x + px16, y + px3, z + px32 - px3, linesBottom);

                    // Top

                    event.renderer.line(x, y + px8, z, x + px16, y + px8, z, linesTop);
                    event.renderer.line(x, y + px8, z + px32, x + px16, y + px8, z + px32, linesTop);
                    event.renderer.line(x, y + px8, z, x , y + px8, z + px32, linesTop);
                    event.renderer.line(x + px16, y + px8, z, x + px16, y + px8, z + px32, linesTop);
                } else {
                    // Edges

                    renderEdgeLines(x, y, z, px3, 1, event);
                    renderEdgeLines(x, y, z + px16 - px3, px3, 2, event);
                    renderEdgeLines(x + px32 - px3, y, z, px3, 3, event);
                    renderEdgeLines(x + px32 - px3, y, z + px16 - px3, px3, 4, event);

                    // High Lines

                    event.renderer.line(x, y + px3, z, x, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x + px32, y + px3, z, x + px32, y + px8, z, linesBottom, linesTop);
                    event.renderer.line(x, y + px3, z + px16, x, y + px8, z + px16, linesBottom, linesTop);
                    event.renderer.line(x + px32, y + px3, z + px16, x + px32, y + px8, z + px16, linesBottom, linesTop);

                    // Connections

                    event.renderer.line(x, y + px3, z + px3, x, y + px3, z + px16 - px3, linesBottom);
                    event.renderer.line(x + px32, y + px3, z + px3, x + px32, y + px3, z + px16 - px3, linesBottom);

                    event.renderer.line(x + px3, y + px3, z, x + px32 - px3, y + px3, z, linesBottom);
                    event.renderer.line(x + px3, y + px3, z + px16, x + px32 - px3, y + px3, z + px16, linesBottom);

                    // Top

                    event.renderer.line(x, y + px8, z, x, y + px8, z + px16, linesTop);
                    event.renderer.line(x + px32, y + px8, z, x + px32, y + px8, z + px16, linesTop);
                    event.renderer.line(x, y + px8, z, x + px32 , y + px8, z, linesTop);
                    event.renderer.line(x, y + px8, z + px16, x + px32, y + px8, z + px16, linesTop);
                }
            }

            // Sides

            if (shapeMode.sides()) {
                if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                    // Horizontal

                    // Bottom


                    sideHorizontal(x, y, z, x + px3, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px16 - px3, y, z, x + px16, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x, y, z + px32 - px3, x + px3, z + px32, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px16 - px3, y, z + px32 - px3, x + px16, z + px32, event, sidesBottom, sidesBottom);


                    // Middle & Top


                    sideHorizontal(x + px3, y + px3, z, x + px16 - px3, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px3, y + px3, z + px32 - px3, x + px16 - px3, z + px32, event, sidesBottom, sidesBottom);

                    sideHorizontal(x, y + px3, z + px3, x + px16, z + px32 - px3, event, sidesBottom, sidesBottom);


                    sideHorizontal(x, y + px8, z, x + px16, z + px32, event, sidesTop, sidesTop);

                    // Vertical

                    // Edges

                    renderEdgeSides(x, y, z, px3, 1, event);
                    renderEdgeSides(x, y, z + px32 - px3, px3, 2, event);
                    renderEdgeSides(x + px16 - px3, y, z, px3, 3, event);
                    renderEdgeSides(x + px16 - px3, y, z + px32 - px3, px3, 4, event);

                    // Sides

                    sideVertical(x, y + px3, z, x + px16, y + px8, z, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z + px32, x + px16, y + px8, z + px32, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z, x, y + px8, z + px32, event, sidesBottom, sidesTop);
                    sideVertical(x + px16, y + px3, z, x + px16, y + px8, z + px32, event, sidesBottom, sidesTop);
                } else {
                    // Horizontal

                    // Bottom


                    sideHorizontal(x, y, z, x + px3, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x, y, z + px16 - px3, x + px3, z + px16, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px32 - px3, y, z, x + px32, z + px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px32 - px3, y, z + px16 - px3, x + px32, z + px16, event, sidesBottom, sidesBottom);


                    // Middle & Top


                    sideHorizontal(x, y + px3, z + px3, x + px3, z + px16 - px3, event, sidesBottom, sidesBottom);
                    sideHorizontal(x + px32 - px3, y + px3, z + px3, x + px32, z + px16 - px3, event, sidesBottom, sidesBottom);

                    sideHorizontal(x + px3, y + px3, z, x + px32 - px3, z + px16, event, sidesBottom, sidesBottom);


                    sideHorizontal(x, y + px8, z, x + px32, z + px16, event, sidesTop, sidesTop);

                    // Vertical

                    // Edges

                    renderEdgeSides(x, y, z, px3, 1, event);
                    renderEdgeSides(x + px32 - px3, y, z, px3, 3, event);
                    renderEdgeSides(x, y, z + px16 - px3, px3, 2, event);
                    renderEdgeSides(x + px32 - px3, y, z + px16 - px3, px3, 4, event);

                    // Sides

                    sideVertical(x, y + px3, z, x, y + px8, z + px16, event, sidesBottom, sidesTop);
                    sideVertical(x + px32, y + px3, z, x + px32, y + px8, z + px16, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z, x + px32, y + px8, z, event, sidesBottom, sidesTop);
                    sideVertical(x, y + px3, z + px16, x + px32, y + px8, z + px16, event, sidesBottom, sidesTop);
                }
            }

            // Resetting the Colors

            sidesTop.a = preSideTopA;
            sidesBottom.a = preSideBottomA;
            linesTop.a = preLineTopA;
            linesBottom.a = preLineBottomA;
        }

        // Render Utils

        private void renderEdgeLines(double x, double y, double z, double px3, int edge, Render3DEvent event) {
            // Horizontal

            if (edge != 2 && edge != 4) event.renderer.line(x, y, z, x + px3, y, z, linesBottom);
            if (edge != 3 && edge != 4) event.renderer.line(x, y, z, x, y, z + px3, linesBottom);

            if (edge != 1 && edge != 2) event.renderer.line(x + px3, y, z, x + px3, y, z + px3, linesBottom);
            if (edge != 1 && edge != 3) event.renderer.line(x, y, z + px3, x + px3, y, z + px3, linesBottom);

            // Vertical

            if (edge != 4) event.renderer.line(x, y, z, x, y + px3, z, linesBottom);
            if (edge != 2) event.renderer.line(x + px3, y, z, x + px3, y + px3, z, linesBottom);
            if (edge != 3) event.renderer.line(x, y, z + px3, x, y + px3, z + px3, linesBottom);
            if (edge != 1) event.renderer.line(x + px3, y, z + px3, x + px3, y + px3, z + px3, linesBottom);
        }

        private void renderEdgeSides(double x, double y, double z, double px3, int edge, Render3DEvent event) {
            // Horizontal

            if (edge != 4 && edge != 2) sideVertical(x, y, z, x + px3, y + px3, z, event, sidesBottom, sidesBottom);
            if (edge != 4 && edge != 3) sideVertical(x, y, z, x, y + px3, z + px3, event, sidesBottom, sidesBottom);
            if (edge != 1 && edge != 2) sideVertical(x + px3, y, z, x + px3, y + px3, z + px3, event, sidesBottom, sidesBottom);
            if (edge != 1 && edge != 3) sideVertical(x, y, z + px3, x + px3, y + px3, z + px3, event, sidesBottom, sidesBottom);
        }

        public void sideHorizontal(double x1, double y, double z1, double x2, double z2, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            side(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, event, bottomSideColor, topSideColor);
        }

        public void sideVertical(double x1, double y1, double z1, double x2, double y2, double z2, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            side(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, event, bottomSideColor, topSideColor);
        }

        private void side(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            event.renderer.quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, topSideColor, topSideColor, bottomSideColor, bottomSideColor);
        }
    }

    // [Enums] //

    public enum RenderMode{
        Off,
        Fade,
        Smooth
    }

    public enum CalculatingMode{
        Normal,
        ByDistance,
        ByRadius
    }

    public enum DamageMode{
        BestDamage,
        MostDamage
    }

    public enum SwapMode{
        Off,
        Normal,
        Silent,
        Inventory
    }
}
