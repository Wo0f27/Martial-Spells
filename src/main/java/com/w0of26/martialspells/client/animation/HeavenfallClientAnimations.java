package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.HeavenfallAnimationStyle;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class HeavenfallClientAnimations {

    /*
     * Temporary Better Combat-derived development
     * placeholders.
     *
     * Replace before distributable release unless
     * permission is obtained.
     */
    private static final ResourceLocation
            LAUNCH_UNARMED =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "heavenfall_launch_unarmed"
            );

    private static final ResourceLocation
            LAUNCH_WEAPON =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "heavenfall_launch_weapon"
            );

    private HeavenfallClientAnimations() {
    }

    public static void handle(
            UUID playerId,
            HeavenfallAnimationPhase phase,
            HeavenfallAnimationStyle style
    ) {
        switch (phase) {
            case LAUNCH ->
                    play(
                            playerId,
                            style,
                            LAUNCH_UNARMED,
                            LAUNCH_WEAPON
                    );

            case DIVE ->
                    play(
                            playerId,
                            style,
                            DIVE_UNARMED,
                            DIVE_WEAPON
                    );

            /*
             * Reserved until the authoritative impact
             * checkpoint exists.
             */
            case IMPACT, STOP -> {
            }
        }
    }

    private static void play(
            UUID playerId,
            HeavenfallAnimationStyle style,
            ResourceLocation unarmedAnimation,
            ResourceLocation weaponAnimation
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        var entity =
                minecraft.level
                        .getPlayerByUUID(
                                playerId
                        );

        if (!(entity
                instanceof AbstractClientPlayer player)) {

            return;
        }

        ResourceLocation animationResource =
                switch (style) {
                    case UNARMED ->
                            unarmedAnimation;

                    case WEAPON ->
                            weaponAnimation;
                };

        var keyframeAnimation =
                PlayerAnimationRegistry
                        .getAnimation(
                                animationResource
                        );

        if (keyframeAnimation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Heavenfall animation {}",
                    animationResource
            );

            return;
        }

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
                            + "was unavailable for {}",
                    player.getGameProfile()
                            .getName()
            );

            return;
        }

        KeyframeAnimationPlayer animationPlayer =
                new KeyframeAnimationPlayer(
                        keyframeAnimation
                );

        ModifierLayer<IAnimation> heavenfallLayer =
                new ModifierLayer<>(
                        animationPlayer
                );

        castingLayer.replaceAnimationWithFade(
                AbstractFadeModifier
                        .standardFadeIn(
                                1,
                                Ease.INOUTSINE
                        ),
                heavenfallLayer,
                true
        );
    }

    private static final ResourceLocation
            DIVE_UNARMED =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "heavenfall_dive_unarmed"
            );

    private static final ResourceLocation
            DIVE_WEAPON =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "heavenfall_dive_weapon"
            );
}