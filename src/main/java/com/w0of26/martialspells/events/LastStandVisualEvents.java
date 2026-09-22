package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.spells.LastStandSpell;
import com.w0of26.martialspells.visual.LastStandVisuals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Frozen Last Stand persistent aura cadence: one pulse every 20 ticks. */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class LastStandVisualEvents {
    private static final int AURA_FREQUENCY_TICKS = 20;

    private LastStandVisualEvents() {}

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel serverLevel)
                || entity.tickCount % AURA_FREQUENCY_TICKS != 0
                || !entity.hasEffect(MartialEffectRegistry.LAST_STAND.get())) {
            return;
        }

        LastStandVisuals.spawnAura(serverLevel, entity);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LastStandSpell.clearState(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LastStandSpell.clearState(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LastStandSpell.clearState(player);
        }
    }
}
