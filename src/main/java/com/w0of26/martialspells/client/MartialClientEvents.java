package com.w0of26.martialspells.client;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.particle.BarrageTrailParticle;
import com.w0of26.martialspells.client.render.BarrageArrowRenderer;
import com.w0of26.martialspells.client.render.DiamondBodyShieldLayer;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MartialClientEvents {
    private MartialClientEvents() {}

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MartialEntityRegistry.BARRAGE_ARROW.get(), BarrageArrowRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MartialParticleRegistry.BARRAGE_TRAIL.get(), BarrageTrailParticle.Provider::new);
    }

    @SubscribeEvent
    public static void addPlayerRenderLayers(EntityRenderersEvent.AddLayers event) {
        for (String skinName : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skinName);
            if (renderer != null) {
                renderer.addLayer(new DiamondBodyShieldLayer(renderer, event.getEntityModels()));
            }
        }
    }
}
