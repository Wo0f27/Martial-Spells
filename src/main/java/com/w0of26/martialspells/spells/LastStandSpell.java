package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncLastStandReleaseAnimationPacket;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import com.w0of26.martialspells.visual.LastStandVisuals;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.item.Scroll;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Forge/Iron's translation of frozen Rogues Last Stand.
 *
 * <p>Iron's native CONTINUOUS cadence does not match Spell Engine 1.10.5.034:
 * for a 50-tick cast it fires too early and terminates around tick 41. This
 * spell therefore uses Iron's held LONG-cast state/UI but schedules the exact
 * upstream channel impacts at ticks 5, 15, 25, 35 and 45, releasing at 50.</p>
 */
public final class LastStandSpell extends AbstractSpell
        implements MartialTechnique {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "last_stand"
            );

    public static final ResourceLocation CAST_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "one_handed_ground_charge"
            );

    public static final ResourceLocation RELEASE_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "one_handed_shout_release"
            );

    public static final int MAX_LEVEL = 1;
    public static final int CAST_TIME_TICKS = 50;
    public static final int CHANNEL_TICKS = 5;
    public static final int CHANNEL_INTERVAL_TICKS = 10;
    public static final int CHANNEL_OFFSET_TICKS = 5;
    public static final int EFFECT_DURATION_TICKS = 200;
    public static final int AMPLIFIER_CAP = 4;
    public static final double MAX_HEALTH_PER_STACK = 0.20D;
    public static final double KNOCKBACK_RESISTANCE_PER_STACK = 0.20D;

    /**
     * Source heal coefficient 0.2 multiplied by the 0.5-second channel-output
     * multiplier = 0.1 of current max health per scheduled channel impact.
     */
    public static final float HEAL_CURRENT_MAX_HEALTH_FRACTION = 0.10F;

    public static final int BASE_COOLDOWN_SECONDS = 60;

    private static final int[] SOURCE_CHANNEL_SCHEDULE =
            {5, 15, 25, 35, 45};

    private static final UUID MOVEMENT_LOCK_ID =
            UUID.fromString(
                    "147f9a4a-ec95-44e5-b998-294f3bbfc98e"
            );

    private static final Map<UUID, ChannelState> CHANNELS =
            new ConcurrentHashMap<>();

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.EPIC)
                    .setSchoolResource(
                            MartialSchoolRegistry.MARTIAL_RESOURCE
                    )
                    .setMaxLevel(MAX_LEVEL)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public LastStandSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = CAST_TIME_TICKS;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.WARRIOR;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        /*
         * Source mechanic is CHANNEL. LONG is intentionally used as Iron's
         * transport because its CONTINUOUS scheduler has different 1.20.1
         * timings. onServerCastTick below reproduces the source cadence.
         */
        return CastType.LONG;
    }

    @Override
    public int getEffectiveCastTime(
            int spellLevel,
            @Nullable LivingEntity entity
    ) {
        // Spell Engine HEALTH has no haste source here: frozen 2.5 seconds.
        return CAST_TIME_TICKS;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(
                MartialSoundRegistry.LAST_STAND_START.get()
        );
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        // Release audio is settled from onServerCastComplete for both full
        // completion and an intentional early channel release.
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return new AnimationHolder(
                CAST_ANIMATION,
                true,
                false
        );
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return new AnimationHolder(
                RELEASE_ANIMATION,
                true,
                false
        );
    }

    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (entity instanceof ServerPlayer player) {
            CHANNELS.put(
                    player.getUUID(),
                    new ChannelState(
                            player.serverLevel().getGameTime()
                    )
            );

            applyMovementLock(player);

            player.serverLevel().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    MartialSoundRegistry.LAST_STAND_CASTING.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        super.onServerPreCast(
                level,
                spellLevel,
                entity,
                playerMagicData
        );
    }

    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player)) {
            return;
        }

        ChannelState state =
                CHANNELS.get(player.getUUID());
        if (state == null) {
            return;
        }

        LastStandVisuals.spawnCasting(
                serverLevel,
                player
        );

        long elapsed =
                serverLevel.getGameTime()
                        - state.startTick;

        while (state.nextImpactIndex
                < SOURCE_CHANNEL_SCHEDULE.length
                && elapsed
                >= SOURCE_CHANNEL_SCHEDULE[
                        state.nextImpactIndex
                ]) {
            applyChannelImpact(player);
            state.nextImpactIndex++;
        }

        /*
         * A Spell Engine CHANNEL releases automatically when its authored
         * duration ends. Iron's LONG transport can otherwise wait for the use
         * key to be released after reaching zero, so settle the full cast at
         * exactly source tick 50 if it is still active.
         */
        if (elapsed >= CAST_TIME_TICKS
                && playerMagicData != null
                && playerMagicData.isCasting()
                && playerMagicData.getCastingSpell().getSpell() == this) {
            CastSource castSource =
                    playerMagicData.getCastSource();

            castSpell(
                    serverLevel,
                    spellLevel,
                    player,
                    castSource,
                    true
            );

            if (castSource == CastSource.SCROLL) {
                Scroll.attemptRemoveScrollAfterCast(player);
            }

            onServerCastComplete(
                    serverLevel,
                    spellLevel,
                    player,
                    playerMagicData,
                    false
            );
            player.stopUsingItem();
        }
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData magicData
    ) {
        /*
         * Gameplay is already delivered at the five exact source channel
         * frames. The final Iron cast only settles its normal full cooldown.
         */
        super.onCast(
                level,
                spellLevel,
                entity,
                castSource,
                magicData
        );
    }

    @Override
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        ChannelState state = entity instanceof ServerPlayer player
                ? CHANNELS.remove(player.getUUID())
                : null;

        CastSource castSource =
                playerMagicData.getCastSource();

        if (entity instanceof ServerPlayer player) {
            removeMovementLock(player);

            if (cancelled && state != null) {
                applyProportionalCooldown(
                        player,
                        castSource,
                        state
                );

                if (castSource == CastSource.SCROLL
                        && state.progress(
                                player.serverLevel()
                                        .getGameTime()
                        ) > 0.0F) {
                    Scroll.attemptRemoveScrollAfterCast(
                            player
                    );
                }
            }
        }

        super.onServerCastComplete(
                level,
                spellLevel,
                entity,
                playerMagicData,
                cancelled
        );

        if (entity instanceof ServerPlayer player
                && state != null
                && player.isAlive()
                && !player.isDeadOrDying()) {
            player.serverLevel().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    MartialSoundRegistry.LAST_STAND_RELEASE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            /*
             * Iron's deliberately suppresses finish animations when LONG casts
             * are cancelled. Source CHANNEL releases still play their release
             * animation, so restore it after Iron's cancelled-finish packet.
             */
            if (cancelled) {
                MartialNetwork.sendToTrackingAndSelf(
                        new SyncLastStandReleaseAnimationPacket(
                                player.getUUID()
                        ),
                        player
                );
            }
        }
    }

    private static void applyChannelImpact(
            ServerPlayer player
    ) {
        MobEffectInstance current =
                player.getEffect(
                        MartialEffectRegistry.LAST_STAND.get()
                );

        int amplifier = current == null
                ? 0
                : Math.min(
                        current.getAmplifier() + 1,
                        AMPLIFIER_CAP
                );

        /*
         * Upstream impact ordering is STATUS_EFFECT first, HEAL second.
         * Therefore getMaxHealth() below already includes the newly applied
         * +20% base max-health stack.
         */
        player.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry.LAST_STAND.get(),
                        EFFECT_DURATION_TICKS,
                        amplifier,
                        false,
                        false,
                        true
                ),
                player
        );

        player.heal(
                player.getMaxHealth()
                        * HEAL_CURRENT_MAX_HEALTH_FRACTION
        );
    }

    private void applyProportionalCooldown(
            ServerPlayer player,
            CastSource castSource,
            ChannelState state
    ) {
        var cooldowns =
                MagicData.getPlayerMagicData(player)
                        .getPlayerCooldowns();

        cooldowns.removeCooldown(getSpellId());

        if (castSource == CastSource.SCROLL) {
            cooldowns.syncToPlayer(player);
            return;
        }

        float progress = state.progress(
                player.serverLevel().getGameTime()
        );

        int fullEffectiveCooldown =
                MagicManager.getEffectiveSpellCooldown(
                        this,
                        player,
                        castSource
                );

        int proportionalCooldown =
                Math.round(
                        fullEffectiveCooldown * progress
                );

        if (proportionalCooldown > 0) {
            cooldowns.addCooldown(
                    this,
                    proportionalCooldown
            );
        }

        cooldowns.syncToPlayer(player);
    }

    private static void applyMovementLock(
            ServerPlayer player
    ) {
        AttributeInstance movement =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );
        if (movement == null) {
            return;
        }

        movement.removeModifier(MOVEMENT_LOCK_ID);
        movement.addTransientModifier(
                new AttributeModifier(
                        MOVEMENT_LOCK_ID,
                        MartialSpells.MOD_ID
                                + ".last_stand_channel_lock",
                        -1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                )
        );
    }

    private static void removeMovementLock(
            ServerPlayer player
    ) {
        AttributeInstance movement =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );
        if (movement != null) {
            movement.removeModifier(MOVEMENT_LOCK_ID);
        }
    }

    public static void clearState(ServerPlayer player) {
        CHANNELS.remove(player.getUUID());
        removeMovementLock(player);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.last_stand_channel",
                        CAST_TIME_TICKS / 20.0F,
                        CHANNEL_TICKS
                ),
                Component.translatable(
                        "ui.martial_spells.last_stand_health",
                        Math.round(
                                MAX_HEALTH_PER_STACK
                                        * 100.0D
                        ),
                        AMPLIFIER_CAP + 1
                ),
                Component.translatable(
                        "ui.martial_spells.last_stand_knockback",
                        Math.round(
                                KNOCKBACK_RESISTANCE_PER_STACK
                                        * 100.0D
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.last_stand_heal",
                        Math.round(
                                HEAL_CURRENT_MAX_HEALTH_FRACTION
                                        * 100.0F
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.last_stand_duration",
                        EFFECT_DURATION_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.last_stand_cooldown",
                        BASE_COOLDOWN_SECONDS
                )
        );
    }

    private static final class ChannelState {
        private final long startTick;
        private int nextImpactIndex;

        private ChannelState(long startTick) {
            this.startTick = startTick;
        }

        private float progress(long gameTime) {
            return Mth.clamp(
                    (gameTime - startTick)
                            / (float) CAST_TIME_TICKS,
                    0.0F,
                    1.0F
            );
        }
    }
}
