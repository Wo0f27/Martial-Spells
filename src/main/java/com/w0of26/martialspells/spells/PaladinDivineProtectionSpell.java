package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Iron's translation of frozen Paladins Divine Protection. */
public final class PaladinDivineProtectionSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "divine_protection"
            );

    public static final int EFFECT_DURATION_TICKS = 8 * 20;
    public static final int BASE_COOLDOWN_SECONDS = 30;
    public static final float AMPLIFIER_POWER_MULTIPLIER = 0.50F;
    public static final int AMPLIFIER_CAP = 2;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.EPIC)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinDivineProtectionSpell() {
        baseManaCost = 40;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
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

    public int getProtectionAmplifier(
            LivingEntity caster
    ) {
        return Math.min(
                AMPLIFIER_CAP,
                Math.max(
                        0,
                        (int) (
                                AMPLIFIER_POWER_MULTIPLIER
                                        * getEntityPowerMultiplier(caster)
                        )
                )
        );
    }

    public int getProtectedHitCount(
            LivingEntity caster
    ) {
        return getProtectionAmplifier(caster) + 1;
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        int amplifier =
                getProtectionAmplifier(caster);

        caster.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry.DIVINE_PROTECTION.get(),
                        EFFECT_DURATION_TICKS,
                        amplifier,
                        false,
                        true,
                        true
                )
        );

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    caster.getX(),
                    caster.getY(),
                    caster.getZ(),
                    MartialSoundRegistry.DIVINE_PROTECTION_RELEASE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            PaladinVfx.divineProtectionApply(
                    serverLevel,
                    caster
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

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.divine_protection_hits",
                        getProtectedHitCount(caster)
                ),
                Component.translatable(
                        "ui.martial_spells.effect_length",
                        EFFECT_DURATION_TICKS / 20.0F
                )
        );
    }
}
