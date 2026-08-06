package com.w0of26.martialspells.item;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.item.ItemStack;

/**
 * Performs atomic Monk Codex container upgrades.
 *
 * The original ItemStack is not modified until the replacement
 * spell container has been completely rebuilt.
 */
public final class MonkCodexUpgradeHelper {
    private MonkCodexUpgradeHelper() {
    }

    public enum UpgradeResult {
        SUCCESS,
        INVALID_ITEM,
        ALREADY_MAXIMUM,
        MISSING_CONTAINER,
        CORE_INSERT_FAILED,
        OPEN_SPELL_COPY_FAILED
    }

    public static UpgradeResult upgradeToNextTier(
            ItemStack codexStack
    ) {
        if (codexStack == null
                || codexStack.isEmpty()
                || !(codexStack.getItem()
                instanceof MonkCodexItem codexItem)) {
            return UpgradeResult.INVALID_ITEM;
        }

        /*
         * Support old or otherwise uninitialized Codices.
         */
        if (!ISpellContainer
                .isSpellContainer(codexStack)) {
            codexItem.initializeSpellContainer(
                    codexStack
            );
        }

        if (!ISpellContainer
                .isSpellContainer(codexStack)) {
            return UpgradeResult.MISSING_CONTAINER;
        }

        MonkCodexTier currentTier =
                MonkCodexItem.getTier(
                        codexStack
                );

        MonkCodexTier targetTier =
                currentTier.getNextTier();

        if (targetTier == currentTier) {
            return UpgradeResult.ALREADY_MAXIMUM;
        }

        ISpellContainer oldContainer =
                ISpellContainer.get(
                        codexStack
                );

        if (oldContainer == null) {
            return UpgradeResult.MISSING_CONTAINER;
        }

        int oldCapacity =
                oldContainer.getMaxSpellCount();

        /*
         * Preserve the Codex's existing total capacity.
         *
         * Tier progression may raise the capacity to the target tier's
         * normal amount, but it must never reduce externally upgraded
         * capacity or exceed the absolute supported limit.
         */
        int targetCapacity =
                Math.min(
                        MonkCodexItem.ABSOLUTE_SLOT_LIMIT,
                        Math.max(
                                oldCapacity,
                                targetTier.getTotalSpellSlots()
                        )
                );

        /*
         * Build a completely separate container first.
         */
        ISpellContainerMutable newContainer =
                ISpellContainer.create(
                        targetCapacity,
                        oldContainer.isSpellWheel(),
                        oldContainer.mustEquip()
                ).mutableCopy();



        /*
         * Preserve Iron's improved-container flag.
         */
        newContainer.setImproved(
                oldContainer.isImproved()
        );

        boolean meditationAdded =
                newContainer.addSpellAtIndex(
                        MartialSpellRegistry
                                .STILLWATER_MEDITATION
                                .get(),
                        targetTier
                                .getCoreTechniqueLevel(),
                        MonkCodexItem
                                .MEDITATION_SLOT,
                        true
                );

        boolean flurryAdded =
                newContainer.addSpellAtIndex(
                        MartialSpellRegistry
                                .FLURRY_OF_BLOWS
                                .get(),
                        targetTier
                                .getCoreTechniqueLevel(),
                        MonkCodexItem
                                .FLURRY_SLOT,
                        true
                );

        if (!meditationAdded
                || !flurryAdded) {
            MartialSpells.LOGGER.error(
                    "Failed to rebuild the core techniques "
                            + "while upgrading a Monk Codex "
                            + "from Tier {} to Tier {}.",
                    currentTier.getDisplayName(),
                    targetTier.getDisplayName()
            );

            return UpgradeResult
                    .CORE_INSERT_FAILED;
        }

        int copiedSlotLimit =
                Math.min(
                        oldCapacity,
                        targetCapacity
                );



        /*
         * Slots 0 and 1 are rebuilt above.
         * Preserve all ordinary spells in their original indices.
         */
        for (int slotIndex = 2;
             slotIndex < copiedSlotLimit;
             slotIndex++) {

            SpellData oldSpell =
                    oldContainer
                            .getSpellAtIndex(
                                    slotIndex
                            );

            /*
             * Iron's empty SpellData has level 0.
             */
            if (oldSpell.getLevel() <= 0) {
                continue;
            }

            boolean copied =
                    newContainer.addSpellAtIndex(
                            oldSpell.getSpell(),
                            oldSpell.getLevel(),
                            slotIndex,
                            oldSpell.isLocked()
                    );

            if (!copied) {
                MartialSpells.LOGGER.error(
                        "Failed to preserve spell {} "
                                + "from slot {} while upgrading "
                                + "a Monk Codex from Tier {} "
                                + "to Tier {}.",
                        oldSpell.getSpell()
                                .getSpellResource(),
                        slotIndex,
                        currentTier.getDisplayName(),
                        targetTier.getDisplayName()
                );

                /*
                 * The original container remains untouched.
                 */
                return UpgradeResult
                        .OPEN_SPELL_COPY_FAILED;
            }
        }

        /*
         * Commit only after every insertion has succeeded.
         */
        ISpellContainer.set(
                codexStack,
                newContainer.toImmutable()
        );

        MonkCodexItem.writeTierData(
                codexStack,
                targetTier
        );

        return UpgradeResult.SUCCESS;
    }
}