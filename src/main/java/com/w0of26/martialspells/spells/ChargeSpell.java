package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.core.particles.ParticleTypes;
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
 * Forge/Iron's translation of frozen Rogues Charge.
 *
 * <p>The source spell is an instant self-buff lasting two seconds. It grants
 * +50% base Movement Speed and +50% base Knockback Resistance using
 * MULTIPLY_BASE modifiers, then enters a 12-second base cooldown.</p>
 */
public final class ChargeSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "charge");

    public static final int MAX_LEVEL = 1;
    public static final int EFFECT_DURATION_TICKS = 40;
    public static final double MOVEMENT_SPEED_BONUS = 0.50D;
    public static final double KNOCKBACK_RESISTANCE_BONUS = 0.50D;
    public static final int BASE_COOLDOWN_SECONDS = 12;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public ChargeSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.WARRIOR;
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
    public boolean allowLooting() {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        // Played manually alongside the translated release presentation.
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        // Frozen Rogues used Spell Engine's one_handed_area_release. Iron's
        // native instant-cast animation preserves the one-handed release role
        // without introducing a Spell Engine runtime dependency.
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.effect_length",
                        EFFECT_DURATION_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.charge_movement_speed",
                        Math.round(MOVEMENT_SPEED_BONUS * 100.0D)
                ),
                Component.translatable(
                        "ui.martial_spells.charge_knockback_resistance",
                        Math.round(KNOCKBACK_RESISTANCE_BONUS * 100.0D)
                ),
                Component.translatable(
                        "ui.martial_spells.base_cooldown",
                        BASE_COOLDOWN_SECONDS
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

        // SET semantics: the same amplifier-0 effect is reapplied, refreshing
        // its remaining duration rather than creating another attribute stack.
        caster.addEffect(new MobEffectInstance(
                MartialEffectRegistry.CHARGE.get(),
                EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        playReleasePresentation(serverLevel, caster);
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private static void playReleasePresentation(ServerLevel level, LivingEntity caster) {
        level.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                MartialSoundRegistry.CHARGE_ACTIVATE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        // Preserve the frozen release silhouette: a dense energetic burst
        // around the torso plus a broad smoke ring at the feet. The exact
        // Spell Engine particle renderer is deliberately not imported.
        level.sendParticles(
                ParticleTypes.CRIT,
                caster.getX(),
                caster.getY() + caster.getBbHeight() * 0.55D,
                caster.getZ(),
                25,
                0.65D,
                0.7D,
                0.65D,
                0.18D
        );
        level.sendParticles(
                ParticleTypes.POOF,
                caster.getX(),
                caster.getY() + caster.getBbHeight() * 0.45D,
                caster.getZ(),
                25,
                0.45D,
                0.55D,
                0.45D,
                0.08D
        );
        level.sendParticles(
                ParticleTypes.CLOUD,
                caster.getX(),
                caster.getY() + 0.1D,
                caster.getZ(),
                50,
                1.25D,
                0.08D,
                1.25D,
                0.04D
        );
    }
}
