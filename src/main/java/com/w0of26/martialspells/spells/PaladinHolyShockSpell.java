package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Iron's translation of frozen Paladins Holy Shock. */
public final class PaladinHolyShockSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "holy_shock"
            );

    public static final int RANGE = 16;
    public static final int CAST_TIME_TICKS = 30;
    public static final int BASE_COOLDOWN_SECONDS = 3;
    public static final float HEAL_COEFFICIENT = 0.40F;
    public static final float DAMAGE_COEFFICIENT = 0.80F;
    public static final double KNOCKBACK_STRENGTH = 0.50D;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.UNCOMMON)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinHolyShockSpell() {
        baseManaCost = 20;
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
        return CastType.LONG;
    }

    @Override
    public boolean checkPreCastConditions(
            Level level,
            int spellLevel,
            LivingEntity caster,
            MagicData magicData
    ) {
        return PaladinHolySpellSupport.targetAnyOrSelf(
                level,
                caster,
                magicData,
                this,
                RANGE
        );
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
            PaladinVfx.holyCasting(
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
        LivingEntity target =
                PaladinHolySpellSupport.getTarget(
                        level,
                        magicData
                );

        if (target == null) {
            return;
        }

        if (Utils.shouldHealEntity(caster, target)) {
            PaladinHolySpellSupport.heal(
                    this,
                    level,
                    spellLevel,
                    caster,
                    target,
                    HEAL_COEFFICIENT
            );

            if (level instanceof ServerLevel serverLevel) {
                PaladinVfx.healPillar(
                        serverLevel,
                        target,
                        15
                );
                PaladinVfx.holyGlimmer(
                        serverLevel,
                        target,
                        15,
                        0.20D,
                        0.25D
                );
            }

            playImpactSound(
                    level,
                    target,
                    true
            );
        } else {
            boolean damaged =
                    PaladinHolySpellSupport.damage(
                            this,
                            level,
                            spellLevel,
                            caster,
                            target,
                            DAMAGE_COEFFICIENT
                    );

            if (damaged) {
                target.knockback(
                        KNOCKBACK_STRENGTH,
                        caster.getX() - target.getX(),
                        caster.getZ() - target.getZ()
                );

                if (level instanceof ServerLevel serverLevel) {
                    PaladinVfx.holyBurst(
                            serverLevel,
                            target,
                            30,
                            0.70D
                    );
                }

                playImpactSound(
                        level,
                        target,
                        false
                );
            }
        }

        super.onCast(
                level,
                spellLevel,
                caster,
                castSource,
                magicData
        );
    }

    private static void playImpactSound(
            Level level,
            LivingEntity target,
            boolean healing
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                healing
                        ? MartialSoundRegistry.HOLY_SHOCK_HEAL.get()
                        : MartialSoundRegistry.HOLY_SHOCK_DAMAGE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.healing",
                        Utils.stringTruncation(
                                PaladinHolySpellSupport.getScaledAmount(
                                        this,
                                        spellLevel,
                                        caster,
                                        HEAL_COEFFICIENT
                                ),
                                2
                        )
                ),
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(
                                PaladinHolySpellSupport.getScaledAmount(
                                        this,
                                        spellLevel,
                                        caster,
                                        DAMAGE_COEFFICIENT
                                ),
                                2
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.paladin_target_range",
                        RANGE
                )
        );
    }
}
