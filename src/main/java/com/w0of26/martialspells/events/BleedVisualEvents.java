package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.visual.ShatterBloodVfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reproduces Spell Engine 1.10.5.034 Bleed's client particle spawner without
 * importing Spell Engine. The source emits amplifier+1 dripping-blood
 * particles every five entity ticks at 0.1-0.3 speed.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BleedVisualEvents {
    private static final int FREQUENCY_TICKS = 5;

    private BleedVisualEvents() {}

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel serverLevel)
                || entity.tickCount % FREQUENCY_TICKS != 0) {
            return;
        }

        MobEffectInstance bleed =
                entity.getEffect(MartialEffectRegistry.BLEED.get());
        if (bleed == null) {
            return;
        }

        ShatterBloodVfx.spawn(
                serverLevel,
                entity,
                bleed.getAmplifier() + 1,
                0.10D,
                0.30D
        );
    }
}
