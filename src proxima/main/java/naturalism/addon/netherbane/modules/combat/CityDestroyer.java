package naturalism.addon.netherbane.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.PlaceBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import naturalism.addon.netherbane.utils.BlockUtil;
import naturalism.addon.netherbane.utils.EntityUtil;
import naturalism.addon.netherbane.utils.Timer;
import naturalism.addon.netherbane.NetherBane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static naturalism.addon.netherbane.utils.RenderUtil.*;

public class CityDestroyer extends Module {
    public CityDestroyer(){
        super(NetherBane.COMBATPLUS, "city-destroyer", "Automatically cities a target by mining the nearest obsidian next to them.");

    }

    public enum MineMode {Vanilla, Packet}
    public enum SwapMode {Normal, Silent}
    public enum PlaceMode {Default, Progress}
    public enum PlacePriority {Consistently, Distance}
    public enum Swing { FULL, PACKET, NONE }

    private final SettingGroup sgDefault = settings.createGroup("Default");
    private final Setting<Double> enemyRange = sgDefault.add(new DoubleSetting.Builder().name("enemy-range").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Double> cityRange = sgDefault.add(new DoubleSetting.Builder().name("city-range").description("The radius in which city get targeted.").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Boolean> holdPos = sgDefault.add(new BoolSetting.Builder().name("hold-city").description("Hold city pos.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(true).build());

    private final SettingGroup sgMining = settings.createGroup("Mining");
    private final Setting<MineMode> mineMode = sgMining.add(new EnumSetting.Builder<MineMode>().name("mining").description("Block breaking method").defaultValue(MineMode.Packet).build());
    private final Setting<Boolean> instant = sgMining.add(new BoolSetting.Builder().name("instant-break").description("Instant break").defaultValue(false).build());
    private final Setting<Boolean> bypass = sgMining.add(new BoolSetting.Builder().name("bypass-break").description("Tries to circumvent the restrictions from the instant mine.").defaultValue(false).visible(instant::get).build());
    private final Setting<Integer> tickDelay = sgMining.add(new IntSetting.Builder().name("break-delay").description("The delay between breaks.").defaultValue(0).min(0).sliderMax(20).visible(instant::get).build());
    private final Setting<Boolean> openestCity = sgMining.add(new BoolSetting.Builder().name("openest-city").description("Selects the most convenient block for punching.").defaultValue(true).build());
    private final Setting<Boolean> breakInterfering = sgMining.add(new BoolSetting.Builder().name("break-interfering").description("Breaks interfering blocks to break through the surround.").defaultValue(true).build());
    private final Setting<Boolean> onIron = sgMining.add(new BoolSetting.Builder().name("on-iron").description("Breaks interfering blocks to break through the surround.").defaultValue(false).visible(breakInterfering::get).build());
    private final Setting<Boolean> breakBurrow = sgMining.add(new BoolSetting.Builder().name("break-burrow").description("Breaks the burrow block first.").defaultValue(true).build());
    private final Setting<List<Block>> burrowBlocks = sgMining.add(new BlockListSetting.Builder().name("burrow-blocks").description("Blocks can be broken on burrow pos.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).build());
    private final Setting<Boolean> ignoreYourOwnSurround = sgMining.add(new BoolSetting.Builder().name("ignore-your-own-surround").description("Ignore the block if your enemy has a common city.").defaultValue(true).build());
    private final Setting<Boolean> ignoreFriendSurround = sgMining.add(new BoolSetting.Builder().name("ignore-friend-surround").description("Ignore the block if your enemy has a common city.").defaultValue(true).build());
    private final Setting<Boolean> ignoreBlocks = sgMining.add(new BoolSetting.Builder().name("ignore-blocks").description("Do not break the surround if this block is selected in the list.").defaultValue(true).build());
    private final Setting<List<Block>> ignoreBlocksList = sgMining.add(new BlockListSetting.Builder().name("ignore-block-list").description("List of ignored blocks.").defaultValue(Collections.singletonList(Blocks.STONE_BUTTON)).build());

    private final SettingGroup sgSwap = settings.createGroup("Swap");
    private final Setting<Keybind> ironPickaxe = sgSwap.add(new KeybindSetting.Builder().name("iron-pickaxe").description("Change the pickaxe slot to an iron one when pressing the button.").defaultValue(Keybind.fromKey(8)).build());
    public final Setting<Boolean> autoSwap = sgSwap.add(new BoolSetting.Builder().name("autoSwap").description("Automatically picks up the pickaxe when breaking.").defaultValue(true).build());
    private final Setting<SwapMode> swapMode = sgSwap.add(new EnumSetting.Builder<SwapMode>().name("swap-mode").description("Which way to swap.").defaultValue(SwapMode.Silent).build());
    public final Setting<Double> swapDelay = sgSwap.add(new DoubleSetting.Builder().name("swapDelay").description("The delay between swap.").defaultValue(0).min(0.0).sliderMax(10).build());

    private final SettingGroup sgSupport = settings.createGroup("Support");
    private final Setting<Boolean> support = sgSupport.add(new BoolSetting.Builder().name("support-place").description("If there is no block below a city block it will place one before mining.").defaultValue(true).build());
    private final Setting<Boolean> crystal = sgSupport.add(new BoolSetting.Builder().name("crystal-place").description("Place crystals for punching the surround.").defaultValue(false).build());
    private final Setting<Boolean> crystalSupport = sgSupport.add(new BoolSetting.Builder().name("crystal-support").description("If there is no block below a crystal place block it will place one before placing.").defaultValue(true).visible(crystal::get).build());
    private final Setting<PlacePriority> placePriority = sgSupport.add(new EnumSetting.Builder<PlacePriority>().name("place-priority").description("Place priority").defaultValue(PlacePriority.Distance).visible(crystal::get).build());
    private final Setting<Boolean> nearTheCity = sgSupport.add(new BoolSetting.Builder().name("near-the-city").description("Place crystals near the city.").defaultValue(false).visible(crystal::get).build());
    private final Setting<Boolean> diagonal = sgSupport.add(new BoolSetting.Builder().name("diagonal").description("Place crystals diagonally.").defaultValue(false).visible(crystal::get).build());
    private final Setting<Boolean> under = sgSupport.add(new BoolSetting.Builder().name("under").description("Place crystals under city block.").defaultValue(false).visible(crystal::get).build());
    private final Setting<Boolean> up = sgSupport.add(new BoolSetting.Builder().name("up").description("Place crystals up city block.").defaultValue(false).visible(crystal::get).build());
    private final Setting<PlaceMode> placeMode = sgSupport.add(new EnumSetting.Builder<PlaceMode>().name("crystal-place-mode").description("Which way to place crystal").defaultValue(PlaceMode.Progress).visible(() -> crystal.get() && !instant.get()).build());
    private final Setting<Double> breakingProgress = sgSupport.add(new DoubleSetting.Builder().name("breaking-progress").description("The progress of breaking after which the crystal is placed.").defaultValue(0.9).min(0).sliderMax(1).visible(() ->  (placeMode.get() == PlaceMode.Progress) && (crystal.get()) && !instant.get()  && mineMode.get().equals(MineMode.Vanilla)).build());
    private final Setting<Double> timer = sgSupport.add(new DoubleSetting.Builder().name("timer").description("The progress of breaking after which the crystal is placed.").defaultValue(700).min(0).sliderMax(3000).visible(() ->  (placeMode.get() == PlaceMode.Progress) && (crystal.get()) && !instant.get() && mineMode.get().equals(MineMode.Packet)).build());
    private final Setting<Boolean> breakCrystal = sgSupport.add(new BoolSetting.Builder().name("break-crystal").description("Breaking the crystal for the explosion of item.").defaultValue(true).visible(crystal::get).build());
    private final Setting<Boolean> placeCrystalOnSurPos = sgSupport.add(new BoolSetting.Builder().name("crystal-on-sur-pos").description("Breaking the crystal for the explosion of item.").defaultValue(true).visible(crystal::get).build());

    private final SettingGroup sgToggleAndPause = settings.createGroup("Toggle and Pause");
    private final Setting<Boolean> selfToggle = sgToggleAndPause.add(new BoolSetting.Builder().name("self-toggle").description("Automatically toggles off after activation.").defaultValue(true).build());
    private final Setting<Boolean> stopPlacingOnEat = sgToggleAndPause.add(new BoolSetting.Builder().name("stop-placing-on-eat").description("Stop-placing-on-eat.").defaultValue(false).build());
    private final Setting<Boolean> stopPlacingOnDrink = sgToggleAndPause.add(new BoolSetting.Builder().name("stop-placing-on-drink").description("Stop-placing-on-drink.").defaultValue(false).build());
    private final Setting<Boolean> stopPlacingOnMine = sgToggleAndPause.add(new BoolSetting.Builder().name("stop-placing-on-mine").description("Stop-placing-on-mine.").defaultValue(false).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Swing> swing = sgMining.add(new EnumSetting.Builder<Swing>().name("swing").description("Block breaking method").defaultValue(Swing.PACKET).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the current block being mined.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder().name("side-color-2").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder().name("line-color-2").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());
    private final Setting<Integer> width = sgRender.add(new IntSetting.Builder().name("width").defaultValue(1).min(1).max(5).sliderMin(1).sliderMax(4).build());
    private final Setting<Boolean> renderProgress = sgRender.add(new BoolSetting.Builder().name("render-progress").description("Renders the current block being mined.").defaultValue(true).build());
    private final Setting<Double> progressTextScale = sgRender.add(new DoubleSetting.Builder().name("text-scale").description("How big the damage text should be.").defaultValue(1.25).min(1).sliderMax(4).visible(renderProgress::get).build());
    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder().name("text-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());

    private PlayerEntity target;
    private BlockPos holdCityPos;
    private BlockPos holdMinePos;
    private BlockPos crystalPos;
    private final Timer instantTimer = new Timer();
    private final Timer swapTimer = new Timer();
    private final Timer packetTimer = new Timer();
    private static BlockPos minePos;
    private Direction direction;
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, -1, 0);
    private final Vec3 vec3 = new Vec3();
    private final Vec3 vec32 = new Vec3();
    private boolean isIron;

    public static ArrayList<BlockPos> surround = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
    }};
    public static ArrayList<BlockPos> diagonalArray = new ArrayList<>() {{
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(1, 0, -1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(-1, 0, -1));
    }};

    @Override
    public void onDeactivate() {
        minePos = null;
        holdMinePos = null;
        holdCityPos = null;
    }

    @Override
    public void onActivate() {
        if (mineMode.get().equals(MineMode.Packet) && placeMode.get().equals(PlaceMode.Progress)){
            packetTimer.reset();
        }

        BlockPos[] array = new BlockPos[]{new BlockPos(1,1,1), new BlockPos(1,2,3)};
    }

    @EventHandler
    private void onBlockBreak(BreakBlockEvent event){
        if (event.blockPos.equals(minePos)){
            if (mineMode.get().equals(MineMode.Packet) && placeMode.get().equals(PlaceMode.Progress)){
                packetTimer.reset();
            }
            if (selfToggle.get()){
                toggle();
                return;
            }
        }
    }

    @EventHandler
    private void onBlockPlace(PlaceBlockEvent event){
        if (event.blockPos.equals(minePos)){
            if (mineMode.get().equals(MineMode.Packet) && placeMode.get().equals(PlaceMode.Progress)){
                packetTimer.reset();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        if (PlayerUtils.shouldPause(stopPlacingOnMine.get(), stopPlacingOnEat.get(), stopPlacingOnDrink.get())) return;
        target = TargetUtils.getPlayerTarget(enemyRange.get(), SortPriority.LowestDistance);
        if (target == null) return;

        BlockPos ppos = target.getBlockPos();

        ArrayList<BlockPos> cityList = new ArrayList<>();
        ArrayList<BlockPos> mineList = new ArrayList<>();

        if (breakBurrow.get() && EntityUtil.isBurrowed(target) && burrowBlocks.get().contains(BlockUtil.getBlock(ppos))) mineList.add(ppos);

        for (BlockPos b : surround) {
            BlockPos pos = ppos.add(b);
            if (BlockUtils.canBreak(pos) && !BlockUtil.isAir(pos) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos) <= cityRange.get()){
                if (ignoreYourOwnSurround.get() && BlockUtil.getSurroundPos(mc.player).contains(pos)) continue;
                try {
                    if (ignoreFriendSurround.get() && BlockUtil.getSurroundPos(getFriendsInRange().get(0)).contains(pos)) continue;
                }catch (IndexOutOfBoundsException ignored){}
                if (ignoreBlocks.get() && ignoreBlocksList.get().contains(BlockUtil.getBlock(pos))) continue;
                cityList.add(pos);
            }
        }
        cityList.sort(Comparator.comparing(blockPos1 -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos1)));
        if (cityList.isEmpty()) return;
        if (minePos != holdCityPos){
            if (instant.get() && !bypass.get()) startInsta(holdCityPos);
        }
        if (openestCity.get()) cityList.sort(Comparator.comparing(this::getOpenPos));
        if (holdCityPos == null) holdCityPos = cityList.get(0);
        if (!holdPos.get()) holdCityPos = cityList.get(0);
        if (breakInterfering.get()){
            if (getOpenPos(holdCityPos) == 3 && !mineList.contains(ppos)){
                mineList.addAll(getFullSurrPos(holdCityPos));
                mineList.sort(Comparator.comparing(blockPos1 -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos1)));
                if (!mineList.isEmpty()){
                    holdMinePos = mineList.get(0);
                }
            }else holdMinePos = null;
        }
        if (ironPickaxe.get().isPressed() && !onIron.get() && breakInterfering.get()){
            mineList.clear();
            holdMinePos = null;
        }
        if (mineList.contains(ppos)) minePos = ppos;
        else if (holdMinePos != null) minePos = holdMinePos;
        else if (holdCityPos != null) minePos = holdCityPos;

        if (BlockUtil.isAir(minePos) && placeCrystalOnSurPos.get()){
            Box box = new Box(minePos);

            if (crystal.get() && !EntityUtils.intersectsWithEntity(box, entity -> entity instanceof EndCrystalEntity || entity instanceof ItemEntity) && minePos == holdCityPos){
                placeCrystal(minePos.down());
                crystalPos = minePos;
            }
        }

        if (!BlockUtil.isAir(minePos) && BlockUtils.canBreak(minePos)) {
            if (support.get() && BlockUtil.isAir(minePos.down())) placeSupportBlock(minePos.down());
            Box box = new Box(minePos);
            if (crystal.get() && !EntityUtils.intersectsWithEntity(box, entity -> entity instanceof EndCrystalEntity) && minePos == holdCityPos){
                ArrayList<BlockPos> crystalList = new ArrayList<>();
                if (nearTheCity.get()){
                    switch (placePriority.get()){
                        case Consistently -> {
                            crystalList = getCrystalPos(minePos);
                            crystalList.sort(Comparator.comparing(blockPos1 -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos1)));;
                        }
                        case Distance -> crystalList.addAll(getCrystalPos(minePos));
                    }
                }
                if (diagonal.get()){
                    switch (placePriority.get()){
                        case Consistently -> {
                            if (crystalList.isEmpty()){
                                crystalList = getDiagonalCrystalPos(minePos);
                                crystalList.sort(Comparator.comparing(blockPos1 -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos1)));;
                            }
                        }
                        case Distance -> crystalList.addAll(getDiagonalCrystalPos(minePos));
                    }
                }
                if (under.get() && crystalList.isEmpty()){
                    switch (placePriority.get()){
                        case Consistently -> {
                            if (crystalList.isEmpty()){
                                crystalList = getUnderCrystalPos(minePos);
                                crystalList.sort(Comparator.comparing(blockPos1 -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos1)));;
                            }
                        }
                        case Distance -> crystalList.addAll(crystalList = getUnderCrystalPos(minePos));
                    }
                }
                if (up.get() && crystalList.isEmpty()){
                    switch (placePriority.get()){
                        case Consistently -> {
                            if (crystalList.isEmpty()) crystalList = new ArrayList<>(){{
                                if (BlockUtil.isAir(minePos.up()) && (BlockUtil.getBlock(minePos) == Blocks.BEDROCK || BlockUtil.getBlock(minePos) == Blocks.OBSIDIAN) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), minePos.up()) <= cityRange.get()){
                                    Box box = new Box(minePos.up());
                                    if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof TntEntity || entity instanceof EndCrystalEntity)){
                                        add(minePos.up());
                                    }
                                }
                            }};
                        }
                        case Distance -> crystalList.addAll(new ArrayList<>(){{
                            if (BlockUtil.isAir(minePos.up()) && (BlockUtil.getBlock(minePos) == Blocks.BEDROCK || BlockUtil.getBlock(minePos) == Blocks.OBSIDIAN) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), minePos.up()) <= cityRange.get()){
                                Box box = new Box(minePos.up());
                                if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof TntEntity || entity instanceof EndCrystalEntity)){
                                    add(minePos.up());
                                }
                            }
                        }});
                    }
                }
                if (!crystalList.isEmpty()){
                    if (placePriority.get().equals(PlacePriority.Distance)) crystalList.sort(Comparator.comparing(blockPos1 -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos1)));
                    if (crystalSupport.get() && BlockUtil.isAir(crystalList.get(0).down())) placeSupportBlock(crystalList.get(0).down());

                    if (placeMode.get().equals(PlaceMode.Progress)) {
                        float ownBreakingStage = ((meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress();
                        if ((ownBreakingStage >= breakingProgress.get() && mineMode.get().equals(MineMode.Vanilla) || instant.get() || (packetTimer.hasPassed(timer.get()) && mineMode.get().equals(MineMode.Packet)))) {
                            placeCrystal(crystalList.get(0).down());
                            crystalPos = crystalList.get(0);
                        }
                    }else {
                        placeCrystal(crystalList.get(0).down());
                        crystalPos = crystalList.get(0);
                    }

                }
            }
            FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
            if (ironPickaxe.get().isPressed()){
                pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.IRON_PICKAXE);
            }
            int prev = mc.player.getInventory().selectedSlot;
            if (!pickaxe.isHotbar()) {
                if (selfToggle.get()) {
                    toggle();
                }
                return;
            }
            if (!swapTimer.hasPassed(swapDelay.get() * 50)) return;
            if (autoSwap.get()) mc.player.getInventory().selectedSlot = pickaxe.slot();
            swapTimer.reset();
            if (instant.get()){
                if (instantTimer.hasPassed(tickDelay.get() * 50)){
                    if (bypass.get()){
                        if (direction == null) direction = Direction.UP;
                        mc.interactionManager.updateBlockBreakingProgress(minePos, direction);
                        mc.interactionManager.cancelBlockBreaking();
                    }else {
                        instantMine(minePos);
                    }
                    instantTimer.reset();
                }
            }else {
                mine(minePos);
                if (swapMode.get() == SwapMode.Silent) {
                    mc.player.getInventory().selectedSlot = prev;
                }
            }
        }
    }

    @EventHandler
    private void onT(TickEvent.Post event) {
        if (breakCrystal.get()) {
            if (target != null) {
                for (Entity crystal : mc.world.getEntities()) {
                    if (crystal instanceof EndCrystalEntity && crystal.getBlockPos().equals(crystalPos)) {
                        for (Entity item : mc.world.getEntities()){
                            if (item instanceof ItemEntity && item.getBlockPos().equals(holdCityPos)){
                                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (instant.get() && bypass.get()){
            instantTimer.reset();
            direction = event.direction;
            blockPos.set(event.blockPos);
            cum();
            event.cancel();
        }
    }

    private void cum() {
        if (!wontMine()) {
            if (direction == null) direction = Direction.UP;
            if (rotate.get()) {Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)));Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)));}
            else {mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));}
        }
    }

    private boolean wontMine() {
        return blockPos.getY() == -1 || !BlockUtils.canBreak(blockPos);
    }

    private void placeCrystal(BlockPos pos){
        if (mc.world.getBlockState(pos.up()).isAir()) {
            placePattern(pos);
        }

    }

    private void placePattern(BlockPos pos){
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, true));
            if(swing.get() == Swing.FULL) mc.player.swingHand(Hand.MAIN_HAND);
            if(swing.get() == Swing.PACKET) mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        else if (mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            int prev = mc.player.getInventory().selectedSlot;
            FindItemResult crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL);
            if (!crystalSlot.found()) {
                return;
            }
            mc.player.getInventory().selectedSlot = crystalSlot.slot();
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, true));
            if(swing.get() == Swing.FULL) mc.player.swingHand(Hand.MAIN_HAND);
            if(swing.get() == Swing.PACKET) mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            if (swapMode.get().equals(SwapMode.Silent)) mc.player.getInventory().selectedSlot = prev;
        }
    }

    private void placeSupportBlock(BlockPos pos){
        BlockUtils.place(pos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
    }

    private void instantMine(BlockPos pos){
        if (instantTimer.hasPassed(tickDelay.get() * 50)){
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,pos, Direction.DOWN)));
            else mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,pos, Direction.DOWN));
            if(swing.get() == Swing.FULL) mc.player.swingHand(Hand.MAIN_HAND);
            if(swing.get() == Swing.PACKET) mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            instantTimer.reset();
        }
    }

    private void startInsta(BlockPos pos){
        if (pos != null){
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN));
        }
    }

    private void mine(BlockPos pos){
        if (mineMode.get().equals(MineMode.Vanilla)) BlockUtils.breakBlock(pos, swing.get() == Swing.FULL || swing.get() == Swing.PACKET);
        else {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN));
            if(swing.get() == Swing.FULL) mc.player.swingHand(Hand.MAIN_HAND);
            if(swing.get() == Swing.PACKET) mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));

        }
    }

    private ArrayList<BlockPos> getFullSurrPos(BlockPos pos){
        ArrayList<BlockPos> pos1 = new ArrayList<>();
        for (CardinalDirection direction : CardinalDirection.values()){
            if (!BlockUtil.isAir(pos.offset(direction.toDirection())) && BlockUtils.canBreak(pos.offset(direction.toDirection())) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos.offset(direction.toDirection())) <= cityRange.get()){
                Box box = new Box(pos.offset(direction.toDirection()));
                if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof ItemEntity || entity instanceof TntEntity)){
                    pos1.add(pos.offset(direction.toDirection()));
                }
            }
        }
        return pos1;
    }

    private int getOpenPos(BlockPos pos){
        int count = 0;
        for (CardinalDirection direction : CardinalDirection.values()){
            if (BlockUtil.isAir(pos.offset(direction.toDirection()))){
                Box box = new Box(pos.offset(direction.toDirection()));
                if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof TntEntity)){
                    count++;
                }
            }
        }
        return 3 - count;
    }

    private ArrayList<BlockPos> getUnderCrystalPos(BlockPos pos){
        ArrayList<BlockPos> list = new ArrayList<>();
        if (BlockUtil.isAir(pos.down()) && (BlockUtil.getBlock(pos.down(2)) == Blocks.BEDROCK || BlockUtil.getBlock(pos.down(2)) == Blocks.OBSIDIAN || BlockUtil.getBlock(pos.down(2)) == Blocks.AIR) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos.down()) <= cityRange.get()){
            Box box = new Box(pos.down());
            if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof TntEntity || entity instanceof EndCrystalEntity)){
                list.add(pos.down());
            }
        }
        for (CardinalDirection direction : CardinalDirection.values()){
            if (BlockUtil.isAir(pos.offset(direction.toDirection()).down()) && (BlockUtil.getBlock(pos.offset(direction.toDirection()).down(2)) == Blocks.BEDROCK || BlockUtil.getBlock(pos.offset(direction.toDirection()).down(2)) == Blocks.OBSIDIAN  || BlockUtil.getBlock(pos.offset(direction.toDirection()).down(2)) == Blocks.AIR)  && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos.offset(direction.toDirection()).down()) <= cityRange.get()){
                Box box = new Box(pos.offset(direction.toDirection()).down());
                if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof TntEntity || entity instanceof EndCrystalEntity)){
                    list.add(pos.offset(direction.toDirection()).down());
                }
            }
        }
        return list;
    }

    private ArrayList<BlockPos> getDiagonalCrystalPos(BlockPos pos){
        ArrayList<BlockPos> list = new ArrayList<>();
        for (BlockPos p : diagonalArray){
            BlockPos newPos = pos.add(p);
            if (BlockUtil.isAir(newPos) && (BlockUtil.getBlock(newPos.down()) == Blocks.BEDROCK || BlockUtil.getBlock(newPos.down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(newPos.down()) == Blocks.AIR) && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), newPos) <= cityRange.get()){
                Box box = new Box(newPos);
                if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof TntEntity || entity instanceof EndCrystalEntity)){
                    list.add(newPos);
                }
            }
        }
        return list;
    }

    private ArrayList<BlockPos> getCrystalPos(BlockPos pos){
        ArrayList<BlockPos> list = new ArrayList<>();
        for (CardinalDirection direction : CardinalDirection.values()){
            if (BlockUtil.isAir(pos.offset(direction.toDirection())) && (BlockUtil.getBlock(pos.offset(direction.toDirection()).down()) == Blocks.BEDROCK || BlockUtil.getBlock(pos.offset(direction.toDirection()).down()) == Blocks.OBSIDIAN || BlockUtil.getBlock(pos.offset(direction.toDirection()).down()) == Blocks.AIR)  && BlockUtil.distance(new BlockPos(mc.player.getEyePos()), pos.offset(direction.toDirection())) <= cityRange.get()){
                Box box = new Box(pos.offset(direction.toDirection()));
                if (!EntityUtils.intersectsWithEntity(box, entity -> entity instanceof PlayerEntity || entity instanceof TntEntity || entity instanceof EndCrystalEntity)){
                    list.add(pos.offset(direction.toDirection()));
                }
            }
        }
        return list;
    }

    private List<PlayerEntity> getFriendsInRange() {

        List<PlayerEntity> stream = mc.world.getPlayers()
            .stream()
            .filter(e -> e != mc.player)
            .filter(e -> e.isAlive())
            .filter(e -> Friends.get().isFriend((PlayerEntity) e))
            .filter(e -> ((PlayerEntity) e).getHealth() > 0)
            .filter(e -> mc.player.distanceTo(e) < enemyRange.get())
            .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
            .collect(Collectors.toList());

        return stream;
    }

    private Timer renderTimer = new Timer();

    public static BlockPos getPos(){
        return minePos;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && minePos != null && !mc.world.getBlockState(minePos).isAir()) {
            if (shapeMode.get().equals(ShapeMode.Lines) || shapeMode.get().equals(ShapeMode.Both)){
                S(event, minePos, 0.99, 0, 0.01, lineColor.get(), lineColor2.get());
                TAB(event, minePos, 0.99, 0.01,true,true, lineColor.get(), lineColor2.get());

                if (width.get() == 2){
                    S(event, minePos, 0.98, 0, 0.02, lineColor.get(), lineColor2.get());
                    TAB(event, minePos, 0.98, 0.02,true,true, lineColor.get(), lineColor2.get());
                }
                if (width.get() == 3){
                    S(event, minePos, 0.97, 0, 0.03, lineColor.get(), lineColor2.get());
                    TAB(event, minePos, 0.97, 0.03,true,true, lineColor.get(), lineColor2.get());
                }
                if (width.get() == 4){
                    S(event, minePos, 0.96, 0, 0.04, lineColor.get(), lineColor2.get());
                    TAB(event, minePos, 0.96, 0.04,true,true, lineColor.get(), lineColor2.get());
                }
            }
            if (shapeMode.get().equals(ShapeMode.Sides) || shapeMode.get().equals(ShapeMode.Both)){
                FS(event, minePos,0, true, true, sideColor.get(), sideColor2.get());
            }
            if (BlockUtil.distance(mc.player.getBlockPos(), minePos) > 6){
                minePos = null;
            }
        }
    }
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get() || !renderProgress.get()) return;

        if (target != null && minePos != null && !mc.world.getBlockState(minePos).isAir() && mineMode.get().equals(MineMode.Vanilla)) {
            vec3.set(minePos.getX() + 0.5, minePos.getY() + 0.5, minePos.getZ() + 0.5);
            vec32.set(minePos.getX() + 0.5, minePos.getY() + 0.8, minePos.getZ() + 0.5);
            float ownBreakingStage = ((meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress();

            if (NametagUtils.to2D(vec32, progressTextScale.get())) {
                NametagUtils.begin(vec32);
                TextRenderer.get().begin(1, true, true);

                String text = target.getEntityName();
                double w = TextRenderer.get().getWidth(text) / 2;
                TextRenderer.get().render(text, -w, 0, textColor.get(), true);

                TextRenderer.get().end();
                NametagUtils.end();
            }

            if (NametagUtils.to2D(vec3, progressTextScale.get())) {
                NametagUtils.begin(vec3);
                TextRenderer.get().begin(1, false, true);

                String text = String.format("%.2f", ownBreakingStage * 100) + "%";
                double w = TextRenderer.get().getWidth(text) / 2;
                TextRenderer.get().render(text, -w, 0, textColor.get(), true);

                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }
}
