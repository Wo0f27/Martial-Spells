package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.spells.StunningStrikeSpell;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StunningStrikeImpactManager {

    /*
     * Temporary timing for the Better Combat placeholder animations.
     * We can tune this when original Stunning Strike animations exist.
     */
    private static final int IMPACT_DELAY_TICKS = 4;

    private static final double ARMOR_SHRED = 0.05D;

    private static final UUID ARMOR_SHRED_MODIFIER_ID =
            UUID.fromString(
                    "32d59568-f575-4203-8d77-11d6caf1fb69"
            );

    private static final AttributeModifier
            STUNNING_STRIKE_ARMOR_SHRED =
            new AttributeModifier(
                    ARMOR_SHRED_MODIFIER_ID,
                    MartialSpells.MOD_ID
                            + ".stunning_strike_armor_shred",
                    ARMOR_SHRED,
                    AttributeModifier.Operation.ADDITION
            );

    private static final Map<UUID, PendingImpact>
            PENDING_IMPACTS =
            new HashMap<>();

    private StunningStrikeImpactManager() {
    }

    public static void begin(
            ServerPlayer player,
            float damage
    ) {
        PENDING_IMPACTS.put(
                player.getUUID(),
                new PendingImpact(
                        player.serverLevel()
                                .getGameTime(),
                        damage
                )
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player
                instanceof ServerPlayer player)) {
            return;
        }

        PendingImpact impact =
                PENDING_IMPACTS.get(
                        player.getUUID()
                );

        if (impact == null) {
            return;
        }

        if (!player.isAlive()
                || player.isDeadOrDying()
                || player.isRemoved()) {
            PENDING_IMPACTS.remove(
                    player.getUUID()
            );
            return;
        }

        long elapsed =
                player.serverLevel()
                        .getGameTime()
                        - impact.startTick;

        if (elapsed < IMPACT_DELAY_TICKS) {
            return;
        }

        PENDING_IMPACTS.remove(
                player.getUUID()
        );

        performImpact(
                player,
                impact.damage
        );
    }

    private static void performImpact(
            ServerPlayer player,
            float damage
    ) {
        /*
         * Re-raycast at the actual contact frame.
         *
         * Turning away or allowing the opponent to escape
         * therefore causes Stunning Strike to whiff.
         */
        LivingEntity target =
                StunningStrikeSpell.findTarget(
                        player
                );

        if (target == null) {
            return;
        }

        AttributeInstance armorShred =
                player.getAttribute(
                        ALObjects.Attributes
                                .ARMOR_SHRED
                                .get()
                );

        boolean damaged;

        if (armorShred == null) {
            /*
             * Defensive fallback. With Apothic Attributes
             * installed this normally cannot occur.
             */
            damaged =
                    target.hurt(
                            MartialDamageTypes
                                    .stunningStrike(
                                            player
                                    ),
                            damage
                    );
        } else {
            /*
             * Ensure no stale temporary modifier exists.
             */
            armorShred.removeModifier(
                    ARMOR_SHRED_MODIFIER_ID
            );

            armorShred.addTransientModifier(
                    STUNNING_STRIKE_ARMOR_SHRED
            );

            try {
                /*
                 * Apothic reads the attacker's Armor Shred
                 * while hurt() resolves armor.
                 */
                damaged =
                        target.hurt(
                                MartialDamageTypes
                                        .stunningStrike(
                                                player
                                        ),
                                damage
                        );
            } finally {
                /*
                 * Stunning Strike's +5% shred applies only
                 * to this individual impact.
                 */
                armorShred.removeModifier(
                        ARMOR_SHRED_MODIFIER_ID
                );
            }
        }

        if (!damaged) {
            return;
        }

        player.serverLevel().playSound(
                null,
                target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS,
                1.0F,
                0.9F
        );

        /*
         * TurtleCore stun is added in Checkpoint 3.
         */
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        PENDING_IMPACTS.remove(
                event.getEntity().getUUID()
        );
    }

    private static final class PendingImpact {

        private final long startTick;
        private final float damage;

        private PendingImpact(
                long startTick,
                float damage
        ) {
            this.startTick = startTick;
            this.damage = damage;
        }
    }
}