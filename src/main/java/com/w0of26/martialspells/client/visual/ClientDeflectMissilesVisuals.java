package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.registry.MartialTags;
import com.w0of26.martialspells.spells.DeflectMissilesSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

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
         * Local player uses Iron's local client casting
         * state.
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
         * Other players use Iron's synchronized casting
         * state so their Deflect Missiles visuals can be
         * rendered correctly in multiplayer.
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
        /*
         * Quarterstaff Deflect Missiles keeps its
         * continuous spinning-item visual for the entire
         * duration of the channel.
         *
         * Empty-hand and gauntlet animations are no longer
         * controlled here. Those are now triggered only by
         * confirmed projectile impacts through the
         * animation packet.
         */
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
}