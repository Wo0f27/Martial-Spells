package com.w0of26.martialspells.client;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.render.PaladinBarrierRenderer;
import com.w0of26.martialspells.client.render.HolyMoteRenderer;
import com.w0of26.martialspells.client.render.LightwellRenderer;
import com.w0of26.martialspells.client.render.BattleBannerRenderer;
import com.w0of26.martialspells.client.model.LightwellModel;
import com.w0of26.martialspells.client.model.BattleBannerModel;
import com.w0of26.martialspells.client.model.BearTrapModel;
import com.w0of26.martialspells.client.particle.BarrageTrailParticle;
import com.w0of26.martialspells.client.particle.ChargeSparkParticle;
import com.w0of26.martialspells.client.particle.ChargeStripeParticle;
import com.w0of26.martialspells.client.particle.DemoralizeSmokeParticle;
import com.w0of26.martialspells.client.particle.ShockPowderArcParticle;
import com.w0of26.martialspells.client.particle.ShockPowderSmokeParticle;
import com.w0of26.martialspells.client.particle.ShatterBloodParticle;
import com.w0of26.martialspells.client.particle.LastStandSparkParticle;
import com.w0of26.martialspells.client.particle.LastStandSmokeParticle;
import com.w0of26.martialspells.client.particle.LastStandAuraParticle;
import com.w0of26.martialspells.client.render.BarrageArrowRenderer;
import com.w0of26.martialspells.client.render.BearTrapRenderer;
import com.w0of26.martialspells.client.render.CaltropFieldRenderer;
import com.w0of26.martialspells.client.render.DiamondBodyShieldLayer;
import com.w0of26.martialspells.client.render.EntanglingArrowRenderer;
import com.w0of26.martialspells.client.render.NettedEffectRenderer;
import com.w0of26.martialspells.client.render.ThrowNetRenderer;
import com.w0of26.martialspells.client.render.ShatteringThrowRenderer;
import com.w0of26.martialspells.client.render.PenanceProjectileRenderer;
import com.w0of26.martialspells.client.render.JudgementVisualRenderer;
import com.w0of26.martialspells.client.render.HolyBeamVisualRenderer;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MartialClientEvents {
    private MartialClientEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(
                NettedEffectRenderer::onRenderLivingPost
        );
        MartialSpells.LOGGER.info(
                "Registered W4 Netted Forge render hook"
        );
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MartialEntityRegistry.BARRAGE_ARROW.get(), BarrageArrowRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.ENTANGLING_ARROW.get(), EntanglingArrowRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.CALTROP_BUNDLE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.CALTROP_FIELD.get(), CaltropFieldRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.BEAR_TRAP.get(), BearTrapRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.THROW_NET.get(), ThrowNetRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.SHATTERING_THROW.get(), ShatteringThrowRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.PENANCE_PROJECTILE.get(), PenanceProjectileRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.JUDGEMENT_VISUAL.get(), JudgementVisualRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.HOLY_BEAM_VISUAL.get(), HolyBeamVisualRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.PALADIN_BARRIER.get(), PaladinBarrierRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.BATTLE_BANNER.get(), BattleBannerRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.LIGHTWELL.get(), LightwellRenderer::new);
        event.registerEntityRenderer(MartialEntityRegistry.HOLY_MOTE.get(), HolyMoteRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ThrowNetRenderer.MODEL);
        event.register(NettedEffectRenderer.MODEL);
        event.register(PenanceProjectileRenderer.MODEL);
        event.register(JudgementVisualRenderer.MODEL);
        event.register(HolyMoteRenderer.MODEL);
    }

    @SubscribeEvent
    public static void verifyAdditionalModels(ModelEvent.BakingCompleted event) {
        var manager = event.getModelManager();
        if (manager.getModel(ThrowNetRenderer.MODEL) == manager.getMissingModel()) {
            MartialSpells.LOGGER.error(
                    "Throw Net projectile model failed to bake: {}",
                    ThrowNetRenderer.MODEL
            );
        }
        if (manager.getModel(NettedEffectRenderer.MODEL) == manager.getMissingModel()) {
            MartialSpells.LOGGER.error(
                    "Netted status-effect model failed to bake: {}",
                    NettedEffectRenderer.MODEL
            );
        }
        if (manager.getModel(PenanceProjectileRenderer.MODEL) == manager.getMissingModel()) {
            MartialSpells.LOGGER.error(
                    "Penance projectile model failed to bake: {}",
                    PenanceProjectileRenderer.MODEL
            );
        }
        if (manager.getModel(JudgementVisualRenderer.MODEL) == manager.getMissingModel()) {
            MartialSpells.LOGGER.error(
                    "Judgement projectile model failed to bake: {}",
                    JudgementVisualRenderer.MODEL
            );
        }
        if (manager.getModel(HolyMoteRenderer.MODEL) == manager.getMissingModel()) {
            MartialSpells.LOGGER.error(
                    "Holy Mote projectile model failed to bake: {}",
                    HolyMoteRenderer.MODEL
            );
        }
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BearTrapModel.LAYER, BearTrapModel::createBodyLayer);
        event.registerLayerDefinition(BattleBannerModel.LAYER, BattleBannerModel::createBodyLayer);
        event.registerLayerDefinition(LightwellModel.LAYER, LightwellModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MartialParticleRegistry.BARRAGE_TRAIL.get(), BarrageTrailParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.SHOCK_POWDER_SMOKE.get(), ShockPowderSmokeParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.SHOCK_POWDER_ARC.get(), ShockPowderArcParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.CHARGE_STRIPE.get(), ChargeStripeParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.CHARGE_SPARK.get(), ChargeSparkParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.DEMORALIZE_SMOKE.get(), DemoralizeSmokeParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.SHATTER_BLOOD.get(), ShatterBloodParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.LAST_STAND_SPARK.get(), LastStandSparkParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.LAST_STAND_SMOKE.get(), LastStandSmokeParticle.Provider::new);
        event.registerSpriteSet(MartialParticleRegistry.LAST_STAND_AURA.get(), LastStandAuraParticle.Provider::new);
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
