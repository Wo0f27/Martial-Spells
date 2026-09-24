package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Iron's translation of frozen Paladins Priest Levitate. */
public final class PaladinLevitateSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "levitate"
            );

    public static final int CAST_TIME_TICKS = 30;
    public static final int CHANNEL_TICKS = 4;
    public static final double UPWARD_VELOCITY = 0.15D;
    public static final int LEVITATE_DURATION_TICKS = 5 * 20;
    public static final int BASE_COOLDOWN_SECONDS = 24;
    public static final int BASE_MANA_COST = 25;

    private static final UUID MOVEMENT_LOCK_ID =
            UUID.fromString(
                    "b12a9e75-7a51-4a79-a7ef-52c39845c0ef"
            );

    private static final Map<UUID, PaladinChannelSupport.ChannelState>
            CHANNELS = new ConcurrentHashMap<>();

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.RARE)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinLevitateSpell() {
        baseManaCost = BASE_MANA_COST;
        manaCostPerLevel = 0;
        baseSpellPower =
                PaladinHolySpellSupport.SOURCE_POWER_REFERENCE;
        spellPowerPerLevel = 0;
        castTime = CAST_TIME_TICKS;
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
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(
                MartialSoundRegistry.HOLY_WARD_IMPACT.get()
        );
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData magicData
    ) {
        if (entity instanceof ServerPlayer player) {
            CHANNELS.put(
                    player.getUUID(),
                    PaladinChannelSupport.begin(
                            player,
                            this,
                            spellLevel,
                            CHANNEL_TICKS
                    )
            );
            applyMovementLock(player);
        }

        super.onServerPreCast(
                level,
                spellLevel,
                entity,
                magicData
        );
    }

    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData magicData
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player)
                || magicData == null) {
            return;
        }

        PaladinChannelSupport.ChannelState state =
                CHANNELS.get(player.getUUID());
        if (state == null) {
            return;
        }

        long gameTime = serverLevel.getGameTime();

        PaladinVfx.levitateChannel(
                serverLevel,
                player
        );

        while (state.isDue(gameTime)) {
            applyLift(player);
            state.markDue();
        }

        if (state.isComplete(gameTime)
                && magicData.isCasting()
                && magicData.getCastingSpell().getSpell() == this) {
            PaladinChannelSupport.settleFullCast(
                    this,
                    player,
                    spellLevel,
                    magicData
            );
        }
    }

    private static void applyLift(
            ServerPlayer player
    ) {
        // Source reset_velocity=true: every release overwrites prior motion.
        player.setDeltaMovement(
                new Vec3(
                        0.0D,
                        UPWARD_VELOCITY,
                        0.0D
                )
        );
        player.hasImpulse = true;
        // Force the server-authoritative reset-velocity kick back to the
        // owning client immediately; unlike a normal Iron's onCast impulse,
        // P3 channel impacts do not get an OnClientCast packet per release.
        player.hurtMarked = true;

        player.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry.LEVITATE.get(),
                        LEVITATE_DURATION_TICKS,
                        0,
                        false,
                        true,
                        true
                ),
                player
        );
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData magicData
    ) {
        // The four source impacts were delivered at their midpoint frames.
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
            MagicData magicData,
            boolean cancelled
    ) {
        PaladinChannelSupport.ChannelState state =
                entity instanceof ServerPlayer player
                        ? CHANNELS.remove(player.getUUID())
                        : null;

        if (entity instanceof ServerPlayer player) {
            removeMovementLock(player);

            if (cancelled && state != null) {
                PaladinChannelSupport.settleCancelledCosts(
                        this,
                        player,
                        spellLevel,
                        magicData.getCastSource(),
                        state
                );
            }
        }

        super.onServerCastComplete(
                level,
                spellLevel,
                entity,
                magicData,
                cancelled
        );
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

        movement.removeModifier(
                MOVEMENT_LOCK_ID
        );
        movement.addTransientModifier(
                new AttributeModifier(
                        MOVEMENT_LOCK_ID,
                        MartialSpells.MOD_ID
                                + ".levitate_channel_lock",
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
            movement.removeModifier(
                    MOVEMENT_LOCK_ID
            );
        }
    }

    public static void clearState(
            ServerPlayer player
    ) {
        CHANNELS.remove(
                player.getUUID()
        );
        removeMovementLock(
                player
        );
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.priest_channel",
                        CAST_TIME_TICKS / 20.0F,
                        CHANNEL_TICKS
                ),
                Component.translatable(
                        "ui.martial_spells.levitate_linger",
                        LEVITATE_DURATION_TICKS / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.levitate_soft_landing",
                        com.w0of26.martialspells.effects
                                .LevitateEffect.SLOW_FALLING_TICKS
                                / 20.0F
                )
        );
    }
}
