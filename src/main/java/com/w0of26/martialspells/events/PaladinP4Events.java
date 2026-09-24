package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.PaladinBarrierEntity;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.PaladinLightwellSpell;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Cross-system integration hooks required by frozen Paladins P4 mechanics. */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PaladinP4Events {
    private static final int LIGHTWELL_FIXED_COOLDOWN_TICKS =
            PaladinLightwellSpell.BASE_COOLDOWN_SECONDS * 20;

    private static final TagKey<DamageType> COMMON_MAGIC =
            damageTag(
                    "c",
                    "is_magic"
            );

    private static final TagKey<DamageType> FORGE_MAGIC =
            damageTag(
                    "forge",
                    "is_magic"
            );

    /*
     * Iron's Forge 1.20.1 publishes each spell school as a damage-type tag.
     * The frozen Paladins barrier points at the common magic umbrella. Spell
     * Engine normally receives that umbrella from Spell Power; CP11 has no
     * Spell Power runtime, so explicitly include Iron's school tags here.
     */
    private static final List<TagKey<DamageType>> IRONS_MAGIC =
            List.of(
                    damageTag("irons_spellbooks", "fire_magic"),
                    damageTag("irons_spellbooks", "ice_magic"),
                    damageTag("irons_spellbooks", "lightning_magic"),
                    damageTag("irons_spellbooks", "holy_magic"),
                    damageTag("irons_spellbooks", "ender_magic"),
                    damageTag("irons_spellbooks", "blood_magic"),
                    damageTag("irons_spellbooks", "evocation_magic"),
                    damageTag("irons_spellbooks", "nature_magic"),
                    damageTag("irons_spellbooks", "eldritch_magic")
            );

    private PaladinP4Events() {
    }

    @SubscribeEvent
    public static void onProtectedAttack(
            LivingAttackEvent event
    ) {
        LivingEntity target =
                event.getEntity();

        if (target.level().isClientSide
                || !target.hasEffect(
                        MartialEffectRegistry
                                .BARRIER_PROTECTED
                                .get()
                )) {
            return;
        }

        if (barrierProtectsAgainst(
                event.getSource()
        )) {
            event.setCanceled(true);
        }
    }

    /**
     * Spell Engine's TwoWayCollisionChecker lets friendly projectiles pass
     * through Barrier while hostile projectiles collide. Forge's projectile
     * impact event is the dependency-free equivalent for vanilla-style
     * projectile raycasts.
     */
    @SubscribeEvent
    public static void onProjectileImpact(
            ProjectileImpactEvent event
    ) {
        if (!(event.getRayTraceResult()
                instanceof EntityHitResult hit)
                || !(hit.getEntity()
                instanceof PaladinBarrierEntity barrier)) {
            return;
        }

        Projectile projectile =
                event.getProjectile();

        if (projectile.level().isClientSide) {
            return;
        }

        Entity shooter =
                projectile.getOwner();

        if (shooter instanceof LivingEntity livingShooter
                && barrier.isProtected(
                        livingShooter
                )) {
            event.setCanceled(true);
        }
    }

    /**
     * Frozen Lightwell explicitly disables haste on its 45-second summon
     * cooldown. Iron's computes cooldown reduction before the Pre event, so
     * force the source-authored fixed duration back here.
     */
    @SubscribeEvent
    public static void onSpellCooldownAdded(
            SpellCooldownAddedEvent.Pre event
    ) {
        if (event.getSpell()
                == MartialSpellRegistry.LIGHTWELL.get()) {
            event.setEffectiveCooldown(
                    LIGHTWELL_FIXED_COOLDOWN_TICKS
            );
        }
    }

    private static boolean barrierProtectsAgainst(
            DamageSource source
    ) {
        if (source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(COMMON_MAGIC)
                || source.is(FORGE_MAGIC)) {
            return true;
        }

        for (TagKey<DamageType> magicTag : IRONS_MAGIC) {
            if (source.is(magicTag)) {
                return true;
            }
        }

        return false;
    }

    private static TagKey<DamageType> damageTag(
            String namespace,
            String path
    ) {
        return TagKey.create(
                Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(
                        namespace,
                        path
                )
        );
    }
}
