package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.SpiritWolfFixedCooldown;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Replaces Iron's post-reduction ticks only for Spirit Wolf. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritWolfCooldownEvents {
    private SpiritWolfCooldownEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCooldown(SpellCooldownAddedEvent.Pre event) {
        if (event.getSpell() == MartialSpellRegistry.SPIRIT_WOLF.get()) {
            event.setEffectiveCooldown(SpiritWolfFixedCooldown.forPlayer(event.getEntity()));
        }
    }
}
