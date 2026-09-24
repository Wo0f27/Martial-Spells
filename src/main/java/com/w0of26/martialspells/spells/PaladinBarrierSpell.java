package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.PaladinBarrierEntity;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Iron's translation of frozen Paladins Barrier. */
public final class PaladinBarrierSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "barrier"
            );

    public static final int RANGE = 4;
    public static final int CAST_TIME_TICKS = 10;
    public static final int BASE_COOLDOWN_SECONDS = 40;
    public static final int BASE_MANA_COST = 60;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.LEGENDARY)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinBarrierSpell() {
        baseManaCost = BASE_MANA_COST;
        manaCostPerLevel = 0;
        baseSpellPower =
                PaladinHolySpellSupport.SOURCE_POWER_REFERENCE;
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
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        PaladinBarrierEntity barrier =
                new PaladinBarrierEntity(
                        MartialEntityRegistry
                                .PALADIN_BARRIER
                                .get(),
                        serverLevel,
                        caster
                );
        serverLevel.addFreshEntity(
                barrier
        );

        serverLevel.playSound(
                null,
                caster.blockPosition(),
                MartialSoundRegistry.HOLY_BARRIER_ACTIVATE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        PaladinVfx.barrierSpawn(
                serverLevel,
                caster.position(),
                RANGE
        );

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
                        "ui.martial_spells.radius",
                        RANGE
                ),
                Component.translatable(
                        "ui.martial_spells.effect_length",
                        PaladinBarrierEntity.LIFE_TICKS / 20.0F
                )
        );
    }
}
