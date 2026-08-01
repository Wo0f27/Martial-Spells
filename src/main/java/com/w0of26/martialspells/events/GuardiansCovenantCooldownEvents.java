package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.GuardiansCovenantSpell;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GuardiansCovenantCooldownEvents {
    private GuardiansCovenantCooldownEvents() {
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(
            SpellCooldownAddedEvent.Pre event
    ) {
        if (event.getSpell()
                != MartialSpellRegistry
                .GUARDIANS_COVENANT
                .get()) {
            return;
        }

        Player player = event.getEntity();

        int spellLevel =
                player.getPersistentData().getInt(
                        GuardiansCovenantSpell
                                .CAST_LEVEL_TAG
                );

        player.getPersistentData().remove(
                GuardiansCovenantSpell.CAST_LEVEL_TAG
        );

        event.setEffectiveCooldown(
                GuardiansCovenantSpell
                        .getCooldownTicksForLevel(
                                spellLevel
                        )
        );
    }
}