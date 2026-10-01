package com.w0of26.martialspells.combat;

/** Fixed target cooldown arithmetic; later equipment integrations supply only the adjustment. */
public final class SpiritWolfFixedCooldown {
    public static final int BASE_TICKS = 900;

    private SpiritWolfFixedCooldown() {}

    /** A future equipment adapter may supply 100; ordinary CDR never enters this calculation. */
    public static int ticks(int equipmentReductionTicks) {
        if (equipmentReductionTicks != 0 && equipmentReductionTicks != 100) {
            throw new IllegalArgumentException("Only the neutral or 100-tick equipment adjustment is supported");
        }
        return BASE_TICKS - equipmentReductionTicks;
    }
}
