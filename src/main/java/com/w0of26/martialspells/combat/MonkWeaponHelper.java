package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.registry.MartialTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

public final class MonkWeaponHelper {
    private MonkWeaponHelper() {
    }

    public static boolean hasValidMainHand(
            Player player
    ) {
        ItemStack mainHand =
                player.getMainHandItem();

        return mainHand.isEmpty()
                || mainHand.is(
                MartialTags.Items.MONK_WEAPONS
        );
    }

    @Nullable
    public static StunningStrikeWeaponStyle
    getStunningStrikeWeaponStyle(
            Player player
    ) {
        if (player == null) {
            return null;
        }

        ItemStack mainHand =
                player.getMainHandItem();

        /*
         * Empty-handed and gauntlet Stunning Strikes
         * use the punch animation.
         */
        if (mainHand.isEmpty()
                || mainHand.is(
                MartialTags.Items.GAUNTLETS
        )) {
            return StunningStrikeWeaponStyle.PUNCH;
        }

        /*
         * Quarterstaff Stunning Strikes use Better Combat's
         * battlestaff spin animation.
         */
        if (mainHand.is(
                MartialTags.Items.QUARTERSTAFFS
        )) {
            return StunningStrikeWeaponStyle.QUARTERSTAFF;
        }

        return null;
    }

    public static boolean
    hasValidStunningStrikeMainHand(
            Player player
    ) {
        return getStunningStrikeWeaponStyle(player)
                != null;
    }

    public static boolean
    hasValidDeflectMissilesMainHand(
            Player player
    ) {
        if (player == null) {
            return false;
        }

        ItemStack mainHand =
                player.getMainHandItem();

        /*
         * Deflect Missiles supports:
         *
         * - empty hand
         * - gauntlets
         * - quarterstaffs
         *
         * Other Monk weapons do not qualify because the
         * technique has dedicated hand and staff animations.
         */
        return mainHand.isEmpty()
                || mainHand.is(
                MartialTags.Items.GAUNTLETS
        )
                || mainHand.is(
                MartialTags.Items.QUARTERSTAFFS
        );
    }
}