package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.RangerSourceVfx;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Source-tier-two, three-shot stash. Arrow transfer is handled at server projectile join. */
public final class PowerShotSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID = ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "power_shot");
    public static final int MAX_LEVEL = 1;
    public static final int COOLDOWN_SECONDS = 8;
    public static final int STASH_TICKS = 240;
    public static final int INITIAL_AMPLIFIER = 2;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL).setCooldownSeconds(COOLDOWN_SECONDS).build();

    public PowerShotSpell() {
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
    @Override public boolean allowLooting() { return false; }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magicData) {
        if (level instanceof ServerLevel server) {
            caster.addEffect(new MobEffectInstance(MartialEffectRegistry.POWER_SHOT_STASH.get(),
                    STASH_TICKS, INITIAL_AMPLIFIER, false, false, true));
            RangerSourceVfx.powerShotRelease(server, caster);
            server.playSound(null, caster.blockPosition(), MartialSoundRegistry.MARKER_SHOT.get(),
                    SoundSource.PLAYERS, 0.5F, 0.9F + server.getRandom().nextFloat() * 0.2F);
        }
        super.onCast(level, spellLevel, caster, source, magicData);
    }
}
