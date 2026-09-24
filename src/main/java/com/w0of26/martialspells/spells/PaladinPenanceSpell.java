package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.PenanceProjectile;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Iron's translation of frozen Paladins Priest Penance. */
public final class PaladinPenanceSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "penance"
            );

    public static final int RANGE = 20;
    public static final int CAST_TIME_TICKS = 30;
    public static final int CHANNEL_TICKS = 3;
    public static final float CHANNEL_VALUE_MULTIPLIER = 0.50F;
    public static final float DAMAGE_COEFFICIENT = 0.55F;
    public static final float KNOCKBACK_STRENGTH = 0.20F;
    public static final float PROJECTILE_VELOCITY = 0.80F;
    public static final float HOMING_DEGREES_PER_TICK = 16.0F;
    public static final double ABSORPTION_RADIUS = 8.0D;
    public static final int ABSORPTION_DURATION_TICKS = 6 * 20;
    public static final int SHIELD_STACKS_PER_BOLT = 1;
    public static final float SHIELD_POWER_COEFFICIENT = 0.10F;
    public static final float ABSORPTION_HEALTH_PER_STACK = 2.0F;
    public static final int BASE_COOLDOWN_SECONDS = 12;
    public static final int BASE_MANA_COST = 45;

    private static final Map<UUID, ChannelState>
            CHANNELS = new ConcurrentHashMap<>();

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.EPIC)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinPenanceSpell() {
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
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public boolean checkPreCastConditions(
            Level level,
            int spellLevel,
            LivingEntity caster,
            MagicData magicData
    ) {
        return Utils.preCastTargetHelper(
                level,
                caster,
                magicData,
                this,
                RANGE,
                PaladinHolySpellSupport.AIM_ASSIST,
                false,
                target ->
                        target != caster
                                && target.isAlive()
                                && !Utils.shouldHealEntity(
                                        caster,
                                        target
                                )
        );
    }

    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData magicData
    ) {
        if (entity instanceof ServerPlayer player
                && magicData != null) {
            LivingEntity target =
                    PaladinHolySpellSupport.getTarget(
                            level,
                            magicData
                    );

            if (target != null) {
                CHANNELS.put(
                        player.getUUID(),
                        new ChannelState(
                                PaladinChannelSupport.begin(
                                        player,
                                        this,
                                        spellLevel,
                                        CHANNEL_TICKS
                                ),
                                target.getUUID()
                        )
                );
            }
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

        ChannelState state =
                CHANNELS.get(
                        player.getUUID()
                );
        if (state == null) {
            return;
        }

        long gameTime =
                serverLevel.getGameTime();

        while (state.channel.isDue(gameTime)) {
            launchBolt(
                    serverLevel,
                    spellLevel,
                    player,
                    state.targetId
            );
            state.channel.markDue();
        }

        if (state.channel.isComplete(gameTime)
                && magicData.isCasting()
                && magicData.getCastingSpell()
                .getSpell() == this) {
            PaladinChannelSupport.settleFullCast(
                    this,
                    player,
                    spellLevel,
                    magicData
            );
        }
    }

    private void launchBolt(
            ServerLevel level,
            int spellLevel,
            ServerPlayer caster,
            UUID targetId
    ) {
        if (!(level.getEntity(targetId)
                instanceof LivingEntity target)
                || !target.isAlive()
                || target.isRemoved()) {
            return;
        }

        float power =
                getSpellPower(
                        spellLevel,
                        caster
                );

        float damage =
                power
                        * DAMAGE_COEFFICIENT
                        * CHANNEL_VALUE_MULTIPLIER;

        float knockback =
                KNOCKBACK_STRENGTH
                        * CHANNEL_VALUE_MULTIPLIER;

        int shieldStacksPerBolt =
                SHIELD_STACKS_PER_BOLT
                        + (int) Math.floor(
                                SHIELD_POWER_COEFFICIENT
                                        * power
                        );

        /*
         * Preserve Spell Engine's executable flooring order exactly:
         * cap = 2 + floor(0.3 * power). This is intentionally not derived
         * from three times the already-floored per-bolt increment; those two
         * formulas diverge at some non-decimal power values.
         */
        int shieldAmplifierCap =
                CHANNEL_TICKS
                        * SHIELD_STACKS_PER_BOLT
                        - 1
                        + (int) Math.floor(
                                SHIELD_POWER_COEFFICIENT
                                        * CHANNEL_TICKS
                                        * power
                        );

        Vec3 look =
                caster.getLookAngle()
                        .normalize();

        // Frozen Spell Engine LaunchGeometry.launchPoint: 0.5 blocks
        // forward from the shoulder-height launch origin.
        Vec3 launchPoint =
                caster.position()
                        .add(
                                0.0D,
                                caster.getEyeHeight()
                                        - caster.getBbHeight()
                                        * 0.15D,
                                0.0D
                        )
                        .add(
                                look.scale(0.5D)
                        );

        /*
         * Source direct_towards_target defaults false. The bolt launches
         * along the caster's look direction and only then curves toward the
         * sticky target through its 16-degree/tick homing.
         */
        Vec3 direction = look;

        PenanceProjectile projectile =
                new PenanceProjectile(
                        MartialEntityRegistry
                                .PENANCE_PROJECTILE
                                .get(),
                        level,
                        caster,
                        damage,
                        knockback,
                        shieldStacksPerBolt,
                        shieldAmplifierCap,
                        targetId
                );

        projectile.setPos(
                launchPoint.x,
                launchPoint.y,
                launchPoint.z
        );

        projectile.shoot(
                direction.x,
                direction.y,
                direction.z,
                PROJECTILE_VELOCITY,
                0.0F
        );

        level.addFreshEntity(
                projectile
        );

        level.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                MartialSoundRegistry.PENANCE_RELEASE.get(),
                SoundSource.PLAYERS,
                0.5F,
                1.0F
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
        // Each source projectile was already released at its channel midpoint.
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
        ChannelState state =
                entity instanceof ServerPlayer player
                        ? CHANNELS.remove(
                                player.getUUID()
                        )
                        : null;

        if (cancelled
                && state != null
                && entity instanceof ServerPlayer player) {
            PaladinChannelSupport.settleCancelledCosts(
                    this,
                    player,
                    spellLevel,
                    magicData.getCastSource(),
                    state.channel
            );
        }

        super.onServerCastComplete(
                level,
                spellLevel,
                entity,
                magicData,
                cancelled
        );
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        float power =
                getSpellPower(
                        spellLevel,
                        caster
                );

        int stacksPerBolt =
                SHIELD_STACKS_PER_BOLT
                        + (int) Math.floor(
                                SHIELD_POWER_COEFFICIENT
                                        * power
                        );

        return List.of(
                Component.translatable(
                        "ui.martial_spells.priest_channel",
                        CAST_TIME_TICKS / 20.0F,
                        CHANNEL_TICKS
                ),
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(
                                power
                                        * DAMAGE_COEFFICIENT
                                        * CHANNEL_VALUE_MULTIPLIER,
                                2
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.penance_absorption",
                        stacksPerBolt
                                * ABSORPTION_HEALTH_PER_STACK,
                        ABSORPTION_RADIUS,
                        ABSORPTION_DURATION_TICKS
                                / 20.0F
                ),
                Component.translatable(
                        "ui.martial_spells.paladin_target_range",
                        RANGE
                )
        );
    }

    private static final class ChannelState {
        private final PaladinChannelSupport.ChannelState channel;
        private final UUID targetId;

        private ChannelState(
                PaladinChannelSupport.ChannelState channel,
                UUID targetId
        ) {
            this.channel = channel;
            this.targetId = targetId;
        }
    }
}
