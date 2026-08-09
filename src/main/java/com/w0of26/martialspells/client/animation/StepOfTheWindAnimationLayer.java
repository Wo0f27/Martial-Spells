package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
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
public final class StepOfTheWindAnimationLayer {

    public static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "step_of_the_wind_wall_run_layer"
            );

    /*
     * Higher than ordinary low-priority locomotion layers,
     * but this animation only controls the legs anyway.
     */
    private static final int LAYER_PRIORITY =
            250;

    private StepOfTheWindAnimationLayer() {
    }

    @SubscribeEvent
    public static void onClientSetup(
            FMLClientSetupEvent event
    ) {
        PlayerAnimationFactory
                .ANIMATION_DATA_FACTORY
                .registerFactory(
                        LAYER_ID,
                        LAYER_PRIORITY,
                        StepOfTheWindAnimationLayer
                                ::createLayer
                );
    }

    private static IAnimation createLayer(
            AbstractClientPlayer player
    ) {
        return new ModifierLayer<>();
    }
}