package com.w0of26.martialspells.movement;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public final class WallRunHelper {

    /*
     * How far outside the player's normal hitbox we probe
     * for a nearby wall.
     */
    private static final double WALL_PROBE_DISTANCE =
            0.16D;

    /*
     * Avoid probing directly against the player's feet
     * and head. This prevents the floor or a low ceiling
     * from being mistaken for a horizontal wall.
     */
    private static final double VERTICAL_INSET =
            0.15D;

    private static final double HORIZONTAL_INSET =
            0.05D;

    private WallRunHelper() {
    }

    /**
     * Returns the horizontal direction from the player
     * toward an adjacent collidable wall.
     *
     * For example:
     *
     * EAST means the wall is east of the player.
     */
    @Nullable
    public static Direction findAdjacentWall(
            ServerPlayer player
    ) {
        AABB playerBox =
                player.getBoundingBox();

        for (Direction direction
                : Direction.Plane.HORIZONTAL) {

            AABB probeBox =
                    createProbeBox(
                            playerBox,
                            direction
                    );

            boolean collision =
                    player.level()
                            .getBlockCollisions(
                                    player,
                                    probeBox
                            )
                            .iterator()
                            .hasNext();

            if (collision) {
                return direction;
            }
        }

        return null;
    }

    private static AABB createProbeBox(
            AABB box,
            Direction direction
    ) {
        double minY =
                box.minY + VERTICAL_INSET;

        double maxY =
                box.maxY - VERTICAL_INSET;

        return switch (direction) {

            case NORTH ->
                    new AABB(
                            box.minX
                                    + HORIZONTAL_INSET,
                            minY,
                            box.minZ
                                    - WALL_PROBE_DISTANCE,
                            box.maxX
                                    - HORIZONTAL_INSET,
                            maxY,
                            box.minZ
                    );

            case SOUTH ->
                    new AABB(
                            box.minX
                                    + HORIZONTAL_INSET,
                            minY,
                            box.maxZ,
                            box.maxX
                                    - HORIZONTAL_INSET,
                            maxY,
                            box.maxZ
                                    + WALL_PROBE_DISTANCE
                    );

            case WEST ->
                    new AABB(
                            box.minX
                                    - WALL_PROBE_DISTANCE,
                            minY,
                            box.minZ
                                    + HORIZONTAL_INSET,
                            box.minX,
                            maxY,
                            box.maxZ
                                    - HORIZONTAL_INSET
                    );

            case EAST ->
                    new AABB(
                            box.maxX,
                            minY,
                            box.minZ
                                    + HORIZONTAL_INSET,
                            box.maxX
                                    + WALL_PROBE_DISTANCE,
                            maxY,
                            box.maxZ
                                    - HORIZONTAL_INSET
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Wall probe requires horizontal direction"
                    );
        };
    }
}