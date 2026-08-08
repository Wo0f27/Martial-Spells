package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.registry.MartialTags;
import com.w0of26.martialspells.spells.DeflectMissilesSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ClientDeflectMissilesVisuals {

    private ClientDeflectMissilesVisuals() {
    }

    public static boolean isChannelingDeflectMissiles(
            Player player
    ) {
        String deflectMissilesId =
                DeflectMissilesSpell
                        .SPELL_ID
                        .toString();

        Minecraft minecraft =
                Minecraft.getInstance();

        /*
         * Local player.
         */
        if (minecraft.player != null
                && minecraft.player
                .getUUID()
                .equals(
                        player.getUUID()
                )) {

            return ClientMagicData.isCasting()
                    && deflectMissilesId.equals(
                    ClientMagicData
                            .getCastingSpellId()
            );
        }

        /*
         * Other players use Iron's synchronized
         * spell-casting state.
         */
        SyncedSpellData syncedData =
                ClientMagicData
                        .getSyncedSpellData(
                                player
                        );

        return syncedData.isCasting()
                && deflectMissilesId.equals(
                syncedData
                        .getCastingSpellId()
        );
    }

    public static boolean shouldSpinQuarterstaff(
            Player player
    ) {
        return isChannelingDeflectMissiles(
                player
        )
                && player
                .getMainHandItem()
                .is(
                        MartialTags.Items
                                .QUARTERSTAFFS
                );
    }

    public static boolean
    shouldPlayHandDeflectAnimation(
            Player player
    ) {
        if (!isChannelingDeflectMissiles(
                player
        )) {
            return false;
        }

        ItemStack heldItem =
                player.getMainHandItem();

        /*
         * Empty hand and gauntlets use the swipe.
         *
         * Quarterstaffs stay on the existing casting
         * animation because their item render itself spins.
         */
        return heldItem.isEmpty()
                || heldItem.is(
                MartialTags.Items.GAUNTLETS
        );
    }
}