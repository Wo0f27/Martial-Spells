package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.registry.MartialAttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Shared damage-scaling calculations for Martial techniques.
 *
 * Martial techniques scale from the caster's Attack Damage and
 * dedicated Martial Spell Power attribute. Generic Iron's Spell
 * Power is intentionally not included.
 */
public final class MartialPowerHelper {

    private MartialPowerHelper() {
    }

    /**
     * Returns the caster's Martial Spell Power multiplier.
     *
     * A value of 1.0 is neutral. Negative effective values are
     * clamped to zero so they cannot create negative damage.
     */
    public static float getMartialPower(
            LivingEntity caster
    ) {
        if (caster == null) {
            return 1.0F;
        }

        return Math.max(
                0.0F,
                (float) caster.getAttributeValue(
                        MartialAttributeRegistry
                                .MARTIAL_SPELL_POWER
                                .get()
                )
        );
    }

    /**
     * Returns the caster's Attack Damage with a technique-specific
     * minimum value.
     */
    public static float getEffectiveAttackDamage(
            LivingEntity caster,
            float minimumAttackDamage
    ) {
        float safeMinimum =
                Math.max(
                        0.0F,
                        minimumAttackDamage
                );

        if (caster == null) {
            return safeMinimum;
        }

        return Math.max(
                safeMinimum,
                (float) caster.getAttributeValue(
                        Attributes.ATTACK_DAMAGE
                )
        );
    }

    /**
     * Calculates raw Martial technique damage before encumbrance
     * or technique-specific defensive handling.
     */
    public static float calculateTechniqueDamage(
            LivingEntity caster,
            float minimumAttackDamage,
            float techniqueCoefficient
    ) {
        float safeCoefficient =
                Math.max(
                        0.0F,
                        techniqueCoefficient
                );

        return getEffectiveAttackDamage(
                caster,
                minimumAttackDamage
        )
                * safeCoefficient
                * getMartialPower(caster);
    }
}