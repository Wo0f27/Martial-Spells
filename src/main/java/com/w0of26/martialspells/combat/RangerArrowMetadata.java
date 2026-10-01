package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

/** Server-owned, Forge-persistent state attached to real ammo-created arrows. */
public final class RangerArrowMetadata {
    private static final String ROOT = MartialSpells.MOD_ID + ":ranger_arrow";
    private static final String TRANSFERS = "transfers";

    private RangerArrowMetadata() {}

    private static CompoundTag state(AbstractArrow arrow) {
        return arrow.getPersistentData().getCompound(ROOT);
    }

    private static void save(AbstractArrow arrow, CompoundTag state) {
        state.putInt("schema", 1);
        arrow.getPersistentData().put(ROOT, state);
    }

    public static void markEligibleShot(AbstractArrow arrow) {
        CompoundTag state = state(arrow);
        state.putBoolean("eligibleShot", true);
        save(arrow, state);
    }

    public static boolean isEligibleShot(AbstractArrow arrow) {
        return state(arrow).getBoolean("eligibleShot");
    }

    /** Returns false on a second stamp with the same transfer identity. */
    public static boolean stampTransfer(AbstractArrow arrow, ResourceLocation identity, CompoundTag payload) {
        CompoundTag state = state(arrow);
        CompoundTag transfers = state.getCompound(TRANSFERS);
        String key = identity.toString();
        if (transfers.contains(key)) return false;
        transfers.put(key, payload.copy());
        state.put(TRANSFERS, transfers);
        save(arrow, state);
        return true;
    }

    public static boolean hasTransfer(AbstractArrow arrow, ResourceLocation identity) {
        return state(arrow).getCompound(TRANSFERS).contains(identity.toString());
    }

    public static CompoundTag transfer(AbstractArrow arrow, ResourceLocation identity) {
        return state(arrow).getCompound(TRANSFERS).getCompound(identity.toString()).copy();
    }

    public static void setIframeBypass(AbstractArrow arrow, boolean enabled) {
        CompoundTag state = state(arrow);
        state.putBoolean("iframeBypass", enabled);
        save(arrow, state);
    }

    public static boolean bypassesIframes(AbstractArrow arrow) {
        return state(arrow).getBoolean("iframeBypass");
    }

    public static void setExtraPierceTargets(AbstractArrow arrow, int extraTargets) {
        if (extraTargets < 0) throw new IllegalArgumentException("extraTargets must be nonnegative");
        CompoundTag state = state(arrow);
        state.putInt("extraPierceTargets", extraTargets);
        save(arrow, state);
    }

    public static int extraPierceTargets(AbstractArrow arrow) {
        return state(arrow).getInt("extraPierceTargets");
    }
}
