package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.client.visual.RangerSourceVfx;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.spells.PowerShotSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;

/** Called only at AbstractArrow's actual entity-hurt call, after uncancelled collision. */
public final class PowerShotImpact {
    public static final int MARK_TICKS = 240;
    public static final int MAX_AMPLIFIER = 2;

    private PowerShotImpact() {}

    public static boolean hurt(AbstractArrow arrow, Entity target, DamageSource source, float amount) {
        if (arrow.level().isClientSide || !(target instanceof LivingEntity living)
                || !RangerArrowMetadata.hasTransfer(arrow, PowerShotSpell.SPELL_ID)) {
            return target.hurt(source, amount);
        }
        int previousIframes = living.invulnerableTime;
        boolean bypass = RangerArrowMetadata.bypassesIframes(arrow);
        if (bypass) living.invulnerableTime = 0;
        boolean damaged;
        try {
            damaged = target.hurt(source, amount);
        } finally {
            if (bypass) living.invulnerableTime = previousIframes;
        }
        // Spell Engine applies transferred impacts even when vanilla damage says false.
        if (RangerArrowMetadata.markTransferredHit(arrow, PowerShotSpell.SPELL_ID, living.getUUID())) {
            MobEffectInstance current = living.getEffect(MartialEffectRegistry.HUNTERS_MARK.get());
            int next = current == null ? 0 : Math.min(MAX_AMPLIFIER, current.getAmplifier() + 1);
            living.addEffect(new MobEffectInstance(MartialEffectRegistry.HUNTERS_MARK.get(),
                    MARK_TICKS, next, false, true, true));
            if (living.level() instanceof ServerLevel server) {
                RangerSourceVfx.huntersMarkImpact(server, living);
            }
        }
        return damaged;
    }
}
