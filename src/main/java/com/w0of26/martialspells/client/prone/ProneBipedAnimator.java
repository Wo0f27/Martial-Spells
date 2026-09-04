package com.w0of26.martialspells.client.prone;

import com.w0of26.martialspells.MartialSpells;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import me.Thelnfamous1.mobplayeranimator.api.MobAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class ProneBipedAnimator {

    /*
     * TEMPORARY diagnostic animation.
     *
     * Once Player + Zombie both use this successfully,
     * this becomes martial_spells:prone_knockdown.
     */
    private static final ResourceLocation TEST_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "stillwater_meditation"
            );

    private ProneBipedAnimator() {
    }

    public static void play(
            LivingEntity entity
    ) {
        if (!ProneClientAnimationRegistry
                .usesBipedAnimator(entity)) {
            return;
        }

        var animation =
                PlayerAnimationRegistry.getAnimation(
                        TEST_ANIMATION
                );

        if (animation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Prone test animation {}",
                    TEST_ANIMATION
            );

            return;
        }

        ModifierLayer<IAnimation> layer =
                getLayer(
                        entity
                );

        if (layer == null) {
            MartialSpells.LOGGER.warn(
                    "No registered Prone animation layer "
                            + "found for entity type {}",
                    entity.getType()
            );

            return;
        }

        MartialSpells.LOGGER.info(
                "Prone client animation layer found: "
                        + "entityType={}, entityId={}, clientSide={}",
                entity.getType(),
                entity.getId(),
                entity.level().isClientSide
        );

        layer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(
                        2,
                        Ease.INOUTSINE
                ),
                new KeyframeAnimationPlayer(
                        animation
                ),
                true
        );
    }

    public static void stop(
            LivingEntity entity
    ) {
        ModifierLayer<IAnimation> layer =
                getLayer(
                        entity
                );

        if (layer == null) {
            return;
        }

        layer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(
                        3,
                        Ease.INOUTSINE
                ),
                null
        );
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> getLayer(
            LivingEntity entity
    ) {
        IAnimation stored;

        if (entity
                instanceof AbstractClientPlayer player) {

            stored =
                    PlayerAnimationAccess
                            .getPlayerAssociatedData(
                                    player
                            )
                            .get(
                                    ProneAnimationClientBootstrap
                                            .PRONE_LAYER_ID
                            );

        } else if (entity
                instanceof Mob mob) {

            stored =
                    MobAnimationAccess
                            .getMobAssociatedData(
                                    mob
                            )
                            .get(
                                    ProneAnimationClientBootstrap
                                            .PRONE_LAYER_ID
                            );

        } else {
            return null;
        }

        if (stored
                instanceof ModifierLayer<?> modifierLayer) {

            return (ModifierLayer<IAnimation>)
                    modifierLayer;
        }

        return null;
    }
}