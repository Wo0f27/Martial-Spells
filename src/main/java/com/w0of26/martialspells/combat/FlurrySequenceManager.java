package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.spells.FlurryOfBlowsSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class FlurrySequenceManager {
    /*
     * Strike timings relative to the moment the 10-tick wind-up
     * successfully finishes.
     *
     * Level I:   2 strikes
     * Level II:  3 strikes
     * Level III: 4 strikes
     * Level IV:  5 strikes
     * Level V:   6 strikes
     *
     * These timings should later be aligned with the exact contact
     * frames of each level's animation.
     */
    private static final int[][] STRIKE_TICKS = {
            {3, 7},
            {3, 7, 11},
            {3, 7, 11, 15},
            {3, 7, 11, 15, 19},
            {3, 7, 11, 15, 19, 23}
    };

    private static final SimpleParticleType PUNCH_PARTICLE =
            ParticleTypes.CLOUD;
    /*
     * Flurry permits movement, but reduces movement speed to 65%
     * while the active punch sequence is running.
     *
     * MULTIPLY_TOTAL with -0.35 means:
     * 100% - 35% = 65% movement speed.
     */
    private static final UUID MOVEMENT_SLOWDOWN_ID =
            UUID.fromString(
                    "4580dfaa-aec2-4ced-a39a-4243cc061f10"
            );

    private static final AttributeModifier
            MOVEMENT_SLOWDOWN =
            new AttributeModifier(
                    MOVEMENT_SLOWDOWN_ID,
                    MartialSpells.MOD_ID
                            + ".flurry_movement_slowdown",
                    -0.35D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );

    private static final Map<UUID, ActiveSequence>
            ACTIVE_SEQUENCES = new HashMap<>();

    private FlurrySequenceManager() {
    }

    /**
     * Starts a target-independent Flurry sequence.
     *
     * No target is stored. Every scheduled strike performs a new
     * raycast using the player's current position and look direction.
     */
    public static void begin(
            ServerPlayer player,
            int spellLevel,
            float damagePerStrike
    ) {
        int levelIndex =
                Math.max(
                        0,
                        Math.min(
                                spellLevel - 1,
                                STRIKE_TICKS.length - 1
                        )
                );

        /*
         * Prevent duplicate movement modifiers or overlapping
         * sequences, including command-cast testing.
         */
        stopSequence(player);

        applyMovementSlowdown(player);

        ACTIVE_SEQUENCES.put(
                player.getUUID(),
                new ActiveSequence(
                        player.level().dimension(),
                        player.serverLevel().getGameTime(),
                        levelIndex,
                        damagePerStrike
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

        ActiveSequence sequence =
                ACTIVE_SEQUENCES.get(
                        player.getUUID()
                );

        if (sequence == null) {
            return;
        }

        /*
         * Cancel the remaining sequence when the caster can no
         * longer validly perform the technique.
         */
        if (!player.isAlive()
                || player.isDeadOrDying()
                || player.isRemoved()
                || !player.level()
                .dimension()
                .equals(sequence.startDimension)
                || !MonkWeaponHelper
                .hasValidMainHand(player)) {
            stopSequence(player);
            return;
        }

        long elapsedTicks =
                player.serverLevel().getGameTime()
                        - sequence.startTick;

        int[] strikeSchedule =
                STRIKE_TICKS[sequence.levelIndex];

        spawnHandParticles(
                player,
                elapsedTicks,
                strikeSchedule
        );

        /*
         * The while loop allows the sequence to catch up when the
         * server skips ticks under load.
         */
        while (sequence.nextStrikeIndex
                < strikeSchedule.length
                && elapsedTicks
                >= strikeSchedule[
                sequence.nextStrikeIndex
                ]) {

            while (sequence.nextStrikeIndex
                    < strikeSchedule.length
                    && elapsedTicks
                    >= strikeSchedule[
                    sequence.nextStrikeIndex
                    ]) {

                performStrike(
                        player,
                        sequence.damagePerStrike
                );

                sequence.nextStrikeIndex++;
            }

            performStrike(
                    player,
                    sequence.damagePerStrike
            );

            sequence.nextStrikeIndex++;
        }

        if (sequence.nextStrikeIndex
                >= strikeSchedule.length) {
            stopSequence(player);
        }
    }

    /**
     * Each strike searches for a target independently.
     *
     * Missing does not cancel the sequence. The player may turn
     * toward another enemy before the next strike.
     */
    private static void performStrike(
            ServerPlayer player,
            float damage
    ) {
        LivingEntity target =
                FlurryOfBlowsSpell.findTarget(player);

        /*
         * This punch whiffs, but later punches still occur.
         */
        if (target == null) {
            return;
        }

        boolean damaged =
                target.hurt(
                        MartialDamageTypes
                                .flurryOfBlows(player),
                        damage
                );

        /*
         * Shields, invulnerability, event cancellation, or another
         * system may still prevent the damage.
         *
         * The remaining sequence continues regardless.
         */
        if (!damaged) {
            return;
        }

        player.serverLevel().playSound(
                null,
                target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS,
                0.8F,
                1.1F
        );
    }

    private static void applyMovementSlowdown(
            ServerPlayer player
    ) {
        AttributeInstance movementSpeed =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (movementSpeed == null) {
            return;
        }

        /*
         * Remove a stale copy before adding the modifier.
         */
        movementSpeed.removeModifier(
                MOVEMENT_SLOWDOWN_ID
        );

        movementSpeed.addTransientModifier(
                MOVEMENT_SLOWDOWN
        );
    }

    private static void removeMovementSlowdown(
            ServerPlayer player
    ) {
        AttributeInstance movementSpeed =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (movementSpeed != null) {
            movementSpeed.removeModifier(
                    MOVEMENT_SLOWDOWN_ID
            );
        }
    }

    private static void stopSequence(
            ServerPlayer player
    ) {
        ACTIVE_SEQUENCES.remove(
                player.getUUID()
        );

        removeMovementSlowdown(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            stopSequence(player);
        } else {
            ACTIVE_SEQUENCES.remove(
                    event.getEntity().getUUID()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            stopSequence(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(
            PlayerEvent.Clone event
    ) {
        /*
         * Prevent a sequence from carrying across death and respawn.
         * Transient attribute modifiers do not need to be copied.
         */
        ACTIVE_SEQUENCES.remove(
                event.getEntity().getUUID()
        );
    }

    private static final class ActiveSequence {
        private final ResourceKey<Level> startDimension;
        private final long startTick;
        private final int levelIndex;
        private final float damagePerStrike;

        private int nextStrikeIndex;

        private ActiveSequence(
                ResourceKey<Level> startDimension,
                long startTick,
                int levelIndex,
                float damagePerStrike
        ) {
            this.startDimension = startDimension;
            this.startTick = startTick;
            this.levelIndex = levelIndex;
            this.damagePerStrike = damagePerStrike;
            this.nextStrikeIndex = 0;
        }
    }

    private static void spawnHandParticles(
            ServerPlayer player,
            long elapsedTicks,
            int[] strikeSchedule
    ) {
        int punchIndex = -1;
        long punchStartTick = 0L;

        /*
         * Every punch begins three ticks before its damage-contact tick.
         *
         * Contact ticks: 3, 7, 11...
         * Punch starts:  0, 4, 8...
         */
        for (int index = 0;
             index < strikeSchedule.length;
             index++) {

            long startTick =
                    strikeSchedule[index] - 3L;

            long contactTick =
                    strikeSchedule[index];

            if (elapsedTicks >= startTick
                    && elapsedTicks <= contactTick) {
                punchIndex = index;
                punchStartTick = startTick;
                break;
            }
        }

        /*
         * No punch is currently active.
         */
        if (punchIndex < 0) {
            return;
        }

        boolean offhandPunch =
                punchIndex % 2 == 1;

        Vec3 lookDirection =
                player.getLookAngle().normalize();

        double yawRadians =
                Math.toRadians(player.getYRot());

        /*
         * Horizontal vector pointing toward the player's right.
         */
        Vec3 rightDirection =
                new Vec3(
                        Math.cos(yawRadians),
                        0.0D,
                        Math.sin(yawRadians)
                );

        boolean punchingFromRight =
                player.getMainArm()
                        == HumanoidArm.RIGHT;

        if (offhandPunch) {
            punchingFromRight =
                    !punchingFromRight;
        }

        double sideOffset =
                punchingFromRight
                        ? 0.32D
                        : -0.32D;

        /*
         * Move the approximate fist position forward as the punch
         * approaches its contact tick.
         */
        double punchProgress =
                (elapsedTicks - punchStartTick)
                        / 3.0D;

        punchProgress =
                Math.max(
                        0.0D,
                        Math.min(1.0D, punchProgress)
                );

        double forwardOffset =
                0.20D
                        + 0.50D * punchProgress;

        Vec3 handPosition =
                player.position()
                        .add(
                                0.0D,
                                player.getBbHeight()
                                        * 0.72D,
                                0.0D
                        )
                        .add(
                                rightDirection.scale(
                                        sideOffset
                                )
                        )
                        .add(
                                lookDirection.scale(
                                        forwardOffset
                                )
                        );

        /*
         * Small cloud cluster around the approximate fist position.
         */
        player.serverLevel().sendParticles(
                PUNCH_PARTICLE,
                handPosition.x,
                handPosition.y,
                handPosition.z,
                3,
                0.055D,
                0.055D,
                0.055D,
                0.005D
        );
    }
}