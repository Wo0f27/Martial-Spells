package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.registry.MartialTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class MonkEncumbranceHelper {
    public static final float DAMAGE_MULTIPLIER =
            0.75F;

    public static final float KI_COST_MULTIPLIER =
            1.50F;

    private MonkEncumbranceHelper() {
    }

    /**
     * Returns the number of equipped armor items marked as heavy.
     */
    public static int getHeavyArmorPieceCount(
            LivingEntity entity
    ) {
        int count = 0;

        for (ItemStack armorStack
                : entity.getArmorSlots()) {
            if (!armorStack.isEmpty()
                    && armorStack.is(
                    MartialTags.Items.HEAVY_ARMOR
            )) {
                count++;
            }
        }

        return count;
    }

    /**
     * The current rule treats any tagged heavy-armor piece as
     * encumbering the Monk.
     */
    public static boolean isEncumbered(
            LivingEntity entity
    ) {
        return getHeavyArmorPieceCount(entity) > 0;
    }

    public static int getEffectiveKiCost(
            int baseCost,
            LivingEntity entity
    ) {
        if (!isEncumbered(entity)) {
            return baseCost;
        }

        return Mth.ceil(
                baseCost * KI_COST_MULTIPLIER
        );
    }

    public static float applyDamagePenalty(
            float baseDamage,
            LivingEntity entity
    ) {
        if (!isEncumbered(entity)) {
            return baseDamage;
        }

        return baseDamage * DAMAGE_MULTIPLIER;
    }
}