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
 * Forge/Iron's translation of frozen Rogues Slice & Dice.
 *
 * <p>Casting creates a ten-second, non-refreshing battle-trance effect at
 * amplifier 0. Each successful player melee impact advances the effect by one
 * amplifier through the Forge event hook in SliceAndDiceEvents, while keeping
 * the original remaining duration.</p>
 */
public final class SliceAndDiceSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "slice_and_dice");

    public static final int MAX_LEVEL = 1;
    public static final int EFFECT_DURATION_TICKS = 200;
    public static final int MAX_AMPLIFIER = 9;
    public static final int BASE_COOLDOWN_SECONDS = 15;
    public static final double ATTACK_DAMAGE_PER_STACK = 0.10D;
    public static final int RELEASE_PARTICLES = 20;
    public static final double RELEASE_PARTICLE_RADIUS = 1.0D;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public SliceAndDiceSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.ROGUE;
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
        // The frozen release sound is played manually with the release VFX.
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        // Frozen Rogues uses Spell Engine's dual_handed_weapon_charge pose.
        // Keep the port dependency-free instead of importing Spell Engine for it.
        return AnimationHolder.none();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.slice_and_dice_duration",
                        EFFECT_DURATION_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.slice_and_dice_initial_bonus",
                        Math.round(ATTACK_DAMAGE_PER_STACK * 100.0D)
                ),
                Component.translatable(
                        "ui.martial_spells.slice_and_dice_max_bonus",
                        Math.round(ATTACK_DAMAGE_PER_STACK * (MAX_AMPLIFIER + 1) * 100.0D)
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

        caster.addEffect(new MobEffectInstance(
                MartialEffectRegistry.SLICE_AND_DICE.get(),
                EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        serverLevel.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                MartialSoundRegistry.SLICE_AND_DICE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        double centerY = caster.getY() + caster.getBbHeight() * 0.5D;
        for (int i = 0; i < RELEASE_PARTICLES; i++) {
            double angle = Math.PI * 2.0D * i / RELEASE_PARTICLES;
            double x = caster.getX() + Math.cos(angle) * RELEASE_PARTICLE_RADIUS;
            double z = caster.getZ() + Math.sin(angle) * RELEASE_PARTICLE_RADIUS;
            serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    x,
                    centerY,
                    z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.001D
            );
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }
}
