package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.spells.StillwaterMeditationSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles external Meditation rules not directly managed by
 * Iron's spell-casting framework.
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
     * Damage still applies, but knockback does not move the player.
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
     * Attempting a melee attack cancels Meditation.
     *
     * The triggering attack is consumed.
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
     * Attempting to break a block cancels Meditation.
     *
     * The triggering block-breaking action is consumed.
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

    /**
     * Applies Meditation's post-mitigation damage reduction.
     *
     * Levels I-III currently have 0% damage reduction.
     * Levels IV-V receive their configured protection.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(
            LivingDamageEvent event
    ) {
        int meditationLevel =
                StillwaterMeditationSpell
                        .getActiveMeditationLevel(
                                event.getEntity()
                        );

        if (meditationLevel <= 0) {
            return;
        }

        float damageReduction =
                StillwaterMeditationSpell
                        .getDamageReduction(
                                meditationLevel
                        );

        if (damageReduction <= 0.0F) {
            return;
        }

        float reducedDamage =
                event.getAmount()
                        * (1.0F - damageReduction);

        event.setAmount(
                Math.max(0.0F, reducedDamage)
        );
    }
}