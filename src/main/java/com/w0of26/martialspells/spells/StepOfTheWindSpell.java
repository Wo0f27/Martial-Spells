package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.w0of26.martialspells.movement.StepOfTheWindMovementEvents;
import com.w0of26.martialspells.movement.StepOfTheWindWallRunEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;


public final class StepOfTheWindSpell
        extends AbstractMonkTechniqueSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "step_of_the_wind"
            );

    private static final int MAX_LEVEL = 5;
    private static final int KI_COST = 1;
    private static final int COOLDOWN_SECONDS = 5;

    /*
     * Checkpoint values.
     *
     * We will tune these from actual runtime movement rather
     * than guessing the final dash distance beforehand.
     */
    private static final double[] HORIZONTAL_SPEED = {
            1.25D,
            1.40D,
            1.55D,
            1.70D,
            1.85D
    };

    /*
     * Looking upward/downward influences the dash, but the
     * vertical component is deliberately capped so Step of
     * the Wind cannot act as unrestricted flight.
     */
    private static final double MIN_VERTICAL_INTENT =
            -0.20D;

    private static final double MAX_VERTICAL_INTENT =
            0.45D;

    private static final double VERTICAL_SPEED_SCALE =
            0.90D;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setSchoolResource(
                            MartialSchoolRegistry.MARTIAL_RESOURCE
                    )
                    .setMinRarity(
                            SpellRarity.UNCOMMON
                    )
                    .setMaxLevel(
                            MAX_LEVEL
                    )
                    .setCooldownSeconds(
                            COOLDOWN_SECONDS
                    )
                    .build();

    public StepOfTheWindSpell() {
        super(MAX_LEVEL);
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
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
    public CastResult canBeCastedBy(
            int spellLevel,
            CastSource castSource,
            MagicData magicData,
            Player player
    ) {
        CastResult normalResult =
                super.canBeCastedBy(
                        spellLevel,
                        castSource,
                        magicData,
                        player
                );

        if (!normalResult.isSuccess()) {
            return normalResult;
        }

        if (!(player
                instanceof ServerPlayer serverPlayer)) {

            return failure(
                    "ui.martial_spells.server_player_required"
            );
        }

        CastResult sourceResult =
                validateMonkTechniqueSource(
                        castSource,
                        player
                );

        if (!sourceResult.isSuccess()) {
            return sourceResult;
        }

        if (!hasValidMovementState(player)) {
            return failure(
                    "ui.martial_spells."
                            + "step_of_the_wind_invalid_movement"
            );
        }

        if (!hasTechniqueKi(
                serverPlayer,
                KI_COST
        )) {
            return failure(
                    "ui.martial_spells.not_enough_ki"
            );
        }

        return success();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        int displayedKiCost =
                caster == null
                        ? KI_COST
                        : getEffectiveTechniqueKiCost(
                        KI_COST,
                        caster
                );

        int level =
                clampTechniqueLevel(
                        spellLevel
                );

        return List.of(
                Component.translatable(
                        "ui.martial_spells.ki_cost",
                        displayedKiCost
                ),
                Component.translatable(
                        "ui.martial_spells.step_of_the_wind_dash_speed",
                        HORIZONTAL_SPEED[
                                level - 1
                                ]
                ),
                Component.translatable(
                        "ui.martial_spells.step_of_the_wind_wall_run"
                ),
                Component.translatable(
                        "ui.martial_spells.step_of_the_wind_wall_jump"
                ),
                Component.translatable(
                        "ui.martial_spells.step_of_the_wind_fall_protection"
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
        if (!(caster
                instanceof ServerPlayer player)) {
            return;
        }

        if (!isValidMonkTechniqueSource(
                castSource,
                player
        )) {
            return;
        }

        /*
         * Recheck movement state on execution so changing
         * state between validation and execution cannot
         * bypass the restriction.
         */
        if (!hasValidMovementState(player)) {
            return;
        }

        if (!consumeTechniqueKi(
                player,
                KI_COST
        )) {
            return;
        }

        int techniqueLevel =
                clampTechniqueLevel(
                        spellLevel
                );

        StepOfTheWindMovementEvents
                .armFallProtection(
                        player
                );

        StepOfTheWindWallRunEvents
                .arm(
                        player,
                        techniqueLevel
                );

        performDash(
                player,
                techniqueLevel
        );
    }

    private static boolean hasValidMovementState(
            Player player
    ) {
        if (player.isPassenger()) {
            return false;
        }

        if (player.isInWaterOrBubble()) {
            return false;
        }

        if (player.onClimbable()) {
            return false;
        }

        if (player.isFallFlying()) {
            return false;
        }

        return !player.getAbilities().flying;
    }

    private static void performDash(
            ServerPlayer player,
            int spellLevel
    ) {
        /*
         * Use yaw for the primary movement direction.
         *
         * This means looking nearly straight upward still
         * produces a forward dash rather than converting
         * Step of the Wind into a vertical launch.
         */
        double yawRadians =
                Math.toRadians(
                        player.getYRot()
                );

        Vec3 horizontalDirection =
                new Vec3(
                        -Math.sin(yawRadians),
                        0.0D,
                        Math.cos(yawRadians)
                );

        double horizontalSpeed =
                HORIZONTAL_SPEED[
                        spellLevel - 1
                        ];

        /*
         * Pitch contributes only a limited amount of
         * vertical movement.
         */
        double verticalIntent =
                Mth.clamp(
                        player.getLookAngle().y,
                        MIN_VERTICAL_INTENT,
                        MAX_VERTICAL_INTENT
                );

        double verticalSpeed =
                verticalIntent
                        * VERTICAL_SPEED_SCALE;

        Vec3 dashVelocity =
                horizontalDirection
                        .scale(horizontalSpeed)
                        .add(
                                0.0D,
                                verticalSpeed,
                                0.0D
                        );

        /*
         * Server owns the authoritative velocity.
         */
        player.setDeltaMovement(
                dashVelocity
        );

        /*
         * Make the entity tracker treat the motion as
         * externally changed.
         */
        player.hurtMarked = true;

        /*
         * Explicitly synchronize the velocity to the
         * controlling player's client.
         *
         * This is important for ServerPlayer movement,
         * where relying only on ordinary entity tracking
         * can make externally-applied motion feel delayed
         * or get overwritten by client movement.
         */
        player.connection.send(
                new ClientboundSetEntityMotionPacket(
                        player
                )
        );
    }
}