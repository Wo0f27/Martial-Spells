package com.w0of26.martialspells.item;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Upgradeable Monk spellbook.
 *
 * Codex progression data is stored on the ItemStack so different
 * Codex tiers can coexist while sharing the same registered item.
 */
public final class MonkCodexItem
        extends SpellBook {

    public static final int MEDITATION_SLOT = 0;
    public static final int FLURRY_SLOT = 1;

    private static final String CODEX_DATA_TAG =
            "MartialSpells";

    private static final String CODEX_TIER_TAG =
            "CodexTier";

    public MonkCodexItem() {
        /*
         * The base item begins with Tier I capacity.
         *
         * Higher-tier stacks receive their actual slot count through
         * their attached ISpellContainer.
         */
        super(
                MonkCodexTier.TIER_I
                        .getTotalSpellSlots(),
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON)
        );
    }

    /**
     * Reads the stack's Codex tier without modifying its NBT.
     *
     * Old Codices created before progression existed safely become
     * Tier I.
     */
    public static MonkCodexTier getTier(
            ItemStack itemStack
    ) {
        if (itemStack == null
                || itemStack.isEmpty()
                || !(itemStack.getItem()
                instanceof MonkCodexItem)) {
            return MonkCodexTier.TIER_I;
        }

        CompoundTag codexData =
                itemStack.getTagElement(
                        CODEX_DATA_TAG
                );

        if (codexData == null
                || !codexData.contains(
                CODEX_TIER_TAG,
                Tag.TAG_INT
        )) {
            return MonkCodexTier.TIER_I;
        }

        return MonkCodexTier
                .fromSerializedId(
                        codexData.getInt(
                                CODEX_TIER_TAG
                        )
                );
    }

    public static int getMaximumKi(
            ItemStack itemStack
    ) {
        return getTier(itemStack)
                .getMaximumKi();
    }

    /**
     * Writes only the tier metadata.
     *
     * The safe upgrader added in the next checkpoint will call this
     * after rebuilding the spell container successfully.
     */
    static void writeTierData(
            ItemStack itemStack,
            MonkCodexTier tier
    ) {
        if (itemStack == null
                || itemStack.isEmpty()
                || !(itemStack.getItem()
                instanceof MonkCodexItem)) {
            throw new IllegalArgumentException(
                    "Cannot assign a Monk Codex tier "
                            + "to a non-Codex ItemStack."
            );
        }

        CompoundTag codexData =
                itemStack.getOrCreateTagElement(
                        CODEX_DATA_TAG
                );

        codexData.putInt(
                CODEX_TIER_TAG,
                tier.getSerializedId()
        );
    }

    @Override
    public void initializeSpellContainer(
            ItemStack itemStack
    ) {
        if (itemStack == null
                || itemStack.isEmpty()) {
            return;
        }

        /*
         * Never overwrite an initialized container. Existing spell
         * preservation will be handled by the dedicated upgrader.
         */
        if (ISpellContainer
                .isSpellContainer(itemStack)) {
            return;
        }

        MonkCodexTier tier =
                getTier(itemStack);

        /*
         * Make the default Tier I explicit on newly initialized
         * Codices while preserving old untagged stacks as Tier I.
         */
        writeTierData(
                itemStack,
                tier
        );

        ISpellContainerMutable spellContainer =
                ISpellContainer.create(
                        tier.getTotalSpellSlots(),
                        true,
                        true
                ).mutableCopy();

        boolean meditationAdded =
                spellContainer.addSpellAtIndex(
                        MartialSpellRegistry
                                .STILLWATER_MEDITATION
                                .get(),
                        tier.getCoreTechniqueLevel(),
                        MEDITATION_SLOT,
                        true
                );

        boolean flurryAdded =
                spellContainer.addSpellAtIndex(
                        MartialSpellRegistry
                                .FLURRY_OF_BLOWS
                                .get(),
                        tier.getCoreTechniqueLevel(),
                        FLURRY_SLOT,
                        true
                );

        if (!meditationAdded) {
            MartialSpells.LOGGER.error(
                    "Failed to add Stillwater Meditation "
                            + "to a Tier {} Monk Codex",
                    tier.getDisplayName()
            );
        }

        if (!flurryAdded) {
            MartialSpells.LOGGER.error(
                    "Failed to add Flurry of Blows "
                            + "to a Tier {} Monk Codex",
                    tier.getDisplayName()
            );
        }

        ISpellContainer.set(
                itemStack,
                spellContainer.toImmutable()
        );
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            @Nullable Level level,
            List<Component> lines,
            TooltipFlag flag
    ) {
        MonkCodexTier tier =
                getTier(itemStack);

        lines.add(
                Component.translatable(
                        "tooltip.martial_spells."
                                + "monk_codex_tier",
                        tier.getDisplayName()
                ).withStyle(
                        ChatFormatting.AQUA
                )
        );

        lines.add(
                Component.translatable(
                        "tooltip.martial_spells."
                                + "monk_codex_max_ki",
                        tier.getMaximumKi()
                ).withStyle(
                        ChatFormatting.DARK_AQUA
                )
        );

        super.appendHoverText(
                itemStack,
                level,
                lines,
                flag
        );
    }
}