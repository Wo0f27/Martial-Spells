package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.BarrageSpell;
import com.w0of26.martialspells.spells.EntanglingArrowSpell;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RangedTechniqueCooldownEvents {
    private RangedTechniqueCooldownEvents() {}

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (event.getSpell() == MartialSpellRegistry.BARRAGE.get()) {
            // Barrage is intentionally immune to Cooldown Reduction.
            event.setEffectiveCooldown(BarrageSpell.COOLDOWN_SECONDS * 20);
            return;
        }

        if (event.getSpell() == MartialSpellRegistry.ENTANGLING_ARROW.get()) {
            // Iron's has already applied Cooldown Reduction before this event.
            // Preserve that reduction, but never allow Entangling Arrow below 7 seconds.
            int minimumTicks = EntanglingArrowSpell.MIN_COOLDOWN_SECONDS * 20;
            event.setEffectiveCooldown(Math.max(event.getEffectiveCooldown(), minimumTicks));
        }
    }
}
