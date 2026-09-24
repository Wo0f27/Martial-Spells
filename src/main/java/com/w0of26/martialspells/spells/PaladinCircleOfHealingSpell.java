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
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Iron's translation of frozen Paladins Circle of Healing. */
public final class PaladinCircleOfHealingSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "circle_of_healing"
            );

    public static final double RANGE = 8.0D;
    public static final double VERTICAL_RANGE_MULTIPLIER = 0.60D;
    public static final int CAST_TIME_TICKS = 10;
    public static final int BASE_COOLDOWN_SECONDS = 10;
    public static final float HEAL_COEFFICIENT = 0.40F;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.EPIC)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinCircleOfHealingSpell() {
        baseManaCost = 40;
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

        PaladinHolySpellSupport.heal(
                this,
                serverLevel,
                spellLevel,
                caster,
                caster,
                HEAL_COEFFICIENT
        );
        PaladinVfx.healPillar(
                serverLevel,
                caster,
                15
        );
        PaladinVfx.holyGlimmer(
                serverLevel,
                caster,
                15,
                0.20D,
                0.25D
        );
        PaladinVfx.circleOfHealingRelease(
                serverLevel,
                caster,
                RANGE
        );

        double verticalRange =
                RANGE * VERTICAL_RANGE_MULTIPLIER;

        AABB search = caster.getBoundingBox().inflate(
                RANGE,
                verticalRange,
                RANGE
        );

        List<LivingEntity> targets =
                serverLevel.getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        target ->
                                target != caster
                                        && target.isAlive()
                                        && !target.isDeadOrDying()
                                        && Utils.shouldHealEntity(
                                                caster,
                                                target
                                        )
                                        && insideSourceArea(
                                                caster,
                                                target,
                                                verticalRange
                                        )
                );

        for (LivingEntity target : targets) {
            PaladinHolySpellSupport.heal(
                    this,
                    serverLevel,
                    spellLevel,
                    caster,
                    target,
                    HEAL_COEFFICIENT
            );
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
        double dx = target.getX() - caster.getX();
        double dz = target.getZ() - caster.getZ();
        double horizontalDistanceSquared =
                dx * dx + dz * dz;

        return horizontalDistanceSquared <= RANGE * RANGE
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
                        "ui.martial_spells.radius",
                        RANGE
                )
        );
    }
}
