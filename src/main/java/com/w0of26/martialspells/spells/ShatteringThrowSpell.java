package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.PhysicalMeleePower;
import com.w0of26.martialspells.entity.ShatteringThrowProjectile;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import com.w0of26.martialspells.technique.ReleaseChargedTechnique;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Forge/Iron's translation of frozen Rogues Shattering Throw. */
public final class ShatteringThrowSpell extends AbstractSpell
        implements MartialTechnique, ReleaseChargedTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "shattering_throw"
            );

    public static final int MAX_LEVEL = 1;
    public static final int CAST_TIME_TICKS = 10;
    public static final float MIN_RELEASE_RATIO = 0.20F;
    public static final float OUTPUT_SCALING = 1.0F;
    public static final float BASE_RANGE = 12.0F;
    public static final float CHARGE_RANGE_BONUS = 12.0F;
    public static final float PROJECTILE_VELOCITY = 0.8F;
    public static final float HOMING_DEGREES_PER_TICK = 2.0F;
    public static final float SPIN_DEGREES_PER_TICK = -36.0F;
    public static final int BOUNCES = 1;
    public static final double DAMAGE_COEFFICIENT = 1.0D;
    public static final float KNOCKBACK_COEFFICIENT = 1.0F;
    public static final int SHATTER_DURATION_TICKS = 160;
    public static final double CONTROL_HEALTH_BASE = 100.0D;
    public static final double CONTROL_POWER_MULTIPLIER = 2.0D;
    public static final int BASE_COOLDOWN_SECONDS = 8;

    private static final Map<UUID, Float> RELEASE_CHARGE =
            new ConcurrentHashMap<>();

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public ShatteringThrowSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = CAST_TIME_TICKS;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.WARRIOR;
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
        return SpellAnimations.ANIMATION_LONG_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.THROW_SINGLE_ITEM;
    }

    @Override
    public boolean releaseOnUseStop(
            ServerPlayer player,
            MagicData magicData
    ) {
        if (!magicData.isCasting()
                || magicData.getCastingSpell().getSpell() != this) {
            return false;
        }

        float chargeRatio = Mth.clamp(
                magicData.getCastCompletionPercent(),
                0.0F,
                1.0F
        );
        if (chargeRatio < MIN_RELEASE_RATIO) {
            return false;
        }

        RELEASE_CHARGE.put(player.getUUID(), chargeRatio);

        int spellLevel = magicData.getCastingSpellLevel();
        CastSource castSource = magicData.getCastSource();

        castSpell(player.level(), spellLevel, player, castSource, true);
        if (castSource == CastSource.SCROLL) {
            Scroll.attemptRemoveScrollAfterCast(player);
        }
        onServerCastComplete(
                player.level(),
                spellLevel,
                player,
                magicData,
                false
        );
        player.stopUsingItem();
        return true;
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

        float chargeRatio = Mth.clamp(
                RELEASE_CHARGE.getOrDefault(caster.getUUID(), 1.0F),
                MIN_RELEASE_RATIO,
                1.0F
        );

        float outputMultiplier =
                1.0F - OUTPUT_SCALING * (1.0F - chargeRatio);
        float range = BASE_RANGE + CHARGE_RANGE_BONUS * chargeRatio;

        double physicalMeleePower = PhysicalMeleePower.get(caster);
        float damage = (float) (
                physicalMeleePower
                        * DAMAGE_COEFFICIENT
                        * outputMultiplier
        );
        double controlHealthLimit =
                CONTROL_HEALTH_BASE
                        + CONTROL_POWER_MULTIPLIER * physicalMeleePower;

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 launchPoint = caster.position()
                .add(
                        0.0D,
                        caster.getEyeHeight()
                                - caster.getBbHeight() * 0.15D,
                        0.0D
                )
                .add(look.scale(0.5D));

        ItemStack held = caster.getMainHandItem();
        ResourceLocation heldItemId =
                BuiltInRegistries.ITEM.getKey(held.getItem());

        ShatteringThrowProjectile projectile =
                new ShatteringThrowProjectile(
                        MartialEntityRegistry.SHATTERING_THROW.get(),
                        serverLevel,
                        caster,
                        damage,
                        controlHealthLimit,
                        range,
                        KNOCKBACK_COEFFICIENT * outputMultiplier,
                        BOUNCES,
                        findAimTarget(caster, range),
                        heldItemId == null ? "" : heldItemId.toString()
                );

        projectile.setPos(
                launchPoint.x,
                launchPoint.y,
                launchPoint.z
        );
        projectile.shoot(
                look.x,
                look.y,
                look.z,
                PROJECTILE_VELOCITY,
                0.0F
        );
        serverLevel.addFreshEntity(projectile);

        serverLevel.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                MartialSoundRegistry.THROW.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
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
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData magicData,
            boolean cancelled
    ) {
        RELEASE_CHARGE.remove(entity.getUUID());
        super.onServerCastComplete(
                level,
                spellLevel,
                entity,
                magicData,
                cancelled
        );
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        double physicalMeleePower = PhysicalMeleePower.get(caster);
        double controlLimit =
                CONTROL_HEALTH_BASE
                        + CONTROL_POWER_MULTIPLIER * physicalMeleePower;

        return List.of(
                Component.translatable(
                        "ui.martial_spells.shattering_throw_charge",
                        CAST_TIME_TICKS / 20.0F,
                        Math.round(MIN_RELEASE_RATIO * 100.0F)
                ),
                Component.translatable(
                        "ui.martial_spells.shattering_throw_range",
                        BASE_RANGE,
                        BASE_RANGE + CHARGE_RANGE_BONUS
                ),
                Component.translatable(
                        "ui.martial_spells.shattering_throw_damage",
                        Math.round(DAMAGE_COEFFICIENT * 100.0D)
                ),
                Component.translatable(
                        "ui.martial_spells.shattering_throw_shatter",
                        SHATTER_DURATION_TICKS / 20.0F,
                        30
                ),
                Component.translatable(
                        "ui.martial_spells.shattering_throw_control_limit",
                        Math.round(controlLimit)
                ),
                Component.translatable(
                        "ui.martial_spells.shattering_throw_bounces",
                        BOUNCES
                ),
                Component.translatable(
                        "ui.martial_spells.base_cooldown",
                        BASE_COOLDOWN_SECONDS
                )
        );
    }

    private static UUID findAimTarget(
            LivingEntity caster,
            float range
    ) {
        HitResult hitResult = RaycastBuilder
                .begin(caster.level(), caster)
                .range(range)
                .checkForBlocks(true)
                .bbInflation(0.30F)
                .filter(entity ->
                        entity instanceof LivingEntity living
                                && living.isAlive()
                                && !living.isSpectator()
                                && living != caster
                                && !caster.isAlliedTo(living)
                )
                .build();

        if (hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target) {
            return target.getUUID();
        }
        return null;
    }
}
