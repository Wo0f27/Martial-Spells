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

import java.util.UUID;

public final class GuardiansCryEffect extends MobEffect {
    public static final String TARGET_UUID_TAG =
            MartialSpells.MOD_ID + "_guardians_cry_target";

    public static final String PREVIOUS_TARGET_UUID_TAG =
            MartialSpells.MOD_ID + "_guardians_cry_previous_target";

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

        if (target instanceof Player player) {
            mob.setLastHurtByPlayer(player);
        }

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

    /**
     * Removes Guardian's Cry's stored target and releases the mob only when
     * its current target is still the entity forced by Guardian's Cry.
     */
    public static void clearTaunt(LivingEntity affectedEntity) {
        if (!(affectedEntity instanceof Mob mob)) {
            return;
        }

        if (!mob.getPersistentData().hasUUID(TARGET_UUID_TAG)) {
            return;
        }

        UUID tauntTargetUuid =
                mob.getPersistentData().getUUID(TARGET_UUID_TAG);

        mob.getPersistentData().remove(TARGET_UUID_TAG);

        LivingEntity currentTarget = mob.getTarget();

        if (currentTarget != null
                && currentTarget.getUUID().equals(tauntTargetUuid)) {
            mob.setTarget(null);
            mob.setAggressive(false);
        }
    }

}