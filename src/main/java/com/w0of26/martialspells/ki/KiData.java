package com.w0of26.martialspells.ki;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Stores a player's current and maximum Ki.
 *
 * The logical server is authoritative. Client-side copies will only be
 * used for displaying the Ki bar.
 */
public final class KiData {
    private static final String CURRENT_KI_TAG = "CurrentKi";
    private static final String MAXIMUM_KI_TAG = "MaximumKi";

    private int currentKi;
    private int maximumKi;

    public int getCurrentKi() {
        return currentKi;
    }

    public int getMaximumKi() {
        return maximumKi;
    }

    /**
     * Sets the current Ki while keeping it between zero and the
     * player's maximum Ki.
     */
    public void setCurrentKi(int amount) {
        currentKi = Mth.clamp(
                amount,
                0,
                maximumKi
        );
    }

    /**
     * Changes the player's maximum Ki.
     *
     * Current Ki is automatically reduced if the new maximum is lower.
     */
    public void setMaximumKi(int amount) {
        maximumKi = Math.max(0, amount);
        currentKi = Math.min(currentKi, maximumKi);
    }

    /**
     * Adds Ki and returns the amount that was actually added.
     */
    public int addKi(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int previousKi = currentKi;

        setCurrentKi(currentKi + amount);

        return currentKi - previousKi;
    }

    /**
     * Removes Ki and returns the amount that was actually removed.
     */
    public int removeKi(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int previousKi = currentKi;

        setCurrentKi(currentKi - amount);

        return previousKi - currentKi;
    }

    /**
     * Attempts to spend exactly the requested amount of Ki.
     *
     * @return true when the Ki was successfully consumed
     */
    public boolean consumeKi(int amount) {
        if (amount < 0 || currentKi < amount) {
            return false;
        }

        currentKi -= amount;
        return true;
    }

    public boolean hasKi(int amount) {
        return amount >= 0 && currentKi >= amount;
    }

    public void fill() {
        currentKi = maximumKi;
    }

    public void reset() {
        currentKi = 0;
        maximumKi = 0;
    }

    /**
     * Copies another Ki instance.
     *
     * This is used for player cloning that is not caused by death,
     * such as returning from the End.
     */
    public void copyFrom(KiData other) {
        maximumKi = Math.max(0, other.maximumKi);
        currentKi = Mth.clamp(
                other.currentKi,
                0,
                maximumKi
        );
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putInt(CURRENT_KI_TAG, currentKi);
        tag.putInt(MAXIMUM_KI_TAG, maximumKi);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        setMaximumKi(
                tag.getInt(MAXIMUM_KI_TAG)
        );

        setCurrentKi(
                tag.getInt(CURRENT_KI_TAG)
        );
    }
}