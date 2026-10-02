package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.RangerArrowMetadata;
import com.w0of26.martialspells.entity.EntanglingArrow;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.spells.PowerShotSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Spend one saved stash charge when each qualifying server arrow is spawned. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PowerShotArrowEvents {
    private PowerShotArrowEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide()
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer owner)) return;

        // Vanilla bow/crossbow release paths and the AR1 ammo helper mark the
        // actual projectile before spawn. Entangling Arrow is an explicit
        // physical-shot type; no held-item timing inference. AR3 Barrage uses
        // the tagged AR1 physical-arrow path and needs no spell-specific branch.
        if (!RangerArrowMetadata.isEligibleShot(arrow)
                && !(arrow instanceof EntanglingArrow)) return;
        RangerArrowMetadata.markEligibleShot(arrow);
        if (!RangerArrowMetadata.markShotEvaluated(arrow)) return;

        MobEffectInstance stash = owner.getEffect(MartialEffectRegistry.POWER_SHOT_STASH.get());
        if (stash == null || stash.getDuration() <= 0) return;
        CompoundTag transfer = new CompoundTag();
        transfer.putUUID("Caster", owner.getUUID());
        if (!RangerArrowMetadata.stampTransfer(arrow, PowerShotSpell.SPELL_ID, transfer)) return;
        RangerArrowMetadata.setIframeBypass(arrow, true);

        int remaining = stash.getDuration();
        int amplifier = stash.getAmplifier();
        owner.removeEffect(MartialEffectRegistry.POWER_SHOT_STASH.get());
        if (amplifier > 0) {
            owner.addEffect(new MobEffectInstance(MartialEffectRegistry.POWER_SHOT_STASH.get(),
                    remaining, amplifier - 1, stash.isAmbient(),
                    stash.isVisible(), stash.showIcon()));
        }
    }
}
