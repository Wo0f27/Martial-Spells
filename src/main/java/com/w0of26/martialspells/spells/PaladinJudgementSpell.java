package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.JudgementImpactManager;
import com.w0of26.martialspells.combat.PaladinHybridPower;
import com.w0of26.martialspells.combat.PhysicalMeleePower;
import com.w0of26.martialspells.combat.StunService;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Iron's translation of frozen Paladins Judgement. */
public final class PaladinJudgementSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "judgement"
            );

    public static final int RANGE = 16;
    public static final int CAST_TIME_TICKS = 10;
    public static final int BASE_COOLDOWN_SECONDS = 15;
    public static final double METEOR_LAUNCH_HEIGHT = 12.0D;
    public static final double METEOR_VELOCITY = 1.2D;
    public static final int METEOR_TRAVEL_TICKS = 10;
    public static final double IMPACT_RADIUS = 6.0D;
    public static final float DAMAGE_COEFFICIENT = 0.90F;
    public static final int STUN_DURATION_TICKS = 3 * 20;
    public static final double CONTROL_HEALTH_BASE = 50.0D;
    public static final double CONTROL_POWER_MULTIPLIER = 2.0D;
    public static final float UNDEAD_POWER_MULTIPLIER = 1.50F;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.EPIC)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinJudgementSpell() {
        baseManaCost = 45;
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
        return Utils.preCastTargetHelper(
                level,
                caster,
                magicData,
                this,
                RANGE,
                0.35F,
                false,
                target ->
                        target != caster
                                && !Utils.shouldHealEntity(
                                        caster,
                                        target
                                )
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
                && level instanceof ServerLevel serverLevel) {
            JudgementImpactManager.queue(
                    serverLevel,
                    caster,
                    target,
                    this
            );
        }

        super.onCast(
                level,
                spellLevel,
                caster,
                castSource,
                magicData
        );
    }

    public float getHybridPower(
            LivingEntity caster
    ) {
        return PaladinHybridPower.meleeDominant(
                this,
                caster
        );
    }

    public float getCenterDamage(
            LivingEntity caster
    ) {
        return getHybridPower(caster)
                * DAMAGE_COEFFICIENT;
    }

    public double getControlHealthLimit(
            LivingEntity caster
    ) {
        return PhysicalMeleePower.controlHealthLimit(
                caster,
                CONTROL_HEALTH_BASE,
                CONTROL_POWER_MULTIPLIER
        );
    }

    public void resolveImpact(
            ServerLevel level,
            LivingEntity caster,
            Vec3 center
    ) {
        double squaredRadius =
                IMPACT_RADIUS * IMPACT_RADIUS;

        AABB search =
                AABB.ofSize(
                        center,
                        IMPACT_RADIUS * 2.0D,
                        IMPACT_RADIUS * 2.0D,
                        IMPACT_RADIUS * 2.0D
                );

        double controlLimit =
                getControlHealthLimit(caster);

        List<LivingEntity> targets =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        target ->
                                target.isAlive()
                                        && !target.isDeadOrDying()
                                        && target != caster
                                        && !Utils.shouldHealEntity(
                                                caster,
                                                target
                                        )
                                        && target.position()
                                                .distanceToSqr(center)
                                                <= squaredRadius
                );

        for (LivingEntity target : targets) {
            double distanceSquared =
                    target.position()
                            .distanceToSqr(center);

            float distanceMultiplier =
                    (float) Math.max(
                            0.0D,
                            (squaredRadius - distanceSquared)
                                    / squaredRadius
                    );

            float damage =
                    getCenterDamage(caster)
                            * distanceMultiplier;

            if (target.getMobType() == MobType.UNDEAD) {
                damage *= UNDEAD_POWER_MULTIPLIER;
            }

            DamageSources.applyDamage(
                    target,
                    damage,
                    getDamageSource(caster)
            );

            if (target.getMaxHealth() <= controlLimit) {
                StunService.apply(
                        target,
                        caster,
                        STUN_DURATION_TICKS
                );
            }
        }

        PaladinVfx.judgementImpact(
                level,
                center
        );

        level.playSound(
                null,
                center.x,
                center.y,
                center.z,
                MartialSoundRegistry.JUDGEMENT_IMPACT.get(),
                SoundSource.PLAYERS,
                1.5F,
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
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(
                                getCenterDamage(caster),
                                2
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.radius",
                        IMPACT_RADIUS
                ),
                Component.translatable(
                        "ui.martial_spells.judgement_control_limit",
                        Math.round(
                                getControlHealthLimit(caster)
                        )
                )
        );
    }
}
