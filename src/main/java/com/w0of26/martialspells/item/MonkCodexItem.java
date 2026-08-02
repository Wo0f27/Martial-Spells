package com.w0of26.martialspells.item;

import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.world.item.ItemStack;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
/**
 * Tier I spellbook for the Monk archetype.
 *
 * Phase 2 will add its two locked core techniques:
 * Stillwater Meditation and Flurry of Blows.
 */
public final class MonkCodexItem extends SpellBook {
    public static final int CODEX_TIER = 1;
    public static final int MAXIMUM_KI = 3;
    public static final int MEDITATION_SLOT = 0;
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

    @Override
    public void initializeSpellContainer(
            ItemStack itemStack
    ) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        /*
         * Do not overwrite an already initialized Codex.
         */
        if (ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        /*
         * ISpellContainer is immutable in Iron's 3.16.2.
         * Create a mutable copy before inserting spells.
         */
        ISpellContainerMutable spellContainer =
                ISpellContainer.create(
                        TOTAL_SPELL_SLOTS,
                        true,
                        true
                ).mutableCopy();

        boolean meditationAdded =
                spellContainer.addSpellAtIndex(
                        MartialSpellRegistry
                                .STILLWATER_MEDITATION
                                .get(),
                        CODEX_TIER,
                        MEDITATION_SLOT,
                        true
                );

        if (!meditationAdded) {
            MartialSpells.LOGGER.error(
                    "Failed to add Stillwater Meditation "
                            + "to the Monk Codex"
            );
        }

        /*
         * Convert the edited container back to its immutable form,
         * then attach it to the Codex ItemStack.
         */
        ISpellContainer.set(
                itemStack,
                spellContainer.toImmutable()
        );
    }
}