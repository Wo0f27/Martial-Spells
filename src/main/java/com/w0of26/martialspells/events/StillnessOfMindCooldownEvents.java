package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.StillnessOfMindSpell;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StillnessOfMindCooldownEvents {

    private StillnessOfMindCooldownEvents() {
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(
            SpellCooldownAddedEvent.Pre event
    ) {
        if (event.getSpell()
                != MartialSpellRegistry
                .STILLNESS_OF_MIND
                .get()) {
            return;
        }

        /*
         * Replace Iron's cooldown after all generic cooldown
         * modifiers have been considered.
         *
         * This guarantees the full three-minute cooldown.
         */
        event.setEffectiveCooldown(
                StillnessOfMindSpell
                        .COOLDOWN_TICKS
        );
    }
}