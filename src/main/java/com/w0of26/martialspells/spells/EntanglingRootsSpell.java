package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.EntanglingRootsCloud;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Distinct from the existing Entangling Arrow hard-root technique. */
public final class EntanglingRootsSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID = ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "entangling_roots");
    public static final int MAX_LEVEL = 1;
    public static final int COOLDOWN_SECONDS = 18;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL).setCooldownSeconds(COOLDOWN_SECONDS).build();

    public EntanglingRootsSpell() {
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
    @Override public AnimationHolder getCastStartAnimation() { return AnimationHolder.none(); }
    @Override public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magicData) {
        if (level instanceof ServerLevel server) {
            EntanglingRootsCloud cloud = new EntanglingRootsCloud(server, caster);
            cloud.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), 0);
            server.addFreshEntity(cloud);
        }
        super.onCast(level, spellLevel, caster, source, magicData);
    }
}
