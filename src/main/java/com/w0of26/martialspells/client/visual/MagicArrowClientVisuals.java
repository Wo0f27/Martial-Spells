package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only render state for Iron's base Magic Arrow while Martial
 * Spells is installed. Nothing in this class mutates item-use state or
 * crossbow NBT; it only answers questions used by render hooks.
 */
public final class MagicArrowClientVisuals {
    public static final String MAGIC_ARROW_ID =
            "irons_spellbooks:magic_arrow";

    private MagicArrowClientVisuals() {
    }

    public static boolean isMagicArrowCasting(
            @Nullable LivingEntity entity
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        /*
         * The local player's exact cast state lives in MagicData.
         * Remote players are represented by Iron's synchronized spell
         * data, so third-person multiplayer rendering works as well.
         */
        if (minecraft.player == player) {
            return ClientMagicData.isCasting()
                    && MAGIC_ARROW_ID.equals(
                    ClientMagicData.getCastingSpellId()
            );
        }

        SyncedSpellData synced =
                ClientMagicData.getSyncedSpellData(player);

        return synced.isCasting()
                && MAGIC_ARROW_ID.equals(
                synced.getCastingSpellId()
        );
    }

    public static boolean isHeldRangedStack(
            @Nullable LivingEntity entity,
            ItemStack stack
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        return stack == player.getMainHandItem()
                || stack == player.getOffhandItem();
    }

    /**
     * Exact local visual pull progress. Remote players do not receive
     * the effective cast duration, so they render as fully drawn while
     * the synchronized cast flag is active.
     */
    public static float getBowPullProgress(
            @Nullable LivingEntity entity
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (entity != null && minecraft.player == entity) {
            return Mth.clamp(
                    ClientMagicData.getCastCompletionPercent(),
                    0.0F,
                    1.0F
            );
        }

        return 1.0F;
    }

    /**
     * Returns the physical player arm currently holding a classified
     * crossbow, preferring the main hand when both hands qualify.
     */
    @Nullable
    public static HumanoidArm getCrossbowArm(Player player) {
        if (RangedWeaponClassifier.isCrossbow(
                player.getMainHandItem()
        )) {
            return player.getMainArm();
        }

        if (RangedWeaponClassifier.isCrossbow(
                player.getOffhandItem()
        )) {
            return player.getMainArm().getOpposite();
        }

        return null;
    }

    public static boolean isSupportedRangedWeapon(ItemStack stack) {
        return RangedWeaponClassifier.isSupported(stack);
    }
}
