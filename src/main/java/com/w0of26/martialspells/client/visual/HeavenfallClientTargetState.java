package com.w0of26.martialspells.client.visual;

public final class HeavenfallClientTargetState {

    private static int targetEntityId =
            -1;

    private HeavenfallClientTargetState() {
    }

    public static void setTargetEntityId(
            int entityId
    ) {
        targetEntityId =
                entityId;
    }

    public static int getTargetEntityId() {
        return targetEntityId;
    }

    public static void clear() {
        targetEntityId =
                -1;
    }
}