package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.effects.GuardiansCovenantTankEffect;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.util.GuardiansCovenantLinkData;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public final class GuardiansCovenantSpell extends AbstractSpell {
    public static final int MAX_LEVEL = 5;

    public static final String CAST_LEVEL_TAG =
            MartialSpells.MOD_ID
                    + "_guardians_covenant_cast_level";

    private static final int[] DURATION_SECONDS = {
            8,
            10,
            12,
            14,
            16
    };

    private static final double[] RADIUS_BLOCKS = {
            8.0D,
            9.0D,
            10.0D,
            11.0D,
            12.0D
    };

    private static final int[] COOLDOWN_SECONDS = {
            120,
            100,
            80,
            60,
            40
    };

    private static final float[] REDIRECT_PERCENTAGES = {
            15.0F,
            17.5F,
            20.0F,
            22.5F,
            25.0F
    };

    /*
     * Measured in health points:
     *
     * 8 health  = 4 hearts
     * 12 health = 6 hearts
     * 16 health = 8 hearts
     * 20 health = 10 hearts
     */
    private static final int[] ABSORPTION_HEALTH = {
            8,
            8,
            12,
            16,
            20
    };

    /*
     * Vanilla Absorption grants four health points per effect level.
     *
     * Amplifier 1 = 8 absorption health
     * Amplifier 2 = 12 absorption health
     * Amplifier 3 = 16 absorption health
     * Amplifier 4 = 20 absorption health
     */
    private static final int[] ABSORPTION_AMPLIFIERS = {
            1,
            1,
            2,
            3,
            4
    };

    private final ResourceLocation spellId =
            new ResourceLocation(
                    MartialSpells.MOD_ID,
                    "guardians_covenant"
            );

    /*
     * The configured cooldown is the Level V fallback.
     * GuardiansCovenantCooldownEvents will enforce the actual
     * level-specific cooldown after casting.
     */
    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.EPIC)
                    .setSchoolResource(
                            SchoolRegistry.HOLY_RESOURCE
                    )
                    .setMaxLevel(MAX_LEVEL)
                    .setCooldownSeconds(40)
                    .build();

    public GuardiansCovenantSpell() {
        this.baseManaCost = 80;
        this.manaCostPerLevel = 20;

        /*
         * Spell Power scaling will be added only after the base
         * covenant and damage-redirection systems are verified.
         */
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;

        this.castTime = 0;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        int level = clampLevel(spellLevel);

        return List.of(
                Component.translatable(
                        "ui.martial_spells.radius",
                        getRadiusForLevel(level)
                ),
                Component.translatable(
                        "ui.martial_spells.effect_length",
                        getDurationSecondsForLevel(level)
                ),
                Component.translatable(
                        "ui.martial_spells.damage_redirect",
                        getRedirectPercentageForLevel(level)
                ),
                Component.translatable(
                        "ui.martial_spells.absorption_health",
                        getAbsorptionHealthForLevel(level)
                ),
                Component.translatable(
                        "ui.martial_spells.armor_bonus",
                        GuardiansCovenantTankEffect
                                .getArmorBonus(level)
                ),
                Component.translatable(
                        "ui.martial_spells.toughness_bonus",
                        GuardiansCovenantTankEffect
                                .getToughnessBonus(level)
                ),
                Component.translatable(
                        "ui.martial_spells.fixed_cooldown",
                        getCooldownSecondsForLevel(level)
                ),
                Component.translatable(
                        "ui.martial_spells.cooldown_reduction_immune"
                )
        );
    }

    @Override
    public void onCast(
            Level world,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        int level = clampLevel(spellLevel);

        /*
         * MagicManager adds the spell cooldown after onCast returns.
         * The cooldown event retrieves this temporary level value.
         */
        if (!world.isClientSide
                && entity instanceof ServerPlayer caster) {
            caster.getPersistentData().putInt(
                    CAST_LEVEL_TAG,
                    level
            );

            applyCovenant(
                    caster,
                    level
            );
        }

        super.onCast(
                world,
                spellLevel,
                entity,
                castSource,
                playerMagicData
        );
    }

    private static void applyCovenant(
            ServerPlayer caster,
            int spellLevel
    ) {
        ServerLevel level = caster.serverLevel();

        int durationTicks =
                getDurationTicksForLevel(spellLevel);

        int effectAmplifier =
                spellLevel - 1;

        double radius =
                getRadiusForLevel(spellLevel);

        /*
         * Recasting or externally resetting the cooldown should not
         * leave old players linked to the same tank.
         */
        removeExistingLinksFromCaster(caster);

        caster.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry
                                .GUARDIANS_COVENANT_TANK
                                .get(),
                        durationTicks,
                        effectAmplifier,
                        false,
                        false,
                        true
                )
        );

        /*
         * Phase 1 uses vanilla Absorption. We will specifically test
         * its interaction with other absorption-granting effects
         * before finalizing the spell.
         */
        caster.addEffect(
                new MobEffectInstance(
                        MobEffects.ABSORPTION,
                        durationTicks,
                        getAbsorptionAmplifierForLevel(
                                spellLevel
                        ),
                        false,
                        false,
                        true
                )
        );

        List<ServerPlayer> nearbyAllies =
                level.getEntitiesOfClass(
                        ServerPlayer.class,
                        caster.getBoundingBox()
                                .inflate(radius),
                        ally ->
                                ally != caster
                                        && ally.isAlive()
                                        && !ally.isSpectator()
                                        && ally.distanceToSqr(caster)
                                        <= radius * radius
                );

        for (ServerPlayer ally : nearbyAllies) {
            applyLink(
                    caster,
                    ally,
                    spellLevel,
                    durationTicks,
                    radius
            );
        }
    }

    private static void applyLink(
            ServerPlayer caster,
            ServerPlayer ally,
            int spellLevel,
            int durationTicks,
            double radius
    ) {
        /*
         * Ensure the previous covenant is removed before writing
         * the new caster UUID.
         */
        ally.removeEffect(
                MartialEffectRegistry
                        .GUARDIANS_COVENANT_LINKED
                        .get()
        );

        GuardiansCovenantLinkData.clearLink(ally);

        ally.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry
                                .GUARDIANS_COVENANT_LINKED
                                .get(),
                        durationTicks,
                        spellLevel - 1,
                        false,
                        false,
                        true
                )
        );

        GuardiansCovenantLinkData.setLink(
                ally,
                caster.getUUID(),
                spellLevel,
                radius
        );
    }

    private static void removeExistingLinksFromCaster(
            ServerPlayer caster
    ) {
        MinecraftServer server = caster.getServer();

        if (server == null) {
            return;
        }

        UUID casterUuid = caster.getUUID();

        for (
                ServerPlayer player
                : server.getPlayerList().getPlayers()
        ) {
            if (!GuardiansCovenantLinkData.hasLink(player)) {
                continue;
            }

            UUID storedCasterUuid =
                    GuardiansCovenantLinkData
                            .getCasterUuid(player);

            if (!casterUuid.equals(storedCasterUuid)) {
                continue;
            }

            player.removeEffect(
                    MartialEffectRegistry
                            .GUARDIANS_COVENANT_LINKED
                            .get()
            );

            GuardiansCovenantLinkData.clearLink(player);
        }
    }

    private static int clampLevel(int spellLevel) {
        return Math.max(
                1,
                Math.min(spellLevel, MAX_LEVEL)
        );
    }

    private static int getArrayIndex(int spellLevel) {
        return clampLevel(spellLevel) - 1;
    }

    public static int getDurationSecondsForLevel(
            int spellLevel
    ) {
        return DURATION_SECONDS[
                getArrayIndex(spellLevel)
                ];
    }

    public static int getDurationTicksForLevel(
            int spellLevel
    ) {
        return getDurationSecondsForLevel(spellLevel)
                * 20;
    }

    public static double getRadiusForLevel(
            int spellLevel
    ) {
        return RADIUS_BLOCKS[
                getArrayIndex(spellLevel)
                ];
    }

    public static int getCooldownSecondsForLevel(
            int spellLevel
    ) {
        return COOLDOWN_SECONDS[
                getArrayIndex(spellLevel)
                ];
    }

    public static int getCooldownTicksForLevel(
            int spellLevel
    ) {
        return getCooldownSecondsForLevel(spellLevel)
                * 20;
    }

    public static float getRedirectPercentageForLevel(
            int spellLevel
    ) {
        return REDIRECT_PERCENTAGES[
                getArrayIndex(spellLevel)
                ];
    }

    public static float getRedirectMultiplierForLevel(
            int spellLevel
    ) {
        return getRedirectPercentageForLevel(spellLevel)
                / 100.0F;
    }

    public static int getAbsorptionHealthForLevel(
            int spellLevel
    ) {
        return ABSORPTION_HEALTH[
                getArrayIndex(spellLevel)
                ];
    }

    private static int getAbsorptionAmplifierForLevel(
            int spellLevel
    ) {
        return ABSORPTION_AMPLIFIERS[
                getArrayIndex(spellLevel)
                ];
    }
}