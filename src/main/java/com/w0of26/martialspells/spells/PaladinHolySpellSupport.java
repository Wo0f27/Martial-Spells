package com.w0of26.martialspells.spells;

import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.particles.HealParticlesPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Shared Iron's 1.20.1 targeting/healing bridge for the frozen Paladins
 * healing-family spell port.
 */
final class PaladinHolySpellSupport {
    static final float SOURCE_POWER_REFERENCE = 5.0F;
    static final float AIM_ASSIST = 0.35F;

    private PaladinHolySpellSupport() {
    }

    static boolean targetFriendlyOrSelf(
            Level level,
            LivingEntity caster,
            MagicData magicData,
            AbstractSpell spell,
            int range
    ) {
        boolean found = Utils.preCastTargetHelper(
                level,
                caster,
                magicData,
                spell,
                range,
                AIM_ASSIST,
                false,
                target -> Utils.shouldHealEntity(caster, target)
        );

        if (!found) {
            magicData.setAdditionalCastData(
                    new TargetEntityCastData(caster)
            );
        }

        return true;
    }

    static boolean targetAnyOrSelf(
            Level level,
            LivingEntity caster,
            MagicData magicData,
            AbstractSpell spell,
            int range
    ) {
        boolean found = Utils.preCastTargetHelper(
                level,
                caster,
                magicData,
                spell,
                range,
                AIM_ASSIST,
                false
        );

        if (!found) {
            magicData.setAdditionalCastData(
                    new TargetEntityCastData(caster)
            );
        }

        return true;
    }

    @Nullable
    static LivingEntity getTarget(
            Level level,
            MagicData magicData
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (!(magicData.getAdditionalCastData()
                instanceof TargetEntityCastData targetData)) {
            return null;
        }

        return targetData.getTarget(serverLevel);
    }

    static float getScaledAmount(
            AbstractSpell spell,
            int spellLevel,
            LivingEntity caster,
            float sourceCoefficient
    ) {
        return spell.getSpellPower(spellLevel, caster)
                * sourceCoefficient;
    }

    static void heal(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            LivingEntity caster,
            LivingEntity target,
            float sourceCoefficient
    ) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        float amount = getScaledAmount(
                spell,
                spellLevel,
                caster,
                sourceCoefficient
        );

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new SpellHealEvent(
                        caster,
                        target,
                        amount,
                        spell.getSchoolType()
                )
        );

        target.heal(amount);

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                target,
                new HealParticlesPacket(target.position())
        );
    }

    static boolean damage(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            LivingEntity caster,
            LivingEntity target,
            float sourceCoefficient
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        float amount = getScaledAmount(
                spell,
                spellLevel,
                caster,
                sourceCoefficient
        );

        boolean damaged = DamageSources.applyDamage(
                target,
                amount,
                spell.getDamageSource(caster)
        );

        if (damaged) {
            MagicManager.spawnParticles(
                    serverLevel,
                    ParticleTypes.END_ROD,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    18,
                    target.getBbWidth() * 0.35D,
                    target.getBbHeight() * 0.25D,
                    target.getBbWidth() * 0.35D,
                    0.08D,
                    false
            );
        }

        return damaged;
    }
}
