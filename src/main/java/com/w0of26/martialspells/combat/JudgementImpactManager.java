package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.JudgementVisualEntity;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.spells.PaladinJudgementSpell;
import com.w0of26.martialspells.spells.PaladinVfx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Lightweight server-authoritative translation of Judgement's frozen meteor.
 *
 * The upstream projectile launches 12 blocks above the target at 1.2
 * blocks/tick, which yields a ten-tick descent. P2 preserves that timing and
 * homing target position while P5 owns the exact authored projectile model.
 */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID)
public final class JudgementImpactManager {
    private static final String PENDING_TAG =
            MartialSpells.MOD_ID
                    + "_judgement_pending";

    private static final String TARGET_TAG =
            MartialSpells.MOD_ID
                    + "_judgement_target";

    private static final String TICKS_TAG =
            MartialSpells.MOD_ID
                    + "_judgement_ticks";

    private static final String X_TAG =
            MartialSpells.MOD_ID
                    + "_judgement_x";

    private static final String Y_TAG =
            MartialSpells.MOD_ID
                    + "_judgement_y";

    private static final String Z_TAG =
            MartialSpells.MOD_ID
                    + "_judgement_z";

    private JudgementImpactManager() {
    }

    public static void queue(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            PaladinJudgementSpell spell
    ) {
        if (!(caster instanceof ServerPlayer player)) {
            spell.resolveImpact(
                    level,
                    caster,
                    target.position()
            );
            return;
        }

        CompoundTag data =
                player.getPersistentData();

        data.putBoolean(
                PENDING_TAG,
                true
        );

        data.putUUID(
                TARGET_TAG,
                target.getUUID()
        );

        data.putInt(
                TICKS_TAG,
                PaladinJudgementSpell.METEOR_TRAVEL_TICKS
        );

        storePosition(
                data,
                target.position()
        );

        Vec3 launch =
                target.position().add(
                        0.0D,
                        PaladinJudgementSpell.METEOR_LAUNCH_HEIGHT,
                        0.0D
                );

        JudgementVisualEntity visual =
                new JudgementVisualEntity(
                        MartialEntityRegistry.JUDGEMENT_VISUAL.get(),
                        level,
                        target,
                        launch
                );
        level.addFreshEntity(visual);
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data =
                player.getPersistentData();

        if (!data.getBoolean(PENDING_TAG)) {
            return;
        }

        ServerLevel level =
                player.serverLevel();

        updateTrackedPosition(
                level,
                data
        );

        int remaining =
                Math.max(
                        0,
                        data.getInt(TICKS_TAG) - 1
                );

        Vec3 impact =
                storedPosition(data);

        double meteorY =
                impact.y
                        + remaining
                        * PaladinJudgementSpell.METEOR_VELOCITY;

        if (remaining > 0) {
            data.putInt(
                    TICKS_TAG,
                    remaining
            );
            return;
        }

        clear(data);

        if (MartialSpellRegistry.JUDGEMENT.get()
                instanceof PaladinJudgementSpell spell) {
            spell.resolveImpact(
                    level,
                    player,
                    impact
            );
        }
    }

    private static void updateTrackedPosition(
            ServerLevel level,
            CompoundTag data
    ) {
        if (!data.hasUUID(TARGET_TAG)) {
            return;
        }

        UUID targetId =
                data.getUUID(TARGET_TAG);

        Entity entity =
                level.getEntity(targetId);

        if (entity instanceof LivingEntity target
                && target.isAlive()
                && !target.isDeadOrDying()) {
            storePosition(
                    data,
                    target.position()
            );
        }
    }

    private static void storePosition(
            CompoundTag data,
            Vec3 position
    ) {
        data.putDouble(
                X_TAG,
                position.x
        );

        data.putDouble(
                Y_TAG,
                position.y
        );

        data.putDouble(
                Z_TAG,
                position.z
        );
    }

    private static Vec3 storedPosition(
            CompoundTag data
    ) {
        return new Vec3(
                data.getDouble(X_TAG),
                data.getDouble(Y_TAG),
                data.getDouble(Z_TAG)
        );
    }

    private static void clear(
            CompoundTag data
    ) {
        data.remove(PENDING_TAG);
        data.remove(TARGET_TAG);
        data.remove(TICKS_TAG);
        data.remove(X_TAG);
        data.remove(Y_TAG);
        data.remove(Z_TAG);
    }
}
