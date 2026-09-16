package com.w0of26.martialspells.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Target-native adapter for the frozen Spell Engine 1.20.1
 * {@code spell_power:physical_melee} POWER source used by Rogues.
 *
 * <p>The source contract is deliberately narrow: single-hand physical-melee
 * power is the caster's current vanilla Attack Damage value. It does not
 * include Martial Spell Power, off-hand weapon damage, or Iron's magic spell
 * power. Those belong to different mechanics.</p>
 */
public final class PhysicalMeleePower {
    private PhysicalMeleePower() {
    }

    /**
     * Returns the caster's current Attack Damage, or zero when the entity does
     * not expose that attribute. The null-safe path mirrors the frozen source,
     * which treats entities without Attack Damage as contributing no melee
     * power rather than throwing.
     */
    public static double get(LivingEntity caster) {
        if (caster == null) {
            return 0.0D;
        }

        AttributeInstance attackDamage = caster.getAttribute(Attributes.ATTACK_DAMAGE);
        return attackDamage == null ? 0.0D : attackDamage.getValue();
    }

    /**
     * Reproduces Rogues' common control-effect health gate:
     * {@code healthBase + powerMultiplier * physicalMeleePower}.
     */
    public static double controlHealthLimit(
            LivingEntity caster,
            double healthBase,
            double powerMultiplier
    ) {
        return healthBase + powerMultiplier * get(caster);
    }
}
