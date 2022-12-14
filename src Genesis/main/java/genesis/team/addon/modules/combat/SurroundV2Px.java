package genesis.team.addon.modules.combat;

import genesis.team.addon.Genesis;
import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import genesis.team.addon.util.ProximaUtil.EntityUtil;
import genesis.team.addon.util.ProximaUtil.PlayerUtil;
import meteordevelopment.meteorclient.events.entity.player.*;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Burrow;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static genesis.team.addon.util.ProximaUtil.RenderUtil.*;


public class SurroundV2Px extends Module {
    public SurroundV2Px(){
        super(Genesis.Px, "surroundV2", "-");

    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> blockPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("Block placements per tick.").defaultValue(4).min(1).sliderMin(1).sliderMax(10).build());
    private final Setting<Boolean> obsidianCheck = sgGeneral.add(new BoolSetting.Builder().name("obsidian-check").description("Checks if obsidian in hotbar. If not, toggles.").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotating towards blocks being placed.").defaultValue(false).build());
    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder().name("only-on-ground").description("Works only when you standing on blocks.").defaultValue(true).build());
    private final Setting<Boolean> sneakOnly = sgGeneral.add(new BoolSetting.Builder().name("require-sneak").description("Only activate while you're sneaking.").defaultValue(false).build());
    private final Setting<Boolean> onlyOnFullBlock = sgGeneral.add(new BoolSetting.Builder().name("only-on-full-block").description("Only activate while you're sneaking.").defaultValue(false).build());
    private final Setting<Center> center = sgGeneral.add(new EnumSetting.Builder<Center>().name("center").description("Teleports you to the center of the block.").defaultValue(Center.Incomplete).build());
    private final Setting<Boolean> noInteract = sgGeneral.add(new BoolSetting.Builder().name("no-interact").description("Teleports you to the center of the block.").defaultValue(true).build());
    private final Setting<Boolean> stopMoving = sgGeneral.add(new BoolSetting.Builder().name("stop-moving").description("Teleports you to the center of the block.").defaultValue(true).build());
    private final Setting<Boolean> noDeSync = sgGeneral.add(new BoolSetting.Builder().name("no-DeSync").description("Teleports you to the center of the block.").defaultValue(true).build());

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final Setting<Boolean> packetPlace = sgPlace.add(new BoolSetting.Builder().name("packet-place").description("Places blocks with packets and lever resistance.").defaultValue(true).build());
    private final Setting<Boolean> packetSneak = sgPlace.add(new BoolSetting.Builder().name("packet-sneak").description("Places blocks with packets and lever resistance.").defaultValue(true).build());
    private final Setting<Boolean> doublePlace = sgPlace.add(new BoolSetting.Builder().name("double-place").description("Checks if obsidian in hotbar. If not, toggles.").defaultValue(false).build());
    private final Setting<Boolean> burrow = sgPlace.add(new BoolSetting.Builder().name("burrow").description("Checks if obsidian in hotbar. If not, toggles.").defaultValue(false).build());
    private final Setting<Boolean> antiFall = sgPlace.add(new BoolSetting.Builder().name("anti-fall").description("Prevents you from falling when 1 blocks below got broken.").defaultValue(true).build());
    private final Setting<Double> upDistance = sgPlace.add(new DoubleSetting.Builder().name("up-distance").description(".").defaultValue(0.1).min(0.04).sliderMax(0.30).visible(antiFall::get).build());
    private final Setting<Boolean> allBlocks = sgPlace.add(new BoolSetting.Builder().name("all-blocks").description("Prevents you from falling when 1 blocks below got broken.").defaultValue(false).build());
    private final Setting<List<Block>> blocks = sgPlace.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final Setting<Boolean> antiBedAura = sgSafety.add(new BoolSetting.Builder().name("anti-bed-aura").description("Breaks dangerous crystals automaticly.").defaultValue(true).build());
    private final Setting<Boolean> putOutTheFire = sgSafety.add(new BoolSetting.Builder().name("put-out-the-fire").description("Breaks dangerous crystals automaticly.").defaultValue(true).build());
    private final Setting<Boolean> anvilBreaker = sgSafety.add(new BoolSetting.Builder().name("anvil-breaker").description("Breaks dangerous crystals automaticly.").defaultValue(true).build());
    private final Setting<Boolean> bedBreaker = sgSafety.add(new BoolSetting.Builder().name("bed-breaker").description("Breaks dangerous crystals automaticly.").defaultValue(true).build());
    private final Setting<Boolean> buttonBreaker = sgSafety.add(new BoolSetting.Builder().name("button-breaker").description("Breaks dangerous crystals automaticly.").defaultValue(true).build());
    private final Setting<Boolean> bypassBreak = sgSafety.add(new BoolSetting.Builder().name("bypass-break").description("Breaks dangerous crystals automaticly.").defaultValue(true).visible(()-> anvilBreaker.get() || buttonBreaker.get()).build());
    private final Setting<List<Block>> breakBlocks = sgSafety.add(new BlockListSetting.Builder().name("break-blocks").description("What blocks to use for surround.").defaultValue(new ArrayList<>()).visible(buttonBreaker::get).build());
    private final Setting<Boolean> crystalBreaker = sgSafety.add(new BoolSetting.Builder().name("crystal-breaker").description("Breaks dangerous crystals automaticly.").defaultValue(true).build());
    private final Setting<Integer> breakDelay = sgSafety.add(new IntSetting.Builder().name("break-delay").description("Block placements per tick.").defaultValue(0).min(0).sliderMax(10).build());
    private final Setting<Double> breakRange = sgSafety.add(new DoubleSetting.Builder().name("crystal-break-range").description("The range crystals will be broken.").defaultValue(1.0).sliderMax(7.0).visible(crystalBreaker::get).build());
    private final Setting<Boolean> placeObby = sgSafety.add(new BoolSetting.Builder().name("place-obby").description("Places blocks on crystals.").defaultValue(true).visible(crystalBreaker::get).build());
    private final Setting<Double> placeRange = sgSafety.add(new DoubleSetting.Builder().name("place-obby-range").description("The range crystals will be broken.").defaultValue(1.0).sliderMax(7.0).visible(()->crystalBreaker.get() || placeObby.get()).build());
    private final Setting<Boolean> onlyLegY = sgSafety.add(new BoolSetting.Builder().name("only-leg-Y").description("Places blocks on crystals.").defaultValue(true).visible(crystalBreaker::get).build());

    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final Setting<Boolean> disableJump = sgToggle.add(new BoolSetting.Builder().name("toggle-on-jump").description("Disable if you jump.").defaultValue(true).build());
    private final Setting<Boolean> disableYchange = sgToggle.add(new BoolSetting.Builder().name("toggle-on-y-change").description("Disable if your Y coord changes.").defaultValue(false).build());
    private final Setting<Boolean> disableOnChorus = sgToggle.add(new BoolSetting.Builder().name("toggle-on-chorus").description("").defaultValue(true).build());
    private final Setting<Boolean> disableOnPearl = sgToggle.add(new BoolSetting.Builder().name("toggle-on-pearl").description("").defaultValue(true).build());
    private final Setting<Boolean> stopPlacingOnEat = sgToggle.add(new BoolSetting.Builder().name("stop-placing-on-eat").description("").defaultValue(false).build());
    private final Setting<Boolean> stopPlacingOnDrink = sgToggle.add(new BoolSetting.Builder().name("stop-placing-on-drink").description("").defaultValue(false).build());
    private final Setting<Boolean> stopPlacingOnMine = sgToggle.add(new BoolSetting.Builder().name("stop-placing-on-mine").description("").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Render surround.").defaultValue(false).build());
    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Which way to render surround.").defaultValue(RenderMode.Fade).build());
    private final Setting<Integer> fadeTick = sgRender.add(new IntSetting.Builder().name("fade-tick").description("The slot auto move moves beds to.").defaultValue(8).visible(() -> renderMode.get() == RenderMode.Fade).build());
    private final Setting<Boolean> alwaysRender = sgRender.add(new BoolSetting.Builder().name("always").description("Render surround blocks always.").defaultValue(false).visible(render::get).visible(() -> renderMode.get() != RenderMode.Fade).build());
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Render swing.").defaultValue(false).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());
    private BlockPos bottomPos;
    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    ArrayList<Vec3d> poses = new ArrayList<>(){{add(new Vec3d(0,-1,0));add(new Vec3d(1,0,0));add(new Vec3d(0,0,1));add(new Vec3d(0,0,-1));add(new Vec3d(-1,0,0));}};
    ArrayList<Vec3d> doublePoses = new ArrayList<>(){{add(new Vec3d(1,1,0));add(new Vec3d(0,1,1));add(new Vec3d(0,1,-1));add(new Vec3d(-1,1,0));}};
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, -1, 0);
    private Direction direction;
    private int breakTimer;

    public enum RenderMode {
        Fade,
        Default
    }

    public enum Center {
        Never,
        OnActivate,
        Incomplete,
        Always
    }

    private ArrayList<Vec3d> getSurrDesign() {
        ArrayList<Vec3d> surrDesign = new ArrayList<Vec3d>(poses);
        if (doublePlace.get()) surrDesign.addAll(doublePoses);
        return surrDesign;
    }

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.RESPAWN_ANCHOR ||
            block == Blocks.ANVIL;
    }

    @EventHandler
    private void onBlockPlace(PlaceBlockEvent event){
        if (noDeSync.get()){
            BlockPos ppos = mc.player.getBlockPos();
            for (int i = 0; i < getSurrDesign().toArray().length; i++) {
                BlockPos bb = ppos.add(getSurrDesign().get(i).x, getSurrDesign().get(i).y, getSurrDesign().get(i).z);
                if (event.blockPos.equals(bb) && !mc.player.getAbilities().creativeMode){
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, event.blockPos, Direction.UP));
                }
            }
        }
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();
        if ((mc.player.getBlockPos().equals(pos) || mc.player.getBlockPos().up().equals(pos)) && BlockUtil.isClickable(BlockUtil.getBlock(pos)) && noInteract.get()) event.cancel();
    }

    @EventHandler
    private void blockBreak(BreakBlockEvent event){
        if (antiFall.get()){
            if (event.blockPos.equals(bottomPos)) mc.player.setVelocity(0, upDistance.get(), 0);
        }
    }

    @EventHandler
    private void onDeath(OpenScreenEvent event){
        if (!(event.screen instanceof DeathScreen)) return;
        toggle();
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event){
        if (stopMoving.get()){
            ((IVec3d)event.movement).set(0.0D, mc.player.getVelocity().getY(), 0.0D);
        }
    }

    @Override
    public void onActivate() {
        if (center.get() == Center.OnActivate) PlayerUtils.centerPlayer();
        if (burrow.get()) Modules.get().get(Burrow.class).toggle();
        bottomPos = mc.player.getBlockPos().down();
        if (onlyOnFullBlock.get() && !mc.world.getBlockState(bottomPos).isFullCube(mc.world, bottomPos) && !BlockUtil.isAir(bottomPos) && mc.player.getY() > bottomPos.getY()){
            bottomPos = bottomPos.up();
        }
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }
    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        if (PlayerUtils.shouldPause(stopPlacingOnMine.get(), stopPlacingOnEat.get(), stopPlacingOnDrink.get())) return;
        doIt();
        if (antiBedAura.get()){
            BlockPos ppos = mc.player.getBlockPos();
            if (onlyOnFullBlock.get() && !mc.world.getBlockState(ppos).isFullCube(mc.world, ppos) && !BlockUtil.isAir(ppos) && mc.player.getY() > ppos.getY() + 0.3){
                ppos = ppos.up();
            }
            for (int i = 0; i < getSurrDesign().toArray().length; i++) {
                BlockPos bb = ppos.add(getSurrDesign().get(i).x, getSurrDesign().get(i).y, getSurrDesign().get(i).z);
                if (BlockUtil.getBlock(bb) instanceof BedBlock){
                    boolean wasSneaking = mc.player.isSneaking();
                    if (wasSneaking) mc.player.setSneaking(false);
                    assert mc.interactionManager != null;
                    mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, bb, false));
                    mc.player.setSneaking(wasSneaking);
                }
            }
        }
        if (putOutTheFire.get()){
            BlockPos ppos = mc.player.getBlockPos();
            if (onlyOnFullBlock.get() && !mc.world.getBlockState(ppos).isFullCube(mc.world, ppos) && !BlockUtil.isAir(ppos) && mc.player.getY() > ppos.getY()){
                ppos = ppos.up();
            }
            for (int i = 0; i < getSurrDesign().toArray().length; i++) {
                BlockPos bb = ppos.add(getSurrDesign().get(i).x, getSurrDesign().get(i).y, getSurrDesign().get(i).z);
                if (mc.world.getBlockState(bb).getBlock() == Blocks.FIRE || mc.world.getBlockState(bb).getBlock() == Blocks.GRASS || mc.world.getBlockState(bb).getBlock() == Blocks.FERN) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bb, Direction.DOWN));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bb, Direction.DOWN));
                }
            }
        }
        if ((anvilBreaker.get() && (BlockUtil.getBlock(mc.player.getBlockPos()) == Blocks.ANVIL || BlockUtil.getBlock(mc.player.getBlockPos()) == Blocks.CHIPPED_ANVIL || BlockUtil.getBlock(mc.player.getBlockPos()) == Blocks.DAMAGED_ANVIL)) || (BlockUtil.getBlock(mc.player.getBlockPos()) instanceof BedBlock && bedBreaker.get())){
            int slot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE).slot();
            if (mc.player.getAbilities().creativeMode) slot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(slot, true);
            if (bypassBreak.get()){
                if (direction == null) direction = Direction.UP;
                mc.interactionManager.updateBlockBreakingProgress(mc.player.getBlockPos(), direction);
                mc.interactionManager.cancelBlockBreaking();
            }
            BlockUtils.breakBlock(mc.player.getBlockPos(), swing.get());
        }
        if (buttonBreaker.get()){
            BlockPos ppos = mc.player.getBlockPos();
            if (onlyOnFullBlock.get() && !mc.world.getBlockState(ppos).isFullCube(mc.world, ppos) && !BlockUtil.isAir(ppos) && mc.player.getY() > ppos.getY() + 0.3){
                ppos = ppos.up();
            }
            for (int i = 0; i < getSurrDesign().toArray().length; i++) {
                BlockPos bb = ppos.add(getSurrDesign().get(i).x, getSurrDesign().get(i).y, getSurrDesign().get(i).z);
                if (breakBlocks.get().contains(BlockUtil.getBlock(bb))) {
                    int slot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE).slot();
                    if (mc.player.getAbilities().creativeMode) slot = mc.player.getInventory().selectedSlot;
                    InvUtils.swap(slot, true);
                    if (bypassBreak.get()){
                        if (direction == null) direction = Direction.UP;
                        mc.interactionManager.updateBlockBreakingProgress(bb, direction);
                        mc.interactionManager.cancelBlockBreaking();
                    }
                    BlockUtils.breakBlock(bb, swing.get());
                }
            }
        }
        BlockPos crystalPos = null;
        Entity crystalEntity = null;
        if (crystalBreaker.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity) {
                    crystalEntity = entity;
                }
            }
        }
        if (crystalEntity != null){
            if (isDangerousCrystal(crystalEntity.getBlockPos())) {
                if (onlyLegY.get() && crystalEntity.getY() != mc.player.getBlockPos().getY()) return;
                if (breakTimer <= 0){
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
                    crystalPos = crystalEntity.getBlockPos();
                    breakTimer = breakDelay.get();
                }else breakTimer--;
            }
        }
        if (crystalPos != null){
            if (placeObby.get() && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), crystalPos) <= placeRange.get()) {
                FindItemResult obi = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
                int prevSlot = mc.player.getInventory().selectedSlot;
                if (!obi.found()){
                    if (allBlocks.get()) obi = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem);
                }
                if (packetSneak.get()) if (packetSneak.get()) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                if (packetPlace.get()) {
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(crystalPos), Rotations.getPitch(crystalPos));
                    InvUtils.swap(obi.slot(), false);
                    PlayerUtil.placeBlock(crystalPos, obi.slot(), Hand.MAIN_HAND, false);
                    mc.player.getInventory().selectedSlot = prevSlot;
                }
                else BlockUtils.place(crystalPos,  obi, rotate.get(), 0, false);
            }
            if (swing.get()) { mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); }
        }
    }

    private ArrayList<BlockPos> getLeverPos(BlockPos sur){
        return new ArrayList<>(){{
            add(sur.down());
            add(sur.west());
            add(sur.east());
            add(sur.south());
            add(sur.north());
        }};
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (bypassBreak.get()){
            if (BlockUtil.getBlock(event.blockPos) == Blocks.ANVIL || BlockUtil.getBlock(event.blockPos) == Blocks.CHIPPED_ANVIL || BlockUtil.getBlock(event.blockPos) == Blocks.DAMAGED_ANVIL || breakBlocks.get().contains(BlockUtil.getBlock(event.blockPos))){
                direction = event.direction;
                blockPos.set(event.blockPos);
                cum();
                event.cancel();
            }
        }
    }

    private void cum() {
        if (!wontMine()) {
            if (rotate.get()) {
                if (direction == null) direction = Direction.UP;
                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));
                Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
            }
        }
    }

    private boolean wontMine() {
        return blockPos.getY() == -1 || !BlockUtils.canBreak(blockPos);
    }

    @EventHandler
    private void finishUsing(FinishUsingItemEvent event){
        if (disableOnChorus.get() && event.itemStack.getItem() == Items.CHORUS_FRUIT){
            toggle();
            return;
        }
    }

    @EventHandler
    private void onUsing(InteractItemEvent event){
        if (event.hand != null){
            if (disableOnPearl.get() && mc.player.getStackInHand(event.hand).getItem() == Items.ENDER_PEARL){
                toggle();
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Post event) {
        if (PlayerUtils.shouldPause(stopPlacingOnMine.get(), stopPlacingOnEat.get(), stopPlacingOnDrink.get())) return;
        doIt();
    }
    private void doIt(){
        int bpt = 0;
        if (center.get() == Center.Always) PlayerUtils.centerPlayer();
        FindItemResult obi = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!obi.found()){
            if (allBlocks.get()) obi = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem);
        }
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if ((disableJump.get() && (mc.options.jumpKey.isPressed() || mc.player.input.jumping)) || (disableYchange.get() && mc.player.prevY < mc.player.getY())) { toggle(); return; }
        if (sneakOnly.get() && !mc.options.sneakKey.isPressed()) return;
        if (obsidianCheck.get()) { if (!obi.found()) { error("Cannot find obsidian in hotbar."); toggle(); return; }}
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
        int safe = 0;
        if (EntityUtil.isInHole(false ,mc.player)) safe = 4;

        int prevSlot = mc.player.getInventory().selectedSlot;
        if (mc.player == null) return;
        BlockPos ppos = mc.player.getBlockPos();
        if (onlyOnFullBlock.get() && !mc.world.getBlockState(ppos).isFullCube(mc.world, ppos) && !BlockUtil.isAir(ppos) && mc.player.getY() > ppos.getY() + 0.3){
            ppos = ppos.up();
        }
        boolean complete = safe == 4;
        if (!complete && center.get() == Center.Incomplete) PlayerUtils.centerPlayer();
        for (int i = 0; i < getSurrDesign().toArray().length; i++){
            if (bpt >= blockPerTick.get()) return;
            BlockPos bb = ppos.add(getSurrDesign().get(i).x, getSurrDesign().get(i).y, getSurrDesign().get(i).z);
            if (BlockUtil.isAir(bb) || BlockUtil.getBlock(bb) == Blocks.LAVA || BlockUtil.getBlock(bb) == Blocks.WATER || BlockUtil.getBlock(bb) == Blocks.FIRE){
                if (packetSneak.get()) {
                    for (BlockPos leverPos : getLeverPos(bb)){
                        if (BlockUtil.getBlock(leverPos) == Blocks.LEVER){
                            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                        }
                    }
                }
                if (packetPlace.get()) {
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(bb), Rotations.getPitch(bb));
                    InvUtils.swap(obi.slot(), false);
                    PlayerUtil.placeBlock(bb, Hand.MAIN_HAND, false);
                    mc.player.getInventory().selectedSlot = prevSlot;
                } else BlockUtils.place(bb, obi, rotate.get(), 0, false);
                bpt++;
                if (swing.get()) { mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND)); }
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                renderBlocks.add(renderBlockPool.get().set(bb, fadeTick.get()));
            }
        }
    }
    private boolean isDangerousCrystal(BlockPos bp) {
        BlockPos ppos = mc.player.getBlockPos();
        for (int i = 0; i < getSurrDesign().toArray().length; i++){
            BlockPos bb = ppos.add(getSurrDesign().get(i).x, getSurrDesign().get(i).y, getSurrDesign().get(i).z);
            if (!bp.equals(bb) && BlockUtil.distance(bb, bp) <= (breakRange.get())) return true;
        }
        return false;
    }
    @EventHandler(priority = EventPriority.LOW)
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            if (renderMode.get() == RenderMode.Fade) {
                renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
                renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get()));
            } else {
                BlockPos bruh = mc.player.getBlockPos();
                for (Vec3d b: getSurrDesign()) {
                    BlockPos bb = bruh.add(b.x, b.y, b.z);
                    if (BlockUtil.getBlock(bb) == Blocks.AIR) renderD(event, bb, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get());
                    if (alwaysRender.get()) renderD(event, bb, sideColor.get(), sideColor2.get(), lineColor.get(), lineColor2.get(), shapeMode.get(), width.get());
                }
            }
        }
    }

    private void renderD(Render3DEvent event, BlockPos pos, Color side1, Color side2, Color line1, Color line2, ShapeMode shapeMode, int width){
        if (shapeMode.equals(ShapeMode.Lines) || shapeMode.equals(ShapeMode.Both)){
            S(event, pos, 0.98,0, 0.02, line1, line2);
            TAB(event, pos, 0.98, 0.02, true,true, line1, line2);

            if (width == 2){
                S(event, pos, 0.96,0, 0.04, line1, line2);
                TAB(event, pos, 0.96, 0.04,true, true, line1, line2);
            }
            if (width == 3){
                S(event, pos, 0.94,0, 0.06, line1, line2);
                TAB(event, pos, 0.94, 0.06, true, true,line1, line2);
            }
            if (width == 4){
                S(event, pos, 0.92,0, 0.08, line1, line2);
                TAB(event, pos, 0.92, 0.08,true, true, line1, line2);
            }
        }
        if (shapeMode.equals(ShapeMode.Sides) || shapeMode.equals(ShapeMode.Both)){
            FS(event, pos,0,true, true, side1, side2);
        }
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos, int ticks) {
            pos.set(blockPos);
            this.ticks = ticks;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color side1, Color side2, Color line1, Color line2, ShapeMode shapeMode, int width) {
            int preSideA = side1.a;
            int preSideB = side2.a;
            int preLineA = line1.a;
            int preLineB = line2.a;

            side1.a *= (double) ticks / 10;
            side2.a *= (double) ticks / 10;
            line1.a *= (double) ticks / 10;
            line2.a *= (double) ticks / 10;

            if (shapeMode.equals(ShapeMode.Lines) || shapeMode.equals(ShapeMode.Both)){
                S(event, pos, 0.98,0, 0.02, line1, line2);
                TAB(event, pos, 0.98, 0.02, true,true, line1, line2);

                if (width == 2){
                    S(event, pos, 0.96,0, 0.04, line1, line2);
                    TAB(event, pos, 0.96, 0.04,true, true, line1, line2);
                }
                if (width == 3){
                    S(event, pos, 0.94,0, 0.06, line1, line2);
                    TAB(event, pos, 0.94, 0.06, true, true,line1, line2);
                }
                if (width == 4){
                    S(event, pos, 0.92,0, 0.08, line1, line2);
                    TAB(event, pos, 0.92, 0.08,true, true, line1, line2);
                }
            }
            if (shapeMode.equals(ShapeMode.Sides) || shapeMode.equals(ShapeMode.Both)){
                FS(event, pos,0,true, true, side1, side2);
            }

            side1.a = preSideA;
            side2.a = preSideB;
            line1.a = preLineA;
            line2.a = preLineB;
        }
    }
}
