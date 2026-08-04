package com.w0of26.martialspells.item;

/**
 * Authoritative progression data for Monk Codices.
 *
 * Slots 0 and 1 are always reserved for the two core techniques.
 * The remaining slots are ordinary open spell slots.
 */
public enum MonkCodexTier {
    TIER_I(
            1,
            "I",
            4,
            1,
            3
    ),
    TIER_II(
            2,
            "II",
            5,
            2,
            3
    ),
    TIER_III(
            3,
            "III",
            6,
            3,
            4
    ),
    TIER_IV(
            4,
            "IV",
            7,
            4,
            4
    ),
    TIER_V(
            5,
            "V",
            8,
            5,
            5
    );

    private static final int CORE_SLOT_COUNT = 2;

    private final int serializedId;
    private final String displayName;
    private final int totalSpellSlots;
    private final int coreTechniqueLevel;
    private final int maximumKi;

    MonkCodexTier(
            int serializedId,
            String displayName,
            int totalSpellSlots,
            int coreTechniqueLevel,
            int maximumKi
    ) {
        this.serializedId = serializedId;
        this.displayName = displayName;
        this.totalSpellSlots = totalSpellSlots;
        this.coreTechniqueLevel =
                coreTechniqueLevel;
        this.maximumKi = maximumKi;
    }

    public int getSerializedId() {
        return serializedId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTotalSpellSlots() {
        return totalSpellSlots;
    }

    public int getOpenSpellSlots() {
        return totalSpellSlots
                - CORE_SLOT_COUNT;
    }

    public int getCoreTechniqueLevel() {
        return coreTechniqueLevel;
    }

    public int getMaximumKi() {
        return maximumKi;
    }

    public boolean isHigherThan(
            MonkCodexTier other
    ) {
        return serializedId
                > other.serializedId;
    }

    public MonkCodexTier getNextTier() {
        return switch (this) {
            case TIER_I -> TIER_II;
            case TIER_II -> TIER_III;
            case TIER_III -> TIER_IV;
            case TIER_IV -> TIER_V;
            case TIER_V -> TIER_V;
        };
    }

    /**
     * Missing, invalid, and future unsupported values safely
     * fall back to Tier I.
     */
    public static MonkCodexTier fromSerializedId(
            int serializedId
    ) {
        for (MonkCodexTier tier : values()) {
            if (tier.serializedId
                    == serializedId) {
                return tier;
            }
        }

        return TIER_I;
    }
}