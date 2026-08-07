package com.w0of26.martialspells.client;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.render.DiamondBodyShieldLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class MartialClientEvents {

    private MartialClientEvents() {
    }

    @SubscribeEvent
    public static void addPlayerRenderLayers(
            EntityRenderersEvent.AddLayers event
    ) {
        for (String skinName : event.getSkins()) {
            PlayerRenderer renderer =
                    event.getSkin(skinName);

            if (renderer == null) {
                continue;
            }

            renderer.addLayer(
                    new DiamondBodyShieldLayer(
                            renderer,
                            event.getEntityModels()
                    )
            );
        }
    }
}