package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.MortalStrikeAttackManager;
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
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Forge/Iron's translation of frozen Rogues Mortal Strike. */
public final class MortalStrikeSpell extends AbstractSpell
        implements MartialTechnique {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "mortal_strike"
            );

    public static final ResourceLocation WINDUP_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "two_handed_slash_vertical_windup"
            );

    public static final ResourceLocation SLASH_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "two_handed_slash_vertical_slash"
            );

    public static final int MAX_LEVEL = 1;
    public static final int CAST_TIME_TICKS = 10;
    public static final float RANGE = 3.0F;

    public static final float DAMAGE_BONUS = 0.50F;
    public static final float ATTACK_DELAY_FRACTION = 0.30F;

    public static final float HITBOX_LENGTH_FACTOR = 1.0F;
    public static final float HITBOX_WIDTH_FACTOR = 0.50F;
    public static final float HITBOX_HEIGHT_FACTOR = 1.50F;
    public static final float ARC_DEGREES = 120.0F;

    public static final int BLEED_DURATION_TICKS = 120;
    public static final int BLEED_BASE_AMPLIFIER = 1;
    public static final double BLEED_POWER_MULTIPLIER = 0.25D;
    public static final int BLEED_IMPACT_PARTICLES = 40;

    public static final int IMPACT_SOUND_CAP = 3;
    public static final int BASE_COOLDOWN_SECONDS = 15;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.EPIC)
                    .setSchoolResource(
                            MartialSchoolRegistry.MARTIAL_RESOURCE
                    )
                    .setMaxLevel(MAX_LEVEL)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public MortalStrikeSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = CAST_TIME_TICKS;
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
        // Source is a 0.5-second STANDARD cast: early release cancels.
        return CastType.LONG;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(
                MartialSoundRegistry.MORTAL_STRIKE_SWING.get()
        );
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return new AnimationHolder(
                WINDUP_ANIMATION,
                true,
                false
        );
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return new AnimationHolder(
                SLASH_ANIMATION,
                true,
                false
        );
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.mortal_strike_range",
                        RANGE
                ),
                Component.translatable(
                        "ui.martial_spells.mortal_strike_damage_bonus",
                        Math.round(DAMAGE_BONUS * 100.0F)
                ),
                Component.translatable(
                        "ui.martial_spells.mortal_strike_bleed",
                        BLEED_DURATION_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.mortal_strike_delay",
                        Math.round(
                                ATTACK_DELAY_FRACTION * 100.0F
                        )
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
        if (!level.isClientSide
                && caster instanceof ServerPlayer player) {
            MortalStrikeAttackManager.begin(player);
        }

        super.onCast(
                level,
                spellLevel,
                caster,
                castSource,
                magicData
        );
    }
}
