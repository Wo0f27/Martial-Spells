package com.w0of26.martialspells.client.visual;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StepOfTheWindClientSurfaceState {

    private static final Map<UUID, Direction> WALL_DIRECTIONS =
            new HashMap<>();

    private StepOfTheWindClientSurfaceState() {
    }

    public static void setWallDirection(
            UUID playerId,
            @Nullable Direction direction
    ) {
        if (direction == null) {
            WALL_DIRECTIONS.remove(playerId);
            return;
        }

        WALL_DIRECTIONS.put(
                playerId,
                direction
        );
    }

    @Nullable
    public static Direction getWallDirection(
            UUID playerId
    ) {
        return WALL_DIRECTIONS.get(
                playerId
        );
    }

    public static void clear() {
        WALL_DIRECTIONS.clear();
    }
}