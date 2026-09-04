package com.w0of26.martialspells.client.prone;

import com.w0of26.martialspells.MartialSpells;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import me.Thelnfamous1.mobplayeranimator.api.MobAnimationFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ProneAnimationClientBootstrap {

    public static final ResourceLocation PRONE_LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "prone_biped"
            );

    /*
     * Prone should override ordinary combat/idle animations.
     *
     * AnimationStack evaluates higher-priority layers later,
     * allowing them to override lower-priority transforms.
     */
    private static final int PRONE_PRIORITY =
            10_000;

    private ProneAnimationClientBootstrap() {
    }

    @SubscribeEvent
    public static void onClientSetup(
            FMLClientSetupEvent event
    ) {
        event.enqueueWork(
                ProneAnimationClientBootstrap
                        ::registerAnimationLayers
        );
    }

    private static void registerAnimationLayers() {

        /*
         * Every client player receives an empty Prone layer.
         *
         * The layer remains inactive until ProneBipedAnimator
         * installs an animation into it.
         */
        PlayerAnimationFactory
                .ANIMATION_DATA_FACTORY
                .registerFactory(
                        PRONE_LAYER_ID,
                        PRONE_PRIORITY,
                        player ->
                                new ModifierLayer<IAnimation>()
                );

        /*
         * Only mobs classified as BIPED receive the
         * equivalent Mob Player Animator layer.
         */
        MobAnimationFactory
                .ANIMATION_DATA_FACTORY
                .registerFactory(
                        PRONE_LAYER_ID,
                        PRONE_PRIORITY,
                        mob -> {
                            if (!ProneClientAnimationRegistry
                                    .usesBipedAnimator(mob)) {
                                return null;
                            }

                            return new ModifierLayer<IAnimation>();
                        }
                );
    }
}