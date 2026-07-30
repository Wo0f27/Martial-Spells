package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.effects.GuardiansCryEffect;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class GuardiansCrySpell extends AbstractSpell {
    private static final float RANGE = 12.0F;
    private static final float DURATION_SECONDS = 6.0F;
    private static final int DURATION_TICKS =
            (int) (DURATION_SECONDS * 20.0F);

    private final ResourceLocation spellId =
            new ResourceLocation(
                    MartialSpells.MOD_ID,
                    "guardians_cry"
            );

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.RARE)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(20)
                    .build();

    public GuardiansCrySpell() {
        this.baseManaCost = 40;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
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
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(RANGE, 1)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.effect_length",
                        Utils.stringTruncation(DURATION_SECONDS, 1)
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
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB searchArea =
                caster.getBoundingBox().inflate(RANGE);

        List<Mob> nearbyHostiles =
                level.getEntitiesOfClass(
                        Mob.class,
                        searchArea,
                        mob -> mob instanceof Enemy
                                && mob.isAlive()
                                && !mob.isDeadOrDying()
                                && !mob.isAlliedTo(caster)
                                && mob.canAttack(caster)
                                && mob.distanceToSqr(caster)
                                <= RANGE * RANGE
                );
        int tauntedCount = 0;
        for (Mob mob : nearbyHostiles) {
            boolean alreadyTaunted =
                    mob.hasEffect(MartialEffectRegistry.GUARDIANS_CRY.get());

            // Save the mob's original target only when beginning a fresh taunt.
            if (!alreadyTaunted) {
                LivingEntity previousTarget = mob.getTarget();

                if (previousTarget != null && previousTarget.isAlive()) {
                    mob.getPersistentData().putUUID(
                            GuardiansCryEffect.PREVIOUS_TARGET_UUID_TAG,
                            previousTarget.getUUID()
                    );
                } else {
                    mob.getPersistentData().remove(
                            GuardiansCryEffect.PREVIOUS_TARGET_UUID_TAG
                    );
                }
            }

            MobEffectInstance effectInstance =
                    new MobEffectInstance(
                            MartialEffectRegistry.GUARDIANS_CRY.get(),
                            DURATION_TICKS,
                            0,
                            false,
                            false
                    );

            boolean effectApplied = mob.addEffect(effectInstance);

            // A fresh application failed, likely because the mob is immune.
            if (!effectApplied && !alreadyTaunted) {
                mob.getPersistentData().remove(
                        GuardiansCryEffect.TARGET_UUID_TAG
                );
                mob.getPersistentData().remove(
                        GuardiansCryEffect.PREVIOUS_TARGET_UUID_TAG
                );
                continue;
            }

            // Store or replace the player currently forcing this mob's attention.
            mob.getPersistentData().putUUID(
                    GuardiansCryEffect.TARGET_UUID_TAG,
                    caster.getUUID()
            );

            mob.setTarget(caster);
            mob.setAggressive(true);

            if (caster instanceof Player player) {
                mob.setLastHurtByPlayer(player);
            }

            serverLevel.sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    mob.getX(),
                    mob.getY() + mob.getBbHeight() * 0.75D,
                    mob.getZ(),
                    4,
                    0.3D,
                    0.35D,
                    0.3D,
                    0.02D
            );

            tauntedCount++;
        }

        if (tauntedCount > 0 && caster instanceof Player player) {
            player.addEffect(
                    new MobEffectInstance(
                            MartialEffectRegistry.GUARDIANS_CRY_ACTIVE.get(),
                            DURATION_TICKS,
                            0,
                            false,
                            false,
                            true
                    )
            );
        }
    }
}