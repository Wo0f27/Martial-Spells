package com.w0of26.martialspells.movement;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncHeavenfallTargetPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HeavenfallStrikeEvents {

    /*
     * Maximum target-selection duration.
     *
     * 120 ticks = 6 seconds.
     *
     * The caster will normally spend less time than this
     * selecting a target, but the longer window combines
     * well with Heavenfall's controlled airborne state.
     */
    private static final int TARGETING_TICKS =
            120;

    /*
     * Prevent the original grounded cast tick from
     * immediately cancelling Heavenfall.
     */
    private static final int GROUND_GRACE_TICKS =
            5;

    /*
     * Maximum initial target-selection range.
     */
    private static final double TARGET_RANGE =
            32.0D;

    /*
     * During target selection the Monk may rise normally,
     * but downward velocity is limited.
     *
     * This creates Heavenfall's slow-fall targeting window
     * without applying the vanilla Slow Falling MobEffect
     * and potentially interfering with other effects.
     */
    private static final double
            TARGETING_MAX_FALL_SPEED =
            -0.10D;

    /*
     * Maximum duration of the committed dive.
     *
     * This is primarily a failsafe.
     */
    private static final int DIVE_TICKS =
            60;

    /*
     * Provisional Heavenfall dive speed.
     */
    private static final double DIVE_SPEED =
            1.60D;

    /*
     * Temporary Checkpoint-3 arrival threshold.
     *
     * Proper swept impact detection comes next.
     */
    private static final double ARRIVAL_DISTANCE =
            1.35D;

    /*
     * Maximum distance a committed target may move away
     * before Heavenfall aborts.
     */
    private static final double
            MAX_COMMITTED_TARGET_DISTANCE =
            40.0D;

    private static final Map<UUID, HeavenfallState>
            ACTIVE_STATES =
            new HashMap<>();

    private HeavenfallStrikeEvents() {
    }

    private enum Phase {
        TARGETING,
        DIVING
    }

    public static void arm(
            ServerPlayer player,
            int spellLevel
    ) {
        int level =
                Math.max(
                        1,
                        Math.min(
                                spellLevel,
                                5
                        )
                );

        HeavenfallState state =
                new HeavenfallState(
                        TARGETING_TICKS,
                        level,
                        player.level()
                                .dimension()
                );

        ACTIVE_STATES.put(
                player.getUUID(),
                state
        );

        /*
         * Remove any stale client-side target indicator.
         */
        MartialNetwork.sendToPlayer(
                new SyncHeavenfallTargetPacket(
                        -1
                ),
                player
        );
    }

    /*
     * Called after the client presses Attack while a
     * Heavenfall target is highlighted.
     *
     * IMPORTANT:
     *
     * The client does NOT send the target ID.
     *
     * state.targetEntityId was already selected by the
     * server during tickTargeting(), so confirmation can
     * safely use that stored server-owned target.
     *
     * Re-running the look ray here created a race between
     * client rotation and server rotation updates.
     */
    public static void tryCommitDive(
            ServerPlayer player
    ) {
        HeavenfallState state =
                ACTIVE_STATES.get(
                        player.getUUID()
                );

        if (state == null) {
            return;
        }

        if (state.phase
                != Phase.TARGETING) {
            return;
        }

        if (state.targetEntityId < 0) {
            return;
        }

        /*
         * Resolve the target that the SERVER previously
         * selected.
         */
        Entity entity =
                player.serverLevel()
                        .getEntity(
                                state.targetEntityId
                        );

        if (!(entity
                instanceof LivingEntity target)) {

            clearSelectedTarget(
                    player,
                    state
            );
            return;
        }

        /*
         * Revalidate all meaningful gameplay conditions.
         */
        if (!isValidTarget(
                player,
                target
        )) {
            clearSelectedTarget(
                    player,
                    state
            );
            return;
        }

        if (!player.hasLineOfSight(
                target
        )) {
            clearSelectedTarget(
                    player,
                    state
            );
            return;
        }

        double selectionRangeSqr =
                TARGET_RANGE
                        * TARGET_RANGE;

        if (player.distanceToSqr(
                target
        ) > selectionRangeSqr) {

            clearSelectedTarget(
                    player,
                    state
            );
            return;
        }

        Vec3 targetPoint =
                getDiveTargetPoint(
                        target
                );

        /*
         * Heavenfall must remain a descending strike.
         *
         * Targets at or above approximately the caster's
         * current height cannot be committed.
         */
        if (targetPoint.y
                >= player.getY()
                - 0.25D) {

            return;
        }

        /*
         * Entity IDs are appropriate for the temporary
         * highlight.
         *
         * Once committed, store the UUID for persistent
         * server-side target tracking.
         */
        state.committedTargetId =
                target.getUUID();

        state.diveTicksRemaining =
                DIVE_TICKS;

        state.phase =
                Phase.DIVING;

        /*
         * Selection is finished.
         */
        clearSelectedTarget(
                player,
                state
        );

        /*
         * Immediately begin the dive instead of waiting for
         * the next server tick.
         *
         * This makes confirmation feel responsive.
         */
        Vec3 toTarget =
                targetPoint.subtract(
                        player.position()
                );

        if (toTarget.lengthSqr()
                > 1.0E-5D
                && toTarget.y < 0.0D) {

            applyDiveMovement(
                    player,
                    toTarget.normalize()
                            .scale(
                                    DIVE_SPEED
                            )
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {

            return;
        }

        if (!(event.player
                instanceof ServerPlayer player)) {

            return;
        }

        HeavenfallState state =
                ACTIVE_STATES.get(
                        player.getUUID()
                );

        if (state == null) {
            return;
        }

        state.ageTicks++;

        /*
         * Heavenfall cannot survive a dimension change.
         */
        if (!player.level()
                .dimension()
                .equals(
                        state.dimension
                )) {

            cancel(
                    player
            );
            return;
        }

        if (!player.isAlive()) {
            cancel(
                    player
            );
            return;
        }

        if (!hasValidHeavenfallState(
                player
        )) {
            cancel(
                    player
            );
            return;
        }

        if (state.phase
                == Phase.TARGETING) {

            tickTargeting(
                    player,
                    state
            );
            return;
        }

        if (state.phase
                == Phase.DIVING) {

            tickDiving(
                    player,
                    state
            );
        }
    }

    /*
     * Target-selection phase.
     */
    private static void tickTargeting(
            ServerPlayer player,
            HeavenfallState state
    ) {
        /*
         * Landing before committing ends Heavenfall.
         */
        if (state.ageTicks
                > GROUND_GRACE_TICKS
                && player.onGround()) {

            cancel(
                    player
            );
            return;
        }

        state.ticksRemaining--;

        if (state.ticksRemaining <= 0) {
            cancel(
                    player
            );
            return;
        }

        /*
         * Give the player time to actually choose a target.
         *
         * Upward velocity from the initial launch remains
         * untouched.
         *
         * Only the downward portion is slowed.
         */
        applyTargetingSlowFall(
                player
        );

        LivingEntity target =
                findLookTarget(
                        player
                );

        int newTargetId =
                target == null
                        ? -1
                        : target.getId();

        if (newTargetId
                == state.targetEntityId) {

            return;
        }

        state.targetEntityId =
                newTargetId;

        MartialNetwork.sendToPlayer(
                new SyncHeavenfallTargetPacket(
                        newTargetId
                ),
                player
        );
    }

    /*
     * Heavenfall's internal slow-fall behavior.
     *
     * We deliberately avoid applying MobEffects.SLOW_FALLING
     * so this technique cannot overwrite/remove Slow Falling
     * supplied by another spell, item, mod, or potion.
     */
    private static void applyTargetingSlowFall(
            ServerPlayer player
    ) {
        Vec3 velocity =
                player.getDeltaMovement();

        /*
         * Leave ascent untouched.
         */
        if (velocity.y
                >= TARGETING_MAX_FALL_SPEED) {

            return;
        }

        Vec3 controlledVelocity =
                new Vec3(
                        velocity.x,
                        TARGETING_MAX_FALL_SPEED,
                        velocity.z
                );

        player.setDeltaMovement(
                controlledVelocity
        );

        player.fallDistance =
                0.0F;

        player.hurtMarked =
                true;

        player.connection.send(
                new ClientboundSetEntityMotionPacket(
                        player
                )
        );
    }

    /*
     * Committed dive phase.
     */
    private static void tickDiving(
            ServerPlayer player,
            HeavenfallState state
    ) {
        state.diveTicksRemaining--;

        if (state.diveTicksRemaining <= 0) {
            cancel(
                    player
            );
            return;
        }

        if (state.committedTargetId
                == null) {

            cancel(
                    player
            );
            return;
        }

        Entity entity =
                player.serverLevel()
                        .getEntity(
                                state.committedTargetId
                        );

        if (!(entity
                instanceof LivingEntity target)) {

            cancel(
                    player
            );
            return;
        }

        if (!target.isAlive()) {
            cancel(
                    player
            );
            return;
        }

        if (!isValidTarget(
                player,
                target
        )) {
            cancel(
                    player
            );
            return;
        }

        double maximumDistanceSqr =
                MAX_COMMITTED_TARGET_DISTANCE
                        * MAX_COMMITTED_TARGET_DISTANCE;

        if (player.distanceToSqr(
                target
        ) > maximumDistanceSqr) {

            cancel(
                    player
            );
            return;
        }

        Vec3 targetPoint =
                getDiveTargetPoint(
                        target
                );

        Vec3 toTarget =
                targetPoint.subtract(
                        player.position()
                );

        /*
         * Temporary Checkpoint-3 arrival behavior.
         */
        double arrivalDistanceSqr =
                ARRIVAL_DISTANCE
                        * ARRIVAL_DISTANCE;

        if (toTarget.lengthSqr()
                <= arrivalDistanceSqr) {

            finishDiveCheckpoint(
                    player
            );
            return;
        }

        /*
         * Do not chase targets upward.
         */
        if (toTarget.y >= 0.0D) {
            cancel(
                    player
            );
            return;
        }

        Vec3 velocity =
                toTarget.normalize()
                        .scale(
                                DIVE_SPEED
                        );

        applyDiveMovement(
                player,
                velocity
        );
    }

    private static void applyDiveMovement(
            ServerPlayer player,
            Vec3 velocity
    ) {
        player.setDeltaMovement(
                velocity
        );

        player.fallDistance =
                0.0F;

        player.hurtMarked =
                true;

        player.connection.send(
                new ClientboundSetEntityMotionPacket(
                        player
                )
        );
    }

    /*
     * Temporary successful Checkpoint-3 completion.
     *
     * Combat effects will be attached to the real impact
     * handler later.
     */
    private static void finishDiveCheckpoint(
            ServerPlayer player
    ) {
        HeavenfallState removed =
                ACTIVE_STATES.remove(
                        player.getUUID()
                );

        if (removed == null) {
            return;
        }

        Vec3 currentVelocity =
                player.getDeltaMovement();

        Vec3 releasedVelocity =
                new Vec3(
                        currentVelocity.x
                                * 0.15D,
                        -0.20D,
                        currentVelocity.z
                                * 0.15D
                );

        player.setDeltaMovement(
                releasedVelocity
        );

        player.fallDistance =
                0.0F;

        player.hurtMarked =
                true;

        player.connection.send(
                new ClientboundSetEntityMotionPacket(
                        player
                )
        );

        MartialNetwork.sendToPlayer(
                new SyncHeavenfallTargetPacket(
                        -1
                ),
                player
        );
    }

    private static void clearSelectedTarget(
            ServerPlayer player,
            HeavenfallState state
    ) {
        if (state.targetEntityId == -1) {
            return;
        }

        state.targetEntityId =
                -1;

        MartialNetwork.sendToPlayer(
                new SyncHeavenfallTargetPacket(
                        -1
                ),
                player
        );
    }

    private static Vec3 getDiveTargetPoint(
            LivingEntity target
    ) {
        return target.position()
                .add(
                        0.0D,
                        target.getBbHeight()
                                * 0.50D,
                        0.0D
                );
    }

    private static boolean hasValidHeavenfallState(
            ServerPlayer player
    ) {
        if (player.isPassenger()) {
            return false;
        }

        if (player.isInWaterOrBubble()) {
            return false;
        }

        if (player.onClimbable()) {
            return false;
        }

        if (player.isFallFlying()) {
            return false;
        }

        return !player.getAbilities()
                .flying;
    }

    @Nullable
    private static LivingEntity findLookTarget(
            ServerPlayer player
    ) {
        Vec3 start =
                player.getEyePosition();

        Vec3 look =
                player.getLookAngle()
                        .normalize();

        Vec3 fullEnd =
                start.add(
                        look.scale(
                                TARGET_RANGE
                        )
                );

        /*
         * Terrain blocks target selection.
         */
        BlockHitResult blockHit =
                player.level()
                        .clip(
                                new ClipContext(
                                        start,
                                        fullEnd,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        player
                                )
                        );

        Vec3 end =
                blockHit.getType()
                        == HitResult.Type.MISS
                        ? fullEnd
                        : blockHit.getLocation();

        Vec3 ray =
                end.subtract(
                        start
                );

        AABB searchBox =
                player.getBoundingBox()
                        .expandTowards(
                                ray
                        )
                        .inflate(
                                1.0D
                        );

        EntityHitResult entityHit =
                ProjectileUtil
                        .getEntityHitResult(
                                player,
                                start,
                                end,
                                searchBox,
                                entity ->
                                        isValidTarget(
                                                player,
                                                entity
                                        ),
                                start.distanceToSqr(
                                        end
                                )
                        );

        if (entityHit == null) {
            return null;
        }

        Entity entity =
                entityHit.getEntity();

        if (entity
                instanceof LivingEntity living) {

            return living;
        }

        return null;
    }

    private static boolean isValidTarget(
            ServerPlayer player,
            Entity entity
    ) {
        if (!(entity
                instanceof LivingEntity living)) {

            return false;
        }

        if (!living.isAlive()) {
            return false;
        }

        if (living == player) {
            return false;
        }

        if (player.isAlliedTo(
                living
        )) {
            return false;
        }

        if (living instanceof Enemy) {
            return true;
        }

        return living instanceof Mob mob
                && mob.getTarget()
                == player;
    }

    private static void cancel(
            ServerPlayer player
    ) {
        HeavenfallState removed =
                ACTIVE_STATES.remove(
                        player.getUUID()
                );

        if (removed == null) {
            return;
        }

        MartialNetwork.sendToPlayer(
                new SyncHeavenfallTargetPacket(
                        -1
                ),
                player
        );
    }

    @SubscribeEvent
    public static void onPlayerLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {

            ACTIVE_STATES.remove(
                    player.getUUID()
            );
        }
    }

    private static final class HeavenfallState {

        private int ticksRemaining;
        private int ageTicks;

        /*
         * Stored now for later combat scaling.
         */
        private final int spellLevel;

        private final ResourceKey<Level>
                dimension;

        /*
         * Current SERVER-selected highlight target.
         */
        private int targetEntityId =
                -1;

        private Phase phase =
                Phase.TARGETING;

        /*
         * Locked server target after confirmation.
         */
        @Nullable
        private UUID committedTargetId;

        private int diveTicksRemaining;

        private HeavenfallState(
                int ticksRemaining,
                int spellLevel,
                ResourceKey<Level> dimension
        ) {
            this.ticksRemaining =
                    ticksRemaining;

            this.spellLevel =
                    spellLevel;

            this.dimension =
                    dimension;
        }
    }
}