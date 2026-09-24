package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.PaladinHybridPower;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Iron's translation of frozen Paladins Blessed Strikes.
 *
 * Iron's CONTINUOUS spells execute once immediately and then every ten ticks.
 * The immediate execution is deliberately a no-op; the five executions at
 * remaining durations 40/30/20/10/0 are the source's five half-second seals.
 */
public final class PaladinBlessedStrikesSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "blessed_strikes"
            );

    public static final int CAST_TIME_TICKS = 50;
    public static final int BASE_COOLDOWN_SECONDS = 12;
    public static final int EFFECT_DURATION_TICKS = 15 * 20;
    public static final int MAX_AMPLIFIER = 5;
    public static final float DAMAGE_COEFFICIENT = 0.50F;
    public static final double KNOCKBACK_STRENGTH = 0.50D;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.RARE)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinBlessedStrikesSpell() {
        // Full channel costs 30 target-side mana:
        // one 5-mana channel-initiation tick + five 5-mana seal ticks.
        baseManaCost = 5;
        manaCostPerLevel = 0;
        baseSpellPower = PaladinHolySpellSupport.SOURCE_POWER_REFERENCE;
        spellPowerPerLevel = 0;
        castTime = CAST_TIME_TICKS;
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
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(
                MartialSoundRegistry.BLESSED_STRIKE_START.get()
        );
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData magicData
    ) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getGameTime() % 2L == 0L) {
            PaladinVfx.blessedGather(
                    serverLevel,
                    entity
            );
        }
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        // Iron's continuous framework fires once immediately at duration 50.
        // Upstream begins granting seals only after the first 0.5 second.
        if (magicData.getCastDurationRemaining() >= CAST_TIME_TICKS) {
            return;
        }

        addSeal(caster);

        if (level instanceof ServerLevel serverLevel) {
            PaladinVfx.blessedRelease(
                    serverLevel,
                    caster
            );
            serverLevel.playSound(
                    null,
                    caster.getX(),
                    caster.getY(),
                    caster.getZ(),
                    MartialSoundRegistry.BLESSED_STRIKE_RELEASE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        super.onCast(
                level,
                spellLevel,
                caster,
                castSource,
                magicData
        );
    }

    private static void addSeal(
            LivingEntity caster
    ) {
        MobEffectInstance current =
                caster.getEffect(
                        MartialEffectRegistry.BLESSED_STRIKES.get()
                );

        int amplifier =
                current == null
                        ? 0
                        : Math.min(
                                MAX_AMPLIFIER,
                                current.getAmplifier() + 1
                        );

        caster.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry.BLESSED_STRIKES.get(),
                        EFFECT_DURATION_TICKS,
                        amplifier,
                        false,
                        true,
                        true
                )
        );
    }

    public float getHybridPower(
            LivingEntity caster
    ) {
        return PaladinHybridPower.holyDominant(
                this,
                caster
        );
    }

    public float getBonusDamage(
            LivingEntity caster
    ) {
        return getHybridPower(caster)
                * DAMAGE_COEFFICIENT;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.blessed_strikes_seals",
                        5
                ),
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(
                                getBonusDamage(caster),
                                2
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.effect_length",
                        EFFECT_DURATION_TICKS / 20.0F
                )
        );
    }
}
