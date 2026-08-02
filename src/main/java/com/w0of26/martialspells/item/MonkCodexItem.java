package com.w0of26.martialspells.item;

import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Tier I spellbook for the Monk archetype.
 *
 * Phase 2 will add its two locked core techniques:
 * Stillwater Meditation and Flurry of Blows.
 */
public final class MonkCodexItem extends SpellBook {
    public static final int CODEX_TIER = 1;
    public static final int MAXIMUM_KI = 3;

    /*
     * Two future locked core-technique slots and two open slots.
     */
    public static final int TOTAL_SPELL_SLOTS = 4;

    public MonkCodexItem() {
        super(
                TOTAL_SPELL_SLOTS,
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON)
        );
    }

    public int getCodexTier() {
        return CODEX_TIER;
    }

    public int getMaximumKi() {
        return MAXIMUM_KI;
    }
}