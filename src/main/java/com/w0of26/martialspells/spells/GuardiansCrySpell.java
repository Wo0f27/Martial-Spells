package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.effects.GuardiansCryEffect;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class GuardiansCrySpell extends AbstractSpell {
    private static final int MAX_LEVEL = 5;
    private static final float RANGE = 12.0F;

    private static final float[] DURATION_SECONDS = {
            6.0F,
            8.0F,
            10.0F,
            12.0F,
            15.0F
    };

    private static final int[] COOLDOWN_SECONDS = {
            40,
            35,
            30,
            25,
            20
    };

    /**
     * Temporarily stores the level of the most recent Guardian's Cry cast.
     * GuardiansCryCooldownEvents reads and removes this after the cast.
     */
    public static final String CAST_LEVEL_TAG =
            MartialSpells.MOD_ID + "_guardians_cry_cast_level";

    private final ResourceLocation spellId =
            new ResourceLocation(
                    MartialSpells.MOD_ID,
                    "guardians_cry"
            );

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.RARE)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(MAX_LEVEL)

                    /*
                     * Iron's requires one configured cooldown value.
                     * The cooldown event replaces this with the exact
                     * level-specific cooldown after every cast.
                     */
                    .setCooldownSeconds(20)
                    .build();

    public GuardiansCrySpell() {
        this.baseManaCost = 40;
        this.manaCostPerLevel = 10;

        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;

        this.castTime = 0;
    }

    private static int clampLevel(int spellLevel) {
        return Math.max(1, Math.min(spellLevel, MAX_LEVEL));
    }

    private static int getLevelIndex(int spellLevel) {
        return clampLevel(spellLevel) - 1;
    }

    public static float getDurationSecondsForLevel(int spellLevel) {
        return DURATION_SECONDS[getLevelIndex(spellLevel)];
    }

    public static int getDurationTicksForLevel(int spellLevel) {
        return Math.round(
                getDurationSecondsForLevel(spellLevel) * 20.0F
        );
    }

    public static int getCooldownSecondsForLevel(int spellLevel) {
        return COOLDOWN_SECONDS[getLevelIndex(spellLevel)];
    }

    public static int getCooldownTicksForLevel(int spellLevel) {
        return getCooldownSecondsForLevel(spellLevel) * 20;
    }

    @Override
    public int getManaCost(int spellLevel) {
        /*
         * Prevent an external spell-level bonus from increasing mana
         * beyond Level V while the effect itself is capped at Level V.
         */
        return super.getManaCost(clampLevel(spellLevel));
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(RANGE, 1)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.effect_length",
                        Utils.stringTruncation(
                                getDurationSecondsForLevel(spellLevel),
                                1
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.fixed_cooldown",
                        getCooldownSecondsForLevel(spellLevel)
                ),
                Component.translatable(
                        "ui.martial_spells.cooldown_reduction_immune"
                )
        );
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int clampedSpellLevel = clampLevel(spellLevel);
        int durationTicks =
                getDurationTicksForLevel(clampedSpellLevel);

        /*
         * Iron's adds the cooldown after onCast finishes.
         * Store the cast level so the cooldown event can retrieve it.
         */
        if (caster instanceof Player player) {
            player.getPersistentData().putInt(
                    CAST_LEVEL_TAG,
                    clampedSpellLevel
            );
        }

        AABB searchArea =
                caster.getBoundingBox().inflate(RANGE);

        List<Mob> nearbyHostiles =
                level.getEntitiesOfClass(
                        Mob.class,
                        searchArea,
                        mob -> mob instanceof Enemy
                                && mob.isAlive()
                                && !mob.isDeadOrDying()
                                && !mob.isAlliedTo(caster)
                                && mob.canAttack(caster)
                                && mob.distanceToSqr(caster)
                                <= RANGE * RANGE
                );

        int tauntedCount = 0;

        for (Mob mob : nearbyHostiles) {
            boolean alreadyTaunted =
                    mob.hasEffect(
                            MartialEffectRegistry.GUARDIANS_CRY.get()
                    );

            /*
             * Save the original target only when beginning a fresh
             * taunt. Refreshing or transferring an existing taunt
             * must not overwrite the original restoration target.
             */
            if (!alreadyTaunted) {
                LivingEntity previousTarget = mob.getTarget();

                if (previousTarget != null
                        && previousTarget.isAlive()) {
                    mob.getPersistentData().putUUID(
                            GuardiansCryEffect
                                    .PREVIOUS_TARGET_UUID_TAG,
                            previousTarget.getUUID()
                    );
                } else {
                    mob.getPersistentData().remove(
                            GuardiansCryEffect
                                    .PREVIOUS_TARGET_UUID_TAG
                    );
                }
            }

            MobEffectInstance effectInstance =
                    new MobEffectInstance(
                            MartialEffectRegistry
                                    .GUARDIANS_CRY
                                    .get(),
                            durationTicks,
                            0,
                            false,
                            false
                    );

            boolean effectApplied =
                    mob.addEffect(effectInstance);

            /*
             * A fresh application failed, usually because the entity
             * rejects the effect.
             */
            if (!effectApplied && !alreadyTaunted) {
                mob.getPersistentData().remove(
                        GuardiansCryEffect.TARGET_UUID_TAG
                );
                mob.getPersistentData().remove(
                        GuardiansCryEffect
                                .PREVIOUS_TARGET_UUID_TAG
                );

                continue;
            }

            /*
             * Store or replace the caster currently forcing this
             * mob's attention.
             */
            mob.getPersistentData().putUUID(
                    GuardiansCryEffect.TARGET_UUID_TAG,
                    caster.getUUID()
            );

            mob.setTarget(caster);
            mob.setAggressive(true);

            serverLevel.sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    mob.getX(),
                    mob.getY()
                            + mob.getBbHeight() * 0.75D,
                    mob.getZ(),
                    4,
                    0.3D,
                    0.35D,
                    0.3D,
                    0.02D
            );

            tauntedCount++;
        }

        if (tauntedCount > 0
                && caster instanceof Player player) {
            player.addEffect(
                    new MobEffectInstance(
                            MartialEffectRegistry
                                    .GUARDIANS_CRY_ACTIVE
                                    .get(),
                            durationTicks,
                            0,
                            false,
                            false,
                            true
                    )
            );
        }
    }
}