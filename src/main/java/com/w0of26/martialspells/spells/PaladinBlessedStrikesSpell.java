package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.PaladinHybridPower;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * User-approved mechanics-only translation of Blessed Strikes.
 *
 * <p>The original 2.5-second channel and its presentation are intentionally
 * removed. Casting is instant and immediately grants five Blessed Strikes
 * charges for 15 seconds. The existing melee trigger still consumes one
 * charge after each successful melee swing, including Better Combat cleaves
 * that hit multiple targets in the same tick.</p>
 */
public final class PaladinBlessedStrikesSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "blessed_strikes"
            );

    public static final int BASE_COOLDOWN_SECONDS = 12;
    public static final int EFFECT_DURATION_TICKS = 15 * 20;
    public static final int MAX_SEALS = 5;
    public static final int FULL_STACK_AMPLIFIER = MAX_SEALS - 1;
    public static final int MANA_COST = 30;
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
        // Preserve the total cost of the former full five-seal channel.
        baseManaCost = MANA_COST;
        manaCostPerLevel = 0;
        baseSpellPower = PaladinHolySpellSupport.SOURCE_POWER_REFERENCE;
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

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        caster.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry.BLESSED_STRIKES.get(),
                        EFFECT_DURATION_TICKS,
                        FULL_STACK_AMPLIFIER,
                        false,
                        true,
                        true
                )
        );

        super.onCast(
                level,
                spellLevel,
                caster,
                castSource,
                magicData
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
                        MAX_SEALS
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
