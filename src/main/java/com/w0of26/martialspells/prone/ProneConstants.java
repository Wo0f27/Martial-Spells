package com.w0of26.martialspells.prone;

import com.w0of26.martialspells.MartialSpells;

public final class ProneConstants {

    /*
     * Default duration used when another Martial Spells
     * mechanic applies Prone without specifying a duration.
     *
     * The effect itself still supports arbitrary durations.
     */
    public static final int DEFAULT_DURATION_SECONDS = 2;

    public static final int DEFAULT_DURATION_TICKS =
            DEFAULT_DURATION_SECONDS * 20;

    /*
     * Recovery penalty after leaving Prone.
     */
    public static final int POST_SLOWNESS_SECONDS = 2;

    public static final int POST_SLOWNESS_TICKS =
            POST_SLOWNESS_SECONDS * 20;

    /*
     * Minecraft amplifier 0 = Slowness I.
     */
    public static final int POST_SLOWNESS_AMPLIFIER = 0;

    public static final String ACTIVE_TRACKER_TAG =
            MartialSpells.MOD_ID
                    + "_prone_active";

    private ProneConstants() {
    }
}