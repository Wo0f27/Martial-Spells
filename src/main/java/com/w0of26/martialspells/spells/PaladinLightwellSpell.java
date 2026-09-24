package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.LightwellEntity;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Iron's translation of frozen Paladins Lightwell. */
public final class PaladinLightwellSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "lightwell"
            );

    public static final int BASE_COOLDOWN_SECONDS = 45;
    public static final int BASE_MANA_COST = 70;
    public static final float WELL_BASE_HEALING_POWER = 1.0F;
    public static final float OWNER_POWER_COEFFICIENT = 0.50F;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.LEGENDARY)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinLightwellSpell() {
        baseManaCost = BASE_MANA_COST;
        manaCostPerLevel = 0;
        baseSpellPower =
                PaladinHolySpellSupport.SOURCE_POWER_REFERENCE;
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

    public float getWellHealingPower(
            LivingEntity caster
    ) {
        return WELL_BASE_HEALING_POWER
                + getSpellPower(
                        1,
                        caster
                )
                * OWNER_POWER_COEFFICIENT;
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

        Vec3 placement =
                PaladinP4Placement.aheadOnGround(
                        serverLevel,
                        caster,
                        1.5D
                );

        LightwellEntity lightwell =
                new LightwellEntity(
                        MartialEntityRegistry
                                .LIGHTWELL
                                .get(),
                        serverLevel,
                        caster,
                        getWellHealingPower(caster)
                );
        lightwell.setPos(
                placement.x,
                placement.y,
                placement.z
        );
        serverLevel.addFreshEntity(
                lightwell
        );

        serverLevel.playSound(
                null,
                lightwell.blockPosition(),
                MartialSoundRegistry.LIGHTWELL_SPAWN.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        PaladinVfx.lightwellSpawn(
                serverLevel,
                placement
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
                        "ui.martial_spells.effect_length",
                        LightwellEntity.ACTIVE_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.lightwell_mote_heal",
                        getWellHealingPower(caster) * 0.35F
                ),
                Component.translatable(
                        "ui.martial_spells.paladin_target_range",
                        (int) LightwellEntity.HEAL_RANGE
                )
        );
    }
}
