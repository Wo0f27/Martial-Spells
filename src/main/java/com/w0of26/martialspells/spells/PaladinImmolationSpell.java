package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.PaladinHybridPower;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.particles.HealParticlesPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/** Iron's translation of frozen Paladins Immolation. */
public final class PaladinImmolationSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "immolation"
            );

    public static final double RANGE = 5.0D;
    public static final double VERTICAL_RANGE_MULTIPLIER = 0.50D;
    public static final int BASE_COOLDOWN_SECONDS = 12;
    public static final float DAMAGE_COEFFICIENT = 1.20F;
    public static final float HEAL_COEFFICIENT = 0.50F;
    public static final int FIRE_DURATION_SECONDS = 4;
    public static final float UNDEAD_POWER_MULTIPLIER = 1.50F;
    public static final float SOURCE_DEFAULT_CRITICAL_MULTIPLIER = 1.50F;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.LEGENDARY)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinImmolationSpell() {
        baseManaCost = 60;
        manaCostPerLevel = 0;
        baseSpellPower = PaladinHolySpellSupport.SOURCE_POWER_REFERENCE;
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

    public float getHybridPower(
            LivingEntity caster
    ) {
        return PaladinHybridPower.holyDominant(
                this,
                caster
        );
    }

    public float getDamage(
            LivingEntity caster
    ) {
        return getHybridPower(caster)
                * DAMAGE_COEFFICIENT;
    }

    public float getHealing(
            LivingEntity caster
    ) {
        return getHybridPower(caster)
                * HEAL_COEFFICIENT;
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

        double verticalRange =
                RANGE * VERTICAL_RANGE_MULTIPLIER;

        AABB search =
                caster.getBoundingBox().inflate(
                        RANGE,
                        verticalRange,
                        RANGE
                );

        List<LivingEntity> targets =
                serverLevel.getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        target ->
                                target.isAlive()
                                        && !target.isDeadOrDying()
                                        && insideSourceArea(
                                                caster,
                                                target,
                                                verticalRange
                                        )
                );

        float baseDamage =
                getDamage(caster);

        float healAmount =
                getHealing(caster);

        for (LivingEntity target : targets) {
            boolean helpful =
                    target == caster
                            || Utils.shouldHealEntity(
                                    caster,
                                    target
                            );

            if (helpful) {
                MinecraftForge.EVENT_BUS.post(
                        new SpellHealEvent(
                                caster,
                                target,
                                healAmount,
                                getSchoolType()
                        )
                );

                target.heal(healAmount);

                PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                        target,
                        new HealParticlesPacket(
                                target.position()
                        )
                );

                continue;
            }

            float damage =
                    baseDamage;

            if (target.getMobType() == MobType.UNDEAD) {
                // Frozen source adds +50% power and guarantees a critical.
                // Iron's 1.20.1 has no spell-crit attribute surface, so
                // preserve the source default 1.5x critical multiplier.
                damage *= UNDEAD_POWER_MULTIPLIER
                        * SOURCE_DEFAULT_CRITICAL_MULTIPLIER;
            }

            boolean damaged =
                    DamageSources.applyDamage(
                            target,
                            damage,
                            getDamageSource(caster)
                    );

            // Frozen FIRE is an independent harmful impact, not conditional
            // on the preceding DAMAGE impact succeeding.
            target.setSecondsOnFire(
                    FIRE_DURATION_SECONDS
            );

            if (damaged) {
                serverLevel.playSound(
                        null,
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        MartialSoundRegistry.HOLY_SHOCK_DAMAGE.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
        }

        serverLevel.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                MartialSoundRegistry.IMMOLATION_RELEASE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                caster.getX(),
                caster.getY() + 0.5D,
                caster.getZ(),
                60,
                RANGE * 0.55D,
                1.0D,
                RANGE * 0.55D,
                0.12D
        );

        serverLevel.sendParticles(
                ParticleTypes.FLAME,
                caster.getX(),
                caster.getY() + 0.25D,
                caster.getZ(),
                40,
                RANGE * 0.45D,
                0.5D,
                RANGE * 0.45D,
                0.08D
        );

        super.onCast(
                level,
                spellLevel,
                caster,
                castSource,
                magicData
        );
    }

    private static boolean insideSourceArea(
            LivingEntity caster,
            LivingEntity target,
            double verticalRange
    ) {
        double dx =
                target.getX() - caster.getX();

        double dz =
                target.getZ() - caster.getZ();

        return dx * dx + dz * dz
                <= RANGE * RANGE
                && Math.abs(
                        target.getY() - caster.getY()
                ) <= verticalRange;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(
                                getDamage(caster),
                                2
                        )
                ),
                Component.translatable(
                        "ui.irons_spellbooks.healing",
                        Utils.stringTruncation(
                                getHealing(caster),
                                2
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.radius",
                        RANGE
                )
        );
    }
}
