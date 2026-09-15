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
 * Forge/Iron's translation of frozen Rogues Vanish.
 *
 * <p>Vanish applies eight seconds of Stealth. The Stealth effect itself owns
 * the exact -50% base movement-speed modifier; target suppression,
 * invisibility and source break conditions are handled by the R5 hooks.</p>
 */
public final class VanishSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "vanish");

    public static final int MAX_LEVEL = 1;
    public static final int EFFECT_DURATION_TICKS = 160;
    public static final int BASE_COOLDOWN_SECONDS = 30;
    public static final double MOVEMENT_SPEED_MULTIPLIER = -0.50D;
    public static final double STEALTH_FOLLOW_DISTANCE = 1.0D;

    private static final int RELEASE_SMOKE_SPHERE_COUNT = 20;
    private static final int RELEASE_SMOKE_RING_COUNT = 20;
    private static final int RELEASE_POOF_COUNT = 10;
    private static final int RELEASE_CAMPFIRE_SMOKE_COUNT = 10;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public VanishSpell() {
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
        // Frozen Rogues uses vanish_combined as its release sound.
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        // Frozen Rogues uses Spell Engine's dual_handed_weapon_cross pose.
        // Keep the port independent instead of importing Spell Engine for it.
        return AnimationHolder.none();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.vanish_duration",
                        EFFECT_DURATION_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.vanish_movement_penalty",
                        Math.round(-MOVEMENT_SPEED_MULTIPLIER * 100.0D)
                ),
                Component.translatable(
                        "ui.martial_spells.vanish_tracking_range",
                        STEALTH_FOLLOW_DISTANCE
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
                MartialEffectRegistry.STEALTH.get(),
                EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        // Make the visual state immediate; LivingEntityStealthMixin keeps the
        // entity invisible for the full lifetime of the Stealth effect.
        caster.setInvisible(true);

        serverLevel.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                MartialSoundRegistry.VANISH_COMBINED.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        emitReleaseParticles(serverLevel, caster);
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private static void emitReleaseParticles(ServerLevel level, LivingEntity caster) {
        double centerY = caster.getY() + caster.getBbHeight() * 0.5D;

        // Frozen source: 20 smoke_medium in a sphere. Spell Engine owns that
        // particle, so use vanilla smoke with the same batch count/silhouette.
        level.sendParticles(
                ParticleTypes.SMOKE,
                caster.getX(),
                centerY,
                caster.getZ(),
                RELEASE_SMOKE_SPHERE_COUNT,
                0.45D,
                0.55D,
                0.45D,
                0.12D
        );

        // Frozen source: 20 smoke_medium in a feet-origin circle.
        for (int i = 0; i < RELEASE_SMOKE_RING_COUNT; i++) {
            double angle = Math.PI * 2.0D * i / RELEASE_SMOKE_RING_COUNT;
            double radius = 0.70D;
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    caster.getX() + Math.cos(angle) * radius,
                    caster.getY() + 0.08D,
                    caster.getZ() + Math.sin(angle) * radius,
                    1,
                    0.0D,
                    0.02D,
                    0.0D,
                    0.12D
            );
        }

        level.sendParticles(
                ParticleTypes.POOF,
                caster.getX(),
                centerY,
                caster.getZ(),
                RELEASE_POOF_COUNT,
                0.40D,
                0.55D,
                0.40D,
                0.01D
        );

        level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                caster.getX(),
                caster.getY() + 0.10D,
                caster.getZ(),
                RELEASE_CAMPFIRE_SMOKE_COUNT,
                0.35D,
                0.50D,
                0.35D,
                0.01D
        );
    }
}
