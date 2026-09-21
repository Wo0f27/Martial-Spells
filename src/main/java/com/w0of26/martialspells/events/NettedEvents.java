package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Frozen Rogues NET_TRAP also installs Spell Engine KnockbackImmunity.
 * Forge's cancellable knockback event reproduces that behavior directly.
 */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NettedEvents {
    private NettedEvents() {}

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity().hasEffect(MartialEffectRegistry.NET_TRAP.get())) {
            event.setCanceled(true);
        }
    }
}
