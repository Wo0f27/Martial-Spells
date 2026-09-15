package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
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
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.CollisionContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Forge/Iron's translation of the frozen Rogues Shadowstep technique.
 *
 * <p>The source technique requires a harmful aimed target within 15 blocks,
 * moves the caster to the default Spell Engine BEHIND_TARGET distance of one
 * block, then applies the short Shadowstep anti-tracking marker.</p>
 */
public final class ShadowstepSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "shadow_step");

    public static final int MAX_LEVEL = 1;
    public static final float RANGE = 15.0F;
    public static final float BEHIND_TARGET_DISTANCE = 1.0F;
    public static final float GROUND_SEARCH_DEPTH = 1.5F;
    public static final int SHADOWSTEP_DURATION_TICKS = 30;
    public static final double STEALTH_FOLLOW_DISTANCE = 5.0D;
    public static final int BASE_COOLDOWN_SECONDS = 12;
    public static final int DEPART_PARTICLES = 20;
    public static final int ARRIVE_PARTICLES = 10;

    private static final float AIM_ASSIST = 0.35F;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public ShadowstepSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.ROGUE;
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
        // Played manually at the departure position before the teleport.
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        // Dependency-native translation of Spell Engine's one_handed_area_release.
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.martial_spells.shadow_step_range", RANGE),
                Component.translatable("ui.martial_spells.shadow_step_distance", BEHIND_TARGET_DISTANCE),
                Component.translatable(
                        "ui.martial_spells.shadow_step_untraceable",
                        SHADOWSTEP_DURATION_TICKS / 20.0F,
                        (int) STEALTH_FOLLOW_DISTANCE
                ),
                Component.translatable("ui.martial_spells.base_cooldown", BASE_COOLDOWN_SECONDS)
        );
    }

    @Override
    public boolean checkPreCastConditions(
            Level level,
            int spellLevel,
            LivingEntity caster,
            MagicData playerMagicData
    ) {
        boolean hasTarget = Utils.preCastTargetHelper(
                level,
                caster,
                playerMagicData,
                this,
                (int) RANGE,
                AIM_ASSIST,
                true,
                target -> target != caster && !caster.isAlliedTo(target)
        );

        if (!hasTarget) {
            return false;
        }

        if (level instanceof ServerLevel serverLevel
                && playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget(serverLevel);
            if (target == null || resolveSafeDestination(serverLevel, caster, target).isEmpty()) {
                if (caster instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("ui.martial_spells.shadow_step_blocked"),
                            true
                    );
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)) {
            return;
        }

        LivingEntity target = targetData.getTarget(serverLevel);
        if (target == null || target == caster || caster.isAlliedTo(target)) {
            return;
        }

        Optional<Vec3> destinationResult = resolveSafeDestination(serverLevel, caster, target);
        if (destinationResult.isEmpty()) {
            return;
        }

        Vec3 departure = caster.position();
        Vec3 destination = destinationResult.get();

        serverLevel.playSound(
                null,
                departure.x,
                departure.y,
                departure.z,
                MartialSoundRegistry.SHADOW_STEP_DEPART.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                departure.x,
                departure.y + caster.getBbHeight() * 0.5D,
                departure.z,
                DEPART_PARTICLES,
                0.45D,
                0.55D,
                0.45D,
                0.075D
        );
        caster.gameEvent(GameEvent.TELEPORT);

        float targetYaw = target.getYRot();
        float casterPitch = caster.getXRot();
        if (caster instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(
                    serverLevel,
                    destination.x,
                    destination.y,
                    destination.z,
                    targetYaw,
                    casterPitch
            );
        } else {
            caster.teleportTo(destination.x, destination.y, destination.z);
            caster.setYRot(targetYaw);
            caster.setYHeadRot(targetYaw);
            caster.setYBodyRot(targetYaw);
        }

        serverLevel.sendParticles(
                ParticleTypes.POOF,
                destination.x,
                destination.y + caster.getBbHeight() * 0.5D,
                destination.z,
                ARRIVE_PARTICLES,
                0.35D,
                0.45D,
                0.35D,
                0.075D
        );

        caster.addEffect(new MobEffectInstance(
                MartialEffectRegistry.SHADOW_STEP.get(),
                SHADOWSTEP_DURATION_TICKS,
                0,
                false,
                true,
                true
        ));

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    private static Optional<Vec3> resolveSafeDestination(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target
    ) {
        Vec3 desired = target.position().add(
                target.getLookAngle().scale(-BEHIND_TARGET_DISTANCE)
        );

        HitResult groundHit = level.clip(new ClipContext(
                desired,
                desired.add(0.0D, -GROUND_SEARCH_DEPTH, 0.0D),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.of(caster)
        ));

        Vec3 candidate = groundHit.getType() == HitResult.Type.BLOCK
                ? new Vec3(desired.x, groundHit.getLocation().y, desired.z)
                : desired;

        if (candidate.y < level.getMinBuildHeight()
                || candidate.y + caster.getBbHeight() > level.getMaxBuildHeight()) {
            return Optional.empty();
        }

        BlockPos candidatePos = BlockPos.containing(candidate);
        if (!level.getWorldBorder().isWithinBounds(candidatePos)) {
            return Optional.empty();
        }

        Vec3 offset = candidate.subtract(caster.position());
        AABB destinationBox = caster.getBoundingBox().move(offset);
        if (!level.noCollision(caster, destinationBox)) {
            return Optional.empty();
        }

        return Optional.of(candidate);
    }
}
