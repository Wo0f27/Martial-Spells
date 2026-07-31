package com.w0of26.martialspells.effects;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public final class GuardiansCryEffect extends MobEffect {
    public static final String TARGET_UUID_TAG =
            MartialSpells.MOD_ID + "_guardians_cry_target";

    public static final String PREVIOUS_TARGET_UUID_TAG =
            MartialSpells.MOD_ID + "_guardians_cry_previous_target";

    public static final String FALLBACK_EXPIRES_AT_TAG =
            MartialSpells.MOD_ID
                    + "_guardians_cry_fallback_expires_at";

    public GuardiansCryEffect() {
        super(MobEffectCategory.HARMFUL, 0xD8B35C);
    }

    @Override
    public void applyEffectTick(LivingEntity affectedEntity, int amplifier) {
        if (!(affectedEntity instanceof Mob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!mob.getPersistentData().hasUUID(TARGET_UUID_TAG)) {
            return;
        }

        UUID targetUuid =
                mob.getPersistentData().getUUID(TARGET_UUID_TAG);

        Entity targetEntity = serverLevel.getEntity(targetUuid);

        if (!(targetEntity instanceof LivingEntity target)
                || !target.isAlive()
                || !mob.canAttack(target)) {
            clearTaunt(mob);
            return;
        }

        mob.setTarget(target);
        mob.setAggressive(true);

        // A restrained visual reminder while the taunt remains active.
        if (mob.tickCount % 10 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    mob.getX(),
                    mob.getY() + mob.getBbHeight() * 0.75D,
                    mob.getZ(),
                    1,
                    0.2D,
                    0.25D,
                    0.2D,
                    0.0D
            );
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    public static boolean hasFallbackTaunt(Mob mob) {
        return mob.getPersistentData().contains(
                FALLBACK_EXPIRES_AT_TAG,
                Tag.TAG_LONG
        );
    }

    /**
     * Starts or refreshes a Guardian's Cry taunt for an entity that rejects
     * the normal MobEffect.
     *
     * The original target must already have been stored by the spell before
     * this method is called.
     */
    public static boolean startOrRefreshFallbackTaunt(
            Mob mob,
            LivingEntity forcedTarget,
            int durationTicks
    ) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (!forcedTarget.isAlive()
                || forcedTarget.isDeadOrDying()
                || !mob.canAttack(forcedTarget)) {
            return false;
        }

        var persistentData = mob.getPersistentData();

        persistentData.putUUID(
                TARGET_UUID_TAG,
                forcedTarget.getUUID()
        );

        persistentData.putLong(
                FALLBACK_EXPIRES_AT_TAG,
                serverLevel.getGameTime()
                        + Math.max(1, durationTicks)
        );

        mob.setTarget(forcedTarget);
        mob.setAggressive(true);

        return true;
    }

    /**
     * Enforces and expires a Guardian's Cry fallback taunt.
     *
     * This is used only for explicitly allowlisted entities that reject the
     * normal Guardian's Cry MobEffect.
     */
    public static void tickFallbackTaunt(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var persistentData = mob.getPersistentData();

        if (!persistentData.contains(
                FALLBACK_EXPIRES_AT_TAG,
                Tag.TAG_LONG
        )) {
            return;
        }

        long expiresAt =
                persistentData.getLong(FALLBACK_EXPIRES_AT_TAG);

        if (serverLevel.getGameTime() >= expiresAt) {
            clearTaunt(mob);
            return;
        }

        if (!persistentData.hasUUID(TARGET_UUID_TAG)) {
            clearTaunt(mob);
            return;
        }

        UUID targetUuid =
                persistentData.getUUID(TARGET_UUID_TAG);

        Entity targetEntity =
                serverLevel.getEntity(targetUuid);

        if (!(targetEntity instanceof LivingEntity target)
                || !target.isAlive()
                || target.isDeadOrDying()
                || !mob.canAttack(target)) {
            clearTaunt(mob);
            return;
        }

        mob.setTarget(target);
        mob.setAggressive(true);

        if (mob.tickCount % 10 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    mob.getX(),
                    mob.getY()
                            + mob.getBbHeight() * 0.75D,
                    mob.getZ(),
                    1,
                    0.2D,
                    0.25D,
                    0.2D,
                    0.0D
            );
        }
    }

    /**
     * Removes Guardian's Cry's stored target and releases the mob only when
     * its current target is still the entity forced by Guardian's Cry.
     */
    public static void clearTaunt(LivingEntity affectedEntity) {
        if (!(affectedEntity instanceof Mob mob)) {
            return;
        }

        var persistentData = mob.getPersistentData();

        UUID tauntTargetUuid =
                persistentData.hasUUID(TARGET_UUID_TAG)
                        ? persistentData.getUUID(TARGET_UUID_TAG)
                        : null;

        UUID previousTargetUuid =
                persistentData.hasUUID(PREVIOUS_TARGET_UUID_TAG)
                        ? persistentData.getUUID(
                        PREVIOUS_TARGET_UUID_TAG
                )
                        : null;

        /*
         * Always clean both tags, even if one of them is missing or the
         * previous target can no longer be resolved.
         */
        persistentData.remove(TARGET_UUID_TAG);
        persistentData.remove(PREVIOUS_TARGET_UUID_TAG);
        persistentData.remove(FALLBACK_EXPIRES_AT_TAG);

        if (tauntTargetUuid == null) {
            return;
        }

        LivingEntity currentTarget = mob.getTarget();

        /*
         * Do not overwrite a target selected independently by another
         * system after Guardian's Cry stopped controlling the mob.
         *
         * A null target is also safe to restore from.
         */
        boolean mayRestorePreviousTarget =
                currentTarget == null
                        || currentTarget.getUUID()
                        .equals(tauntTargetUuid);

        if (!mayRestorePreviousTarget) {
            return;
        }

        LivingEntity restoredTarget = null;

        if (previousTargetUuid != null
                && mob.level() instanceof ServerLevel serverLevel) {
            Entity previousEntity =
                    serverLevel.getEntity(previousTargetUuid);

            if (previousEntity
                    instanceof LivingEntity previousTarget
                    && previousTarget.isAlive()
                    && !previousTarget.isDeadOrDying()
                    && !mob.isAlliedTo(previousTarget)
                    && mob.canAttack(previousTarget)) {
                restoredTarget = previousTarget;
            }
        }

        mob.setTarget(restoredTarget);
        mob.setAggressive(restoredTarget != null);
    }

}