package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.BattleBannerEntity;
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

/** Iron's translation of frozen Paladins Battle Banner. */
public final class PaladinBattleBannerSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "battle_banner"
            );

    public static final float BASE_RADIUS = 3.0F;
    public static final float EXTRA_RADIUS_POWER_COEFFICIENT = 1.0F;
    public static final float EXTRA_RADIUS_POWER_CAP = 4.0F;
    public static final int BASE_COOLDOWN_SECONDS = 45;
    public static final int BASE_MANA_COST = 60;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.LEGENDARY)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinBattleBannerSpell() {
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

    public float getBannerRadius(
            LivingEntity caster
    ) {
        float power =
                PaladinHolySpellSupport
                        .getSourceEquivalentPower(
                                this,
                                1,
                                caster
                        );

        return BASE_RADIUS
                + EXTRA_RADIUS_POWER_COEFFICIENT
                * Math.min(
                        EXTRA_RADIUS_POWER_CAP,
                        power
                );
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
                        2.0D
                );

        BattleBannerEntity banner =
                new BattleBannerEntity(
                        MartialEntityRegistry
                                .BATTLE_BANNER
                                .get(),
                        serverLevel,
                        caster,
                        getBannerRadius(caster),
                        caster.getYRot() + 20.0F
                );
        banner.setPos(
                placement.x,
                placement.y,
                placement.z
        );
        serverLevel.addFreshEntity(
                banner
        );

        serverLevel.playSound(
                null,
                banner.blockPosition(),
                MartialSoundRegistry.BATTLE_BANNER_RELEASE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        PaladinVfx.holyBurstAt(
                serverLevel,
                placement.add(0.0D, 1.0D, 0.0D),
                30,
                0.45D
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
                        getBannerRadius(caster)
                ),
                Component.translatable(
                        "ui.martial_spells.effect_length",
                        BattleBannerEntity.ACTIVE_TICKS / 20.0F
                )
        );
    }
}
