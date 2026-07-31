package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.effects.GuardiansCryEffect;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GuardiansCryEvents {
    private GuardiansCryEvents() {
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effectInstance = event.getEffectInstance();

        if (effectInstance.getEffect()
                == MartialEffectRegistry.GUARDIANS_CRY.get()) {
            GuardiansCryEffect.clearTaunt(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect()
                == MartialEffectRegistry.GUARDIANS_CRY.get()) {
            GuardiansCryEffect.clearTaunt(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!GuardiansCryEffect.hasFallbackTaunt(mob)) {
            return;
        }

        GuardiansCryEffect.tickFallbackTaunt(mob);
    }
}