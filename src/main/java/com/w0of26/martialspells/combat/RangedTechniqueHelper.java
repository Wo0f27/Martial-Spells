package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import net.fabric_extras.ranged_weapon.api.CustomRangedWeapon;
import net.fabric_extras.ranged_weapon.api.RangedConfig;
import net.fabric_extras.ranged_weapon.internal.ScalingUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * Shared ranged-weapon bridge for Ranger techniques.
 *
 * RangedWeaponAPI remains authoritative for the weapon's configured
 * baseline damage and velocity. Martial Spells only layers a technique
 * multiplier on top, then lets Apothic Attributes modify the spawned
 * arrow normally through its EntityJoinLevelEvent.
 */
public final class RangedTechniqueHelper {
    public static final String BARRAGE_ARROW_TAG =
            MartialSpells.MOD_ID + ":barrage_arrow";

    private RangedTechniqueHelper() {
    }

    public static boolean isSupportedRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    /**
     * Main hand wins when both hands contain supported ranged weapons.
     */
    public static ItemStack findHeldRangedWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (isSupportedRangedWeapon(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (isSupportedRangedWeapon(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Barrage requires a real arrow stack but deliberately does not
     * consume it. Firework rockets are not accepted.
     */
    public static ItemStack findArrowAmmo(Player player) {
        for (int slot = 0;
                slot < player.getInventory().getContainerSize();
                slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()
                    && stack.getItem() instanceof ArrowItem) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static AbstractArrow createTechniqueArrow(
            Level level,
            Player player,
            ItemStack weapon,
            ItemStack ammo,
            float techniqueDamageMultiplier
    ) {
        if (!(ammo.getItem() instanceof ArrowItem arrowItem)) {
            throw new IllegalArgumentException(
                    "Ranged technique ammo must be an ArrowItem"
            );
        }

        AbstractArrow arrow =
                arrowItem.createArrow(level, ammo, player);

        boolean bow = weapon.getItem() instanceof BowItem;

        applyWeaponEnchantments(
                arrow,
                weapon,
                bow
        );

        applyRangedWeaponApiScaling(
                arrow,
                weapon,
                bow,
                techniqueDamageMultiplier
        );

        /*
         * The source arrow stack is only a requirement/template. Barrage
         * never consumes it, so spawned arrows must not become free ammo.
         */
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

        return arrow;
    }

    public static float getProjectileVelocity(ItemStack weapon) {
        boolean bow = weapon.getItem() instanceof BowItem;
        RangedConfig config = getRangedConfig(weapon, bow);
        var baseline = bow
                ? ScalingUtil.BOW_BASELINE
                : ScalingUtil.CROSSBOW_BASELINE;

        return config.velocity() > 0.0F
                ? config.velocity()
                : (float) baseline.velocity();
    }

    public static void markBarrageArrow(AbstractArrow arrow) {
        arrow.getPersistentData().putBoolean(
                BARRAGE_ARROW_TAG,
                true
        );
    }

    public static boolean isBarrageArrow(AbstractArrow arrow) {
        return arrow.getPersistentData().getBoolean(
                BARRAGE_ARROW_TAG
        );
    }

    private static void applyWeaponEnchantments(
            AbstractArrow arrow,
            ItemStack weapon,
            boolean bow
    ) {
        if (bow) {
            /*
             * Barrage represents a fully drawn bow shot, so preserve the
             * vanilla bow's normal full-draw projectile behavior.
             */
            arrow.setCritArrow(true);

            int power = EnchantmentHelper.getItemEnchantmentLevel(
                    Enchantments.POWER_ARROWS,
                    weapon
            );
            if (power > 0) {
                arrow.setBaseDamage(
                        arrow.getBaseDamage()
                                + power * 0.5D
                                + 0.5D
                );
            }

            int punch = EnchantmentHelper.getItemEnchantmentLevel(
                    Enchantments.PUNCH_ARROWS,
                    weapon
            );
            if (punch > 0) {
                arrow.setKnockback(punch);
            }

            if (EnchantmentHelper.getItemEnchantmentLevel(
                    Enchantments.FLAMING_ARROWS,
                    weapon
            ) > 0) {
                arrow.setSecondsOnFire(100);
            }

            return;
        }

        int piercing = EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.PIERCING,
                weapon
        );
        if (piercing > 0) {
            arrow.setPierceLevel((byte) piercing);
        }

        /*
         * Multishot is intentionally ignored. Barrage owns its fixed
         * three-projectile count and must never expand to nine arrows.
         */
    }

    private static void applyRangedWeaponApiScaling(
            AbstractArrow arrow,
            ItemStack weapon,
            boolean bow,
            float techniqueDamageMultiplier
    ) {
        RangedConfig config = getRangedConfig(weapon, bow);
        var baseline = bow
                ? ScalingUtil.BOW_BASELINE
                : ScalingUtil.CROSSBOW_BASELINE;

        double configuredDamage = config.damage() > 0.0F
                ? config.damage()
                : baseline.damage();

        /*
         * This is the same compensation RWA applies to normal configured
         * bow/crossbow shots. Do not read Apothic attributes here: those
         * are applied once, later, when the arrow joins the level.
         */
        double rangedWeaponMultiplier =
                ScalingUtil.arrowDamageMultiplier(
                        baseline.damage(),
                        configuredDamage,
                        baseline.velocity(),
                        config.velocity()
                );

        arrow.setBaseDamage(
                arrow.getBaseDamage()
                        * rangedWeaponMultiplier
                        * techniqueDamageMultiplier
        );
    }

    private static RangedConfig getRangedConfig(
            ItemStack weapon,
            boolean bow
    ) {
        RangedConfig fallback = bow
                ? RangedConfig.BOW
                : RangedConfig.CROSSBOW;

        if (!(weapon.getItem()
                instanceof CustomRangedWeapon rangedWeapon)) {
            return fallback;
        }

        RangedConfig config =
                rangedWeapon.getRangedWeaponConfig();

        if (config == null || config.damage() <= 0.0F) {
            return fallback;
        }

        return config;
    }
}
