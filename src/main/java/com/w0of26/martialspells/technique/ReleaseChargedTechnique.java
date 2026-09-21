package com.w0of26.martialspells.technique;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Narrow bridge for source techniques that must resolve when the player
 * releases the use key before Iron's LONG cast reaches its full duration.
 *
 * <p>Iron's native LONG cast cancels on early release. Frozen Rogues charged
 * techniques instead permit a release after a source-defined minimum ratio.
 * Implementations return true only when they consumed that release.</p>
 */
public interface ReleaseChargedTechnique {
    boolean releaseOnUseStop(ServerPlayer player, MagicData magicData);
}
