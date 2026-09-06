package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEntityTypeTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Shared Martial Spells stun application rules.
 *
 * Callers provide their desired base duration. This service owns the common
 * TurtleCore effect lookup plus Martial Spells stun-immunity/resistance rules,
 * so techniques and external integrations do not need to duplicate them.
 */
public final class StunService {

    private static final ResourceLocation STUNNED_EFFECT_ID =
            new ResourceLocation("turtlecore", "stunned");

    private StunService() {
    }

    /**
     * Applies TurtleCore's Stunned effect using Martial Spells compatibility
     * rules.
     *
     * @return true when the effect was accepted by the target; false when the
     * target is immune, invalid, the effect is unavailable, or application was
     * rejected by the target/modded effect pipeline.
     */
    public static boolean apply(
            LivingEntity target,
            Entity source,
            int baseDurationTicks
    ) {
        if (target == null
                || !target.isAlive()
                || target.isDeadOrDying()
                || target.isRemoved()
                || baseDurationTicks <= 0) {
            return false;
        }

        if (target.getType().is(MartialEntityTypeTags.STUN_IMMUNE)) {
            return false;
        }

        MobEffect stunnedEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(STUNNED_EFFECT_ID);

        if (stunnedEffect == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find TurtleCore Stunned effect: {}",
                    STUNNED_EFFECT_ID
            );
            return false;
        }

        int duration = baseDurationTicks;
        if (target.getType().is(MartialEntityTypeTags.STUN_RESISTANT)) {
            duration = Math.max(1, Math.round(duration * 0.50F));
        }

        return target.addEffect(
                new MobEffectInstance(
                        stunnedEffect,
                        duration,
                        0,
                        false,
                        true,
                        true
                ),
                source
        );
    }
}
