package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only render state for Iron's base Magic Arrow while Martial
 * Spells is installed. Nothing here starts real item use or mutates
 * ranged-weapon gameplay state; it only exposes the visual state needed
 * to make the held weapon look as though it is being prepared normally.
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

    /**
     * Main hand is authoritative when both hands contain recognized
     * ranged weapons. This same selection drives validation, arm pose,
     * item-model predicates and optional compatibility render bridges.
     */
    @Nullable
    public static InteractionHand getSelectedRangedHand(Player player) {
        if (RangedWeaponClassifier.isSupported(
                player.getMainHandItem()
        )) {
            return InteractionHand.MAIN_HAND;
        }

        if (RangedWeaponClassifier.isSupported(
                player.getOffhandItem()
        )) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    public static RangedWeaponClassifier.Type getSelectedRangedType(
            Player player
    ) {
        InteractionHand hand = getSelectedRangedHand(player);
        if (hand == null) {
            return RangedWeaponClassifier.Type.NONE;
        }

        return RangedWeaponClassifier.classify(
                player.getItemInHand(hand)
        );
    }

    @Nullable
    public static HumanoidArm getSelectedRangedArm(Player player) {
        InteractionHand hand = getSelectedRangedHand(player);
        if (hand == null) {
            return null;
        }

        return hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
    }

    public static boolean isSelectedRangedStack(
            @Nullable LivingEntity entity,
            ItemStack stack
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        InteractionHand hand = getSelectedRangedHand(player);
        return hand != null
                && player.getItemInHand(hand) == stack;
    }

    /**
     * Used by optional custom-item render bridges such as Cataclysm's
     * bows. Identity matching deliberately avoids affecting an identical
     * stack being rendered in a GUI or held by another player.
     */
    @Nullable
    public static Player findSelectedRangedStackHolder(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        for (Player player : minecraft.level.players()) {
            if (!isMagicArrowCasting(player)) {
                continue;
            }

            InteractionHand hand = getSelectedRangedHand(player);
            if (hand != null && player.getItemInHand(hand) == stack) {
                return player;
            }
        }

        return null;
    }

    /**
     * Exact local cast progress. Iron's synchronized remote-player data
     * does not expose the effective remaining duration, so remote casts
     * use a fully prepared visual while their casting flag is active.
     */
    public static float getCastProgress(
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
}
