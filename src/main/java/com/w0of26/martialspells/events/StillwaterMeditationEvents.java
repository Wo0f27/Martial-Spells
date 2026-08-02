package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.spells.StillwaterMeditationSpell;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles rules that are external to Iron's spell casting framework,
 * such as preventing physical knockback during advanced Meditation.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StillwaterMeditationEvents {
    private StillwaterMeditationEvents() {
    }

    /**
     * Level IV and V Meditation completely prevent knockback.
     *
     * The damage itself is still received. Only displacement and
     * damage-based cast interruption are prevented.
     */
    @SubscribeEvent
    public static void onLivingKnockback(
            LivingKnockBackEvent event
    ) {
        if (!StillwaterMeditationSpell
                .isMeditatingAtOrAbove(
                        event.getEntity(),
                        StillwaterMeditationSpell
                                .UNINTERRUPTIBLE_LEVEL
                )) {
            return;
        }

        event.setCanceled(true);
    }
}