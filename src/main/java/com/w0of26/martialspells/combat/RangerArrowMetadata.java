package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

import java.util.UUID;

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

    /** Prevents a rejoined/reloaded projectile from spending a later stash. */
    public static boolean markShotEvaluated(AbstractArrow arrow) {
        CompoundTag state = state(arrow);
        if (state.getBoolean("shotEvaluated")) return false;
        state.putBoolean("shotEvaluated", true);
        save(arrow, state);
        return true;
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

    /** Per projectile/target hit guard; stored on the arrow across reloads. */
    public static boolean markTransferredHit(AbstractArrow arrow, ResourceLocation identity, UUID targetId) {
        CompoundTag state = state(arrow);
        CompoundTag hits = state.getCompound("transferredHits");
        CompoundTag spellHits = hits.getCompound(identity.toString());
        String key = targetId.toString();
        if (spellHits.getBoolean(key)) return false;
        spellHits.putBoolean(key, true);
        hits.put(identity.toString(), spellHits);
        state.put("transferredHits", hits);
        save(arrow, state);
        return true;
    }

    public static void setIframeBypass(AbstractArrow arrow, boolean enabled) {
        CompoundTag state = state(arrow);
        state.putBoolean("iframeBypass", enabled);
        save(arrow, state);
    }

    public static boolean bypassesIframes(AbstractArrow arrow) {
        return state(arrow).getBoolean("iframeBypass");
    }

    /** Barrage hit-time identity, distinct from any Power Shot transfer. */
    public static void markBarrage(AbstractArrow arrow) {
        CompoundTag state = state(arrow);
        state.putBoolean("barrage", true);
        save(arrow, state);
    }

    public static boolean isBarrage(AbstractArrow arrow) {
        return state(arrow).getBoolean("barrage");
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
