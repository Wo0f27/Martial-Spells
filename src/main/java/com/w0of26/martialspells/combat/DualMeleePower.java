package com.w0of26.martialspells.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Frozen Spell Power 1.20.1 physical_melee_dual translation shared by
 * post-R6 Rogue techniques without adding Spell Power as a runtime dependency.
 *
 * <p>R6 Mutilate keeps its already-validated private copy untouched. R7 uses
 * this helper to snapshot the same value into each placed Bear Trap.</p>
 */
public final class DualMeleePower {
    private static final double EPSILON = 1.0E-7D;

    private DualMeleePower() {
    }

    public static float calculate(ServerPlayer player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return 0.0F;
        }

        double singleHanded = attackDamage.getValue();
        double offhandDamage = calculateOffhandAttackDamage(player, attackDamage);
        return (float) Math.max(0.0D, singleHanded + offhandDamage);
    }

    private static double calculateOffhandAttackDamage(
            ServerPlayer player,
            AttributeInstance attackDamage
    ) {
        double weaponDamage = attackDamage.getBaseValue()
                + flatAttackDamageFrom(player.getOffhandItem());

        if (Math.abs(weaponDamage) <= EPSILON) {
            return 0.0D;
        }

        double multiplyBase = 1.0D;
        double multiplyTotal = 1.0D;

        for (AttributeModifier modifier : attackDamage.getModifiers()) {
            switch (modifier.getOperation()) {
                case ADDITION -> {
                    // Frozen helper intentionally ignores additive entity
                    // modifiers here; only the offhand stack's flat bonus is
                    // added to the attribute base above.
                }
                case MULTIPLY_BASE -> multiplyBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> multiplyTotal += modifier.getAmount();
            }
        }

        return weaponDamage * multiplyBase * multiplyTotal;
    }

    private static double flatAttackDamageFrom(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }

        double total = 0.0D;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (AttributeModifier modifier :
                    stack.getAttributeModifiers(slot).get(Attributes.ATTACK_DAMAGE)) {
                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    total += modifier.getAmount();
                }
            }
        }
        return total;
    }
}
