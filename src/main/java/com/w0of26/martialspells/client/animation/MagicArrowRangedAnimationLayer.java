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

/**
 * High-priority arm-only layer used to replace Iron's bow-charge arm
 * pose with a loaded-crossbow hold during Magic Arrow.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class MagicArrowRangedAnimationLayer {
    public static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "magic_arrow_ranged_pose_layer"
            );

    private static final int LAYER_PRIORITY = 260;

    private MagicArrowRangedAnimationLayer() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                LAYER_ID,
                LAYER_PRIORITY,
                MagicArrowRangedAnimationLayer::createLayer
        );
    }

    private static IAnimation createLayer(
            AbstractClientPlayer player
    ) {
        return new ModifierLayer<>();
    }
}
