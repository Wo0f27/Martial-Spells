package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.spells.StunningStrikeSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import com.w0of26.martialspells.registry.MartialEntityTypeTags;



import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StunningStrikeImpactManager {

    /*
     * Temporary timing for the Better Combat placeholder animations.
     * We can tune this when original Stunning Strike animations exist.
     */
    private static final int IMPACT_DELAY_TICKS = 4;

    /*
     * Stunning duration by spell level:
     *
     * I   = 5.00 seconds
     * II  = 5.75 seconds
     * III = 6.50 seconds
     * IV  = 7.25 seconds
     * V   = 8.00 seconds
     *
     * Minecraft runs at 20 ticks per second.
     */
    private static final int[] STUN_DURATION_TICKS = {
            100,
            115,
            130,
            145,
            160
    };

    /*
     * Iron's Rend II for five seconds.
     *
     * Rend I  = -5% Armor
     * Rend II = -10% Armor
     */
    private static final int REND_DURATION_TICKS = 100;
    private static final int REND_AMPLIFIER = 1;

    /*
     * Verified in the Forge 1.20.1 runtime:
     * /effect give ... turtlecore:stunned works.
     */
    private static final ResourceLocation STUNNED_EFFECT_ID =
            new ResourceLocation(
                    "turtlecore",
                    "stunned"
            );

    private static final Map<UUID, PendingImpact>
            PENDING_IMPACTS =
            new HashMap<>();

    private StunningStrikeImpactManager() {
    }

    public static void begin(
            ServerPlayer player,
            float damage,
            int spellLevel
    ) {
        PENDING_IMPACTS.put(
                player.getUUID(),
                new PendingImpact(
                        player.serverLevel()
                                .getGameTime(),
                        damage,
                        spellLevel
                )
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player
                instanceof ServerPlayer player)) {
            return;
        }

        PendingImpact impact =
                PENDING_IMPACTS.get(
                        player.getUUID()
                );

        if (impact == null) {
            return;
        }

        /*
         * Cancel a pending impact if the caster is no longer
         * capable of completing the technique.
         */
        if (!player.isAlive()
                || player.isDeadOrDying()
                || player.isRemoved()) {

            PENDING_IMPACTS.remove(
                    player.getUUID()
            );

            return;
        }

        long elapsed =
                player.serverLevel()
                        .getGameTime()
                        - impact.startTick;

        if (elapsed < IMPACT_DELAY_TICKS) {
            return;
        }

        /*
         * Remove before processing so this impact cannot
         * accidentally execute twice.
         */
        PENDING_IMPACTS.remove(
                player.getUUID()
        );

        performImpact(
                player,
                impact.damage,
                impact.spellLevel
        );
    }

    private static void performImpact(
            ServerPlayer player,
            float damage,
            int spellLevel
    ) {
        /*
         * Re-raycast at the actual contact frame.
         *
         * Turning away or allowing the opponent to leave
         * range therefore causes Stunning Strike to whiff.
         */
        LivingEntity target =
                StunningStrikeSpell.findTarget(
                        player
                );

        if (target == null) {
            return;
        }

        /*
         * Deal damage through the normal Minecraft/modded
         * damage pipeline.
         */
        boolean damaged =
                target.hurt(
                        MartialDamageTypes
                                .stunningStrike(
                                        player
                                ),
                        damage
                );

        /*
         * Stunning Strike only applies its secondary effects
         * if the actual damage impact succeeds.
         */
        if (!damaged) {
            return;
        }

        applyStunned(
                target,
                player,
                spellLevel
        );

        applyRend(
                target,
                player
        );

        player.serverLevel().playSound(
                null,
                target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS,
                1.0F,
                0.9F
        );
    }

    private static void applyStunned(
            LivingEntity target,
            ServerPlayer player,
            int spellLevel
    ) {
        /*
         * Martial Spells-level stun immunity takes priority.
         *
         * This does not prevent Stunning Strike damage or Rend.
         */
        if (target.getType().is(
                MartialEntityTypeTags.STUN_IMMUNE
        )) {
            return;
        }

        MobEffect stunnedEffect =
                ForgeRegistries.MOB_EFFECTS
                        .getValue(
                                STUNNED_EFFECT_ID
                        );

        if (stunnedEffect == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find TurtleCore Stunned effect: {}",
                    STUNNED_EFFECT_ID
            );

            return;
        }

        int duration =
                getStunDurationTicks(
                        spellLevel
                );

        /*
         * Stun-resistant entities receive half duration.
         *
         * Math.round is used because durations must be whole
         * Minecraft ticks.
         */
        if (target.getType().is(
                MartialEntityTypeTags.STUN_RESISTANT
        )) {
            duration =
                    Math.round(
                            duration * 0.50F
                    );
        }

        /*
         * Normal addEffect is intentional.
         *
         * TurtleCore/entity-specific immunity is still respected
         * even if the entity is not in our own immunity tag.
         */
        target.addEffect(
                new MobEffectInstance(
                        stunnedEffect,
                        duration,
                        0,
                        false,
                        true,
                        true
                ),
                player
        );
    }

    private static int getStunDurationTicks(
            int spellLevel
    ) {
        int clampedLevel =
                Math.max(
                        1,
                        Math.min(
                                spellLevel,
                                STUN_DURATION_TICKS.length
                        )
                );

        return STUN_DURATION_TICKS[
                clampedLevel - 1
                ];
    }

    private static void applyRend(
            LivingEntity target,
            ServerPlayer player
    ) {
        target.addEffect(
                new MobEffectInstance(
                        MobEffectRegistry.REND.get(),
                        REND_DURATION_TICKS,
                        REND_AMPLIFIER,
                        false,
                        true,
                        true
                ),
                player
        );
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        PENDING_IMPACTS.remove(
                event.getEntity().getUUID()
        );
    }

    private static final class PendingImpact {

        private final long startTick;
        private final float damage;
        private final int spellLevel;

        private PendingImpact(
                long startTick,
                float damage,
                int spellLevel
        ) {
            this.startTick = startTick;
            this.damage = damage;
            this.spellLevel = spellLevel;
        }
    }
}