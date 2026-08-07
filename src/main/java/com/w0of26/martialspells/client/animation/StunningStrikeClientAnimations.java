package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.StunningStrikeWeaponStyle;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

import java.util.UUID;

public final class StunningStrikeClientAnimations {

    /*
     * Temporary Better Combat animation resources.
     *
     * These are referenced directly rather than copied into
     * Martial Spells. They can later be replaced with original
     * Martial Spells animations without changing the spell logic.
     */
    private static final ResourceLocation
            PUNCH_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "stunning_strike_punch"
            );

    private static final ResourceLocation
            QUARTERSTAFF_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "stunning_strike_quarterstaff"
            );


    private StunningStrikeClientAnimations() {
    }

    public static void play(
            UUID playerId,
            StunningStrikeWeaponStyle weaponStyle
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        var entity =
                minecraft.level
                        .getPlayerByUUID(playerId);

        if (!(entity
                instanceof AbstractClientPlayer player)) {
            return;
        }

        ResourceLocation animationResource =
                switch (weaponStyle) {
                    case PUNCH ->
                            PUNCH_ANIMATION;

                    case QUARTERSTAFF ->
                            QUARTERSTAFF_ANIMATION;
                };

        var keyframeAnimation =
                PlayerAnimationRegistry.getAnimation(
                        animationResource
                );

        if (keyframeAnimation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Stunning Strike "
                            + "animation {}",
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

        ModifierLayer<IAnimation> strikeLayer =
                new ModifierLayer<>(
                        animationPlayer
                );

        /*
         * Better Combat's punch is authored for a
         * right-main-arm player.
         */
        if (weaponStyle
                == StunningStrikeWeaponStyle.PUNCH
                && player.getMainArm()
                == HumanoidArm.LEFT) {

            strikeLayer.addModifierBefore(
                    new MirrorModifier(true)
            );
        }

        castingLayer.replaceAnimationWithFade(
                AbstractFadeModifier
                        .standardFadeIn(
                                1,
                                Ease.INOUTSINE
                        ),
                strikeLayer,
                true
        );
    }
}