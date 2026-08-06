package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.effects.DiamondBodyEffect;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID
)
public final class DiamondBodyDamageEvents {

    private DiamondBodyDamageEvents() {
    }

    @SubscribeEvent(
            priority = EventPriority.NORMAL
    )
    public static void reduceDamage(
            LivingDamageEvent event
    ) {
        if (event.getEntity()
                .level()
                .isClientSide) {
            return;
        }

        if (event.getAmount() <= 0.0F) {
            return;
        }

        var diamondBody =
                event.getEntity()
                        .getEffect(
                                MartialEffectRegistry
                                        .DIAMOND_BODY
                                        .get()
                        );

        if (diamondBody == null) {
            return;
        }

        float multiplier =
                DiamondBodyEffect
                        .getDamageMultiplier(
                                diamondBody
                                        .getAmplifier()
                        );

        event.setAmount(
                event.getAmount()
                        * multiplier
        );
    }
}