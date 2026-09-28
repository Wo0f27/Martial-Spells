package com.w0of26.martialspells.integration;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * CP11-P5 source-equivalent Paladin/Priest spell availability contract.
 *
 * <p>This is a catalog, not a second spell registry. Iron's remains the
 * casting backend and MartialSpellRegistry remains the owner of every custom
 * spell. The catalog exposes the frozen upstream grouping to progression,
 * quest, and equipment integrations without duplicating spell instances.</p>
 */
public final class PaladinPriestSpellCatalog {
    public static final ResourceLocation HOLY_WAND_BINDING =
            ResourceLocation.fromNamespaceAndPath(
                    "irons_spellbooks",
                    "blessing_of_life"
            );

    public static final ResourceLocation HOLY_STAFF_BINDING =
            martial("holy_shock");

    public static final List<ResourceLocation> PALADIN_LIBRAM =
            List.of(
                    martial("flash_heal"),
                    martial("blessed_strikes"),
                    martial("divine_protection"),
                    martial("judgement"),
                    martial("battle_banner"),
                    martial("immolation")
            );

    public static final List<ResourceLocation> PRIEST_HOLY_BOOK =
            List.of(
                    martial("holy_beam"),
                    martial("circle_of_healing"),
                    martial("barrier"),
                    martial("lightwell"),
                    martial("levitate"),
                    martial("penance")
            );

    /** Internal Lightwell helper; never part of player spell availability. */
    public static final ResourceLocation HOLY_MOTE_HELPER =
            martial("lightwell_orb");

    /**
     * Fourteen source-equivalent player-facing entries:
     * one native wand replacement, one staff spell, and twelve book spells.
     */
    public static final List<ResourceLocation> PLAYER_FACING_SOURCE_EQUIVALENTS =
            List.of(
                    HOLY_WAND_BINDING,
                    HOLY_STAFF_BINDING,
                    PALADIN_LIBRAM.get(0),
                    PALADIN_LIBRAM.get(1),
                    PALADIN_LIBRAM.get(2),
                    PALADIN_LIBRAM.get(3),
                    PALADIN_LIBRAM.get(4),
                    PALADIN_LIBRAM.get(5),
                    PRIEST_HOLY_BOOK.get(0),
                    PRIEST_HOLY_BOOK.get(1),
                    PRIEST_HOLY_BOOK.get(2),
                    PRIEST_HOLY_BOOK.get(3),
                    PRIEST_HOLY_BOOK.get(4),
                    PRIEST_HOLY_BOOK.get(5)
            );

    private PaladinPriestSpellCatalog() {
    }

    private static ResourceLocation martial(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                MartialSpells.MOD_ID,
                path
        );
    }
}
