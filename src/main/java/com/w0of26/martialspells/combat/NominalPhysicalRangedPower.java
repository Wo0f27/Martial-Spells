package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.fabric_extras.ranged_weapon.api.CustomRangedWeapon;
import net.fabric_extras.ranged_weapon.api.RangedConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;

/** Nominal damage units for Ranger effects that are not AbstractArrow hits. */
public final class NominalPhysicalRangedPower {
    public static final double BOW_FALLBACK = 6.0D;
    public static final double CROSSBOW_FALLBACK = 9.0D;

    private NominalPhysicalRangedPower() {}

    /** Main hand wins; a supported offhand weapon is used only if the main hand is unsupported. */
    public static ItemStack selectedWeapon(LivingEntity caster) {
        if (caster == null) return ItemStack.EMPTY;
        if (RangedWeaponClassifier.isSupported(caster.getMainHandItem())) return caster.getMainHandItem();
        if (RangedWeaponClassifier.isSupported(caster.getOffhandItem())) return caster.getOffhandItem();
        return ItemStack.EMPTY;
    }

    /**
     * An unarmed caster uses the neutral bow baseline of 6, matching the
     * project's bow fallback. This is an explicit target convention for wolf
     * scaling; it does not assert an upstream unarmed source value.
     */
    public static double get(LivingEntity caster) {
        return weaponDamage(selectedWeapon(caster)) * arrowDamageAttribute(caster);
    }

    public static double weaponDamage(ItemStack weapon) {
        RangedWeaponClassifier.Type type = RangedWeaponClassifier.classify(weapon);
        if (type == RangedWeaponClassifier.Type.NONE) return BOW_FALLBACK;
        if (weapon.getItem() instanceof CustomRangedWeapon ranged) {
            RangedConfig config = ranged.getRangedWeaponConfig();
            if (config != null && config.damage() > 0.0F) return config.damage();
        }
        return type == RangedWeaponClassifier.Type.CROSSBOW ? CROSSBOW_FALLBACK : BOW_FALLBACK;
    }

    public static double arrowDamageAttribute(LivingEntity caster) {
        if (caster == null) return 1.0D;
        AttributeInstance attribute = caster.getAttribute(ALObjects.Attributes.ARROW_DAMAGE.get());
        return attribute == null ? 1.0D : Math.max(0.0D, attribute.getValue());
    }
}
