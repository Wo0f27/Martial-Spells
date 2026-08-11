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
import com.w0of26.martialspells.client.animation.HeavenfallAnimationPhase;
import com.w0of26.martialspells.combat.HeavenfallAnimationStyle;
import com.w0of26.martialspells.combat.MonkWeaponHelper;
import com.w0of26.martialspells.network.SyncHeavenfallAnimationPacket;
import com.w0of26.martialspells.combat.HeavenfallCombatHelper;
import com.w0of26.martialspells.client.visual.HeavenfallImpactVfxManager;

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
     * Small additional tolerance around the target for
     * Heavenfall's swept contact test.
     */
    private static final double IMPACT_PADDING =
            0.15D;

    /*
     * Prevent the player from retaining full dive momentum
     * after a successful impact.
     */
    private static final double POST_IMPACT_Y_SPEED =
            -0.10D;

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
         * Store the player's current body center before the first
         * dive movement is applied.
         *
         * On the following server tick we can sweep from this
         * position to the new one and detect targets crossed
         * between ticks.
         */
        state.previousDiveCenter =
                getPlayerCenter(
                        player
                );

        HeavenfallAnimationStyle animationStyle =
                MonkWeaponHelper
                        .getHeavenfallAnimationStyle(
                                player
                        );

        if (animationStyle != null) {
            MartialNetwork.sendToTrackingAndSelf(
                    new SyncHeavenfallAnimationPacket(
                            player.getUUID(),
                            HeavenfallAnimationPhase.DIVE,
                            animationStyle
                    ),
                    player
            );
        }

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

        /*
         * ====================================================
         * SWEPT IMPACT DETECTION
         * ====================================================
         *
         * Checking only the player's current position is not
         * sufficient because Heavenfall moves quickly.
         *
         * Instead, test the entire segment travelled since
         * the previous server tick.
         */
        Vec3 currentCenter =
                getPlayerCenter(
                        player
                );

        if (hasSweptImpact(
                player,
                target,
                state.previousDiveCenter,
                currentCenter
        )) {
            finishImpactCheckpoint(
                    player,
                    target
            );
            return;
        }

        /*
         * Save this tick's position for the next sweep.
         */
        state.previousDiveCenter =
                currentCenter;

        /*
         * Terrain intercepted the dive before the selected
         * target was struck.
         *
         * Do not treat this as a successful Heavenfall hit.
         */
        if (player.onGround()
                || player.horizontalCollision) {

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
         * If the target has somehow moved above the caster,
         * Heavenfall must not become upward homing flight.
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

    private static Vec3 getPlayerCenter(
            ServerPlayer player
    ) {
        return player.position()
                .add(
                        0.0D,
                        player.getBbHeight()
                                * 0.50D,
                        0.0D
                );
    }

    private static boolean hasSweptImpact(
            ServerPlayer player,
            LivingEntity target,
            @Nullable Vec3 previousCenter,
            Vec3 currentCenter
    ) {
        /*
         * The first DIVING tick should normally have a
         * previous position from tryCommitDive().
         *
         * Fall back safely if one is unavailable.
         */
        Vec3 start =
                previousCenter == null
                        ? currentCenter
                        : previousCenter;

        /*
         * Treat the player's center as a swept point and
         * expand the target's box by the player's dimensions.
         *
         * This approximates player-body versus target-body
         * contact rather than requiring the exact center of
         * the player to enter the target's original AABB.
         */
        double horizontalExpansion =
                player.getBbWidth()
                        * 0.50D
                        + IMPACT_PADDING;

        double verticalExpansion =
                player.getBbHeight()
                        * 0.50D
                        + IMPACT_PADDING;

        AABB impactBox =
                target.getBoundingBox()
                        .inflate(
                                horizontalExpansion,
                                verticalExpansion,
                                horizontalExpansion
                        );

        /*
         * Already inside the expanded target volume.
         */
        if (impactBox.contains(
                currentCenter
        )) {
            return true;
        }

        /*
         * Detect the important tunnelling case:
         *
         * previous position
         *       |
         *       | 1.60 block movement
         *       V
         * current position
         *
         * even when neither endpoint itself is inside the
         * target.
         */
        return impactBox
                .clip(
                        start,
                        currentCenter
                )
                .isPresent();
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

    private static void finishImpactCheckpoint(
            ServerPlayer player,
            LivingEntity target
    ) {
        HeavenfallState removed =
                ACTIVE_STATES.remove(
                        player.getUUID()
                );

        if (removed == null) {
            return;
        }

        /*
         * Checkpoint 5A:
         *
         * Apply Heavenfall's combat package only to the
         * selected primary target.
         */
        HeavenfallCombatHelper.applyPrimaryImpact(
                player,
                target,
                removed.spellLevel
        );

        /*
         * Surrounding enemies receive only reduced damage
         * and outward knockback.
         *
         * No stun, Blight, or Rend.
         */
        HeavenfallCombatHelper.applyShockwave(
                player,
                target,
                removed.spellLevel
        );

        /*
         * Final Heavenfall landing presentation.
         *
         * Combat has already been resolved above.
         * This is purely sound/camera/terrain VFX.
         */
        HeavenfallImpactVfxManager.begin(
                player.serverLevel(),
                target.position(),
                removed.spellLevel
        );

        /*
         * Stop the high-speed homing movement.
         *
         * Keep a tiny downward velocity so the player settles
         * naturally instead of appearing suspended beside the
         * target.
         */
        player.setDeltaMovement(
                0.0D,
                POST_IMPACT_Y_SPEED,
                0.0D
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

        /*
         * Clear the client target state.
         */
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

        @Nullable
        private Vec3 previousDiveCenter;

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