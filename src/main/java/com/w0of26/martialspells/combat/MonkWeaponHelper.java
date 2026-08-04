package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.registry.MartialTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
}