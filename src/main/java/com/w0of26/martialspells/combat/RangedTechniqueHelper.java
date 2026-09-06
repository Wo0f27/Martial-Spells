package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.entity.BarrageArrow;
import com.w0of26.martialspells.entity.EntanglingArrow;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import net.fabric_extras.ranged_weapon.api.CustomRangedWeapon;
import net.fabric_extras.ranged_weapon.api.RangedConfig;
import net.fabric_extras.ranged_weapon.internal.ScalingUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public final class RangedTechniqueHelper {
    private RangedTechniqueHelper() {}

    public static ItemStack findHeldRangedWeapon(Player player) {
        if (RangedWeaponClassifier.isSupported(player.getMainHandItem())) return player.getMainHandItem();
        if (RangedWeaponClassifier.isSupported(player.getOffhandItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public static BarrageArrow createBarrageArrow(Level level, Player player, ItemStack weapon, float techniqueMultiplier) {
        RangedWeaponClassifier.Type type = requireRangedType(weapon, "Barrage");
        BarrageArrow arrow = new BarrageArrow(level, player);
        applyEnchantments(arrow, weapon, type);
        applyScaling(arrow, player, weapon, type, techniqueMultiplier);
        return arrow;
    }

    public static EntanglingArrow createEntanglingArrow(Level level, Player player, ItemStack weapon,
                                                        float techniqueMultiplier, int rootDurationTicks) {
        RangedWeaponClassifier.Type type = requireRangedType(weapon, "Entangling Arrow");
        EntanglingArrow arrow = new EntanglingArrow(level, player);
        arrow.setRootDurationTicks(rootDurationTicks);
        applyEnchantments(arrow, weapon, type);
        applyScaling(arrow, player, weapon, type, techniqueMultiplier);
        return arrow;
    }

    public static float getProjectileVelocity(ItemStack weapon) {
        RangedWeaponClassifier.Type type = RangedWeaponClassifier.classify(weapon);
        ScalingUtil.Scaling baseline = baseline(type);
        RangedConfig config = customConfig(weapon);
        return config != null && config.velocity() > 0.0F
                ? config.velocity()
                : (float) baseline.velocity();
    }

    private static RangedWeaponClassifier.Type requireRangedType(ItemStack weapon, String techniqueName) {
        RangedWeaponClassifier.Type type = RangedWeaponClassifier.classify(weapon);
        if (type == RangedWeaponClassifier.Type.NONE) {
            throw new IllegalArgumentException(techniqueName + " requires a classified bow or crossbow.");
        }
        return type;
    }

    private static void applyEnchantments(AbstractArrow arrow, ItemStack weapon,
                                          RangedWeaponClassifier.Type type) {
        if (type == RangedWeaponClassifier.Type.BOW) {
            int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, weapon);
            if (power > 0) arrow.setBaseDamage(arrow.getBaseDamage() + power * 0.5D + 0.5D);

            int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, weapon);
            if (punch > 0) arrow.setKnockback(punch);

            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, weapon) > 0) {
                arrow.setSecondsOnFire(100);
            }
            return;
        }

        int piercing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, weapon);
        if (piercing > 0) arrow.setPierceLevel((byte) piercing);
    }

    private static void applyScaling(AbstractArrow arrow, Player player, ItemStack weapon,
                                     RangedWeaponClassifier.Type type, float techniqueMultiplier) {
        ScalingUtil.Scaling baseline = baseline(type);
        RangedConfig config = customConfig(weapon);
        double configuredDamage = config != null && config.damage() > 0.0F
                ? config.damage()
                : baseline.damage();
        float configuredVelocity = config == null ? 0.0F : config.velocity();

        double rwaMultiplier = ScalingUtil.arrowDamageMultiplier(
                baseline.damage(), configuredDamage, baseline.velocity(), configuredVelocity);
        double martialPower = MartialPowerHelper.getMartialPower(player);

        arrow.setBaseDamage(arrow.getBaseDamage()
                * rwaMultiplier
                * Math.max(0.0F, techniqueMultiplier)
                * martialPower);
    }

    private static ScalingUtil.Scaling baseline(RangedWeaponClassifier.Type type) {
        return type == RangedWeaponClassifier.Type.CROSSBOW
                ? ScalingUtil.CROSSBOW_BASELINE
                : ScalingUtil.BOW_BASELINE;
    }

    private static RangedConfig customConfig(ItemStack weapon) {
        return weapon.getItem() instanceof CustomRangedWeapon ranged
                ? ranged.getRangedWeaponConfig()
                : null;
    }
}
