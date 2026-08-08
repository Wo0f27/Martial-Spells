package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.ClientDeflectMissilesVisuals;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
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
     * Compress it into 8 ticks so that Deflect Missiles
     * looks like rapid defensive hand movements.
     */
    private static final float SWIPE_DURATION_TICKS =
            8.0F;

    /*
     * Start the next swipe every 8 ticks.
     *
     * This produces:
     *
     * right -> left -> right -> left
     *
     * without a large pause between swipes.
     */
    private static final int SWIPE_REPEAT_TICKS =
            8;

    /*
     * Next game tick on which each player should
     * perform another swipe.
     */
    private static final Map<UUID, Integer>
            NEXT_SWIPE_TICK =
            new HashMap<>();

    /*
     * false = main-hand version
     * true  = mirrored/offhand version
     */
    private static final Map<UUID, Boolean>
            NEXT_SWIPE_OFFHAND =
            new HashMap<>();

    private DeflectMissilesClientAnimations() {
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        /*
         * Clear client visual state when leaving a world.
         */
        if (minecraft.level == null) {
            NEXT_SWIPE_TICK.clear();
            NEXT_SWIPE_OFFHAND.clear();
            return;
        }

        Set<UUID> activePlayers =
                new HashSet<>();

        /*
         * Check every visible client player.
         *
         * This lets the animation work for both the local
         * player and other players in multiplayer.
         */
        for (Player rawPlayer
                : minecraft.level.players()) {

            if (!(rawPlayer
                    instanceof AbstractClientPlayer player)) {
                continue;
            }

            /*
             * Only empty-hand / gauntlet Deflect Missiles
             * uses this animation.
             *
             * Quarterstaff Deflect continues using the
             * spinning-item renderer instead.
             */
            if (!ClientDeflectMissilesVisuals
                    .shouldPlayHandDeflectAnimation(
                            player
                    )) {
                continue;
            }

            UUID playerId =
                    player.getUUID();

            activePlayers.add(
                    playerId
            );

            /*
             * New Deflect channel:
             *
             * make the first swipe happen immediately.
             */
            int nextSwipeTick =
                    NEXT_SWIPE_TICK
                            .getOrDefault(
                                    playerId,
                                    player.tickCount
                            );

            if (player.tickCount
                    < nextSwipeTick) {
                continue;
            }

            /*
             * false:
             *     normal/main-hand animation
             *
             * true:
             *     mirrored/offhand animation
             */
            boolean offhandSwipe =
                    NEXT_SWIPE_OFFHAND
                            .getOrDefault(
                                    playerId,
                                    false
                            );

            playSwipe(
                    player,
                    offhandSwipe
            );

            /*
             * Alternate the next swipe.
             */
            NEXT_SWIPE_OFFHAND.put(
                    playerId,
                    !offhandSwipe
            );

            /*
             * Schedule the next defensive swipe.
             */
            NEXT_SWIPE_TICK.put(
                    playerId,
                    player.tickCount
                            + SWIPE_REPEAT_TICKS
            );
        }

        /*
         * Remove state for players who:
         *
         * - released Deflect Missiles
         * - changed to a quarterstaff
         * - changed to an unsupported weapon
         * - disappeared from the world
         */
        NEXT_SWIPE_TICK
                .keySet()
                .removeIf(
                        playerId ->
                                !activePlayers.contains(
                                        playerId
                                )
                );

        NEXT_SWIPE_OFFHAND
                .keySet()
                .removeIf(
                        playerId ->
                                !activePlayers.contains(
                                        playerId
                                )
                );
    }

    private static void playSwipe(
            AbstractClientPlayer player,
            boolean offhandSwipe
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
         * This is the same layer used by Flurry of Blows.
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
         * The source Better Combat animation is authored
         * for one side.
         *
         * Every second Deflect movement mirrors it to
         * create the opposite-hand swipe.
         */
        boolean mirrored =
                offhandSwipe;

        /*
         * Respect Minecraft's configured dominant hand.
         *
         * Right-handed:
         *
         * false -> right/main
         * true  -> left/offhand
         *
         * Left-handed:
         *
         * false -> left/main
         * true  -> right/offhand
         */
        if (player.getMainArm()
                == HumanoidArm.LEFT) {
            mirrored = !mirrored;
        }

        KeyframeAnimationPlayer animationPlayer =
                new KeyframeAnimationPlayer(
                        keyframeAnimation
                );

        /*
         * Compress the Better Combat animation into our
         * shorter Deflect interval.
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
         * Same JSON:
         *
         * mirrored false = original side
         * mirrored true  = opposite side
         */
        swipeLayer.addModifierBefore(
                new MirrorModifier(
                        mirrored
                )
        );

        /*
         * Temporarily replace Iron's normal casting pose
         * with this defensive swipe.
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