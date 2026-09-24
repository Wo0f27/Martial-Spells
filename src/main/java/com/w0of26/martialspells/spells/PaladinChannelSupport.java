package com.w0of26.martialspells.spells;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.common.MinecraftForge;

/**
 * Shared transport for the frozen Paladins/Priests Spell Engine channels.
 *
 * <p>Spell Engine distributes channel deliveries at the midpoints of equal
 * intervals across the effective cast duration. Iron's 1.20.1 CONTINUOUS
 * scheduler has different timing, so CP11 uses a LONG cast only as the held
 * casting transport and drives the source channel cadence here.</p>
 */
final class PaladinChannelSupport {
    private PaladinChannelSupport() {
    }

    static ChannelState begin(
            ServerPlayer player,
            AbstractSpell spell,
            int spellLevel,
            int channelTicks
    ) {
        return new ChannelState(
                player.serverLevel().getGameTime(),
                Math.max(1, spell.getEffectiveCastTime(spellLevel, player)),
                channelTicks
        );
    }

    static void settleFullCast(
            AbstractSpell spell,
            ServerPlayer player,
            int spellLevel,
            MagicData magicData
    ) {
        CastSource castSource = magicData.getCastSource();

        spell.castSpell(
                player.serverLevel(),
                spellLevel,
                player,
                castSource,
                true
        );

        if (castSource == CastSource.SCROLL) {
            Scroll.attemptRemoveScrollAfterCast(player);
        }

        spell.onServerCastComplete(
                player.serverLevel(),
                spellLevel,
                player,
                magicData,
                false
        );
        player.stopUsingItem();
    }

    static void settleCancelledCosts(
            AbstractSpell spell,
            ServerPlayer player,
            int spellLevel,
            CastSource castSource,
            ChannelState state
    ) {
        float progress = state.progress(
                player.serverLevel().getGameTime()
        );

        applyProportionalMana(
                spell,
                player,
                spellLevel,
                castSource,
                progress
        );
        applyProportionalCooldown(
                spell,
                player,
                castSource,
                progress
        );

        if (castSource == CastSource.SCROLL
                && progress > 0.0F) {
            Scroll.attemptRemoveScrollAfterCast(player);
        }
    }

    private static void applyProportionalMana(
            AbstractSpell spell,
            ServerPlayer player,
            int spellLevel,
            CastSource castSource,
            float progress
    ) {
        if (progress <= 0.0F) {
            return;
        }

        int proportionalMana = Math.round(
                spell.getManaCost(spellLevel) * progress
        );

        /*
         * A full Iron's cast always exposes SpellOnCastEvent before consuming
         * mana. Partial P3 channels do the same, with the proportional cost as
         * the event's original cost so mana-cost integrations still work.
         */
        SpellOnCastEvent event =
                new SpellOnCastEvent(
                        player,
                        spell.getSpellId(),
                        spellLevel,
                        proportionalMana,
                        spell.getSchoolType(),
                        castSource
                );
        MinecraftForge.EVENT_BUS.post(event);

        if (!castSource.consumesMana()
                || (player.isCreative()
                && !ServerConfigs.CREATIVE_MANA_COST.get())
                || event.getManaCost() <= 0) {
            return;
        }

        MagicData magicData =
                MagicData.getPlayerMagicData(player);

        magicData.setMana(
                Math.max(
                        0.0F,
                        magicData.getMana()
                                - event.getManaCost()
                )
        );

        PacketDistributor.sendToPlayer(
                player,
                new SyncManaPacket(magicData)
        );
    }

    private static void applyProportionalCooldown(
            AbstractSpell spell,
            ServerPlayer player,
            CastSource castSource,
            float progress
    ) {
        if (castSource == CastSource.SCROLL
                || (player.isCreative()
                && !ServerConfigs.CREATIVE_COOLDOWN.get())
                || progress <= 0.0F) {
            return;
        }

        int fullEffectiveCooldown =
                MagicManager.getEffectiveSpellCooldown(
                        spell,
                        player,
                        castSource
                );

        int proportionalCooldown =
                Math.round(
                        fullEffectiveCooldown * progress
                );

        if (proportionalCooldown <= 0) {
            return;
        }

        /*
         * Preserve Iron's normal cooldown integration surface. The event sees
         * the actual partial cooldown being applied, so other Iron's addons
         * can cancel or modify it exactly as they can a normal cast.
         */
        SpellCooldownAddedEvent.Pre pre =
                new SpellCooldownAddedEvent.Pre(
                        proportionalCooldown,
                        spell,
                        player,
                        castSource
                );

        if (MinecraftForge.EVENT_BUS.post(pre)) {
            return;
        }

        int finalCooldown =
                Math.max(
                        0,
                        pre.getEffectiveCooldown()
                );

        if (finalCooldown <= 0) {
            return;
        }

        var cooldowns =
                MagicData.getPlayerMagicData(player)
                        .getPlayerCooldowns();

        cooldowns.addCooldown(
                spell,
                finalCooldown
        );
        cooldowns.syncToPlayer(player);

        MinecraftForge.EVENT_BUS.post(
                new SpellCooldownAddedEvent.Post(
                        finalCooldown,
                        spell,
                        player,
                        castSource
                )
        );
    }

    static final class ChannelState {
        private final long startTick;
        private final int effectiveCastTicks;
        private final int channelTicks;
        private int nextImpactIndex;

        private ChannelState(
                long startTick,
                int effectiveCastTicks,
                int channelTicks
        ) {
            this.startTick = startTick;
            this.effectiveCastTicks = effectiveCastTicks;
            this.channelTicks = Math.max(1, channelTicks);
        }

        long elapsed(long gameTime) {
            return Math.max(
                    0L,
                    gameTime - startTick
            );
        }

        boolean isDue(long gameTime) {
            if (nextImpactIndex >= channelTicks) {
                return false;
            }

            float interval =
                    effectiveCastTicks
                            / (float) channelTicks;
            float dueAt =
                    interval
                            * (nextImpactIndex + 0.5F);

            return elapsed(gameTime) >= dueAt;
        }

        void markDue() {
            nextImpactIndex++;
        }

        boolean isComplete(long gameTime) {
            return elapsed(gameTime)
                    >= effectiveCastTicks;
        }

        float progress(long gameTime) {
            return Mth.clamp(
                    elapsed(gameTime)
                            / (float) effectiveCastTicks,
                    0.0F,
                    1.0F
            );
        }

        int deliveredImpacts() {
            return nextImpactIndex;
        }
    }
}
