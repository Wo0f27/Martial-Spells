package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import net.fabric_extras.ranged_weapon.api.CustomRangedWeapon;
import net.fabric_extras.ranged_weapon.api.RangedConfig;
import net.fabric_extras.ranged_weapon.internal.ScalingUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Optional;

/**
 * The source-compatible physical-arrow construction path. It is deliberately
 * separate from RangedTechniqueHelper and has no Martial Power multiplication.
 * The caller owns ammo payment and addFreshEntity after preparation succeeds.
 */
public final class ArchersPhysicalArrow {
    private ArchersPhysicalArrow() {}

    public static ItemStack findAmmunition(Player shooter, ItemStack weapon) {
        return shooter.getProjectile(weapon);
    }

    public static Optional<AbstractArrow> prepare(ServerLevel level, Player shooter, ItemStack weapon,
                                                   ItemStack ammunition, float techniqueMultiplier,
                                                   int addedKnockback, int extraPierceTargets,
                                                   boolean critical, boolean bypassIframes) {
        RangedWeaponClassifier.Type type = RangedWeaponClassifier.classify(weapon);
        if (type == RangedWeaponClassifier.Type.NONE || ammunition.isEmpty()
                || !(ammunition.getItem() instanceof ArrowItem arrowItem)
                || !Float.isFinite(techniqueMultiplier) || techniqueMultiplier < 0.0F
                || addedKnockback < 0 || extraPierceTargets < 0) return Optional.empty();

        AbstractArrow arrow = arrowItem.createArrow(level, ammunition, shooter);
        if (type == RangedWeaponClassifier.Type.BOW && weapon.getItem() instanceof BowItem bow) {
            arrow = bow.customArrow(arrow);
        }
        arrow.setCritArrow(critical);

        if (type == RangedWeaponClassifier.Type.BOW) {
            int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, weapon);
            if (power > 0) arrow.setBaseDamage(arrow.getBaseDamage() + power * 0.5D + 0.5D);
            int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, weapon);
            arrow.setKnockback(punch + addedKnockback);
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, weapon) > 0) {
                arrow.setSecondsOnFire(100);
            }
        } else {
            arrow.setKnockback(addedKnockback);
        }

        int innatePierce = type == RangedWeaponClassifier.Type.CROSSBOW
                ? EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, weapon) : 0;
        int totalPierce = innatePierce + extraPierceTargets;
        if (totalPierce > Byte.MAX_VALUE) return Optional.empty();
        arrow.setPierceLevel((byte) totalPierce);

        ScalingUtil.Scaling baseline = type == RangedWeaponClassifier.Type.CROSSBOW
                ? ScalingUtil.CROSSBOW_BASELINE : ScalingUtil.BOW_BASELINE;
        RangedConfig config = weapon.getItem() instanceof CustomRangedWeapon ranged
                ? ranged.getRangedWeaponConfig() : null;
        double configuredDamage = config != null && config.damage() > 0.0F
                ? config.damage() : baseline.damage();
        float configuredVelocity = config == null ? 0.0F : config.velocity();
        double ratio = ScalingUtil.arrowDamageMultiplier(
                baseline.damage(), configuredDamage, baseline.velocity(), configuredVelocity);
        arrow.setBaseDamage(arrow.getBaseDamage() * ratio * techniqueMultiplier);

        RangerArrowMetadata.markEligibleShot(arrow);
        RangerArrowMetadata.setIframeBypass(arrow, bypassIframes);
        RangerArrowMetadata.setExtraPierceTargets(arrow, extraPierceTargets);
        return Optional.of(arrow);
    }

    /** Called before addFreshEntity; Apothic then applies Arrow Damage/Velocity on join once. */
    public static void launch(AbstractArrow arrow, Player shooter, float velocity, float inaccuracy) {
        arrow.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, velocity, inaccuracy);
    }
}
