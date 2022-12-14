package genesis.team.addon.modules.combat.AutoCrystalRewrite;

import genesis.team.addon.Genesis;
import genesis.team.addon.modules.combat.PistonAura;
import genesis.team.addon.util.CombatUtil.RenderUtils;
import genesis.team.addon.util.InfoUtil.RenderInfo;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoCrystalRewrite extends Module {
    public final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final SettingGroup sgPrediction = settings.createGroup("Prediction");
    public final SettingGroup sgDamage = settings.createGroup("Damage");
    public final SettingGroup sgMiscellaneous = settings.createGroup("Miscellaneous");
    public final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<Swap> swap = sgGeneral.add(new EnumSetting.Builder<Swap>().name("swap").defaultValue(Swap.Normal).build());
    public final Setting<Integer> swapDelay = sgGeneral.add(new IntSetting.Builder().name("swap-delay").defaultValue(0).range(0, 20).visible(() -> swap.get() != Swap.OFF).build());
    public final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder().name("place-delay").defaultValue(0).range(0, 10).build());
    public final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").defaultValue(4.5).range(0, 7).build());
    public final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder().name("break-delay").defaultValue(0).range(0, 10).build());
    public final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder().name("break-range").defaultValue(4.5).range(0, 7).build());
    public final Setting<Boolean> smartRange = sgGeneral.add(new BoolSetting.Builder().name("smart-range").defaultValue(false).build());
    public final Setting<FastBreak> fastBreak = sgGeneral.add(new EnumSetting.Builder<FastBreak>().name("fast-break").defaultValue(FastBreak.OFF).build());
    public final Setting<Frequency> frequency = sgGeneral.add(new EnumSetting.Builder<Frequency>().name("frequency").defaultValue(Frequency.OFF).build());
    public final Setting<Integer> value = sgGeneral.add(new IntSetting.Builder().name("value").defaultValue(20).range(0, 20).visible(() -> frequency.get() != Frequency.OFF).build());
    public final Setting<Integer> ticksExisted = sgGeneral.add(new IntSetting.Builder().name("ticks-existed").defaultValue(0).range(0, 5).build());
    public final Setting<Boolean> multiPlace = sgGeneral.add(new BoolSetting.Builder().name("multi-place").defaultValue(false).build());
    public final Setting<Boolean> oneTwelve = sgGeneral.add(new BoolSetting.Builder().name("1.12").defaultValue(false).build());
    public final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(false).build());
    public final Setting<Boolean> raytrace = sgGeneral.add(new BoolSetting.Builder().name("raytrace").defaultValue(false).build());
    public final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder().name("ignore-terrain").defaultValue(false).build());
    public final Setting<Integer> blockUpdate = sgGeneral.add(new IntSetting.Builder().name("block-update").defaultValue(0).range(0, 200).build());
    public final Setting<Boolean> limit = sgGeneral.add(new BoolSetting.Builder().name("limit").defaultValue(false).build());
    public final Setting<Integer> limitAttacks = sgGeneral.add(new IntSetting.Builder().name("limit-attacks").defaultValue(5).range(1, 10).visible(limit::get).build());
    public final Setting<Integer> passedTicks = sgGeneral.add(new IntSetting.Builder().name("passed-ticks").defaultValue(5).range(1, 10).visible(limit::get).build());
    public final Setting<Priority> priority = sgGeneral.add(new EnumSetting.Builder<Priority>().name("priority").defaultValue(Priority.Place).build());

    public final Setting<Boolean> prediction = sgPrediction.add(new BoolSetting.Builder().name("prediction").defaultValue(false).build());
    public final Setting<Boolean> collision = sgPrediction.add(new BoolSetting.Builder().name("collision").defaultValue(false).visible(prediction::get).build());
    public final Setting<Double> offset = sgPrediction.add(new DoubleSetting.Builder().name("offset").defaultValue(0.50).range(0, 3).visible(prediction::get).build());
    public final Setting<Boolean> predictID = sgPrediction.add(new BoolSetting.Builder().name("predict-ID").defaultValue(false).build());
    public final Setting<Boolean> considerAge = sgPrediction.add(new BoolSetting.Builder().name("consider-age").defaultValue(false).visible(predictID::get).build());
    public final Setting<Integer> delayID = sgPrediction.add(new IntSetting.Builder().name("delay-ID").defaultValue(0).range(0, 5).visible(predictID::get).build());

    public final Setting<Place> doPlace = sgDamage.add(new EnumSetting.Builder<Place>().name("place").defaultValue(Place.BestDMG).build());
    public final Setting<Break> doBreak = sgDamage.add(new EnumSetting.Builder<Break>().name("break").defaultValue(Break.BestDMG).build());
    public final Setting<Double> minDmg = sgDamage.add(new DoubleSetting.Builder().name("min-dmg").defaultValue(7.5).range(0, 36).build());
    public final Setting<Double> safety = sgDamage.add(new DoubleSetting.Builder().name("safety").defaultValue(25).range(0, 100).build());
    public final Setting<Boolean> antiSelfPop = sgDamage.add(new BoolSetting.Builder().name("anti-self-pop").defaultValue(false).build());
    public final Setting<Boolean> antiFriendDamage = sgDamage.add(new BoolSetting.Builder().name("anti-friend-damage").defaultValue(false).build());
    public final Setting<Double> friendMaxDmg = sgDamage.add(new DoubleSetting.Builder().name("max-dmg").defaultValue(25).range(0, 100).visible(antiFriendDamage::get).build());

    public final Setting<Double> facePlace = sgMiscellaneous.add(new DoubleSetting.Builder().name("face-place").defaultValue(11).range(0, 36).build());
    public final Setting<Double> armorBreaker = sgMiscellaneous.add(new DoubleSetting.Builder().name("armor-breaker").defaultValue(25).range(0, 36).build());
    public final Setting<SurroundBreak> surroundBreak = sgMiscellaneous.add(new EnumSetting.Builder<SurroundBreak>().name("surround-break").defaultValue(SurroundBreak.OnMine).build());
    public final Setting<Boolean> eatPause = sgMiscellaneous.add(new BoolSetting.Builder().name("eat-pause").defaultValue(false).build());
    public final Setting<Boolean> minePause = sgMiscellaneous.add(new BoolSetting.Builder().name("mine-pause").defaultValue(false).build());

    private final Setting<RenderUtils.RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderUtils.RenderMode>().name("render").defaultValue(RenderUtils.RenderMode.Box).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 170, 10)).visible(() -> RenderUtils.visibleSide(shapeMode.get())).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(255, 0, 170, 90)).visible(() -> RenderUtils.visibleLine(shapeMode.get())).build());
    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder().name("height").defaultValue(0.99).sliderRange(0, 1).visible(() -> RenderUtils.visibleHeight(renderMode.get())).build());
    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("render-time").defaultValue(5).range(0, 10).build());

    private final ExecutorService thread = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * (1 + 13 / 3));

    public BlockPoz updatedBlock, renderPos, bestPos;
    public int attacks, ticksPassed;

    public double bestDamage = 0;

    public int renderTimer;
    public Box renderBox;

    public int lastEntityId, last;

    public final int[] second = new int[20];
    public static int cps;
    public int tick, i, lastSpawned = 20;

    private final IntSet brokenCrystals = new IntOpenHashSet();
    private final List<CrystalMap> crystalMap = new ArrayList<>();

    private final List<Location>
            currLocation = new ArrayList<>(),
            prevLocation = new ArrayList<>();

    private final TimerUtils
            placeTimer = new TimerUtils(),
            breakTimer = new TimerUtils(),
            blockTimer = new TimerUtils(),
            swapTimer = new TimerUtils(),
            idTimer = new TimerUtils();

    // Bro, i know ya wanna skid it
    public AutoCrystalRewrite() {
        super(Genesis.Px, "proxima-auto-crystal", "Automatically places and blows up crystals.");
    }

    @Override
    public void onActivate() {
        updatedBlock = null;
        renderPos = null;
        bestPos = null;

        renderTimer = renderTime.get();
        renderBox = null;

        tick = 0;
        Arrays.fill(second, 0);
        i = 0;

        brokenCrystals.clear();
        crystalMap.clear();

        placeTimer.reset();
        blockTimer.reset();
        swapTimer.reset();
        idTimer.reset();
    }

    @EventHandler
    public void onPre(TickEvent.Pre event) {
        prevLocation.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            prevLocation.add(new Location(player));
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        currLocation.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            currLocation.add(new Location(player));
        }

        // Main code
        getCPS();
        updateBlock();

        if (renderTimer > 0 && renderPos != null) renderTimer--;

        if (ticksPassed >= 0) ticksPassed--;
        else {
            ticksPassed = 20;
            attacks = 0;
        }

        if (eatPause.get() && mc.player.isUsingItem() && (mc.player.getMainHandStack().isFood() || mc.player.getOffHandStack().isFood()))
            return;
        if (minePause.get() && mc.interactionManager.isBreakingBlock()) return;
        if (Modules.get().get(PistonAura.class).shouldPause()) return;

        thread.execute(this::doSurroundBreak);
        thread.execute(this::doCalculate);

        if (priority.get() == Priority.Place) {
            doPlace();
            doBreak();
        } else {
            doBreak();
            doPlace();
        }

        crystalMap.forEach(CrystalMap::tick);
    }

    @EventHandler
    public void onAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if ((fastBreak.get() == FastBreak.Instant || fastBreak.get() == FastBreak.All)) {
            if (bestPos == null) return;
            BlockPoz entityPos = new BlockPoz(event.entity.getBlockPos().down());

            if (bestPos.equals(entityPos)) {
                doBreak(event.entity, false);
            }
        }

        last = event.entity.getId() - lastEntityId;
        lastEntityId = event.entity.getId();
    }

    @EventHandler
    public void onRemove(EntityRemovedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if (brokenCrystals.contains(event.entity.getId())) {
            lastSpawned = 20;
            tick++;

            removeId(event.entity);
        }
    }

    @EventHandler
    private void onSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            swapTimer.reset();
        }
    }

    @EventHandler
    private void onReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlaySoundIdS2CPacket packet)) return;

        if (fastBreak.get() == FastBreak.Sequential || fastBreak.get() == FastBreak.All) {
            if (packet.getCategory().getName().equals(SoundCategory.BLOCKS.getName()) && packet.getSoundId().getPath().equals("entity.generic.explode")) {
                brokenCrystals.forEach(crystalMap -> mc.world.removeEntity(crystalMap, Entity.RemovalReason.KILLED));
            }
        }
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        if (event.newState.isAir()) {
            updatedBlock = new BlockPoz(event.pos);
            blockTimer.reset();
        }
    }

    private void doPlace() {
        doPlace(bestPos);
    }

    private void doPlace(BlockPoz blockPos) {
        if (blockPos == null) bestDamage = 0;
        if (blockPos == null || !placeTimer.passedTicks(placeDelay.get())) return;

        int prevSlot = mc.player.getInventory().selectedSlot;

        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.found()) return;

        Hand hand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (ACUtils.distanceTo(ACUtils.closestVec3d(blockPos.getBoundingBox())) > placeRange.get()) return;
        BlockHitResult hitResult = getResult(blockPos);

        if (swap.get() != Swap.OFF && !crystal.isOffhand() && !crystal.isMainHand())
            InvUtils.swap(crystal.slot(), false);

        if (crystal.isOffhand() || crystal.isMainHand()) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos.get()), Rotations.getPitch(blockPos.get()));

            if (!ACUtils.hasEntity(blockPos.up().getBoundingBox())) mc.player.swingHand(hand);
            else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult,0));
        }

        if (predictID.get() && idTimer.passedTicks(delayID.get())) {
            EndCrystalEntity endCrystal = new EndCrystalEntity(mc.world, blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5);
            endCrystal.setShowBottom(false);
            endCrystal.setId(lastEntityId + last);

            doBreak(endCrystal, considerAge.get());
            endCrystal.kill();

            idTimer.reset();
        }

        if (swap.get() == AutoCrystalRewrite.Swap.Silent) InvUtils.swap(prevSlot, false);


        placeTimer.reset();
        setRender(blockPos);
    }

    private void doBreak() {
        doBreak(getCrystal(), true);
    }

    private void doBreak(Entity entity, boolean checkAge) {
        if (entity == null || !breakTimer.passedTicks(breakDelay.get()) || !swapTimer.passedTicks(swapDelay.get()) || !frequency() || (checkAge && entity.age < ticksExisted.get()))
            return;

        if (limit.get()) {
            CrystalMap crystal = getCrystal(entity);

            if (!crystal.hittable() && crystal.attacks > limitAttacks.get()) {
                crystal.shouldWait = true;
            }
        }

        if (ACUtils.distanceTo(ACUtils.closestVec3d(entity.getBoundingBox())) > breakRange.get()) return;
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (!matchesCrystal(entity)) crystalMap.add(new CrystalMap(entity.getId(), 1));
        else getCrystal(entity).attacks++;

        if (fastBreak.get() == FastBreak.Kill) {
            entity.kill();

            lastSpawned = 20;
            tick++;
        }

        addBroken(entity);
        attacks++;
        breakTimer.reset();
    }

    private BlockHitResult getResult(BlockPoz blockPos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        if (!raytrace.get())
            return new BlockHitResult(ACUtils.closestVec3d(new Box(blockPos)), Direction.UP, blockPos, false);
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(blockPos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                    blockPos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                    blockPos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                return result;
            }
        }

        return new BlockHitResult(ACUtils.closestVec3d(new Box(blockPos)), blockPos.getY() == 255 ? Direction.DOWN : Direction.UP, blockPos, false);
    }

    private boolean matchesCrystal(Entity entity) {
        return getCrystal(entity).attacks != 0;
    }

    private CrystalMap getCrystal(Entity entity) {
        for (CrystalMap crystal : crystalMap) {
            if (crystal.id() == entity.getId()) return crystal;
        }

        return new CrystalMap(-9999, 0);
    }

    private void doSurroundBreak() {
        if (surroundBreak.get() == SurroundBreak.OFF) return;
        if (isFacePlacing()) return;

        if (surroundBreak.get() == SurroundBreak.OnMine && !mc.interactionManager.isBreakingBlock()) return;
        List<BlockPoz> vulnerablePos = new ArrayList<>();

        try {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (Friends.get().isFriend(player)) continue;
                if (!ACUtils.isSurrounded(player)) continue;

                for (BlockPoz bp : ACUtils.getSphere(player, 5)) {
                    if (ACUtils.hasEntity(bp.getBoundingBox(), entity -> entity == mc.player || entity == player || entity instanceof ItemEntity))
                        continue;

                    boolean canPlace = bp.up().isAir() && (bp.isOf(Blocks.OBSIDIAN) || bp.isOf(Blocks.BEDROCK));

                    if (!canPlace) continue;
                    Vec3d vec3d = new Vec3d(bp.getX(), bp.getY() + 1, bp.getZ());
                    Box endCrystal = new Box(vec3d.x - 0.5, vec3d.y, vec3d.z - 0.5, vec3d.x + 1.5, vec3d.y + 2, vec3d.z + 1.5);

                    for (BlockPoz surround : ACUtils.getSurroundBlocks(player, true)) {
                        if (surround.getHardness() <= 0) return;

                        if (surroundBreak.get() == SurroundBreak.OnMine && mc.player.getMainHandStack().getItem() instanceof PickaxeItem) {
                            BlockPoz breakingPos = new BlockPoz(((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getCurrentBreakingBlockPos());

                            if (!surround.equals(breakingPos)) continue;
                        }
                        Box box = surround.getBoundingBox();

                        if (endCrystal.intersects(box)) vulnerablePos.add(bp);
                    }
                }
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        if (vulnerablePos.isEmpty()) return;
        vulnerablePos.sort(Comparator.comparingDouble(ACUtils::distanceTo));
        BlockPoz blockPos = vulnerablePos.get(0);

        if (ACUtils.hasEntity(blockPos.up().getBoundingBox()) || ACUtils.distanceTo(ACUtils.closestVec3d(blockPos.getBoundingBox())) > placeRange.get())
            return;
        doPlace(blockPos);
    }

    private void doCalculate() {
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.found()) return;

        List<BlockPoz> sphere = ACUtils.getSphere(mc.player, Math.ceil(placeRange.get()));
        BlockPoz bestPos = null;
        double bestDamage = 0.0;
        double safety;

        try {
            for (BlockPoz bp : sphere) {
                if (ACUtils.distanceTo(ACUtils.closestVec3d(bp.getBoundingBox())) > placeRange.get()) continue;

                boolean canPlace = bp.up().isAir() && (bp.isOf(Blocks.OBSIDIAN) || bp.isOf(Blocks.BEDROCK));

                if (!canPlace) continue;
                if (updatedBlock != null && updatedBlock.equals(bp.up())) continue;

                EndCrystalEntity fakeCrystal = new EndCrystalEntity(mc.world, bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5);

                if (smartRange.get() && ACUtils.distanceTo(ACUtils.closestVec3d(fakeCrystal.getBoundingBox())) > breakRange.get())
                    continue;
                if (oneTwelve.get() && !bp.up(2).isAir()) continue;

                double targetDamage = getHighestDamage(ACUtils.roundVec(bp));
                double selfDamage = DamageUtils.crystalDamage(mc.player, ACUtils.roundVec(bp));
                safety = (targetDamage / 36 - selfDamage / 36) * 100;

                if (safety < this.safety.get() || antiSelfPop.get() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount())
                    continue;

                boolean validPos = true;
                if (antiFriendDamage.get()) {
                    for (PlayerEntity friend : mc.world.getPlayers()) {
                        if (!Friends.get().isFriend(friend)) continue;

                        double friendDamage = DamageUtils.crystalDamage(friend, ACUtils.roundVec(bp));
                        if (friendDamage > friendMaxDmg.get()) {
                            validPos = false;
                            break;
                        }
                    }
                }
                if (!validPos) continue;
                if (intersectsWithEntity(bp, multiPlace.get(), fakeCrystal)) continue;

                if (targetDamage > bestDamage) {
                    bestDamage = targetDamage;
                    bestPos = bp;
                }
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        this.bestPos = bestPos;
        this.bestDamage = bestDamage;
    }

    private boolean intersectsWithEntity(BlockPoz blockPos, boolean multiPlace, EndCrystalEntity fakeCrystal) {
        if (multiPlace) {
            return ACUtils.hasEntity(blockPos.getBoundingBox().stretch(0, 2, 0));
        } else {
            return ACUtils.hasEntity(blockPos.getBoundingBox().stretch(0, 2, 0), entity -> !(entity instanceof EndCrystalEntity && entity.getPos().getX() == fakeCrystal.getPos().getX() && entity.getPos().getY() == fakeCrystal.getPos().getY() && entity.getPos().getZ() == fakeCrystal.getPos().getZ()));
        }
    }

    private void updateBlock() {
        if (updatedBlock != null && blockTimer.passedMillis(blockUpdate.get())) {
            updatedBlock = null;
        }
    }

    private double getHighestDamage(Vec3d vec3d) {
        if (mc.world == null || mc.player == null) return 0;
        double highestDamage = 0;

        for (PlayerEntity target : mc.world.getPlayers()) {
            if (Friends.get().isFriend(target)) continue;
            if (target == mc.player) continue;
            if (target.isDead() || target.getHealth() == 0) continue;

            double targetDamage = 0;
            boolean skipPredict = false;
            if (prediction.get()) {
                if (!ACUtils.isSurrounded(target)) {
                    OtherClientPlayerEntity predictedTarget = ACUtils.predictedTarget(target);
                    double x = getPredict(target, offset.get())[0];
                    double z = getPredict(target, offset.get())[1];

                    predictedTarget.setPosition(x, target.getY(), z);

                    if (collision.get()) {
                        Iterable<VoxelShape> collisions = mc.world.getBlockCollisions(predictedTarget, predictedTarget.getBoundingBox());

                        if (collisions.iterator().hasNext()) skipPredict = true;
                    }

                    targetDamage = skipPredict ? targetDamage : DamageUtils.crystalDamage(predictedTarget, vec3d, false, null, ignoreTerrain.get());
                }
            }
            if (!prediction.get() || skipPredict) targetDamage = DamageUtils.crystalDamage(target, vec3d, false, null, ignoreTerrain.get());
            if (targetDamage < minDmg.get() && !shouldFacePlace(target, targetDamage)) continue;

            if (doPlace.get() == Place.BestDMG) {
                if (targetDamage > highestDamage) {
                    highestDamage = targetDamage;
                }
            } else highestDamage += targetDamage;
        }

        return highestDamage;
    }

    private double[] getPredict(PlayerEntity player, double offset) {
        double x = player.getX(), z = player.getZ();

        if (currLocation.isEmpty() || prevLocation.isEmpty()) return new double[]{0, 0};
        Location cLoc = getLocation(player, currLocation);
        Location pLoc = getLocation(player, prevLocation);

        if (cLoc == null || pLoc == null) return new double[]{0, 0};
        double distance = cLoc.vec3d.distanceTo(pLoc.vec3d);

        if (cLoc.x > pLoc.x) {
            x += distance + offset;
        } else if (cLoc.x < pLoc.x) {
            x -= distance + offset;
        }

        if (cLoc.z > pLoc.z) {
            z += distance + offset;
        } else if (cLoc.z < pLoc.z) {
            z -= distance + offset;
        }

        return new double[]{x, z};
    }

    private EndCrystalEntity getCrystal() {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;
            if (mc.player.distanceTo(entity) > breakRange.get()) continue;

            if (getCrystal(entity).shouldWait) continue;
            if (doBreak.get() == Break.All) return (EndCrystalEntity) entity;

            double tempDamage = getHighestDamage(ACUtils.roundVec(entity));
            if (tempDamage > minDmg.get()) return (EndCrystalEntity) entity;
        }

        if (bestPos == null) return null;
        return ACUtils.getEntity(bestPos.up());
    }

    private double getBestDamage() {
        return ((double) Math.round(bestDamage * 100) / 100);
    }

    private boolean frequency() {
        switch (frequency.get()) {
            case EachTick -> {
                if (attacks > value.get()) return false;
            }
            case Divide -> {
                if (!divide(value.get()).contains(ticksPassed)) return false;
            }
            case OFF -> {
                return true;
            }
        }

        return true;
    }

    public void getCPS() {
        i++;
        if (i >= second.length) i = 0;

        second[i] = tick;
        tick = 0;

        cps = 0;
        for (int i : second) cps += i;

        lastSpawned--;
        if (lastSpawned >= 0 && cps > 0) cps--;
        if (cps == 0) bestDamage = 0.0;
    }

    public ArrayList<Integer> divide(int frequency) {
        ArrayList<Integer> freqAttacks = new ArrayList<>();
        int size = 0;

        if (20 < frequency) return freqAttacks;
        else if (20 % frequency == 0) {
            for (int i = 0; i < frequency; i++) {
                size += 20 / frequency;
                freqAttacks.add(size);
            }
        } else {
            int zp = frequency - (20 % frequency);
            int pp = 20 / frequency;

            for (int i = 0; i < frequency; i++) {
                if (i >= zp) {
                    size += pp + 1;
                    freqAttacks.add(size);
                } else {
                    size += pp;
                    freqAttacks.add(size);
                }
            }
        }

        return freqAttacks;
    }

    private void addBroken(Entity entity) {
        if (!brokenCrystals.contains(entity.getId())) brokenCrystals.add(entity.getId());
    }

    private void removeId(Entity entity) {
        if (brokenCrystals.contains(entity.getId())) brokenCrystals.remove(entity.getId());
    }

    public Location getLocation(PlayerEntity player, List<Location> locations) {
        return locations.stream().filter(location -> location.player == player).findFirst().orElse(null);
    }

    private boolean shouldFacePlace(PlayerEntity player, double damage) {
        if (facePlace.get() == 0 || damage < 1.5) return false;
        if (!ACUtils.isSurrounded(player) || ACUtils.isFaceTrapped()) return false;
        return (player.getHealth() + player.getAbsorptionAmount()) <= facePlace.get() || ACUtils.getWorstArmor(player) <= armorBreaker.get();
    }

    private boolean isFacePlacing() {
        return bestPos != null && partOfSurround(bestPos);
    }

    private boolean partOfSurround(BlockPoz blockPos) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            if (ACUtils.getSurroundBlocks(player, true).contains(blockPos)) return true;
        }

        return false;
    }

    public void setRender(BlockPoz blockPos) {
        renderPos = blockPos;
        renderTimer = renderTime.get();
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (renderMode.get() == RenderUtils.RenderMode.Box) return;
        if (renderTimer == 0) renderPos = null;
        if (renderPos == null) return;
        RenderInfo ri = new RenderInfo(event, renderMode.get(), shapeMode.get());

        RenderUtils.render(ri, renderPos, sideColor.get(), lineColor.get(), height.get());
    }

    @Override
    public String getInfoString() {
        return getBestDamage() + ", " + cps;
    }

    public enum Swap {
        Silent, Normal, OFF
    }

    public enum FastBreak {
        Kill, Instant, Sequential, All, OFF
    }

    public enum Attack {
        Packet, Client, Vanilla
    }

    public enum Frequency {
        EachTick, Divide, OFF
    }

    public enum Priority {
        Place, Break
    }

    public enum Place {
        BestDMG, MostDMG
    }

    public enum Break {
        All, PlacePos, BestDMG
    }

    public enum SurroundBreak {
        Always, OnMine, OFF
    }

    public enum Render {
        Box, Smooth, OFF
    }
}
