package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Iron's translation of frozen Paladins Holy Light (holy_beam).
 *
 * <p>The source is a five-second beam with 25 evenly distributed channel
 * deliveries. Spell Engine scales each delivery by the authored interval
 * (0.2 seconds), so the 0.4/0.8 source coefficients become 0.08 heal and
 * 0.16 damage per pulse while preserving the exact full-channel throughput.</p>
 */
public final class PaladinHolyBeamSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "holy_beam"
            );

    public static final int RANGE = 32;
    public static final int CAST_TIME_TICKS = 5 * 20;
    public static final int CHANNEL_TICKS = 25;
    public static final float CHANNEL_VALUE_MULTIPLIER = 0.20F;
    public static final int BASE_COOLDOWN_SECONDS = 10;
    public static final float HEAL_COEFFICIENT = 0.40F;
    public static final float DAMAGE_COEFFICIENT = 0.80F;
    public static final double KNOCKBACK_STRENGTH = 0.50D;
    public static final int BASE_MANA_COST = 40;

    private static final Map<UUID, PaladinChannelSupport.ChannelState>
            CHANNELS = new ConcurrentHashMap<>();

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.RARE)
                    .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
                    .setMaxLevel(1)
                    .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
                    .build();

    public PaladinHolyBeamSpell() {
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
        // LONG is only the held-cast transport. P3 drives Spell Engine's exact
        // midpoint channel cadence from onServerCastTick.
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(
                MartialSoundRegistry.HOLY_BEAM_START_CASTING.get()
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

            player.serverLevel().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    MartialSoundRegistry.HOLY_BEAM_CASTING.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
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

        while (state.isDue(gameTime)) {
            applyPulse(
                    serverLevel,
                    spellLevel,
                    player
            );
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

    private void applyPulse(
            ServerLevel level,
            int spellLevel,
            ServerPlayer caster
    ) {
        /*
         * Frozen Spell Engine BEAM targeting is not a first-hit raycast:
         * every eligible entity whose bounding box intersects the live beam
         * receives the delivery, ordered nearest-to-farthest, while a block
         * cuts the beam off. Rebuild that behavior on Iron's own intersection
         * helper instead of approximating BEAM with RaycastBuilder#build().
         */
        for (LivingEntity target : targetsOnBeam(
                level,
                caster
        )) {
            if (Utils.shouldHealEntity(caster, target)) {
                PaladinHolySpellSupport.heal(
                        this,
                        level,
                        spellLevel,
                        caster,
                        target,
                        HEAL_COEFFICIENT
                                * CHANNEL_VALUE_MULTIPLIER
                );

                playImpactSound(
                        level,
                        target,
                        true
                );
                continue;
            }

            boolean damaged =
                    PaladinHolySpellSupport.damage(
                            this,
                            level,
                            spellLevel,
                            caster,
                            target,
                            DAMAGE_COEFFICIENT
                                    * CHANNEL_VALUE_MULTIPLIER
                    );

            if (damaged) {
                target.knockback(
                        KNOCKBACK_STRENGTH
                                * CHANNEL_VALUE_MULTIPLIER,
                        caster.getX() - target.getX(),
                        caster.getZ() - target.getZ()
                );

                playImpactSound(
                        level,
                        target,
                        false
                );
            }
        }
    }

    private static List<LivingEntity> targetsOnBeam(
            ServerLevel level,
            ServerPlayer caster
    ) {
        Vec3 start = caster.getEyePosition();
        Vec3 end =
                start.add(
                        caster.getLookAngle()
                                .normalize()
                                .scale(RANGE)
                );

        HitResult blockHit =
                level.clip(
                        new ClipContext(
                                start,
                                end,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                caster
                        )
                );

        Vec3 rayEnd =
                blockHit.getType()
                        == HitResult.Type.BLOCK
                        ? blockHit.getLocation()
                        : end;

        AABB searchBox =
                caster.getBoundingBox()
                        .expandTowards(
                                rayEnd.subtract(start)
                        )
                        .inflate(
                                PaladinHolySpellSupport.AIM_ASSIST
                        );

        List<TargetHit> hits =
                new ArrayList<>();

        for (Entity entity : level.getEntities(
                caster,
                searchBox,
                candidate ->
                        candidate instanceof LivingEntity living
                                && living.isAlive()
                                && !living.isSpectator()
                                && Utils.canHitWithRaycast(
                                        candidate
                                )
        )) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            HitResult hit =
                    Utils.checkEntityIntersecting(
                            target,
                            start,
                            rayEnd,
                            PaladinHolySpellSupport.AIM_ASSIST
                    );

            if (hit.getType()
                    == HitResult.Type.MISS) {
                continue;
            }

            hits.add(
                    new TargetHit(
                            target,
                            hit.getLocation()
                                    .distanceToSqr(start)
                    )
            );
        }

        hits.sort(
                Comparator.comparingDouble(
                        TargetHit::distanceToSourceSqr
                )
        );

        return hits.stream()
                .map(TargetHit::target)
                .toList();
    }

    private record TargetHit(
            LivingEntity target,
            double distanceToSourceSqr
    ) {
    }

    private static void playImpactSound(
            ServerLevel level,
            LivingEntity target,
            boolean healing
    ) {
        level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                healing
                        ? MartialSoundRegistry.HOLY_BEAM_HEAL.get()
                        : MartialSoundRegistry.HOLY_BEAM_DAMAGE.get(),
                SoundSource.PLAYERS,
                0.7F,
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
        // All gameplay was delivered at source channel frames.
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

        if (cancelled
                && state != null
                && entity instanceof ServerPlayer player) {
            PaladinChannelSupport.settleCancelledCosts(
                    this,
                    player,
                    spellLevel,
                    magicData.getCastSource(),
                    state
            );
        }

        super.onServerCastComplete(
                level,
                spellLevel,
                entity,
                magicData,
                cancelled
        );

        if (state != null
                && entity instanceof ServerPlayer player
                && player.isAlive()
                && !player.isDeadOrDying()) {
            player.serverLevel().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    MartialSoundRegistry.HOLY_BEAM_RELEASE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
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
                        "ui.irons_spellbooks.healing",
                        Utils.stringTruncation(
                                PaladinHolySpellSupport
                                        .getScaledAmount(
                                                this,
                                                spellLevel,
                                                caster,
                                                HEAL_COEFFICIENT
                                        ),
                                2
                        )
                ),
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(
                                PaladinHolySpellSupport
                                        .getScaledAmount(
                                                this,
                                                spellLevel,
                                                caster,
                                                DAMAGE_COEFFICIENT
                                        ),
                                2
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.paladin_target_range",
                        RANGE
                )
        );
    }
}
