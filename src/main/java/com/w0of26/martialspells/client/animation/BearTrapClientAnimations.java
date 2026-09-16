package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.spells.BearTrapSpell;
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

/** Explicitly plays Bear Trap's frozen ground-release animation. */
public final class BearTrapClientAnimations {
    private BearTrapClientAnimations() {
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
                PlayerAnimationRegistry.getAnimation(BearTrapSpell.RELEASE_ANIMATION);
        if (keyframeAnimation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Bear Trap animation {}. Run tools/sync-rogue-r7-assets.ps1 and restart the client.",
                    BearTrapSpell.RELEASE_ANIMATION
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
                    "Iron's casting animation layer was unavailable for Bear Trap caster {}",
                    player.getGameProfile().getName()
            );
            return;
        }

        KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(keyframeAnimation);
        ModifierLayer<IAnimation> bearTrapLayer = new ModifierLayer<>(animationPlayer);
        castingLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(1, Ease.INOUTSINE),
                bearTrapLayer,
                true
        );
    }
}
