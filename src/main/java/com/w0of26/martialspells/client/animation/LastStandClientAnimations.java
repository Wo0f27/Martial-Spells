package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.spells.LastStandSpell;
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
 * Plays Last Stand's release animation after an intentional early release.
 *
 * <p>Iron's suppresses finish animations for cancelled LONG casts; Spell Engine
 * CHANNEL releases play the release clip. Full completion still uses Iron's
 * ordinary finish-animation packet.</p>
 */
public final class LastStandClientAnimations {
    private LastStandClientAnimations() {}

    public static void playRelease(UUID playerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var entity = minecraft.level.getPlayerByUUID(playerId);
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        var animation = PlayerAnimationRegistry.getAnimation(
                LastStandSpell.RELEASE_ANIMATION
        );
        if (animation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Last Stand release animation {}",
                    LastStandSpell.RELEASE_ANIMATION
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
                    "Iron's casting animation layer unavailable for Last Stand caster {}",
                    player.getGameProfile().getName()
            );
            return;
        }

        ModifierLayer<IAnimation> releaseLayer =
                new ModifierLayer<>(
                        new KeyframeAnimationPlayer(animation)
                );

        castingLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(
                        1,
                        Ease.INOUTSINE
                ),
                releaseLayer,
                true
        );
    }
}
