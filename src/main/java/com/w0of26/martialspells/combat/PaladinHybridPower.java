package com.w0of26.martialspells.combat;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;

/**
 * Target-native translation of Paladins' Spell Engine hybrid power blends.
 *
 * The frozen source uses weighted averages:
 * - Holy-dominant: base Healing weight 1 + melee weight 1/3 = 75% Holy / 25% melee.
 * - Melee-dominant: base melee weight 1 + Healing weight 1/3 = 75% melee / 25% Holy.
 */
public final class PaladinHybridPower {
    private PaladinHybridPower() {
    }

    public static float holyReference(
            AbstractSpell spell,
            LivingEntity caster
    ) {
        if (spell == null || caster == null) {
            return 0.0F;
        }

        return Math.max(
                0.0F,
                spell.getSpellPower(1, caster)
        );
    }

    public static float melee(
            LivingEntity caster
    ) {
        return Math.max(
                0.0F,
                (float) PhysicalMeleePower.get(caster)
        );
    }

    public static float holyDominant(
            AbstractSpell spell,
            LivingEntity caster
    ) {
        return holyReference(spell, caster) * 0.75F
                + melee(caster) * 0.25F;
    }

    public static float meleeDominant(
            AbstractSpell spell,
            LivingEntity caster
    ) {
        return melee(caster) * 0.75F
                + holyReference(spell, caster) * 0.25F;
    }
}
