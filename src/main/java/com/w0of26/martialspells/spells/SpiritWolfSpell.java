package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.SpiritWolfPending;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Two owner-bound Direwolves, prepared at release and inserted at 0/+5 ticks. */
public final class SpiritWolfSpell extends AbstractSpell implements MartialTechnique, FixedCooldownSpell {
    public static final ResourceLocation SPELL_ID = ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "spirit_wolf");
    private static final ResourceLocation RELEASE_ANIMATION = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "one_handed_healing_release");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(1).setCooldownSeconds(45).build();

    public SpiritWolfSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override public MartialTechniqueClass getTechniqueClass() { return MartialTechniqueClass.RANGER; }
    @Override public ResourceLocation getSpellResource() { return SPELL_ID; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public int getMaxLevel() { return 1; }
    @Override public int getSpellCooldown() { return 900; }
    @Override public boolean allowLooting() { return false; }
    @Override public AnimationHolder getCastStartAnimation() { return AnimationHolder.none(); }
    @Override public AnimationHolder getCastFinishAnimation() {
        return new AnimationHolder(RELEASE_ANIMATION, true, false);
    }

    @Override public void onCast(Level level, int spellLevel, LivingEntity caster,
                                 CastSource source, MagicData magicData) {
        if (level instanceof ServerLevel server && caster instanceof ServerPlayer player) {
            SpiritWolfPending.begin(server, player);
            server.playSound(null, caster.blockPosition(), MartialSoundRegistry.SPIRIT_WOLF_SUMMON.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        super.onCast(level, spellLevel, caster, source, magicData);
    }
}
