package com.w0of26.martialspells.quivering;

import com.w0of26.martialspells.combat.MartialPowerHelper;
import com.w0of26.martialspells.combat.MonkEncumbranceHelper;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.registry.MartialEntityTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative Quivering Palm detonation damage.
 *
 * Each generation scales the whole detonation package:
 * 100% -> 75% -> 55% -> 40%.
 *
 * Boss-like entities use a fixed Martial component plus capped
 * percentage-of-max-health damage, with a hard per-detonation ceiling.
 */
public final class QuiveringPalmCombatHelper {

    private static final float MINIMUM_EFFECTIVE_ATTACK_DAMAGE = 4.0F;

    private static final float[] PRIMARY_DAMAGE_COEFFICIENTS = {
            3.0F,
            3.5F,
            4.0F,
            4.5F,
            5.0F
    };

    private static final float[] AOE_DAMAGE_MULTIPLIERS = {
            0.50F,
            0.55F,
            0.60F,
            0.65F,
            0.70F
    };

    private static final float[] GENERATION_STRENGTH = {
            1.00F,
            0.75F,
            0.55F,
            0.40F
    };

    private static final float[] BOSS_MAX_HEALTH_PERCENT = {
            0.06F,
            0.07F,
            0.08F,
            0.09F,
            0.10F
    };

    private static final float[] BOSS_PERCENT_DAMAGE_CAP = {
            50.0F,
            75.0F,
            100.0F,
            125.0F,
            150.0F
    };

    /*
     * Even extreme Martial Power cannot let one primary detonation
     * remove more than 20% of a tagged boss's max health before armor.
     * Secondary AOE uses half of this cap.
     */
    private static final float BOSS_PRIMARY_TOTAL_MAX_HEALTH_CAP = 0.20F;
    private static final float BOSS_AOE_FACTOR = 0.50F;

    private QuiveringPalmCombatHelper() {
    }

    public record DetonationResult(
            boolean primaryDamageAccepted,
            List<LivingEntity> survivingSecondaryHits
    ) {
        public DetonationResult {
            survivingSecondaryHits =
                    List.copyOf(survivingSecondaryHits);
        }
    }

    public static DetonationResult detonate(
            ServerPlayer player,
            LivingEntity primaryTarget,
            int spellLevel,
            int generation,
            float radius
    ) {
        int levelIndex = getLevelIndex(spellLevel);
        float generationStrength =
                getGenerationStrength(generation);

        float rawPrimaryFixedDamage =
                MartialPowerHelper.calculateTechniqueDamage(
                        player,
                        MINIMUM_EFFECTIVE_ATTACK_DAMAGE,
                        PRIMARY_DAMAGE_COEFFICIENTS[levelIndex]
                );

        float primaryDamage =
                calculateTargetDamage(
                        player,
                        primaryTarget,
                        rawPrimaryFixedDamage,
                        levelIndex,
                        generationStrength,
                        false
                );

        boolean primaryDamageAccepted =
                primaryTarget.hurt(
                        MartialDamageTypes.quiveringPalm(player),
                        primaryDamage
                );

        ServerLevel level = player.serverLevel();
        double radiusSquared = radius * radius;

        List<LivingEntity> secondaryTargets =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        primaryTarget
                                .getBoundingBox()
                                .inflate(radius),
                        candidate ->
                                isValidSecondaryTarget(
                                        player,
                                        primaryTarget,
                                        candidate,
                                        radiusSquared
                                )
                );

        float rawAoeFixedDamage =
                rawPrimaryFixedDamage
                        * AOE_DAMAGE_MULTIPLIERS[levelIndex];

        List<LivingEntity> survivingSecondaryHits =
                new ArrayList<>();

        for (LivingEntity secondary : secondaryTargets) {
            float secondaryDamage =
                    calculateTargetDamage(
                            player,
                            secondary,
                            rawAoeFixedDamage,
                            levelIndex,
                            generationStrength,
                            true
                    );

            boolean damaged =
                    secondary.hurt(
                            MartialDamageTypes.quiveringPalm(player),
                            secondaryDamage
                    );

            if (damaged && secondary.isAlive()) {
                survivingSecondaryHits.add(secondary);
            }
        }

        return new DetonationResult(
                primaryDamageAccepted,
                survivingSecondaryHits
        );
    }

    private static boolean isValidSecondaryTarget(
            ServerPlayer player,
            LivingEntity primaryTarget,
            LivingEntity candidate,
            double radiusSquared
    ) {
        if (candidate == player
                || candidate == primaryTarget
                || !candidate.isAlive()
                || candidate.isSpectator()
                || player.isAlliedTo(candidate)) {
            return false;
        }

        return candidate.distanceToSqr(primaryTarget)
                <= radiusSquared;
    }

    private static float calculateTargetDamage(
            ServerPlayer player,
            LivingEntity target,
            float rawFixedDamage,
            int levelIndex,
            float generationStrength,
            boolean secondary
    ) {
        float fixedDamage =
                rawFixedDamage * generationStrength;

        if (!target.getType().is(
                MartialEntityTypeTags.QUIVERING_PALM_BOSS
        )) {
            return MonkEncumbranceHelper.applyDamagePenalty(
                    fixedDamage,
                    player
            );
        }

        float secondaryFactor =
                secondary ? BOSS_AOE_FACTOR : 1.0F;

        float percentageDamage =
                target.getMaxHealth()
                        * BOSS_MAX_HEALTH_PERCENT[levelIndex]
                        * generationStrength
                        * secondaryFactor;

        float percentageCap =
                BOSS_PERCENT_DAMAGE_CAP[levelIndex]
                        * generationStrength
                        * secondaryFactor;

        percentageDamage =
                Math.min(
                        percentageDamage,
                        percentageCap
                );

        float combinedDamage =
                fixedDamage + percentageDamage;

        combinedDamage =
                MonkEncumbranceHelper.applyDamagePenalty(
                        combinedDamage,
                        player
                );

        float totalHealthCap =
                target.getMaxHealth()
                        * BOSS_PRIMARY_TOTAL_MAX_HEALTH_CAP
                        * generationStrength
                        * secondaryFactor;

        return Math.min(
                combinedDamage,
                totalHealthCap
        );
    }

    public static float getPrimaryDamageCoefficient(
            int spellLevel
    ) {
        return PRIMARY_DAMAGE_COEFFICIENTS[
                getLevelIndex(spellLevel)
                ];
    }

    public static float getAoeDamageMultiplier(
            int spellLevel
    ) {
        return AOE_DAMAGE_MULTIPLIERS[
                getLevelIndex(spellLevel)
                ];
    }

    public static float getGenerationStrength(
            int generation
    ) {
        int index = Math.max(
                0,
                Math.min(
                        generation,
                        GENERATION_STRENGTH.length - 1
                )
        );

        return GENERATION_STRENGTH[index];
    }

    private static int getLevelIndex(
            int spellLevel
    ) {
        return Math.max(
                0,
                Math.min(
                        spellLevel - 1,
                        PRIMARY_DAMAGE_COEFFICIENTS.length - 1
                )
        );
    }
}
