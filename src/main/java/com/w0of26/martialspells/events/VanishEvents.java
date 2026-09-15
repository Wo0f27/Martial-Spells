package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.spells.VanishSpell;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Frozen Rogues Vanish break/removal behavior translated to Forge 1.20.1.
 */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID)
public final class VanishEvents {
    private static final int LEAVE_PARTICLES = 20;

    private VanishEvents() {}

    /**
     * Source ENTITY_ANY_ATTACK is a LivingEntity direct-attack hook. Restricting
     * this to player_attack preserves that behavior and intentionally does not
     * break Stealth when a previously-fired projectile lands.
     *
     * Source RemoveOnHit.ANY_HIT also breaks Stealth on the victim of a
     * non-cancelled incoming attack.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof Player attacker) {
            breakStealth(attacker);
        }

        breakStealth(event.getEntity());
    }

    /** Timed item use such as food, bows, crossbows, shields and similar use actions. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        breakStealth(event.getEntity());
    }

    /**
     * Covers instant right-click item use which never enters the timed-use
     * lifecycle. Empty-hand right clicks do not break Stealth.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide && !event.getItemStack().isEmpty()) {
            breakStealth(event.getEntity());
        }
    }

    /** Casting any Iron's spell other than Vanish itself breaks Stealth. */
    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        Player caster = event.getEntity();
        if (caster.level().isClientSide) {
            return;
        }

        if (!VanishSpell.SPELL_ID.toString().equals(event.getSpellId())) {
            breakStealth(caster);
        }
    }

    /** Manual/forced removal path. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onStealthRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == MartialEffectRegistry.STEALTH.get()
                && event.getEffectInstance() != null) {
            playStealthLeave(event.getEntity());
        }
    }

    /** Natural eight-second expiry path. */
    @SubscribeEvent
    public static void onStealthExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect() == MartialEffectRegistry.STEALTH.get()) {
            playStealthLeave(event.getEntity());
        }
    }

    private static void breakStealth(LivingEntity entity) {
        if (!entity.level().isClientSide && entity.hasEffect(MartialEffectRegistry.STEALTH.get())) {
            entity.removeEffect(MartialEffectRegistry.STEALTH.get());
        }
    }

    private static void playStealthLeave(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                MartialSoundRegistry.STEALTH_LEAVE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        // Frozen source uses a 20-count Spell Engine smoke_medium feet circle.
        // Keep the shape/count but translate the dependency-owned sprite to
        // vanilla smoke.
        for (int i = 0; i < LEAVE_PARTICLES; i++) {
            double angle = Math.PI * 2.0D * i / LEAVE_PARTICLES;
            double radius = 0.70D;
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    entity.getX() + Math.cos(angle) * radius,
                    entity.getY() + 0.08D,
                    entity.getZ() + Math.sin(angle) * radius,
                    1,
                    0.0D,
                    0.03D,
                    0.0D,
                    0.18D
            );
        }
    }
}
