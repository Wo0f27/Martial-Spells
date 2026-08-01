package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.damage.GuardiansCovenantRedirectManager;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.spells.GuardiansCovenantSpell;
import com.w0of26.martialspells.util.GuardiansCovenantLinkData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GuardiansCovenantDamageEvents {
    private GuardiansCovenantDamageEvents() {
    }

    /**
     * LOWEST priority lets most other LivingHurtEvent modifiers
     * adjust the raw incoming amount before Covenant splits it.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(
            LivingHurtEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer ally)) {
            return;
        }

        if (!ally.hasEffect(
                MartialEffectRegistry
                        .GUARDIANS_COVENANT_LINKED
                        .get()
        )) {
            return;
        }

        if (!GuardiansCovenantLinkData.hasLink(ally)) {
            return;
        }

        float originalDamage = event.getAmount();

        if (!Float.isFinite(originalDamage)
                || originalDamage <= 0.0F) {
            return;
        }

        DamageSource source = event.getSource();

        /*
         * Never redirect Covenant damage again. This prevents loops,
         * chains between multiple tanks, and recursive event calls.
         */
        if (MartialDamageTypes
                .isGuardiansCovenantRedirect(source)) {
            return;
        }

        /*
         * Excludes /kill-like sources and damage explicitly intended
         * to ignore ordinary invulnerability protections.
         */
        if (source.is(
                DamageTypeTags.BYPASSES_INVULNERABILITY
        )) {
            return;
        }

        MinecraftServer server = ally.getServer();

        if (server == null) {
            return;
        }

        UUID tankUuid =
                GuardiansCovenantLinkData
                        .getCasterUuid(ally);

        ServerPlayer tank =
                server.getPlayerList()
                        .getPlayer(tankUuid);

        /*
         * A missing, dead, dimension-separated, or inactive tank
         * permanently invalidates this link.
         */
        if (!isValidTank(ally, tank)) {
            invalidateLink(ally);
            return;
        }

        double radius =
                GuardiansCovenantLinkData
                        .getRadius(ally);

        if (!Double.isFinite(radius)
                || radius <= 0.0D) {
            invalidateLink(ally);
            return;
        }

        /*
         * Leaving the radius pauses protection but does not erase
         * the snapshot link. Re-entering resumes it.
         */
        if (ally.distanceToSqr(tank)
                > radius * radius) {
            return;
        }

        if (!isEligibleAttributedDamage(
                ally,
                tank,
                source
        )) {
            return;
        }

        int spellLevel =
                GuardiansCovenantLinkData
                        .getSpellLevel(ally);

        float redirectMultiplier =
                GuardiansCovenantSpell
                        .getRedirectMultiplierForLevel(
                                spellLevel
                        );

        float requestedRedirect =
                originalDamage * redirectMultiplier;

        if (!Float.isFinite(requestedRedirect)
                || requestedRedirect <= 0.0F) {
            return;
        }

        /*
         * queueRedirect may accept less than requested if the shared
         * per-tank token bucket is nearly exhausted.
         */
        float acceptedRedirect =
                GuardiansCovenantRedirectManager
                        .queueRedirect(
                                tank,
                                requestedRedirect
                        );

        if (acceptedRedirect <= 0.0F) {
            return;
        }

        /*
         * Only remove damage that was successfully reserved for the
         * tank. The ally retains any portion rejected by the budget.
         */
        event.setAmount(
                Math.max(
                        0.0F,
                        originalDamage
                                - acceptedRedirect
                )
        );
    }

    private static boolean isValidTank(
            ServerPlayer ally,
            ServerPlayer tank
    ) {
        if (tank == null) {
            return false;
        }

        if (!tank.isAlive()
                || tank.isSpectator()) {
            return false;
        }

        if (tank.level() != ally.level()) {
            return false;
        }

        return tank.hasEffect(
                MartialEffectRegistry
                        .GUARDIANS_COVENANT_TANK
                        .get()
        );
    }

    /**
     * Initial damage-source policy:
     *
     * Included:
     * - hostile melee attacks;
     * - attributed projectiles;
     * - attributed explosions;
     * - attributed hostile spells;
     * - boss attacks with a causing entity;
     * - hostile PvP damage.
     *
     * Excluded:
     * - fall damage;
     * - drowning;
     * - starvation;
     * - suffocation;
     * - lava and environmental fire;
     * - void damage;
     * - unattributed damage-over-time;
     * - self-inflicted damage;
     * - damage caused by the Covenant tank;
     * - damage from allied entities.
     */
    private static boolean isEligibleAttributedDamage(
            ServerPlayer ally,
            ServerPlayer tank,
            DamageSource source
    ) {
        Entity attacker = source.getEntity();

        /*
         * Environmental damage normally has no causing entity.
         * Requiring one is deliberately cross-mod friendly: modded
         * melee, projectile, explosion, spell, and boss sources can
         * qualify without hardcoding every damage-type ID.
         */
        if (attacker == null) {
            return false;
        }

        if (attacker == ally
                || attacker == tank) {
            return false;
        }

        /*
         * Excludes scoreboard teammates, owned allied entities, and
         * other entities Minecraft considers allied to the victim.
         */
        return !ally.isAlliedTo(attacker);
    }

    private static void invalidateLink(
            ServerPlayer ally
    ) {
        GuardiansCovenantLinkData.clearLink(ally);

        ally.removeEffect(
                MartialEffectRegistry
                        .GUARDIANS_COVENANT_LINKED
                        .get()
        );
    }
}