package naturalism.addon.netherbane.utils;

import meteordevelopment.meteorclient.utils.player.DamageUtils;
import naturalism.addon.netherbane.modules.combat.CrystalBoomer;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.explosion.Explosion;

import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CrystalUtil {
    public static float calculateDamage(EndCrystalEntity crystal, Entity entity, boolean predict, BlockPos obsidianPos, boolean ignoreTerrain) {
        if (crystal == null) return 0;
        return calculateDamage(crystal.getX(), crystal.getY(), crystal.getZ(), entity, predict, obsidianPos, ignoreTerrain);
    }

    public static float calculateDamage(BlockPos pos, Entity entity, boolean predict, BlockPos obsidianPos, boolean ignoreTerrain) {
        if (pos == null) return 0;
        return calculateDamage(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, entity, predict, pos, ignoreTerrain);
    }

    public static float calculateDamage(double posX, double posY, double posZ, Entity entity, boolean predict, BlockPos obsidianPos, boolean ignoreTerrain) {
        double finald = DamageUtils.crystalDamage((PlayerEntity) entity, new Vec3d(posX, posY, posZ), predict, obsidianPos, ignoreTerrain);
        return (float) finald;
    }


    public static boolean terrainIgnore = false;

    public static float getExplosionDamage(EndCrystalEntity entity, LivingEntity target) {
        return getExplosionDamage(entity.getPos(), 6F, target);
    }

    private static CrystalBoomer autoCrystal = new CrystalBoomer();

    public static float getExplosionDamage(Vec3d explosionPos, float power, LivingEntity target) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL)
            return 0f;

        Explosion explosion = new Explosion(mc.world, null, explosionPos.x, explosionPos.y, explosionPos.z, power, false, Explosion.DestructionType.DESTROY);

        double maxDist = power * 2;
        if (!mc.world.getOtherEntities(null, new Box(
            MathHelper.floor(explosionPos.x - maxDist - 1.0),
            MathHelper.floor(explosionPos.y - maxDist - 1.0),
            MathHelper.floor(explosionPos.z - maxDist - 1.0),
            MathHelper.floor(explosionPos.x + maxDist + 1.0),
            MathHelper.floor(explosionPos.y + maxDist + 1.0),
            MathHelper.floor(explosionPos.z + maxDist + 1.0))).contains(target)) {
            return 0f;
        }

        if (!target.isImmuneToExplosion() && !target.isInvulnerable()) {
            double distExposure = MathHelper.sqrt((float) target.squaredDistanceTo(explosionPos)) / maxDist;
            if (distExposure <= 1.0) {
                double xDiff = target.getX() - explosionPos.x;
                double yDiff = target.getEyeY() - explosionPos.y;
                double zDiff = target.getZ() - explosionPos.z;
                double diff = MathHelper.sqrt((float) (xDiff * xDiff + yDiff * yDiff + zDiff * zDiff));
                if (diff != 0.0) {
                    if (autoCrystal.terrainIgnore.get()) {
                        terrainIgnore = true;
                    }
                    double exposure = Explosion.getExposure(explosionPos, target);
                    terrainIgnore = false;
                    double finalExposure = (1.0 - distExposure) * exposure;

                    float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0);

                    if (target instanceof PlayerEntity) {
                        if (mc.world.getDifficulty() == Difficulty.EASY) {
                            toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                        } else if (mc.world.getDifficulty() == Difficulty.HARD) {
                            toDamage = toDamage * 3f / 2f;
                        }
                    }

                    toDamage = DamageUtil.getDamageLeft(toDamage, target.getArmor(),
                        (float) target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

                    if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                        int resistance = 25 - (target.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1) * 5;
                        float resistance_1 = toDamage * resistance;
                        toDamage = Math.max(resistance_1 / 25f, 0f);
                    }

                    if (toDamage <= 0f) {
                        toDamage = 0f;
                    } else {
                        int protAmount = EnchantmentHelper.getProtectionAmount(target.getArmorItems(), explosion.getDamageSource());
                        if (protAmount > 0) {
                            toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount);
                        }
                    }

                    return toDamage;
                }
            }
        }

        return 0f;
    }

    public static Vec3d getMotionVec(Entity entity, int ticks) {
        double dX = entity.getX() - entity.prevX;
        double dZ = entity.getZ() - entity.prevZ;
        double entityMotionPosX = 0;
        double entityMotionPosZ = 0;
        if (autoCrystal.collision.get()) {
            for (int i = 1; i <= ticks; i++) {
                if (mc.world.getBlockState(new BlockPos(entity.getX() + dX * i, entity.getY(), entity.getZ() + dZ * i)).getBlock() instanceof AirBlock) {
                    entityMotionPosX = dX * i;
                    entityMotionPosZ = dZ * i;
                } else {
                    break;
                }
            }
        } else {
            entityMotionPosX = dX * ticks;
            entityMotionPosZ = dZ * ticks;
        }

        return new Vec3d(entityMotionPosX, 0, entityMotionPosZ);
    }

    public static boolean isVisible(Vec3d vec3d) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), (mc.player.getBoundingBox()).minY + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        return mc.world.raycastBlock(eyesPos, vec3d, new BlockPos(vec3d.x, vec3d.y, vec3d.z), VoxelShapes.fullCube(), mc.world.getBlockState(new BlockPos(vec3d.x, vec3d.y, vec3d.z))) == null;
    }

    public static float getDamageMultiplied(float damage) {
        int diff = mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0 : (diff == 2 ? 1 : (diff == 1 ? 0.5f : 1.5f)));
    }

    public static float getBlastReduction(LivingEntity entity, float damageInput, Explosion explosion) {
        float damage = damageInput;
        if (entity instanceof PlayerEntity) {
            PlayerEntity ep = (PlayerEntity) entity;
            DamageSource ds = DamageSource.explosion(explosion);
            damage = DamageUtil.getDamageLeft(damage, (float) ep.getArmor(), (float) ep.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
            int k = 0;
            float f = MathHelper.clamp(k, 0.0F, 20.0F);
            damage = damage * (1.0F - f / 25.0F);

            if (entity.hasStatusEffect(StatusEffects.RESISTANCE)) {
                damage = damage - (damage / 4);
            }

            damage = Math.max(damage, 0.0F);
            return damage;

        }
        damage = DamageUtil.getDamageLeft(damage, (float) entity.getArmor(), (float) entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
        return damage;
    }
    public static int getCrystalSlot() {
        int crystalSlot = -1;

        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
            crystalSlot = mc.player.getInventory().selectedSlot;
        }


        if (crystalSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (mc.player.getInventory().getStack(l).getItem() == Items.END_CRYSTAL) {
                    crystalSlot = l;
                    break;
                }
            }
        }

        return crystalSlot;
    }

    public static int ping() {
        if (mc.getNetworkHandler() == null) {
            return 50;
        } else if (mc.player == null) {
            return 50;
        } else {
            try {
                return mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
            } catch (NullPointerException ignored) {
            }
            return 50;
        }
    }

    public static int getSwordSlot() {
        int swordSlot = -1;

        if (mc.player.getMainHandStack().getItem() == Items.DIAMOND_SWORD) {
            swordSlot = mc.player.getInventory().selectedSlot;
        }

        if (swordSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (mc.player.getInventory().getStack(l).getItem() == Items.DIAMOND_SWORD) {
                    swordSlot = l;
                    break;
                }
            }
        }

        return swordSlot;
    }

    public static boolean canPlaceCrystal(BlockPos blockPos) {
        BlockPos boost = blockPos.add(0, 1, 0);
        BlockPos boost2 = blockPos.add(0, 2, 0);
        try {
            if (mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN) {
                return false;
            }

            if (!(mc.world.getBlockState(boost).getBlock() == Blocks.AIR && mc.world.getBlockState(boost2).getBlock() == Blocks.AIR)) {
                return false;
            }

            for (Entity entity : mc.world.getEntitiesByClass(Entity.class, new Box(boost), new Predicate<Entity>() {
                @Override
                public boolean test(Entity entity) {
                    return false;
                }
            })) {
                if (!(entity instanceof EndCrystalEntity)) {
                    return false;
                }
            }

            for (Entity entity : mc.world.getEntitiesByClass(Entity.class, new Box(boost2), new Predicate<Entity>() {
                @Override
                public boolean test(Entity entity) {
                    return false;
                }
            })) {
                if (!(entity instanceof EndCrystalEntity)) {
                    return false;
                }
            }
        } catch (Exception ignored) {
            return false;
        }

        return true;
    }

    public static boolean rayTracePlace(BlockPos pos) {
        if (autoCrystal.directionMode.get() != CrystalBoomer.DirectionMode.Vanilla) {
            double increment = 0.45D;
            double start = 0.05D;
            double end = 0.95D;

            Vec3d eyesPos = new Vec3d(mc.player.getX(), (mc.player.getBoundingBox()).minY + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

            for (double xS = start; xS <= end; xS += increment) {
                for (double yS = start; yS <= end; yS += increment) {
                    for (double zS = start; zS <= end; zS += increment) {
                        Vec3d posVec = (new Vec3d(pos.getX(), pos.getY(), pos.getZ())).add(xS, yS, zS);

                        double distToPosVec = eyesPos.distanceTo(posVec);

                        if (autoCrystal.strictDirection.get()) {
                            if (distToPosVec > autoCrystal.placeRange.get()) continue;
                        }

                        double diffX = posVec.x - eyesPos.x;
                        double diffY = posVec.y - eyesPos.y;
                        double diffZ = posVec.z - eyesPos.z;
                        double diffXZ = MathHelper.sqrt((float) (diffX * diffX + diffZ * diffZ));

                        double[] tempPlaceRotation = new double[]{MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F), MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)))};

                        // inline values for slightly better perfornamce
                        // Entity.getVectorForRotation()
                        float yawCos = MathHelper.cos((float) (-tempPlaceRotation[0] * 0.017453292F - 3.1415927F));
                        float yawSin = MathHelper.sin((float) (-tempPlaceRotation[0] * 0.017453292F - 3.1415927F));
                        float pitchCos = -MathHelper.cos((float) (-tempPlaceRotation[1] * 0.017453292F));
                        float pitchSin = MathHelper.sin((float) (-tempPlaceRotation[1] * 0.017453292F));

                        Vec3d rotationVec = new Vec3d((yawSin * pitchCos), pitchSin, (yawCos * pitchCos));
                        Vec3d eyesRotationVec = eyesPos.add(rotationVec.x * distToPosVec, rotationVec.y * distToPosVec, rotationVec.z * distToPosVec);

                        BlockHitResult rayTraceResult = mc.world.raycastBlock(eyesPos, eyesRotationVec, new BlockPos(pos), VoxelShapes.fullCube(), mc.world.getBlockState(pos));
                        if (rayTraceResult != null) {
                            if (rayTraceResult.getType() == BlockHitResult.Type.BLOCK) {
                                if (rayTraceResult.getBlockPos().equals(pos)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        } else {
            for (Direction facing : Direction.values()) {
                Vec3d cVector = new Vec3d(pos.getX() + 0.5 + facing.getVector().getX() * 0.5,
                    pos.getY() + 0.5 + facing.getVector().getY() * 0.5,
                    pos.getZ() + 0.5 + facing.getVector().getZ() * 0.5);
                if (autoCrystal.strictDirection.get()) {
                    if (mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0).distanceTo(cVector) > autoCrystal.placeRange.get()) {
                        continue;
                    }
                }
                BlockHitResult rayTraceResult = mc.world.raycastBlock(new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), cVector, pos, VoxelShapes.fullCube(), mc.world.getBlockState(pos));
                if (rayTraceResult != null && rayTraceResult.getType().equals(BlockHitResult.Type.BLOCK) && rayTraceResult.getBlockPos().equals(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean rayTraceBreak(double x, double y, double z) {
        if (mc.world.raycastBlock(new Vec3d(mc.player.getX(), mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), new Vec3d(x, y + 1.8, z), new BlockPos(x, y, z), VoxelShapes.fullCube(), mc.world.getBlockState(new BlockPos(x, y, z))) == null) {
            return true;
        }
        if (mc.world.raycastBlock(new Vec3d(mc.player.getX(), mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), new Vec3d(x, y + 1.5, z), new BlockPos(x, y, z),  VoxelShapes.fullCube(), mc.world.getBlockState(new BlockPos(x, y, z))) == null) {
            return true;
        }
        return mc.world.raycastBlock(new Vec3d(mc.player.getX(), mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), new Vec3d(x, y, z), new BlockPos(x, y, z),  VoxelShapes.fullCube(), mc.world.getBlockState(new BlockPos(x, y, z))) == null;
    }

}
