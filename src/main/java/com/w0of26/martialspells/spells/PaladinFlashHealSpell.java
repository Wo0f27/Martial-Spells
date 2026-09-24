package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Iron's translation of frozen Paladins Flash Heal. */
public final class PaladinFlashHealSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "flash_heal"
            );

    public static final int RANGE = 16;
    public static final int CAST_TIME_TICKS = 10;
    public static final int BASE_COOLDOWN_SECONDS = 6;
    public static final float HEAL_COEFFICIENT = 1.20F;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.RARE)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinFlashHealSpell() {
        baseManaCost = 30;
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
        return PaladinHolySpellSupport.targetFriendlyOrSelf(
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
        if (level instanceof ServerLevel serverLevel) {
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

        if (target != null
                && Utils.shouldHealEntity(caster, target)) {
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
                        30
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
                        "ui.martial_spells.paladin_target_range",
                        RANGE
                )
        );
    }
}
