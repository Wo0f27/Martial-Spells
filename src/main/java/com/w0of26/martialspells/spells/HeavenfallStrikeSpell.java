package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.HeavenfallCombatHelper;
import com.w0of26.martialspells.movement.StepOfTheWindMovementEvents;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.w0of26.martialspells.movement.HeavenfallStrikeEvents;
import com.w0of26.martialspells.client.animation.HeavenfallAnimationPhase;
import com.w0of26.martialspells.combat.HeavenfallAnimationStyle;
import com.w0of26.martialspells.combat.MonkWeaponHelper;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncHeavenfallAnimationPacket;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;


import java.util.List;

public final class HeavenfallStrikeSpell
        extends AbstractMonkTechniqueSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "heavenfall_strike"
            );

    private static final int MAX_LEVEL = 5;

    /*
     * Provisional balance value.
     *
     * Heavy armor automatically modifies this through the
     * shared Monk technique framework.
     */
    private static final int KI_COST = 2;

    /*
     * Provisional cooldown.
     *
     * We can tune this once the entire technique exists.
     */
    private static final int COOLDOWN_SECONDS = 20;

    /*
     * Initial vertical launch velocity.
     *
     * Keep this independent of level for the first movement
     * checkpoint. Level scaling should reward combat output,
     * targeting, and impact rather than making each version
     * have radically different movement timing.
     */
    private static final double LAUNCH_SPEED =
            1.25D;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setSchoolResource(
                            MartialSchoolRegistry.MARTIAL_RESOURCE
                    )
                    .setMinRarity(
                            SpellRarity.RARE
                    )
                    .setMaxLevel(
                            MAX_LEVEL
                    )
                    .setCooldownSeconds(
                            COOLDOWN_SECONDS
                    )
                    .build();

    public HeavenfallStrikeSpell() {
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

        if (!(player instanceof ServerPlayer serverPlayer)) {
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

        if (!hasValidLaunchState(player)) {
            return failure(
                    "ui.martial_spells.heavenfall_invalid_movement"
            );
        }

        if (!hasLaunchClearance(serverPlayer)) {
            return failure(
                    "ui.martial_spells.heavenfall_no_clearance"
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
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        if (!(caster instanceof ServerPlayer player)) {
            return;
        }

        /*
         * Revalidate everything at execution time.
         */
        if (!isValidMonkTechniqueSource(
                castSource,
                player
        )) {
            return;
        }

        if (!hasValidLaunchState(player)) {
            return;
        }

        if (!hasLaunchClearance(player)) {
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

        /*
         * Heavenfall's entire aerial sequence will eventually
         * be one protected movement.
         *
         * Reusing the Step landing-based protection prevents
         * Heavenfall itself from killing the caster during
         * development.
         */
        StepOfTheWindMovementEvents
                .armFallProtection(
                        player
                );

        HeavenfallStrikeEvents
                .arm(
                        player,
                        techniqueLevel
                );


        HeavenfallAnimationStyle animationStyle =
                MonkWeaponHelper
                        .getHeavenfallAnimationStyle(
                                player
                        );

        if (animationStyle != null) {
            MartialNetwork.sendToTrackingAndSelf(
                    new SyncHeavenfallAnimationPacket(
                            player.getUUID(),
                            HeavenfallAnimationPhase.LAUNCH,
                            animationStyle
                    ),
                    player
            );
        }

        launch(
                player
        );
    }

    private static boolean hasValidLaunchState(
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

    /*
     * Very small initial ceiling safeguard.
     *
     * We only need enough room for the launch to begin.
     * Later targeting-state logic will continuously handle
     * unexpected collisions.
     */
    private static boolean hasLaunchClearance(
            ServerPlayer player
    ) {
        return player.level()
                .noCollision(
                        player,
                        player.getBoundingBox()
                                .move(
                                        0.0D,
                                        1.0D,
                                        0.0D
                                )
                );
    }

    private static void launch(
            ServerPlayer player
    ) {
        Vec3 currentVelocity =
                player.getDeltaMovement();

        /*
         * Preserve a modest amount of existing horizontal
         * momentum while replacing vertical velocity with
         * the Heavenfall launch.
         */
        Vec3 launchVelocity =
                new Vec3(
                        currentVelocity.x * 0.35D,
                        LAUNCH_SPEED,
                        currentVelocity.z * 0.35D
                );

        player.setDeltaMovement(
                launchVelocity
        );

        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        player.connection.send(
                new ClientboundSetEntityMotionPacket(
                        player
                )
        );
    }

    private static String toRomanNumeral(
            int value
    ) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(
                    value
            );
        };
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

        int primaryDamagePercent =
                Math.round(
                        HeavenfallCombatHelper
                                .getPrimaryDamageMultiplier(
                                        spellLevel
                                )
                                * 100.0F
                );

        int shockwaveDamagePercent =
                Math.round(
                        HeavenfallCombatHelper
                                .getShockwaveDamageMultiplier(
                                        spellLevel
                                )
                                * 100.0F
                );

        String rendLevel =
                toRomanNumeral(
                        HeavenfallCombatHelper
                                .getRendLevel(
                                        spellLevel
                                )
                );

        String blightLevel =
                toRomanNumeral(
                        HeavenfallCombatHelper
                                .getBlightLevel(
                                        spellLevel
                                )
                );

        return List.of(
                Component.translatable(
                        "ui.martial_spells.ki_cost",
                        displayedKiCost
                ),

                Component.translatable(
                        "ui.martial_spells.heavenfall_impact_damage",
                        primaryDamagePercent
                ),

                Component.translatable(
                        "ui.martial_spells.heavenfall_shockwave_damage",
                        shockwaveDamagePercent
                ),

                Component.translatable(
                        "ui.martial_spells.heavenfall_shockwave_radius",
                        HeavenfallCombatHelper
                                .getShockwaveRadius(
                                        spellLevel
                                )
                ),

                Component.translatable(
                        "ui.martial_spells.heavenfall_stun_duration",
                        HeavenfallCombatHelper
                                .getStunDurationSeconds(
                                        spellLevel
                                )
                ),

                Component.translatable(
                        "ui.martial_spells.heavenfall_debuffs",
                        rendLevel,
                        blightLevel,
                        HeavenfallCombatHelper
                                .getRendDurationSeconds()
                ),

                Component.translatable(
                        "ui.martial_spells.scales_with_martial_power"
                ),

                Component.translatable(
                        "ui.martial_spells.heavy_armor_penalty"
                )
        );
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }


}