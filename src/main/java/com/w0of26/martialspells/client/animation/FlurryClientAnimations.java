package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class FlurryClientAnimations {
    private static final ResourceLocation[]
            LEVEL_ANIMATIONS = {
            animation("flurry_of_blows_1"),
            animation("flurry_of_blows_2"),
            animation("flurry_of_blows_3"),
            animation("flurry_of_blows_4"),
            animation("flurry_of_blows_5")
    };

    private FlurryClientAnimations() {
    }

    public static void play(
            LivingEntity entity,
            int spellLevel
    ) {
        if (!(entity
                instanceof AbstractClientPlayer player)) {
            return;
        }

        ResourceLocation animationId =
                LEVEL_ANIMATIONS[
                        clampLevel(spellLevel) - 1
                        ];

        var keyframeAnimation =
                PlayerAnimationRegistry.getAnimation(
                        animationId
                );

        if (keyframeAnimation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Flurry animation {}",
                    animationId
            );
            return;
        }

        /*
         * Use Iron's existing casting-animation layer. This makes
         * Flurry follow the same animation priority as other spells.
         */
        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> animationLayer =
                (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess
                                .getPlayerAssociatedData(
                                        player
                                )
                                .get(
                                        SpellAnimations
                                                .ANIMATION_RESOURCE
                                );

        if (animationLayer == null) {
            MartialSpells.LOGGER.warn(
                    "Iron's casting animation layer was not "
                            + "available for {}",
                    player.getGameProfile().getName()
            );
            return;
        }

        KeyframeAnimationPlayer animationPlayer =
                new KeyframeAnimationPlayer(
                        keyframeAnimation
                );

        animationLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(
                        2,
                        Ease.INOUTSINE
                ),
                animationPlayer,
                true
        );
    }

    private static ResourceLocation animation(
            String path
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                MartialSpells.MOD_ID,
                path
        );
    }

    private static int clampLevel(int spellLevel) {
        return Math.max(
                1,
                Math.min(spellLevel, LEVEL_ANIMATIONS.length)
        );
    }
}