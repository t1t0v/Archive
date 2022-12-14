package naturalism.addon.netherbane.modules.movement;


import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Jesus;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import naturalism.addon.netherbane.events.InteractEvent;
import naturalism.addon.netherbane.events.PlayerMoveEvent;
import naturalism.addon.netherbane.events.PlayerUpdateEvent;
import naturalism.addon.netherbane.events.UpdateWalkingPlayerEvent;
import naturalism.addon.netherbane.mixins.IPlayerMoveC2SPacket;
import naturalism.addon.netherbane.utils.TimerManager;
import net.minecraft.block.Blocks;
import net.minecraft.block.IceBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import naturalism.addon.netherbane.NetherBane;

import naturalism.addon.netherbane.utils.PlayerUtil;
import naturalism.addon.netherbane.utils.Timer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


public class SpeedPlus extends Module {
        public SpeedPlus(){
            super(NetherBane.MOVEMENTPLUS, "speed-plus", "");
        }

        public enum Mode{
            STRAFE, STRAFESTRICT, ONGROUND, LOWHOP, SMALLHOP, TP, STRAFEOLD
        }

        private final SettingGroup sgDefault = settings.createGroup("Default");
        private final Setting<Double> speed = sgDefault.add(new DoubleSetting.Builder().name("speed").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).build());
        private final Setting<Boolean> useTimer = sgDefault.add(new BoolSetting.Builder().name("use-timer").description("Automatically rotates you towards the city block.").defaultValue(true).build());
        private final Setting<Double> timerFactor = sgDefault.add(new DoubleSetting.Builder().name("timer-factor").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).visible(useTimer::get).build());

        private final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>().name("mode").description("Block breaking method").defaultValue(Mode.STRAFE).build());
        private final Setting<Boolean> bypass = sgDefault.add(new BoolSetting.Builder().name("bypass").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> getMode() == Mode.ONGROUND).build());
        private final Setting<Boolean> hypixel = sgDefault.add(new BoolSetting.Builder().name("hypixel").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> getMode() == Mode.STRAFEOLD).build());
        private final Setting<Boolean> allowEat = sgDefault.add(new BoolSetting.Builder().name("allow-eat").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> getMode() == Mode.STRAFESTRICT).build());
        private final Setting<Boolean> strict = sgDefault.add(new BoolSetting.Builder().name("allow-eat").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> getMode() == Mode.STRAFE).build());
        private final Setting<Boolean> disableOnSneak = sgDefault.add(new BoolSetting.Builder().name("disable-on-sneak").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> getMode() == Mode.STRAFE).build());
        private final Setting<Boolean> forceSprint = sgDefault.add(new BoolSetting.Builder().name("force-sprint").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> getMode() == Mode.STRAFE).build());
        private final Setting<Boolean> boost = sgDefault.add(new BoolSetting.Builder().name("boost").description("Automatically rotates you towards the city block.").defaultValue(false).visible(() -> getMode() == Mode.SMALLHOP || getMode() == Mode.STRAFE || getMode() == Mode.STRAFEOLD || getMode() == Mode.STRAFESTRICT).build());
        private final Setting<Double> boostFactor = sgDefault.add(new DoubleSetting.Builder().name("boostFactor").description("The radius in which players get targeted.").defaultValue(1).min(0).sliderMax(10).visible(() -> getMode() == Mode.SMALLHOP && boost.get()).build());

        private final SettingGroup sgAlternative = settings.createGroup("Alternative");
        private final Setting<Mode> altMode = sgAlternative.add(new EnumSetting.Builder<Mode>().name("alt-mode").description("Block breaking method").defaultValue(Mode.ONGROUND).build());
        private final Setting<Keybind> altBind = sgAlternative.add(new KeybindSetting.Builder().name("alt-bind").description("Change the pickaxe slot to an iron one when pressing the button.").defaultValue(Keybind.fromKey(8)).build());

        private Mode getMode() {
            if (altBind.get().isPressed()) {
                return altMode.get();
            }
            return mode.get();
        }

        private int strafeStage = 1;

        public int hopStage;

        private double ncpPrevMotion = 0.0D;

        private double horizontal;

        // strafe normal
        private double currentSpeed = 0.0D;
        private double prevMotion = 0.0D;
        private boolean oddStage = false;
        private int state = 4;

        // aac
        private double aacSpeed = 0.2873D;
        private int aacCounter;
        private int aacState = 4;
        private int ticksPassed = 0;

        private boolean sneaking;

        private double maxVelocity = 0;
        private Timer velocityTimer = new Timer();

        private Timer setbackTimer = new Timer();

        private int lowHopStage;
        private double lowHopSpeed;
        private boolean even;

        private int onGroundStage;
        private double onGroundSpeed;
        private boolean forceGround;

        public TimerManager timerManager =  new TimerManager();

        @EventHandler
        public void onPlayerUseItem(InteractEvent event) {
            if (!sneaking && getMode() == Mode.STRAFESTRICT && allowEat.get()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                sneaking = true;
            }
        }

        @EventHandler
        private void onTickPre(TickEvent.Pre event){
            MeteorClient.EVENT_BUS.post(timerManager);
            MeteorClient.EVENT_BUS.subscribe(timerManager);
            if (mc.player == null || mc.world == null) return;
            double dX = mc.player.getX() - mc.player.prevX;
            double dZ = mc.player.getZ() - mc.player.prevZ;
            prevMotion = Math.sqrt(dX * dX + dZ * dZ);
        }

        @EventHandler
        public void onUpdate(TickEvent.Post event) {
            MeteorClient.EVENT_BUS.post(timerManager);
            MeteorClient.EVENT_BUS.subscribe(timerManager);
            if (mc.player == null || mc.world == null) return;
            if ((Modules.get().isActive(ElytraFly.class) || Modules.get().isActive(naturalism.addon.netherbane.modules.movement.ElytraFly.class)) && mc.player.isFallFlying()) {
                timerManager.resetTimer(this);
                return;
            }
            int minY = MathHelper.floor(mc.player.getBoundingBox().minY - 0.2D);
            boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
            if (Modules.get().isActive(Jesus.class) && (mc.player.isTouchingWater() || mc.player.isInLava() || inLiquid)) return;
            if(disableOnSneak.get() && mc.player.isSneaking()) return;
            if ((getMode() == Mode.STRAFEOLD || getMode() == Mode.STRAFE || getMode() == Mode.LOWHOP) && useTimer.get()) {
                timerManager.updateTimer(this, 10, (float) (1.080f + (0.008f * timerFactor.get())));
            } else if (getMode() != Mode.STRAFESTRICT && getMode() != Mode.SMALLHOP) {
                timerManager.resetTimer(this);
            }
            switch (getMode()) {
                case STRAFEOLD: {
                    if (Modules.get().isActive(ElytraFly.class) || Modules.get().isActive(naturalism.addon.netherbane.modules.movement.ElytraFly.class))
                        return;
                    if (forceSprint.get()) {
                        if (!mc.player.isSprinting() && PlayerUtil.isPlayerMoving()) {
                            mc.player.setSprinting(true);
                            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                        }
                    }

                    ncpPrevMotion = Math.sqrt((mc.player.getX() - mc.player.prevX) * (mc.player.getX() - mc.player.prevX) + (mc.player.getZ() - mc.player.prevZ) * (mc.player.getZ() - mc.player.prevZ));
                    break;
                }
                case SMALLHOP: {
                    // Ily auto
                    if (!PlayerUtil.isPlayerMoving() || mc.player.horizontalCollision) {
                        timerManager.resetTimer(this);
                        return;
                    }
                    if (mc.player.isOnGround()) {
                        timerManager.updateTimer(this, 10, 1.15f);
                        mc.player.jump();
                        boolean ice = mc.world.getBlockState(new BlockPos(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ())).getBlock() instanceof IceBlock || mc.world.getBlockState(new BlockPos(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ())).getBlock() == Blocks.PACKED_ICE;
                        double[] dirSpeed = PlayerUtil.directionSpeed((getBaseMotionSpeed() * speed.get()) + (boost.get() ? (ice ? 0.3 : 0.06 * boostFactor.get()) : 0D));
                        mc.player.setVelocity(dirSpeed[0], mc.player.getVelocity().y, dirSpeed[1]);
                    } else {
                        mc.player.setVelocity(mc.player.getVelocity().x, -1, mc.player.getVelocity().z);
                        timerManager.resetTimer(this);
                    }
                    break;
                }
            }

            Item item = mc.player.getActiveItem().getItem();
            if (getMode() == Mode.STRAFESTRICT && allowEat.get() && sneaking && ((!mc.player.isUsingItem() && item.isFood() || item instanceof BowItem || item instanceof PotionItem) || (!(item.isFood()) || !(item instanceof BowItem) || !(item instanceof PotionItem)))) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                sneaking = false;
            }
        }

        @EventHandler
        public void onPlayerUpdate(PlayerUpdateEvent event) {
            MeteorClient.EVENT_BUS.post(timerManager);
            MeteorClient.EVENT_BUS.subscribe(timerManager);
            if(mc.player == null || mc.world == null) return;

            if ((Modules.get().isActive(ElytraFly.class) || Modules.get().isActive(naturalism.addon.netherbane.modules.movement.ElytraFly.class)) && mc.player.isFallFlying()) {
                return;
            }

            switch (getMode()) {
                case TP: {
                    for (double x = 0.0625; x < speed.get(); x += 0.262) {
                        double[] dir = PlayerUtil.directionSpeed(x);
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1], mc.player.isOnGround()));
                    }
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + mc.player.getVelocity().x, mc.player.getY() <= 10 ? 255 : 1, mc.player.getZ() + mc.player.getVelocity().z, mc.player.isOnGround()));
                }
            }
        }

        @EventHandler(priority = 1000)
        public void onUpdateWalkingPlayerPre(UpdateWalkingPlayerEvent.Pre event) {
            if ((Modules.get().isActive(ElytraFly.class) || Modules.get().isActive(naturalism.addon.netherbane.modules.movement.ElytraFly.class)) && mc.player.isFallFlying()) {
                return;
            }

            if (!PlayerUtil.isPlayerMoving()) {
                currentSpeed = 0.0;
                if (getMode() != Mode.SMALLHOP) {
                    mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
                    return;
                }
            }

            if (getMode() == Mode.STRAFE || getMode() == Mode.STRAFESTRICT || getMode() == Mode.LOWHOP) {
                double dX = mc.player.getX() - mc.player.prevX;
                double dZ = mc.player.getZ() - mc.player.prevZ;
                prevMotion = Math.sqrt(dX * dX + dZ * dZ);
            }
        }

        @EventHandler
        public void onPacketSend(PacketEvent.Send event) {
            if (event.packet instanceof PlayerMoveC2SPacket && forceGround) {
                forceGround = false;
                ((IPlayerMoveC2SPacket) event.packet).setOnGround(true);
            }
        }

        @EventHandler(priority = 69)
        public void onPacketReceive(PacketEvent.Receive event) {
            if ((Modules.get().isActive(ElytraFly.class) || Modules.get().isActive(naturalism.addon.netherbane.modules.movement.ElytraFly.class)) && mc.player.isFallFlying()) {
                return;
            }

            if (event.packet instanceof PlayerPositionLookS2CPacket) {
                timerManager.resetTimer(this);
                ncpPrevMotion = 0.0D;
                currentSpeed = 0.0D;
                horizontal = 0;
                state = 4;
                aacSpeed = 0.2873;
                aacState = 4;
                prevMotion = 0;
                aacCounter = 0;
                maxVelocity = 0;
                setbackTimer.reset();
                lowHopStage = 4;
                onGroundStage = 2;
                onGroundSpeed = 0;
            } else if (event.packet instanceof ExplosionS2CPacket) {
                ExplosionS2CPacket velocity = (ExplosionS2CPacket) event.packet;
                maxVelocity = Math.sqrt(velocity.getPlayerVelocityX() * velocity.getPlayerVelocityX() + velocity.getPlayerVelocityZ() * velocity.getPlayerVelocityZ());
                velocityTimer.reset();
            }
        }

        @EventHandler
        public void onMove(PlayerMoveEvent event) {
            if (mc.player == null || mc.world == null) return;
            if(disableOnSneak.get() && mc.player.isSneaking()) return;

            if ((Modules.get().isActive(ElytraFly.class) || Modules.get().isActive(naturalism.addon.netherbane.modules.movement.ElytraFly.class)) && mc.player.isFallFlying()) {
                return;
            }

            boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
            if (Modules.get().isActive(Jesus.class) && (mc.player.isTouchingWater() || mc.player.isInLava() || inLiquid)) return;

            switch (getMode()) {
                // Normal Strafe
                case STRAFE: {
                    if (state != 1 || (mc.player.forwardSpeed == 0.0f || mc.player.sidewaysSpeed == 0.0f)) {
                        if (state == 2 && (mc.player.forwardSpeed != 0.0f || mc.player.sidewaysSpeed != 0.0f)) {
                            double jumpSpeed = 0.0D;

                            if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                                jumpSpeed += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
                            }

                            mc.player.setVelocity(mc.player.getVelocity().x, (hypixel.get() ? 0.3999999463558197D : 0.3999D) + jumpSpeed, mc.player.getVelocity().z);
                            event.setY(mc.player.getVelocity().y);
                            currentSpeed *= oddStage ? 1.6835D : 1.395D;
                        } else if (state == 3) {
                            double adjustedMotion = 0.66D * (prevMotion - getBaseMotionSpeed());
                            currentSpeed = prevMotion - adjustedMotion;
                            oddStage = !oddStage;
                        } else {
                            List<VoxelShape> collisionBoxes = mc.world.getEntityCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0));
                            if ((collisionBoxes.size() > 0 || mc.player.verticalCollision) && state > 0) {
                                state = mc.player.forwardSpeed == 0.0f && mc.player.sidewaysSpeed == 0.0f ? 0 : 1;
                            }
                            currentSpeed = prevMotion - prevMotion / 159.0;
                        }
                    } else {
                        currentSpeed = 1.35D * getBaseMotionSpeed() - 0.01D;
                    }

                    currentSpeed = Math.max(currentSpeed, getBaseMotionSpeed());

                    if (maxVelocity > 0 && boost.get() && !velocityTimer.hasPassed(75) && !mc.player.horizontalCollision) {
                        currentSpeed = Math.max(currentSpeed, maxVelocity);
                    } else if (strict.get()) {
                        currentSpeed = Math.min(currentSpeed, 0.433D);
                    }

                    double forward = mc.player.input.movementForward;
                    double strafe = mc.player.input.movementSideways;
                    float yaw = mc.player.getYaw();

                    if (forward == 0.0D && strafe == 0.0D) {
                        event.setX(0.0D);
                        event.setZ(0.0D);
                    } else {
                        if (forward != 0.0D) {
                            if (strafe > 0.0D) {
                                yaw += (float)(forward > 0.0D ? -45 : 45);
                            } else if (strafe < 0.0D) {
                                yaw += (float)(forward > 0.0D ? 45 : -45);
                            }

                            strafe = 0.0D;

                            if (forward > 0.0D) {
                                forward = 1.0D;
                            } else if (forward < 0.0D) {
                                forward = -1.0D;
                            }
                        }

                        event.setX(forward * currentSpeed * Math.cos(Math.toRadians(yaw + 90.0F)) + strafe * currentSpeed * Math.sin(Math.toRadians(yaw + 90.0F)));
                        event.setZ(forward * currentSpeed * Math.sin(Math.toRadians(yaw + 90.0F)) - strafe * currentSpeed * Math.cos(Math.toRadians(yaw + 90.0F)));
                    }


                    if (mc.player.forwardSpeed == 0.0f && mc.player.sidewaysSpeed == 0.0f) {
                        return;
                    }

                    state++;
                    break;
                }
                // Legacy NCP Strafe - instead of directly modifing movement we do manual calculations
                case STRAFEOLD: {
                    if (getMode() == Mode.STRAFEOLD && (Modules.get().isActive(ElytraFly.class) || Modules.get().isActive(naturalism.addon.netherbane.modules.movement.ElytraFly.class)))
                        return;
                    if (!mc.player.isSprinting()) {
                        mc.player.setSprinting(true);
                        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                    }

                    double adjustedSpeed = speed.get() * 0.99;

                    double vertical;

                    switch (strafeStage) {
                        case 0:
                            strafeStage++;
                            ncpPrevMotion = 0.0D;
                            break;
                        case 2:
                            vertical = hypixel.get() ? 0.3999999463558197D : 0.40123128D;
                            if ((mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F) && mc.player.isOnGround()) {
                                mc.player.setVelocity(mc.player.getVelocity().x, vertical, mc.player.getVelocity().z);
                                event.setY(mc.player.getVelocity().y);
                                horizontal *= 2.149D;
                            }
                            break;
                        case 3:
                            horizontal = ncpPrevMotion - 0.76D * (ncpPrevMotion - getBaseMotionSpeed());
                            break;
                        default:
                            horizontal = ncpPrevMotion - ncpPrevMotion / 159D;
                            if ((mc.world.getEntityCollisions(mc.player, mc.player.getBoundingBox().offset(0.0D, mc.player.getVelocity().y, 0.0D)).size() > 0 || mc.player.verticalCollision) && strafeStage > 0) {
                                strafeStage = (mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F) ? 1 : 0;
                            }
                            break;
                    }

                    horizontal = Math.max(horizontal, getBaseMotionSpeed());

                    if (maxVelocity > 0 && boost.get() && !velocityTimer.hasPassed(75) && !mc.player.horizontalCollision) {
                        horizontal = Math.max(horizontal, maxVelocity);
                    }

                    float forward = mc.player.input.movementForward;
                    float strafe = mc.player.input.movementSideways;

                    if (forward == 0 && strafe == 0) {
                        event.setX(0D);
                        event.setZ(0D);
                    } else if (forward != 0.0D && strafe != 0.0D) {
                        // Magical Numbers wtf :flushed:
                        forward *= Math.sin(0.7853981633974483D);
                        strafe *= Math.cos(0.7853981633974483D);
                    }
                    event.setX((forward * horizontal * -Math.sin(Math.toRadians(mc.player.getYaw())) + strafe * horizontal * Math.cos(Math.toRadians(mc.player.getYaw()))) * adjustedSpeed);
                    event.setZ((forward * horizontal * Math.cos(Math.toRadians(mc.player.getYaw())) - strafe * horizontal * -Math.sin(Math.toRadians(mc.player.getYaw()))) * adjustedSpeed);

                    strafeStage++;
                    break;
                }
                // Strict NPC Strafe for NCP-Updated
                case STRAFESTRICT: {
                    aacCounter++;
                    aacCounter %= 5;

                    if (aacCounter != 0) {
                        timerManager.resetTimer(this);
                    } else if (PlayerUtil.isPlayerMoving()) {
                        timerManager.updateTimer(this, 10, 1.3F);
                        mc.player.setVelocity(mc.player.getVelocity().x * 1.0199999809265137D, mc.player.getVelocity().y, mc.player.getVelocity().z * 1.0199999809265137D);
                    }

                    if (mc.player.isOnGround() && PlayerUtil.isPlayerMoving()) {
                        aacState = 2;
                    }

                    if (round(mc.player.getY() - (int)mc.player.getY(), 3) == round(0.138D, 3)) {
                        mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y - 0.08D, mc.player.getVelocity().z);
                        event.setY(event.getY() - 0.09316090325960147D);
                        mc.player.setPos(mc.player.getX(), mc.player.getY() - 0.09316090325960147D, mc.player.getZ());
                    }

                    if (aacState == 1 && (mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F)) {
                        aacState = 2;
                        aacSpeed = 1.38D * getBaseMotionSpeed() - 0.01D;
                    } else if (aacState == 2) {
                        aacState = 3;
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.399399995803833D, mc.player.getVelocity().z);
                        event.setY(0.399399995803833D);
                        aacSpeed *= 2.149D;
                    } else if (aacState == 3) {
                        aacState = 4;
                        double adjustedMotion = 0.66D * (prevMotion - getBaseMotionSpeed());
                        aacSpeed = prevMotion - adjustedMotion;
                    } else {
                        if (mc.world.getEntityCollisions(mc.player, mc.player
                            .getBoundingBox().offset(0.0D, mc.player.getVelocity().y, 0.0D)).size() > 0 || mc.player.verticalCollision)
                            aacState = 1;
                        aacSpeed = prevMotion - prevMotion / 159.0D;
                    }

                    aacSpeed = Math.max(aacSpeed, getBaseMotionSpeed());

                    if (maxVelocity > 0 && boost.get() && !velocityTimer.hasPassed(75) && !mc.player.horizontalCollision) {
                        aacSpeed = Math.max(aacSpeed, maxVelocity);
                    } else {
                        aacSpeed = Math.min(aacSpeed, (ticksPassed > 25) ? 0.449D : 0.433D);
                    }

                    float forward = mc.player.input.movementForward;
                    float strafe = mc.player.input.movementSideways;
                    float yaw = mc.player.getYaw();

                    ticksPassed++;

                    if (ticksPassed > 50)
                        ticksPassed = 0;
                    if (forward == 0.0F && strafe == 0.0F) {
                        event.setX(0.0D);
                        event.setZ(0.0D);
                    } else if (forward != 0.0F) {
                        if (strafe >= 1.0F) {
                            yaw += ((forward > 0.0F) ? -45 : 45);
                            strafe = 0.0F;
                        } else if (strafe <= -1.0F) {
                            yaw += ((forward > 0.0F) ? 45 : -45);
                            strafe = 0.0F;
                        }
                        if (forward > 0.0F) {
                            forward = 1.0F;
                        } else if (forward < 0.0F) {
                            forward = -1.0F;
                        }
                    }

                    double cos = Math.cos(Math.toRadians((yaw + 90.0F)));
                    double sin = Math.sin(Math.toRadians((yaw + 90.0F)));

                    event.setX(forward * aacSpeed * cos + strafe * aacSpeed * sin);
                    event.setZ(forward * aacSpeed * sin - strafe * aacSpeed * cos);

                    if (forward == 0.0F && strafe == 0.0F) {
                        event.setX(0.0D);
                        event.setZ(0.0D);
                    }

                    break;
                }
                // LowHop Speed
                case LOWHOP: {
                    if (!setbackTimer.hasPassed(100L)) return;

                    double jumpSpeed = 0.0D;

                    if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                        jumpSpeed += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
                    }

                    if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.4, 3)) {
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.31 + jumpSpeed, mc.player.getVelocity().z);
                        event.setY(mc.player.getVelocity().y);
                    } else if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.71, 3)) {
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.04 + jumpSpeed, mc.player.getVelocity().z);
                        event.setY(mc.player.getVelocity().y);
                    } else if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.75, 3)) {
                        mc.player.setVelocity(mc.player.getVelocity().x, -0.2 - jumpSpeed, mc.player.getVelocity().z);
                        event.setY(mc.player.getVelocity().y);
                    } else if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.55, 3)) {
                        mc.player.setVelocity(mc.player.getVelocity().x, -0.14 + jumpSpeed, mc.player.getVelocity().z);
                        event.setY(mc.player.getVelocity().y);
                    } else {
                        if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.41, 3)) {
                            mc.player.setVelocity(mc.player.getVelocity().x, -0.2 + jumpSpeed, mc.player.getVelocity().z);
                            event.setY(mc.player.getVelocity().y);
                        }
                    }

                    if (lowHopStage == 1 && (mc.player.forwardSpeed != 0F || mc.player.sidewaysSpeed != 0F)) {
                        lowHopSpeed = 1.35 * getBaseMotionSpeed() - 0.01;
                    } else if (lowHopStage == 2 && (mc.player.forwardSpeed != 0F || mc.player.sidewaysSpeed != 0F)) {
                        mc.player.setVelocity(mc.player.getVelocity().x, (checkHeadspace() ? 0.2 : 0.3999) + jumpSpeed, mc.player.getVelocity().z);
                        event.setY(mc.player.getVelocity().y);
                        lowHopSpeed = lowHopSpeed * (even ? 1.5685 : 1.3445);
                    } else if (lowHopStage == 3) {
                        double dV = 0.66 * (prevMotion - getBaseMotionSpeed());
                        lowHopSpeed = prevMotion - dV;
                        even = !even;
                    } else {
                        if (mc.player.isOnGround() && lowHopStage > 0) {
                            lowHopStage = mc.player.forwardSpeed != 0.0f || mc.player.sidewaysSpeed != 0.0f ? 1 : 0;
                        }
                        lowHopSpeed = prevMotion - prevMotion / 159.0D;
                    }

                    lowHopSpeed = Math.max(lowHopSpeed, getBaseMotionSpeed());

                    float forward = mc.player.input.movementForward;
                    float strafe = mc.player.input.movementSideways;

                    if (forward == 0 && strafe == 0) {
                        event.setX(0D);
                        event.setZ(0D);
                    } else if (forward != 0.0D && strafe != 0.0D) {
                        forward *= Math.sin(0.7853981633974483D);
                        strafe *= Math.cos(0.7853981633974483D);
                    }
                    event.setX(forward * lowHopSpeed * -Math.sin(Math.toRadians(mc.player.getYaw())) + strafe * lowHopSpeed * Math.cos(Math.toRadians(mc.player.getYaw())));
                    event.setZ(forward * lowHopSpeed * Math.cos(Math.toRadians(mc.player.getYaw())) - strafe * lowHopSpeed * -Math.sin(Math.toRadians(mc.player.getYaw())));

                    if (mc.player.forwardSpeed == 0F && mc.player.sidewaysSpeed == 0F) return;
                    lowHopStage++;
                    break;
                }
                case ONGROUND: {
                    if (!mc.player.isOnGround()) {
                        if (onGroundStage != 3) return;
                    }
                    if (!((mc.player.horizontalCollision || mc.player.forwardSpeed == 0) && mc.player.sidewaysSpeed == 0)) {
                        if (onGroundStage == 2) {
                            mc.player.setVelocity(mc.player.getVelocity().x, -0.5, mc.player.getVelocity().z);
                            event.setY(checkHeadspace() ? 0.2 : 0.4);
                            onGroundSpeed *= 2.149;
                            onGroundStage = 3;

                            if (bypass.get()) {
                                forceGround = true;
                            }
                        } else if (onGroundStage == 3) {
                            double adjustedSpeed = 0.66 * (prevMotion - getBaseMotionSpeed());
                            onGroundSpeed = prevMotion - adjustedSpeed;
                            onGroundStage = 2;
                        } else {
                            if (checkHeadspace() || mc.player.verticalCollision) {
                                onGroundStage = 1;
                            }
                        }
                    }

                    onGroundSpeed = Math.min(Math.max(onGroundSpeed, getBaseMotionSpeed()), speed.get());

                    float forward = mc.player.input.movementForward;
                    float strafe = mc.player.input.movementSideways;

                    if (forward == 0 && strafe == 0) {
                        event.setX(0D);
                        event.setZ(0D);
                    } else if (forward != 0.0D && strafe != 0.0D) {
                        forward *= Math.sin(0.7853981633974483D);
                        strafe *= Math.cos(0.7853981633974483D);
                    }
                    event.setX(forward * onGroundSpeed * -Math.sin(Math.toRadians(mc.player.getYaw())) + strafe * onGroundSpeed * Math.cos(Math.toRadians(mc.player.getYaw())));
                    event.setZ(forward * onGroundSpeed * Math.cos(Math.toRadians(mc.player.getYaw())) - strafe * onGroundSpeed * -Math.sin(Math.toRadians(mc.player.getYaw())));

                    break;
                }
            }
        }

        private boolean checkHeadspace() {
            return mc.world.getEntityCollisions(mc.player, mc.player.getBoundingBox().offset(0D, 0.21D, 0D)).size() > 0;
        }

        @Override
        public void onActivate() {
            if (mc.player == null || mc.world == null) {
                toggle();
                return;
            }
            maxVelocity = 0;
            hopStage = 1;
            lowHopStage = 4;
            onGroundStage = 2;
            switch (getMode()) {
                case STRAFEOLD: {
                    if (!mc.player.isSprinting() && PlayerUtil.isPlayerMoving()) {
                        mc.player.setSprinting(true);
                        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                    }
                }
                case STRAFE: {
                    state = 4;
                    currentSpeed = getBaseMotionSpeed();
                    prevMotion = 0;
                }
            }
        }

        @Override
        public void onDeactivate() {
            if (mc.player == null || mc.world == null) return;
            if (getMode() == Mode.SMALLHOP) {
                mc.player.setVelocity(0, 0, 0);
            }
            timerManager.resetTimer(this);
        }

        private double getBaseMotionSpeed() {
            double baseSpeed = (getMode() == Mode.STRAFE || getMode() == Mode.STRAFESTRICT || getMode() == Mode.SMALLHOP || getMode() == Mode.ONGROUND || getMode() == Mode.LOWHOP) ? 0.2873D : 0.272D;
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
                baseSpeed *= 1.0D + 0.2D * ((double) amplifier + 1);
            }
            return baseSpeed;
        }

        private double round(double value, int places) {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }

    }
