package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.spells.StillwaterMeditationSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles external Meditation rules not directly managed by Iron's
 * casting framework.
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
     * Damage still applies, but it does not interrupt the cast.
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

    /**
     * Attempting a melee attack manually cancels Meditation.
     *
     * The triggering attack is canceled, so the player must attack
     * again after leaving Meditation.
     */
    @SubscribeEvent
    public static void onAttackEntity(
            AttackEntityEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        if (!StillwaterMeditationSpell
                .isMeditating(player)) {
            return;
        }

        event.setCanceled(true);
        Utils.serverSideCancelCast(player);
    }

    /**
     * Prevents block breaking during Meditation and treats the
     * attempt as a voluntary cancellation.
     */
    @SubscribeEvent
    public static void onLeftClickBlock(
            PlayerInteractEvent.LeftClickBlock event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        if (!StillwaterMeditationSpell
                .isMeditating(player)) {
            return;
        }

        event.setCanceled(true);
        Utils.serverSideCancelCast(player);
    }
}