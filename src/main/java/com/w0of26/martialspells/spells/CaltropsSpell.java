package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.CaltropBundleProjectile;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class CaltropsSpell extends AbstractSpell
        implements MartialTechnique, FixedCooldownSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "caltrops"
            );

    public static final int MAX_LEVEL = 5;
    public static final int COOLDOWN_SECONDS = 15;
    public static final int DAMAGE_INTERVAL_TICKS = 10;

    private static final float THROW_VELOCITY = 1.5F;
    private static final float THROW_INACCURACY = 1.0F;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.COMMON)
                    .setSchoolResource(
                            MartialSchoolRegistry.MARTIAL_RESOURCE
                    )
                    .setMaxLevel(MAX_LEVEL)
                    .setCooldownSeconds(COOLDOWN_SECONDS)
                    .build();

    public CaltropsSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.RANGER;
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
    public boolean allowLooting() {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    public static int clampLevel(int spellLevel) {
        return Mth.clamp(spellLevel, 1, MAX_LEVEL);
    }

    public static int getFieldSize(int spellLevel) {
        return switch (clampLevel(spellLevel)) {
            case 4 -> 4;
            case 5 -> 5;
            default -> 3;
        };
    }

    public static float getDamagePerTick(int spellLevel) {
        return switch (clampLevel(spellLevel)) {
            case 1 -> 2.0F;
            case 2 -> 2.5F;
            case 3 -> 3.0F;
            case 4 -> 3.5F;
            default -> 4.0F;
        };
    }

    public static int getSlownessAmplifier(int spellLevel) {
        return clampLevel(spellLevel) >= 3 ? 1 : 0;
    }

    public static String getSlownessLevelName(int spellLevel) {
        return getSlownessAmplifier(spellLevel) == 0 ? "I" : "II";
    }

    public static int getDurationSeconds(int spellLevel) {
        return switch (clampLevel(spellLevel)) {
            case 1 -> 8;
            case 2 -> 10;
            case 3 -> 12;
            case 4 -> 14;
            default -> 16;
        };
    }

    public static int getDurationTicks(int spellLevel) {
        return getDurationSeconds(spellLevel) * 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        int size = getFieldSize(spellLevel);

        return List.of(
                Component.translatable(
                        "ui.martial_spells.caltrops_field_size",
                        size,
                        size
                ),
                Component.translatable(
                        "ui.martial_spells.caltrops_damage_tick",
                        getDamagePerTick(spellLevel)
                ),
                Component.translatable(
                        "ui.martial_spells.caltrops_slow",
                        getSlownessLevelName(spellLevel)
                ),
                Component.translatable(
                        "ui.martial_spells.caltrops_duration",
                        getDurationSeconds(spellLevel)
                ),
                Component.translatable(
                        "ui.martial_spells.scales_with_martial_power"
                ),
                Component.translatable(
                        "ui.martial_spells.fixed_cooldown",
                        COOLDOWN_SECONDS
                ),
                Component.translatable(
                        "ui.martial_spells.cooldown_reduction_immune"
                )
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
        if (!level.isClientSide
                && caster instanceof ServerPlayer player) {
            CaltropBundleProjectile bundle =
                    new CaltropBundleProjectile(
                            level,
                            player,
                            clampLevel(spellLevel)
                    );

            bundle.shootFromRotation(
                    player,
                    player.getXRot(),
                    player.getYRot(),
                    0.0F,
                    THROW_VELOCITY,
                    THROW_INACCURACY
            );

            level.addFreshEntity(bundle);

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS,
                    0.8F,
                    0.9F
                            + player.getRandom().nextFloat()
                            * 0.2F
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
}
