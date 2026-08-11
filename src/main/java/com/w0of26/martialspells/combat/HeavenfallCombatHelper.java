package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.registry.MartialEntityTypeTags;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HeavenfallCombatHelper {

    /*
     * Same minimum Attack Damage floor used by
     * Stunning Strike.
     */
    private static final float
            MINIMUM_EFFECTIVE_ATTACK_DAMAGE =
            4.0F;

    /*
     * Heavenfall shockwave radius:
     *
     * I   = 2.5 blocks
     * II  = 3.0 blocks
     * III = 3.5 blocks
     * IV  = 4.0 blocks
     * V   = 4.5 blocks
     */
    private static final double[]
            SHOCKWAVE_RADIUS = {
            2.5D,
            3.0D,
            3.5D,
            4.0D,
            4.5D
    };

    /*
     * Secondary targets take substantially less damage
     * than Heavenfall's selected primary victim.
     */
    private static final float[]
            SHOCKWAVE_DAMAGE_MULTIPLIERS = {
            0.70F,
            0.75F,
            0.80F,
            0.85F,
            0.90F
    };

    /*
     * Outward crowd-control force.
     */
    private static final double[]
            SHOCKWAVE_KNOCKBACK = {
            0.65D,
            0.75D,
            0.85D,
            0.95D,
            1.05D
    };

    /*
     * Primary-target damage scaling:
     *
     * I   = 1.30x
     * II  = 1.40x
     * III = 1.50x
     * IV  = 1.60x
     * V   = 1.70x
     */
    private static final float[]
            PRIMARY_DAMAGE_MULTIPLIERS = {
            1.30F,
            1.40F,
            1.50F,
            1.60F,
            1.70F
    };

    /*
     * Stun:
     *
     * I   = 1.00 sec
     * II  = 1.25 sec
     * III = 1.50 sec
     * IV  = 1.75 sec
     * V   = 2.00 sec
     */
    private static final int[]
            STUN_TICKS = {
            20,
            25,
            30,
            35,
            40
    };

    /*
     * Potion amplifiers are zero-based.
     *
     * 1 = Rend II
     * 2 = Rend III
     * 3 = Rend IV
     */
    private static final int[]
            REND_AMPLIFIERS = {
            1,
            1,
            2,
            2,
            3
    };

    /*
     * 0 = Blight I
     * 1 = Blight II
     */
    private static final int[]
            BLIGHT_AMPLIFIERS = {
            0,
            0,
            0,
            0,
            1
    };

    /*
     * Five seconds for both debuffs for now.
     */
    private static final int
            REND_DURATION_TICKS =
            100;

    private static final int
            BLIGHT_DURATION_TICKS =
            100;

    /*
     * Same TurtleCore stun effect used by
     * Stunning Strike.
     */
    private static final ResourceLocation
            STUNNED_EFFECT_ID =
            new ResourceLocation(
                    "turtlecore",
                    "stunned"
            );

    private HeavenfallCombatHelper() {
    }

    public static void applyPrimaryImpact(
            ServerPlayer player,
            LivingEntity target,
            int spellLevel
    ) {
        int index =
                getLevelIndex(
                        spellLevel
                );

        /*
         * Attack Damage
         * x Heavenfall coefficient
         * x Martial Spell Power.
         */
        float rawDamage =
                MartialPowerHelper
                        .calculateTechniqueDamage(
                                player,
                                MINIMUM_EFFECTIVE_ATTACK_DAMAGE,
                                PRIMARY_DAMAGE_MULTIPLIERS[
                                        index
                                        ]
                        );

        /*
         * Preserve the shared Monk heavy-armor
         * damage penalty.
         */
        float finalDamage =
                MonkEncumbranceHelper
                        .applyDamagePenalty(
                                rawDamage,
                                player
                        );

        /*
         * Normal armor-respecting Martial damage.
         */
        boolean damaged =
                target.hurt(
                        MartialDamageTypes
                                .heavenfallStrike(
                                        player
                                ),
                        finalDamage
                );

        /*
         * Do not apply secondary effects when the
         * damage itself was rejected.
         */
        if (!damaged) {
            return;
        }

        /*
         * The target may have died from the impact.
         */
        if (!target.isAlive()) {
            return;
        }

        applyRend(
                player,
                target,
                index
        );

        applyBlight(
                player,
                target,
                index
        );

        applyStun(
                player,
                target,
                index
        );
    }

    private static void applyRend(
            ServerPlayer player,
            LivingEntity target,
            int index
    ) {
        target.addEffect(
                new MobEffectInstance(
                        MobEffectRegistry.REND.get(),
                        REND_DURATION_TICKS,
                        REND_AMPLIFIERS[
                                index
                                ],
                        false,
                        true,
                        true
                ),
                player
        );
    }

    private static void applyBlight(
            ServerPlayer player,
            LivingEntity target,
            int index
    ) {
        target.addEffect(
                new MobEffectInstance(
                        MobEffectRegistry.BLIGHT.get(),
                        BLIGHT_DURATION_TICKS,
                        BLIGHT_AMPLIFIERS[
                                index
                                ],
                        false,
                        true,
                        true
                ),
                player
        );
    }

    private static void applyStun(
            ServerPlayer player,
            LivingEntity target,
            int index
    ) {
        /*
         * Martial Spells stun immunity only prevents
         * Stunned.
         *
         * Damage, Rend and Blight have already happened.
         */
        if (target.getType().is(
                MartialEntityTypeTags.STUN_IMMUNE
        )) {
            return;
        }

        MobEffect stunnedEffect =
                ForgeRegistries.MOB_EFFECTS
                        .getValue(
                                STUNNED_EFFECT_ID
                        );

        if (stunnedEffect == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find TurtleCore "
                            + "Stunned effect: {}",
                    STUNNED_EFFECT_ID
            );

            return;
        }

        int duration =
                STUN_TICKS[
                        index
                        ];

        /*
         * Same resistance rule as Stunning Strike:
         * half stun duration.
         */
        if (target.getType().is(
                MartialEntityTypeTags.STUN_RESISTANT
        )) {
            duration =
                    Math.round(
                            duration
                                    * 0.50F
                    );
        }

        /*
         * Normal addEffect is intentional so
         * entity/effect-specific stun immunities are
         * still respected.
         */
        target.addEffect(
                new MobEffectInstance(
                        stunnedEffect,
                        duration,
                        0,
                        false,
                        true,
                        true
                ),
                player
        );
    }

    public static void applyShockwave(
            ServerPlayer player,
            LivingEntity primaryTarget,
            int spellLevel
    ) {
        int index =
                getLevelIndex(
                        spellLevel
                );

        double radius =
                SHOCKWAVE_RADIUS[
                        index
                        ];

        /*
         * Center the shockwave on the actual selected victim
         * that Heavenfall physically struck.
         */
        Vec3 impactCenter =
                primaryTarget.position();

        /*
         * Give the shockwave a reasonable vertical tolerance,
         * but keep its main identity as a ground-level radial
         * impact rather than a spherical explosion.
         */
        AABB searchBox =
                new AABB(
                        impactCenter.x - radius,
                        impactCenter.y - 2.0D,
                        impactCenter.z - radius,
                        impactCenter.x + radius,
                        impactCenter.y + 2.5D,
                        impactCenter.z + radius
                );

        float rawDamage =
                MartialPowerHelper
                        .calculateTechniqueDamage(
                                player,
                                MINIMUM_EFFECTIVE_ATTACK_DAMAGE,
                                SHOCKWAVE_DAMAGE_MULTIPLIERS[
                                        index
                                        ]
                        );

        /*
         * Preserve the normal Monk heavy-armor penalty.
         */
        float finalDamage =
                MonkEncumbranceHelper
                        .applyDamagePenalty(
                                rawDamage,
                                player
                        );

        double radiusSqr =
                radius * radius;

        var secondaryTargets =
                player.serverLevel()
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                searchBox,
                                entity ->
                                        isValidShockwaveTarget(
                                                player,
                                                primaryTarget,
                                                entity,
                                                impactCenter,
                                                radiusSqr
                                        )
                        );

        for (LivingEntity secondary
                : secondaryTargets) {

            /*
             * Same normal armor-respecting Heavenfall
             * damage source.
             */
            boolean damaged =
                    secondary.hurt(
                            MartialDamageTypes
                                    .heavenfallStrike(
                                            player
                                    ),
                            finalDamage
                    );

            /*
             * A target that rejected the actual hit should
             * not receive the associated knockback.
             */
            if (!damaged) {
                continue;
            }

            applyShockwaveKnockback(
                    secondary,
                    impactCenter,
                    SHOCKWAVE_KNOCKBACK[
                            index
                            ]
            );
        }
    }

    private static boolean isValidShockwaveTarget(
            ServerPlayer player,
            LivingEntity primaryTarget,
            LivingEntity candidate,
            Vec3 impactCenter,
            double radiusSqr
    ) {
        /*
         * The selected victim already received the full
         * primary-target Heavenfall package.
         */
        if (candidate == primaryTarget) {
            return false;
        }

        if (candidate == player) {
            return false;
        }

        if (!candidate.isAlive()) {
            return false;
        }

        if (candidate.isSpectator()) {
            return false;
        }

        if (player.isAlliedTo(
                candidate
        )) {
            return false;
        }

        /*
         * Horizontal distance defines the shockwave radius.
         *
         * Vertical range is already constrained by searchBox.
         */
        double dx =
                candidate.getX()
                        - impactCenter.x;

        double dz =
                candidate.getZ()
                        - impactCenter.z;

        return dx * dx
                + dz * dz
                <= radiusSqr;
    }

    private static void applyShockwaveKnockback(
            LivingEntity target,
            Vec3 impactCenter,
            double strength
    ) {
        /*
         * LivingEntity#knockback subtracts the supplied
         * source-direction vector, so supplying
         *
         * impact center -> target
         *
         * in reverse causes the entity to move outward.
         */
        double sourceX =
                impactCenter.x
                        - target.getX();

        double sourceZ =
                impactCenter.z
                        - target.getZ();

        /*
         * Prevent undefined normalization when two entity
         * centers happen to be essentially identical.
         */
        if (sourceX * sourceX
                + sourceZ * sourceZ
                < 1.0E-6D) {

            return;
        }

        target.knockback(
                strength,
                sourceX,
                sourceZ
        );
    }

    public static float getPrimaryDamageMultiplier(
            int spellLevel
    ) {
        return PRIMARY_DAMAGE_MULTIPLIERS[
                getLevelIndex(
                        spellLevel
                )
                ];
    }

    public static float getShockwaveDamageMultiplier(
            int spellLevel
    ) {
        return SHOCKWAVE_DAMAGE_MULTIPLIERS[
                getLevelIndex(
                        spellLevel
                )
                ];
    }

    public static double getShockwaveRadius(
            int spellLevel
    ) {
        return SHOCKWAVE_RADIUS[
                getLevelIndex(
                        spellLevel
                )
                ];
    }

    public static float getStunDurationSeconds(
            int spellLevel
    ) {
        return STUN_TICKS[
                getLevelIndex(
                        spellLevel
                )
                ] / 20.0F;
    }

    public static int getRendLevel(
            int spellLevel
    ) {
        return REND_AMPLIFIERS[
                getLevelIndex(
                        spellLevel
                )
                ] + 1;
    }

    public static int getBlightLevel(
            int spellLevel
    ) {
        return BLIGHT_AMPLIFIERS[
                getLevelIndex(
                        spellLevel
                )
                ] + 1;
    }

    public static float getRendDurationSeconds() {
        return REND_DURATION_TICKS
                / 20.0F;
    }

    public static float getBlightDurationSeconds() {
        return BLIGHT_DURATION_TICKS
                / 20.0F;
    }

    private static int getLevelIndex(
            int spellLevel
    ) {
        int clampedLevel =
                Math.max(
                        1,
                        Math.min(
                                spellLevel,
                                PRIMARY_DAMAGE_MULTIPLIERS.length
                        )
                );

        return clampedLevel - 1;
    }
}