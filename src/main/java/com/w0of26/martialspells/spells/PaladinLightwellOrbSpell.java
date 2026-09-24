package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Internal-only Holy Mote spell identity used by Lightwell healing events.
 * Delivery is owned by LightwellEntity/HolyMoteProjectile, not player casting.
 */
public final class PaladinLightwellOrbSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "lightwell_orb"
            );

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.COMMON)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(1.5F)
                    .build();

    public PaladinLightwellOrbSpell() {
        baseManaCost = 0;
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

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        // Internal helper only; Lightwell owns delivery.
    }
}
