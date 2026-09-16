package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.spells.MutilateSpell;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * Plays Mutilate's frozen dual-cross-slash pose on Iron's existing
 * PlayerAnimator casting layer.
 *
 * <p>This deliberately mirrors the already-proven Stunning Strike animation
 * path instead of depending on Iron's instant-spell finish callback. The server
 * explicitly synchronizes the visual to the caster and tracking clients.</p>
 */
public final class MutilateClientAnimations {
    private MutilateClientAnimations() {
    }

    public static void play(UUID playerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var entity = minecraft.level.getPlayerByUUID(playerId);
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        var keyframeAnimation =
                PlayerAnimationRegistry.getAnimation(MutilateSpell.MUTILATE_ANIMATION);
        if (keyframeAnimation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Mutilate animation {}. Run tools/sync-rogue-r6-assets.ps1 and restart the client.",
                    MutilateSpell.MUTILATE_ANIMATION
            );
            return;
        }

        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> castingLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess
                        .getPlayerAssociatedData(player)
                        .get(SpellAnimations.ANIMATION_RESOURCE);

        if (castingLayer == null) {
            MartialSpells.LOGGER.warn(
                    "Iron's casting animation layer was unavailable for Mutilate caster {}",
                    player.getGameProfile().getName()
            );
            return;
        }

        KeyframeAnimationPlayer animationPlayer =
                new KeyframeAnimationPlayer(keyframeAnimation);
        ModifierLayer<IAnimation> mutilateLayer =
                new ModifierLayer<>(animationPlayer);

        castingLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(1, Ease.INOUTSINE),
                mutilateLayer,
                true
        );
    }
}
