package genesis.team.addon.modules.movement.ElytraFly;

import genesis.team.addon.Genesis;
import genesis.team.addon.events.ElytraEvent;
import genesis.team.addon.events.PlayerMoveEvent;
import genesis.team.addon.events.UpdateWalkingPlayerEvent;
import genesis.team.addon.mixins.IPlayerMovePacket;
import genesis.team.addon.util.ProximaUtil.PlayerUtil;
import genesis.team.addon.util.ProximaUtil.Timer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class ElytraFly extends Module {

    public enum Mode {
        BOOST, CONTROL, FIREWORK, PACKET
    }

    public enum StrictMode {
        NONE, NORMAL, NCP, GLIDE
    }

    private final SettingGroup sgDefault = settings.createGroup("Default");

    private final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>().name("mode").description("Block breaking method").defaultValue(Mode.CONTROL).build());

    private final Setting<Double> packetDelay = sgDefault.add(new DoubleSetting.Builder().name("packetDelay").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(5).visible(() -> mode.get() == Mode.BOOST).build());
    private final Setting<Double> staticDelay = sgDefault.add(new DoubleSetting.Builder().name("staticDelay").description("The radius in which players get targeted.").defaultValue(5).min(0).sliderMax(20).visible(() -> mode.get() == Mode.BOOST).build());
    private final Setting<Double> timeout = sgDefault.add(new DoubleSetting.Builder().name("timeout").description("The radius in which players get targeted.").defaultValue(0.5).min(0).sliderMax(1).visible(() -> mode.get() == Mode.BOOST).build());

    private final Setting<Boolean> stopMotion = sgDefault.add(new BoolSetting.Builder().name("stopMotion").description("Automatically rotates you towards the city block.").defaultValue(true).visible(() -> mode.get() == Mode.BOOST).build());

    private final Setting<Boolean> cruiseControl = sgDefault.add(new BoolSetting.Builder().name("cruiseControl").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> mode.get() == Mode.BOOST).build());
    private final Setting<Double> minUpSpeed = sgDefault.add(new DoubleSetting.Builder().name("minUpSpeed").description("The radius in which players get targeted.").defaultValue(0.5).min(0).sliderMax(5).visible(() -> mode.get() == Mode.BOOST && cruiseControl.get()).build());

    private final Setting<Boolean> autoSwitch = sgDefault.add(new BoolSetting.Builder().name("autoSwitch").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> mode.get() == Mode.FIREWORK).build());

    private final Setting<Double> factor = sgDefault.add(new DoubleSetting.Builder().name("factor").description("The radius in which players get targeted.").defaultValue(1.5).min(0).sliderMax(50).build());
    private final Setting<Integer> minSpeed = sgDefault.add(new IntSetting.Builder().name("minSpeed").description("The delay between breaks.").defaultValue(20).min(0).sliderMax(50).visible(() -> mode.get() == Mode.FIREWORK).build());
    private final Setting<Double> upFactor = sgDefault.add(new DoubleSetting.Builder().name("upFactor").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(50).build());
    private final Setting<Double> downFactor = sgDefault.add(new DoubleSetting.Builder().name("downFactor").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).build());

    private final Setting<Boolean> forceHeight = sgDefault.add(new BoolSetting.Builder().name("forceHeight").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> mode.get() == Mode.FIREWORK || (mode.get() == Mode.BOOST && cruiseControl.get())).build());
    private final Setting<Integer> manualHeight = sgDefault.add(new IntSetting.Builder().name("manualHeight").description("The delay between breaks.").defaultValue(121).min(0).sliderMax(350).visible(() -> mode.get() == Mode.FIREWORK || (mode.get() == Mode.BOOST && cruiseControl.get()) && forceHeight.get()).build());

    private final Setting<Boolean> groundSafety = sgDefault.add(new BoolSetting.Builder().name("groundSafety").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> mode.get() == Mode.FIREWORK).build());
    private final Setting<Integer> triggerHeight = sgDefault.add(new IntSetting.Builder().name("triggerHeight").description("The delay between breaks.").defaultValue(121).min(0).sliderMax(350).visible(() -> mode.get() == Mode.FIREWORK && groundSafety.get()).build());

    // Normal/Boost/Glide settings

    private final Setting<Double> speed = sgDefault.add(new DoubleSetting.Builder().name("speed").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).visible(() -> mode.get() == Mode.CONTROL).build());
    private final Setting<Double> sneakDownSpeed = sgDefault.add(new DoubleSetting.Builder().name("sneakDownSpeed").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).visible(() -> mode.get() == Mode.CONTROL).build());

    private final Setting<Boolean> instantFly = sgDefault.add(new BoolSetting.Builder().name("instantFly").description("Automatically rotates you towards the city block.").defaultValue(true).visible(() -> mode.get() == Mode.PACKET).build());
    private final Setting<Boolean> boostTimer = sgDefault.add(new BoolSetting.Builder().name("boostTimer").description("Automatically rotates you towards the city block.").defaultValue(true).visible(() -> mode.get() == Mode.BOOST).build());

    private final Setting<Boolean> speedLimit = sgDefault.add(new BoolSetting.Builder().name("speedLimit").description("Automatically rotates you towards the city block.").defaultValue(true).visible(() -> mode.get() == Mode.BOOST).build());
    private final Setting<Double> maxSpeed = sgDefault.add(new DoubleSetting.Builder().name("maxSpeed").description("The radius in which players get targeted.").defaultValue(2.5).min(0).sliderMax(10).visible(() -> mode.get() != Mode.PACKET && mode.get() != Mode.FIREWORK).build());

    private final Setting<Boolean> noDrag = sgDefault.add(new BoolSetting.Builder().name("noDrag").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> mode.get() != Mode.PACKET && mode.get() != Mode.FIREWORK).build());

    // Packet settings
    private final Setting<Boolean> accelerate = sgDefault.add(new BoolSetting.Builder().name("accelerate").description("Automatically rotates you towards the city block.").defaultValue(true).visible(() -> mode.get() == Mode.PACKET).build());
    private final Setting<Double> acceleration = sgDefault.add(new DoubleSetting.Builder().name("acceleration").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(5).visible(() -> mode.get() == Mode.PACKET).build());
    private final Setting<StrictMode> strict = sgDefault.add(new EnumSetting.Builder<StrictMode>().name("strict").description("Block breaking method").defaultValue(StrictMode.NONE).visible(() -> mode.get() == Mode.PACKET).build());
    private final Setting<Boolean> antiKick = sgDefault.add(new BoolSetting.Builder().name("antiKick").description("Automatically rotates you towards the city block.").defaultValue(true).visible(() -> mode.get() == Mode.PACKET).build());
    private final Setting<Boolean> infDurability = sgDefault.add(new BoolSetting.Builder().name("infDurability").description("Automatically rotates you towards the city block.").defaultValue(true).visible(() -> mode.get() == Mode.PACKET).build());


    private static boolean hasElytra = false;

    private boolean rSpeed;

    private double curSpeed;
    public double tempSpeed;

    private double height;

    private final Random random = new Random();

    private final Timer instantFlyTimer = new Timer();
    private final Timer staticTimer = new Timer();

    private final Timer rocketTimer = new Timer();

    private final Timer strictTimer = new Timer();



    private boolean isJumping = false;
    private boolean hasTouchedGround = false;

    public ElytraFly() {
        super(Genesis.Move ,"ElytraFly", "ElytraPlus");
    }

    @Override
    public void onActivate() {
        rSpeed = false;
        curSpeed = 0.0D;
        if (mc.player != null) {
            height = mc.player.getY();
            if (!mc.player.isCreative()) mc.player.getAbilities().allowFlying = false;
            mc.player.getAbilities().flying = false;
        }
        isJumping = false;
        hasElytra = false;
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            if (!mc.player.isCreative()) mc.player.getAbilities().allowFlying = false;
            mc.player.getAbilities().flying = false;
        }
        timerManager.resetTimer(this);
        hasElytra = false;
    }


    @EventHandler
    public void onUpdate(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        if (mc.player.isOnGround()) {
            hasTouchedGround = true;
        }

        if (!cruiseControl.get()) {
            height = mc.player.getY();
        }

        for(ItemStack is : mc.player.getArmorItems()) {
            if(is.getItem() instanceof ElytraItem) {
                hasElytra = true;
                break;
            } else {
                hasElytra = false;
            }
        }

        if (strictTimer.hasPassed(1500F) && !strictTimer.hasPassed(2000F)) {
            timerManager.resetTimer(this);
        }

        if (!mc.player.isFallFlying() && mode.get() != Mode.PACKET) {
            if (hasTouchedGround && boostTimer.get() != boostTimer.get() && !mc.player.isOnGround()) {
                timerManager.updateTimer(this, 25, 0.3F);
            }
            if (!mc.player.isOnGround() && instantFly.get() && mc.player.getVelocity().y < 0D) {
                if (!instantFlyTimer.hasPassed(1000F * timeout.get()))
                    return;
                instantFlyTimer.reset();
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                hasTouchedGround = false;
                strictTimer.reset();
            }
            return;
        }
    }

    public static boolean isHasElytra() {
        return hasElytra;
    }
    private boolean checkIfBlockInBB(int minY) {
        for(int iX = MathHelper.floor(mc.player.getBoundingBox().minX); iX < MathHelper.ceil(mc.player.getBoundingBox().maxX); iX++) {
            for(int iZ = MathHelper.floor(mc.player.getBoundingBox().minZ); iZ < MathHelper.ceil(mc.player.getBoundingBox().maxZ); iZ++) {
                BlockState state = mc.world.getBlockState(new BlockPos(iX, minY, iZ));
                if (state.getBlock() != Blocks.AIR) {
                    return false;
                }
            }
        }
        return true;
    }

    // Firework mode
    @EventHandler
    public void onTickStart(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (!mc.player.isFallFlying()) return;

        if (mode.get() != Mode.FIREWORK) return;

        if (mc.options.jumpKey.isPressed()) {
            height += upFactor.get() * 0.5;
        } else if (mc.options.sneakKey.isPressed()) {
            height -= downFactor.get() * 0.5;
        }

        if (forceHeight.get()) {
            height = manualHeight.get();
        }

        Vec3d motionVector = new Vec3d(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
        double bps = motionVector.length() * 20;

        double horizSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
        double horizPct = MathHelper.clamp(horizSpeed / 1.7, 0.0, 1.0);
        double heightPct = 1 - Math.sqrt(horizPct);
        double minAngle = 0.6;

        if (horizPct >= 0.5 || mc.player.getY() > height + 1) {
            double pitch = -((45 - minAngle) * heightPct + minAngle);

            double diff = (height + 1 - mc.player.getY()) * 2;
            double heightDiffPct = MathHelper.clamp(Math.abs(diff), 0.0, 1.0);
            double pDist = -Math.toDegrees(Math.atan2(Math.abs(diff), horizSpeed * 30.0)) * Math.signum(diff);

            double adjustment = (pDist - pitch) * heightDiffPct;

            mc.player.setPitch((float) pitch);
            mc.player.setPitch(mc.player.getPitch() + (float) adjustment);
            mc.player.prevPitch = mc.player.getPitch();
        }

        if (rocketTimer.hasPassed(1000 * factor.get())) {
            double heightDiff = height - mc.player.getY();
            boolean shouldBoost = (heightDiff > 0.25 && heightDiff < 1.0) || bps < minSpeed.get();

            if (groundSafety.get()) {
                Block bottomBlock = mc.world.getBlockState(new BlockPos(mc.player.getBlockPos()).down()).getBlock();
                if (bottomBlock != Blocks.AIR && !(bottomBlock instanceof FluidBlock)) {
                    if (mc.player.getBoundingBox().minY - Math.floor(mc.player.getBoundingBox().minY) > triggerHeight.get()) {
                        shouldBoost = true;
                    }
                }
            }

            if (autoSwitch.get() && shouldBoost && mc.player.getMainHandStack().getItem() != Items.FIREWORK_ROCKET) {
                for (int l = 0; l < 9; ++l) {
                    if (mc.player.getInventory().getStack(l).getItem() == Items.FIREWORK_ROCKET) {
                        mc.player.getInventory().selectedSlot = l;
                        mc.player.getInventory().updateItems();
                        break;
                    }
                }
            }

            if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET && shouldBoost) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                rocketTimer.reset();
            }
        }
    }

    // Normal/Boost/Control mode
    @EventHandler
    public void onElytra(ElytraEvent event) {
        if (mc.world == null || mc.player == null || !hasElytra || !mc.player.isFallFlying()) return;

        if (mode.get() == Mode.PACKET || mode.get() == Mode.FIREWORK) return;

        if (event.getEntity() == mc.player && !mc.player.isTouchingWater() || mc.player != null && mc.player.getAbilities().flying && !mc.player.isInLava() || mc.player.getAbilities().flying && mc.player.isFallFlying()) {

            event.setCancelled(true);

            if (mode.get() != Mode.BOOST) {

                Vec3d lookVec = Utils.vec3d(mc.player.getBlockPos().offset(mc.player.getHorizontalFacing()));

                float pitch = mc.player.getPitch() * 0.017453292F;

                double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                double motionDist = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
                double lookVecDist = lookVec.length();

                float cosPitch = MathHelper.cos(pitch);
                cosPitch = (float) ((double) cosPitch * (double) cosPitch * Math.min(1.0D, lookVecDist / 0.4D));

                // Vanilla Glide
                if (mode.get() != Mode.CONTROL) {
                    mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y + -0.08D + (double) cosPitch * (0.06D / downFactor.get()), mc.player.getVelocity().z);
                }

                // Downwards movement
                if (mode.get() == Mode.CONTROL) {
                    // Goes down when sneaking, glides otherwise
                    if (mc.options.sneakKey.isPressed()) {
                        mc.player.setVelocity(mc.player.getVelocity().x,  -sneakDownSpeed.get(), mc.player.getVelocity().z);
                    } else if (!mc.options.jumpKey.isPressed()) {
                        mc.player.setVelocity(mc.player.getVelocity().x,  -0.00000000000003D * downFactor.get(), mc.player.getVelocity().z);
                    }
                } else if (mode.get() != Mode.CONTROL && mc.player.getVelocity().y < 0.0D && lookDist > 0.0D) {
                    // Uses pitch go go down and gain speed
                    double downSpeed = mc.player.getVelocity().y * -0.1D * (double) cosPitch;

                    mc.player.setVelocity(mc.player.getVelocity().x + (lookVec.x * downSpeed / lookDist) * factor.get(), mc.player.getVelocity().y + downSpeed, mc.player.getVelocity().z + (lookVec.z * downSpeed / lookDist) * factor.get());
                }

                // Upwards Movement
                if (pitch < 0.0F && mode.get() != Mode.CONTROL) {
                    // Normal/Boost mode - uses pitch to go up
                    double rawUpSpeed = motionDist * (double) (-MathHelper.sin(pitch)) * 0.04D;
                    mc.player.setVelocity(mc.player.getVelocity().x - lookVec.x * rawUpSpeed / lookDist, mc.player.getVelocity().y + rawUpSpeed * 3.2D * upFactor.get(), mc.player.getVelocity().z - lookVec.z * rawUpSpeed / lookDist);
                } else if (mode.get() == Mode.CONTROL && mc.options.jumpKey.isPressed()) {
                    // Control mode - goes up for as long as possible, then accelerates, then goes up again
                    if (motionDist > upFactor.get() / upFactor.get()) {
                        double rawUpSpeed = motionDist * 0.01325D;
                        mc.player.setVelocity(mc.player.getVelocity().x - lookVec.x * rawUpSpeed / lookDist, mc.player.getVelocity().y + rawUpSpeed * 3.2D, mc.player.getVelocity().z - lookVec.z * rawUpSpeed / lookDist);
                    } else {
                        double[] dir = PlayerUtil.directionSpeed(speed.get());
                        mc.player.setVelocity(dir[0], mc.player.getVelocity().y, dir[1]);
                    }
                }

                // Turning
                if (lookDist > 0.0D) {
                    mc.player.setVelocity(mc.player.getVelocity().x + (lookVec.x / lookDist * motionDist - mc.player.getVelocity().x) * 0.1D, mc.player.getVelocity().y, mc.player.getVelocity().z + (lookVec.z / lookDist * motionDist - mc.player.getVelocity().z) * 0.1D);
                }

                if (mode.get() == Mode.CONTROL && !mc.options.jumpKey.isPressed()) {
                    // Sets motion in control mode
                    double[] dir = PlayerUtil.directionSpeed(speed.get());
                    mc.player.setVelocity(dir[0], mc.player.getVelocity().y, dir[1]);
                }

                if (!noDrag.get()) {
                    mc.player.setVelocity(mc.player.getVelocity().x * 0.9900000095367432D, mc.player.getVelocity().y * 0.9800000190734863D, mc.player.getVelocity().z * 0.9900000095367432D);
                }

                // Max speed
                double finalDist = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);

                if (speedLimit.get() && finalDist > maxSpeed.get()) {
                    mc.player.setVelocity(mc.player.getVelocity().x * maxSpeed.get() / finalDist, mc.player.getVelocity().y, mc.player.getVelocity().z * maxSpeed.get() / finalDist);
                }

                mc.player.move(MovementType.SELF, new Vec3d(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z));
            } else {
                boolean shouldBoost = false;
                float moveForward = mc.player.input.movementForward;

                if (cruiseControl.get()) {
                    if (mc.options.jumpKey.isPressed()) {
                        height += upFactor.get() * 0.5;
                    } else if (mc.options.sneakKey.isPressed()) {
                        height -= downFactor.get() * 0.5;
                    }

                    if (forceHeight.get()) {
                        height = manualHeight.get();
                    }

                    double horizSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
                    double horizPct = MathHelper.clamp(horizSpeed / 1.7, 0.0, 1.0);
                    double heightPct = 1 - Math.sqrt(horizPct);
                    double minAngle = 0.6;

                    if (horizSpeed >= minUpSpeed.get() && instantFlyTimer.hasPassed(2000F * packetDelay.get())) {
                        double pitch = -((45 - minAngle) * heightPct + minAngle);

                        double diff = (height + 1 - mc.player.getY()) * 2;
                        double heightDiffPct = MathHelper.clamp(Math.abs(diff), 0.0, 1.0);
                        double pDist = -Math.toDegrees(Math.atan2(Math.abs(diff), horizSpeed * 30.0)) * Math.signum(diff);

                        double adjustment = (pDist - pitch) * heightDiffPct;

                        mc.player.setPitch((float) pitch);
                        mc.player.setPitch(mc.player.getPitch() + (float) adjustment);
                        mc.player.prevPitch = mc.player.getPitch();
                    } else {
                        mc.player.setPitch(0.25F);
                        mc.player.prevPitch = 0.25F;
                        moveForward = 1F;
                    }
                }

                Vec3d vec3d = mc.player.getVelocity();

                float f = mc.player.getPitch() * 0.017453292F;

                double d6 = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
                double d8 = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
                double d1 = vec3d.length();
                float f4 = MathHelper.cos(f);
                f4 = (float)((double)f4 * (double)f4 * Math.min(1.0D, d1 / 0.4D));
                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y + -0.08D + (double)f4 * 0.06D, mc.player.getVelocity().z);

                if (mc.player.getVelocity().y < 0.0D && d6 > 0.0D) {
                    double d2 = mc.player.getVelocity().y * -0.1D * (double)f4;
                    mc.player.setVelocity(mc.player.getVelocity().x + vec3d.x * d2 / d6, mc.player.getVelocity().y + d2, mc.player.getVelocity().z + vec3d.z * d2 / d6);
                }

                if (f < 0.0F) {
                    double d10 = d8 * (double)(-MathHelper.sin(f)) * 0.04D;
                    mc.player.setVelocity(mc.player.getVelocity().x - vec3d.x * d10 / d6, mc.player.getVelocity().y + d10 * 3.2D, mc.player.getVelocity().z - vec3d.z * d10 / d6);
                }

                if (d6 > 0.0D) {
                    mc.player.setVelocity(mc.player.getVelocity().x + (vec3d.x / d6 * d8 - mc.player.getVelocity().x) * 0.1D, mc.player.getVelocity().y, mc.player.getVelocity().z + (vec3d.z / d6 * d8 - mc.player.getVelocity().z) * 0.1D);
                }

                if (!noDrag.get()) {
                    mc.player.setVelocity(mc.player.getVelocity().x * 0.9900000095367432D, mc.player.getVelocity().y * 0.9800000190734863D, mc.player.getVelocity().z * 0.9900000095367432D);
                }

                float yaw = mc.player.getYaw() * 0.017453292F;

                if (f > 0F && (mc.player.getVelocity().y < 0D || shouldBoost)) {
                    if (moveForward != 0F && instantFlyTimer.hasPassed(2000F * packetDelay.get()) && staticTimer.hasPassed(1000F * staticDelay.get())) {
                        if (stopMotion.get()) {
                            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
                        }
                        instantFlyTimer.reset();
                        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    } else if (!instantFlyTimer.hasPassed(2000F * packetDelay.get())) {
                        mc.player.setVelocity(mc.player.getVelocity().x - moveForward * Math.sin(yaw) * factor.get() / 20F, mc.player.getVelocity().y, mc.player.getVelocity().z + moveForward * Math.cos(yaw) * factor.get() / 20F);
                        staticTimer.reset();
                    }
                }

                double finalDist = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);

                if (speedLimit.get() && finalDist > maxSpeed.get()) {
                    mc.player.setVelocity(mc.player.getVelocity().x * maxSpeed.get() / finalDist, mc.player.getVelocity().y, mc.player.getVelocity().z * maxSpeed.get() / finalDist);
                }


                mc.player.move(MovementType.SELF, new Vec3d(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z) );
            }
        }
    }

    // Packet Mode
    @EventHandler(priority = 30)
    public void onWalkingUpdatePlayer(UpdateWalkingPlayerEvent.Pre event) {
        MeteorClient.EVENT_BUS.post(timerManager);
        MeteorClient.EVENT_BUS.subscribe(timerManager);
        if (!mc.player.isOnGround() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && mode.get() == Mode.PACKET) {
            if (infDurability.get() || !mc.player.isFallFlying()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mode.get() == Mode.PACKET) {
            if (mc.player.isOnGround()) {
                return;
            }

            if (!hasElytra) return;

            if (accelerate.get()) {
                if (rSpeed) {
                    curSpeed = 1.0;
                    rSpeed = false;
                }
                if (curSpeed < factor.get()) {
                    curSpeed += 0.1 * acceleration.get();
                }
                if (curSpeed - 0.1 > factor.get()) {
                    curSpeed -= 0.1 * acceleration.get();
                }
            } else {
                curSpeed = factor.get();
            }

            if (mc.options.jumpKey.isPressed()) {
                mc.player.setVelocity(mc.player.getVelocity().x, upFactor.get(), mc.player.getVelocity().z);
                event.setY(mc.player.getVelocity().y);
            } else if (mc.options.sneakKey.isPressed()) {
                mc.player.setVelocity(mc.player.getVelocity().x, -downFactor.get(), mc.player.getVelocity().z);
                event.setY(mc.player.getVelocity().y);
            } else if (strict.get() == StrictMode.NORMAL) {
                if (mc.player.age % 32 == 0 && !rSpeed && (Math.abs(event.getX()) >= 0.05 || Math.abs(event.getZ()) >= 0.05)) {
                    mc.player.setVelocity(mc.player.getVelocity().x, -2.0E-4, mc.player.getVelocity().z);
                    event.setY(0.006200000000000001);
                } else {
                    mc.player.setVelocity(mc.player.getVelocity().x, -2.0E-4, mc.player.getVelocity().z);
                    event.setY(-2.0E-4);
                }
            } else if (strict.get() == StrictMode.GLIDE) {
                mc.player.setVelocity(mc.player.getVelocity().x, -0.00001F, mc.player.getVelocity().z);
                event.setY(-0.00001F);
            } else {
                mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
                event.setY(0.0);
            }

            event.setX(event.getX() * (rSpeed ? 0.0 : curSpeed));
            event.setZ(event.getZ() * (rSpeed ? 0.0 : curSpeed));

            if (antiKick.get() && event.getX() == 0.0 && event.getZ() == 0.0 && !rSpeed) {
                event.setX(Math.sin(Math.toRadians(mc.player.age % 360)) * 0.03);
                event.setZ(Math.cos(Math.toRadians(mc.player.age % 360)) * 0.03);
            }

            rSpeed = false;
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket && strict.get() == StrictMode.NCP && mode.get() == Mode.PACKET && !rSpeed && (Math.abs(mc.player.getVelocity().x) >= 0.05 || Math.abs(mc.player.getVelocity().z) >= 0.05)) {
            double randomV = 1.0E-8 + 1.0E-8 * (1.0 + random.nextInt(1 + (random.nextBoolean() ? random.nextInt(34) : random.nextInt(43))));
            if (mc.player.isOnGround() || mc.player.age % 2 == 0) {
                ((IPlayerMovePacket) event.packet).setY(((IPlayerMovePacket) event.packet).getY() + randomV);
            } else {
                ((IPlayerMovePacket) event.packet).setY(((IPlayerMovePacket) event.packet).getY() - randomV);
            }
        }
    }

    public TimerManager timerManager =  new TimerManager();

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;
        if (event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround) {
            if (mode.get() == Mode.PACKET || mode.get() == Mode.FIREWORK) {
                if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                    rSpeed = true;
                }
                if (mc.player.isFallFlying()) {
                    rocketTimer.reset();
                    if (mc.player != null) {
                        height = mc.player.getY();
                    }
                }
            }
        } else if (event.packet instanceof EntityStatusS2CPacket) {
            EntityStatusS2CPacket packet = (EntityStatusS2CPacket) event.packet;
            if (packet.getEntity(mc.world) == mc.player) {
                timerManager.resetTimer(this);
                if (mode.get() == Mode.PACKET) {
                    event.cancel();
                }
            }
        }
    }
}
