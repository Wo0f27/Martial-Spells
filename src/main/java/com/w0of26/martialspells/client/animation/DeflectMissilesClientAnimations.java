package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeflectMissilesClientAnimations {

    /*
     * Copied Better Combat:
     * one_handed_swipe_horizontal_right
     *
     * Resource file:
     *
     * assets/martial_spells/player_animation/
     * deflect_missiles_swipe.json
     */
    private static final ResourceLocation SWIPE_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "deflect_missiles_swipe"
            );

    /*
     * Better Combat's source animation has its important
     * motion ending around tick 20.
     *
     * Compress it into 8 ticks so that an individual
     * projectile deflection looks quick and reactive.
     */
    private static final float SWIPE_DURATION_TICKS =
            8.0F;

    /*
     * Prevent extremely rapid projectile impacts from
     * restarting the animation every single game tick.
     *
     * This is intentionally very short. Deflect Missiles
     * should still visibly react to rapid barrages.
     */
    private static final int MIN_SWIPE_INTERVAL_TICKS =
            2;

    /*
     * Last game tick on which a Deflect Missiles swipe
     * was started for each visible player.
     */
    private static final Map<UUID, Integer>
            LAST_SWIPE_TICK =
            new HashMap<>();

    private DeflectMissilesClientAnimations() {
    }

    /*
     * Called by the clientbound Deflect Missiles animation
     * packet after the server confirms that a projectile
     * was actually deflected.
     *
     * mirroredChoice is selected by the server so every
     * tracking client sees the same left/right deflection.
     *
     * false = base/original animation
     * true  = mirrored animation
     */
    public static void play(
            UUID playerId,
            boolean mirroredChoice
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            LAST_SWIPE_TICK.clear();
            return;
        }

        AbstractClientPlayer player =
                findPlayer(
                        minecraft,
                        playerId
                );

        if (player == null) {
            return;
        }

        /*
         * Avoid constantly resetting the animation when
         * multiple projectiles impact almost simultaneously.
         */
        int currentTick =
                player.tickCount;

        Integer lastSwipeTick =
                LAST_SWIPE_TICK.get(
                        playerId
                );

        if (lastSwipeTick != null
                && currentTick >= lastSwipeTick
                && currentTick - lastSwipeTick
                < MIN_SWIPE_INTERVAL_TICKS) {
            return;
        }

        LAST_SWIPE_TICK.put(
                playerId,
                currentTick
        );

        playSwipe(
                player,
                mirroredChoice
        );
    }

    /*
     * Find the player represented by the UUID supplied by
     * the server packet.
     *
     * This supports both:
     *
     * - the local player
     * - other players currently visible to this client
     */
    private static AbstractClientPlayer findPlayer(
            Minecraft minecraft,
            UUID playerId
    ) {
        for (Player rawPlayer
                : minecraft.level.players()) {

            if (!(rawPlayer
                    instanceof AbstractClientPlayer player)) {
                continue;
            }

            if (player.getUUID()
                    .equals(playerId)) {

                return player;
            }
        }

        return null;
    }

    private static void playSwipe(
            AbstractClientPlayer player,
            boolean mirroredChoice
    ) {
        var keyframeAnimation =
                PlayerAnimationRegistry
                        .getAnimation(
                                SWIPE_ANIMATION
                        );

        if (keyframeAnimation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Deflect Missiles "
                            + "swipe animation {}",
                    SWIPE_ANIMATION
            );
            return;
        }

        /*
         * Get Iron's existing spell-casting animation layer.
         *
         * Deflect Missiles temporarily replaces Iron's
         * normal continuous casting pose only when a
         * projectile is actually intercepted.
         */
        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> castingLayer =
                (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess
                                .getPlayerAssociatedData(
                                        player
                                )
                                .get(
                                        SpellAnimations
                                                .ANIMATION_RESOURCE
                                );

        if (castingLayer == null) {
            MartialSpells.LOGGER.warn(
                    "Iron's casting animation layer "
                            + "was not available for {}",
                    player.getGameProfile()
                            .getName()
            );
            return;
        }

        /*
         * The server randomly chooses whether the base
         * or mirrored animation should play.
         */
        boolean mirrored =
                mirroredChoice;

        /*
         * Respect the player's configured dominant arm.
         *
         * Right-handed:
         *
         * false -> right-side deflection
         * true  -> left-side deflection
         *
         * Left-handed:
         *
         * false -> left-side deflection
         * true  -> right-side deflection
         */
        if (player.getMainArm()
                == HumanoidArm.LEFT) {

            mirrored =
                    !mirrored;
        }

        KeyframeAnimationPlayer animationPlayer =
                new KeyframeAnimationPlayer(
                        keyframeAnimation
                );

        /*
         * Compress the source animation into the shorter
         * Deflect Missiles impact reaction.
         */
        float animationSpeed =
                Math.max(
                        1.0F,
                        keyframeAnimation.endTick
                                / SWIPE_DURATION_TICKS
                );

        ModifierLayer<IAnimation> swipeLayer =
                new ModifierLayer<>(
                        animationPlayer
                );

        swipeLayer.addModifierBefore(
                new SpeedModifier(
                        animationSpeed
                )
        );

        /*
         * The same animation JSON provides both sides.
         *
         * false = original
         * true  = mirrored
         */
        swipeLayer.addModifierBefore(
                new MirrorModifier(
                        mirrored
                )
        );

        /*
         * Temporarily replace Iron's normal casting pose
         * with the projectile-deflection movement.
         */
        castingLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(
                        1,
                        Ease.INOUTSINE
                ),
                swipeLayer,
                true
        );
    }
}