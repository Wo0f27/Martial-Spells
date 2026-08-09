package com.w0of26.martialspells.movement;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncStepOfWindSurfacePacket;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
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
public final class StepOfTheWindWallRunEvents {

    /*
     * Maximum duration of advanced Step movement.
     *
     * Level I:   2.0 seconds
     * Level II:  2.5 seconds
     * Level III: 3.0 seconds
     * Level IV:  3.5 seconds
     * Level V:   4.0 seconds
     */
    private static final int[] MOVEMENT_TICKS = {
            40,
            50,
            60,
            70,
            80
    };

    /*
     * Speed maintained while moving across the wall plane.
     *
     * Since vertical movement is preserved, this controls:
     *
     * - horizontal wall-running
     * - diagonal wall-running
     * - vertical wall-climbing
     */
    private static final double[] WALL_RUN_SPEED = {
            0.40D,
            0.425D,
            0.45D,
            0.475D,
            0.50D
    };

    /*
     * Small force toward the wall keeps the Monk attached.
     */
    private static final double WALL_ADHESION =
            0.06D;

    private static final double WALL_JUMP_AWAY_SPEED = 0.55D;
    private static final double WALL_JUMP_UP_SPEED = 0.55D;

    private static final int WALL_JUMP_REATTACH_LOCK_TICKS = 6;

    /*
     * Prevent numerical instability when the projected
     * movement vector becomes extremely small.
     */
    private static final double MIN_DIRECTION_LENGTH_SQR =
            1.0E-5D;

    private static final Map<UUID, WallRunState>
            ACTIVE_STATES = new HashMap<>();

    private StepOfTheWindWallRunEvents() {
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
                                MOVEMENT_TICKS.length
                        )
                );

        ACTIVE_STATES.put(
                player.getUUID(),
                new WallRunState(
                        MOVEMENT_TICKS[
                                level - 1
                                ],
                        level
                )
        );
    }

    private static boolean hasValidMovementState(
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

        return !player.getAbilities().flying;
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

        UUID playerId =
                player.getUUID();

        WallRunState state =
                ACTIVE_STATES.get(
                        playerId
                );

        if (state == null) {
            return;
        }

        state.remainingTicks--;

        /*
         * Technique duration expired.
         *
         * Restore normal player rendering before deleting
         * the server-side movement state.
         */
        if (state.remainingTicks <= 0) {

            syncVisualWall(
                    player,
                    state,
                    null
            );

            ACTIVE_STATES.remove(
                    playerId
            );

            return;
        }

        /*
         * Sneaking deliberately releases Step wall movement.
         */
        if (player.isShiftKeyDown()) {

            syncVisualWall(
                    player,
                    state,
                    null
            );

            ACTIVE_STATES.remove(
                    playerId
            );

            return;
        }

        /*
         * Mounted, swimming, climbing, Elytra flight, or
         * creative-style flight cannot use wall movement.
         *
         * The technique state itself remains temporarily
         * armed so it may resume if the invalid state ends
         * before the Step duration expires.
         *
         * Visually, however, the player must immediately
         * return to normal orientation.
         */
        if (!hasValidMovementState(
                player
        )) {

            syncVisualWall(
                    player,
                    state,
                    null
            );

            return;
        }

        /*
         * Ground movement remains the normal Step dash.
         *
         * Wall movement and wall orientation only exist
         * while airborne.
         */
        if (player.onGround()) {

            syncVisualWall(
                    player,
                    state,
                    null
            );

            state.reattachLockTicks = 0;
            state.lockedWallDirection = null;

            return;
        }

        Direction wallDirection =
                WallRunHelper.findAdjacentWall(
                        player
                );

        /*
         * Count down the short post-wall-jump lock.
         */
        if (state.reattachLockTicks > 0) {

            state.reattachLockTicks--;

            /*
             * No wall nearby:
             * keep travelling through the air.
             */
            if (wallDirection == null) {

                syncVisualWall(
                        player,
                        state,
                        null
                );

                return;
            }

            /*
             * Still touching the same side we just jumped from:
             * do not let adhesion immediately pull us back.
             */
            if (wallDirection
                    == state.lockedWallDirection) {

                syncVisualWall(
                        player,
                        state,
                        null
                );

                return;
            }

            /*
             * Reaching a DIFFERENT wall is allowed.
             *
             * This will eventually permit wall-to-wall movement.
             */
            state.reattachLockTicks = 0;
            state.lockedWallDirection = null;
        }

        /*
         * Normal loss of wall contact.
         */
        if (wallDirection == null) {

            syncVisualWall(
                    player,
                    state,
                    null
            );

            return;
        }

        /*
         * Tell the client which wall currently acts as the
         * Monk's visual floor.
         *
         * syncVisualWall only sends when the direction
         * actually changes, so this does not spam packets
         * every tick.
         */
        syncVisualWall(
                player,
                state,
                wallDirection
        );

        applyWallRun(
                player,
                wallDirection,
                state.techniqueLevel
        );
    }

    public static void tryWallJump(
            ServerPlayer player
    ) {
        WallRunState state =
                ACTIVE_STATES.get(
                        player.getUUID()
                );

        /*
         * The client cannot manufacture a wall jump just by
         * sending the packet.
         */
        if (state == null
                || state.remainingTicks <= 0) {
            return;
        }

        if (player.onGround()) {

            return;
        }

        if (!hasValidMovementState(
                player
        )) {
            return;
        }

        /*
         * Verify that there really is still an adjacent wall
         * on the authoritative server.
         */
        Direction wallDirection =
                WallRunHelper.findAdjacentWall(
                        player
                );

        if (wallDirection == null) {
            return;
        }

        /*
         * A jump cannot fire again while the player is still
         * inside the immediate reattachment lock for this wall.
         */
        if (state.reattachLockTicks > 0
                && state.lockedWallDirection
                == wallDirection) {
            return;
        }

        /*
         * Direction points FROM player TOWARD wall.
         */
        Vec3 wallNormal =
                new Vec3(
                        wallDirection.getStepX(),
                        0.0D,
                        wallDirection.getStepZ()
                );

        Vec3 currentVelocity =
                player.getDeltaMovement();

        /*
         * Preserve some existing horizontal movement along
         * the wall.
         *
         * This makes a horizontal/diagonal wall run flow into
         * the rebound instead of stopping dead.
         */
        Vec3 horizontalVelocity =
                new Vec3(
                        currentVelocity.x,
                        0.0D,
                        currentVelocity.z
                );

        Vec3 alongWallVelocity =
                projectOntoWall(
                        horizontalVelocity,
                        wallNormal
                )
                        .scale(
                                0.65D
                        );

        /*
         * Negating the wall normal points AWAY from the wall.
         */
        Vec3 awayFromWall =
                wallNormal.scale(
                        -WALL_JUMP_AWAY_SPEED
                );

        Vec3 launchVelocity =
                alongWallVelocity
                        .add(
                                awayFromWall
                        )
                        .add(
                                0.0D,
                                WALL_JUMP_UP_SPEED,
                                0.0D
                        );

        /*
         * Immediately stop rendering the wall as the player's
         * floor.
         */
        syncVisualWall(
                player,
                state,
                null
        );

        /*
         * Prevent the exact wall we just jumped from
         * immediately capturing the player again.
         */
        state.reattachLockTicks =
                WALL_JUMP_REATTACH_LOCK_TICKS;

        state.lockedWallDirection =
                wallDirection;

        applyMovement(
                player,
                launchVelocity
        );
    }
    private static void applyWallRun(
            ServerPlayer player,
            Direction wallDirection,
            int techniqueLevel
    ) {
        Vec3 currentVelocity =
                player.getDeltaMovement();

        /*
         * Horizontal surface normal pointing from the
         * player toward the adjacent wall.
         */
        Vec3 wallNormal =
                new Vec3(
                        wallDirection.getStepX(),
                        0.0D,
                        wallDirection.getStepZ()
                );

        /*
         * Use the player's complete look vector.
         *
         * Projecting this onto the wall plane preserves
         * vertical movement:
         *
         * horizontal look
         * -> horizontal wall-run
         *
         * upward look
         * -> wall climb
         *
         * diagonal upward look
         * -> diagonal wall-run
         */
        Vec3 lookDirection =
                player.getLookAngle();

        Vec3 wallMovementDirection =
                projectOntoWall(
                        lookDirection,
                        wallNormal
                );

        /*
         * Looking almost directly into or away from the
         * wall may leave almost no usable movement.
         *
         * Fall back to the player's existing momentum.
         */
        if (wallMovementDirection.lengthSqr()
                < MIN_DIRECTION_LENGTH_SQR) {

            wallMovementDirection =
                    projectOntoWall(
                            currentVelocity,
                            wallNormal
                    );
        }

        /*
         * If neither look direction nor momentum provides
         * meaningful movement along the wall, cling instead
         * of arbitrarily choosing a direction.
         */
        if (wallMovementDirection.lengthSqr()
                < MIN_DIRECTION_LENGTH_SQR) {

            applyWallCling(
                    player,
                    wallDirection
            );

            return;
        }

        wallMovementDirection =
                wallMovementDirection.normalize();

        double wallRunSpeed =
                WALL_RUN_SPEED[
                        techniqueLevel - 1
                        ];

        /*
         * The resulting vector lies on the wall's plane
         * and may contain horizontal and vertical motion.
         */
        Vec3 alongWall =
                wallMovementDirection.scale(
                        wallRunSpeed
                );

        /*
         * Keep gentle pressure toward the wall so normal
         * Minecraft collision keeps the Monk attached.
         */
        Vec3 towardWall =
                wallNormal.scale(
                        WALL_ADHESION
                );

        Vec3 adjustedVelocity =
                alongWall.add(
                        towardWall
                );

        applyMovement(
                player,
                adjustedVelocity
        );
    }

    /*
     * Removes the part of a vector pointing directly
     * toward or away from the wall.
     *
     * Vertical movement is deliberately preserved.
     *
     * The returned vector therefore lies somewhere on
     * the wall's two-dimensional movement plane.
     */
    private static Vec3 projectOntoWall(
            Vec3 vector,
            Vec3 wallNormal
    ) {
        double normalComponent =
                vector.dot(
                        wallNormal
                );

        return vector.subtract(
                wallNormal.scale(
                        normalComponent
                )
        );
    }

    /*
     * Used when the player has no meaningful movement
     * direction along the wall.
     */
    private static void applyWallCling(
            ServerPlayer player,
            Direction wallDirection
    ) {
        Vec3 currentVelocity =
                player.getDeltaMovement();

        /*
         * Allow only a tiny descent while clinging.
         */
        double verticalVelocity =
                Math.max(
                        currentVelocity.y,
                        -0.03D
                );

        Vec3 towardWall =
                new Vec3(
                        wallDirection.getStepX(),
                        0.0D,
                        wallDirection.getStepZ()
                )
                        .scale(
                                WALL_ADHESION
                        );

        Vec3 adjustedVelocity =
                new Vec3(
                        currentVelocity.x,
                        verticalVelocity,
                        currentVelocity.z
                )
                        .add(
                                towardWall
                        );

        applyMovement(
                player,
                adjustedVelocity
        );
    }

    private static void applyMovement(
            ServerPlayer player,
            Vec3 velocity
    ) {
        StepOfTheWindMovementEvents
                .maintainFallProtection(
                        player
                );

        player.setDeltaMovement(
                velocity
        );

        /*
         * Wall-running itself must not accumulate fall
         * distance.
         */
        player.fallDistance = 0.0F;

        /*
         * Tell Minecraft that externally-controlled motion
         * changed this entity.
         */
        player.hurtMarked = true;

        /*
         * Synchronize server-authoritative movement to the
         * controlling player's client.
         */
        player.connection.send(
                new ClientboundSetEntityMotionPacket(
                        player
                )
        );
    }

    /*
     * Synchronizes the wall currently acting as the
     * player's visual floor.
     *
     * null means:
     *
     * restore ordinary upright rendering.
     *
     * The cached direction prevents sending the same packet
     * every server tick.
     */
    private static void syncVisualWall(
            ServerPlayer player,
            WallRunState state,
            @Nullable Direction wallDirection
    ) {
        if (state.syncedWallDirection
                == wallDirection) {
            return;
        }

        state.syncedWallDirection =
                wallDirection;

        MartialNetwork.sendToTrackingAndSelf(
                new SyncStepOfWindSurfacePacket(
                        player.getUUID(),
                        wallDirection
                ),
                player
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        ACTIVE_STATES.remove(
                event.getEntity()
                        .getUUID()
        );
    }

    private static final class WallRunState {

        private int remainingTicks;

        private final int techniqueLevel;

        private int reattachLockTicks;

        @Nullable
        private Direction lockedWallDirection;

        /*
         * Last wall direction sent to clients.
         *
         * null means the player is rendered normally.
         */
        @Nullable
        private Direction syncedWallDirection;

        private WallRunState(
                int remainingTicks,
                int techniqueLevel
        ) {
            this.remainingTicks =
                    remainingTicks;

            this.techniqueLevel =
                    techniqueLevel;

            this.syncedWallDirection = null;
            this.reattachLockTicks = 0;
            this.lockedWallDirection = null;
        }
    }
}