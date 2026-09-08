package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.quivering.QuiveringPalmCastData;
import com.w0of26.martialspells.quivering.QuiveringPalmCombatHelper;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Quivering Palm mark/detonation technique.
 *
 * The initial cast places a Generation 0 mark. Each successful recast
 * detonates one currently marked target, damages nearby enemies, and
 * turns surviving valid AOE victims into the next generation of marks.
 * Iron's native recast system owns the visible timer/count and starts
 * the configured cooldown when the chain is exhausted, times out, or
 * can no longer propagate.
 */
public final class QuiveringPalmSpell
        extends AbstractMonkTechniqueSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "quivering_palm"
            );

    private static final int MAX_LEVEL = 5;

    /*
     * RecastInstance starts with totalRecasts - 1 remaining recasts.
     * Five total casts therefore means:
     *   1 initial mark + 4 detonation recasts.
     */
    private static final int TOTAL_CHAIN_CASTS = 5;

    private static final int RECAST_WINDOW_SECONDS = 30;
    private static final int RECAST_WINDOW_TICKS =
            RECAST_WINDOW_SECONDS * 20;

    private static final int COOLDOWN_SECONDS = 90;

    private static final float TARGET_RANGE = 4.0F;
    private static final float RAYCAST_INFLATION = 0.30F;

    private static final int MAX_PROPAGATION_GENERATION = 3;

    private static final float[] PROPAGATION_RADII = {
            4.0F,
            4.5F,
            5.0F,
            5.5F,
            6.0F
    };

    /*
     * Tier V Monk Codex caps stored Ki at 10, so Quivering Palm must
     * remain castable at every technique level without requiring
     * Stillness of Mind. Recasts remain free after the initial mark.
     */
    private static final int[] KI_COSTS = {
            6,
            7,
            8,
            9,
            10
    };

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setSchoolResource(
                            MartialSchoolRegistry
                                    .MARTIAL_RESOURCE
                    )
                    .setMinRarity(
                            SpellRarity.LEGENDARY
                    )
                    .setMaxLevel(
                            MAX_LEVEL
                    )
                    .setCooldownSeconds(
                            COOLDOWN_SECONDS
                    )
                    .build();

    public QuiveringPalmSpell() {
        super(MAX_LEVEL);
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
        return CastType.INSTANT;
    }

    @Override
    public int getRecastCount(
            int spellLevel,
            @Nullable LivingEntity entity
    ) {
        return TOTAL_CHAIN_CASTS;
    }

    public static int getRecastWindowSeconds() {
        return RECAST_WINDOW_SECONDS;
    }

    public static int getDetonationRecastCount() {
        return TOTAL_CHAIN_CASTS - 1;
    }

    public static float getTargetRange() {
        return TARGET_RANGE;
    }

    public static float getPropagationRadius(
            int spellLevel
    ) {
        int index = Math.max(
                0,
                Math.min(
                        spellLevel - 1,
                        PROPAGATION_RADII.length - 1
                )
        );
        return PROPAGATION_RADII[index];
    }

    private int getBaseKiCost(int spellLevel) {
        return KI_COSTS[
                getTechniqueLevelIndex(spellLevel)
                ];
    }

    @Override
    public CastResult canBeCastedBy(
            int spellLevel,
            CastSource castSource,
            MagicData magicData,
            Player player
    ) {
        CastResult normalResult =
                super.canBeCastedBy(
                        spellLevel,
                        castSource,
                        magicData,
                        player
                );

        if (!normalResult.isSuccess()) {
            return normalResult;
        }

        if (!(player
                instanceof ServerPlayer serverPlayer)) {
            return failure(
                    "ui.martial_spells."
                            + "server_player_required"
            );
        }

        CastResult sourceResult =
                validateMonkTechniqueSource(
                        castSource,
                        player
                );

        if (!sourceResult.isSuccess()) {
            return sourceResult;
        }

        LivingEntity target = findTarget(serverPlayer);
        if (target == null) {
            return failure(
                    "ui.martial_spells."
                            + "no_quivering_palm_target"
            );
        }

        var recasts = magicData.getPlayerRecasts();
        if (recasts.hasRecastForSpell(getSpellId())) {
            RecastInstance recastInstance =
                    recasts.getRecastInstance(
                            getSpellId()
                    );

            if (recastInstance == null
                    || !(recastInstance.getCastData()
                    instanceof QuiveringPalmCastData castData)
                    || !castData.isActiveMark(
                    target.getUUID()
            )) {
                return failure(
                        "ui.martial_spells."
                                + "quivering_palm_target_not_marked"
                );
            }

            /*
             * Recasts consume no additional Ki.
             */
            return success();
        }

        if (!hasTechniqueKi(
                serverPlayer,
                getBaseKiCost(spellLevel)
        )) {
            return failure(
                    "ui.martial_spells.not_enough_ki"
            );
        }

        return success();
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        if (!(caster
                instanceof ServerPlayer player)) {
            return;
        }

        if (!isValidMonkTechniqueSource(
                castSource,
                player
        )) {
            return;
        }

        LivingEntity target = findTarget(player);
        if (target == null) {
            return;
        }

        var recasts = magicData.getPlayerRecasts();
        boolean recastActive =
                recasts.hasRecastForSpell(
                        getSpellId()
                );

        if (!recastActive) {
            if (!consumeTechniqueKi(
                    player,
                    getBaseKiCost(spellLevel)
            )) {
                return;
            }

            QuiveringPalmCastData castData =
                    new QuiveringPalmCastData();
            castData.replaceMarks(
                    List.of(target.getUUID()),
                    0
            );

            recasts.addRecast(
                    new RecastInstance(
                            getSpellId(),
                            clampTechniqueLevel(spellLevel),
                            TOTAL_CHAIN_CASTS,
                            RECAST_WINDOW_TICKS,
                            castSource,
                            castData
                    ),
                    magicData
            );

            spawnMarkPreview(
                    (ServerLevel) level,
                    target
            );
        } else {
            RecastInstance recastInstance =
                    recasts.getRecastInstance(
                            getSpellId()
                    );

            if (recastInstance == null
                    || !(recastInstance.getCastData()
                    instanceof QuiveringPalmCastData castData)
                    || !castData.isActiveMark(
                    target.getUUID()
            )) {
                return;
            }

            ServerLevel serverLevel =
                    (ServerLevel) level;

            int currentGeneration =
                    castData.getActiveGeneration();

            QuiveringPalmCombatHelper.DetonationResult
                    detonationResult =
                    QuiveringPalmCombatHelper.detonate(
                            player,
                            target,
                            spellLevel,
                            currentGeneration,
                            getPropagationRadius(
                                    spellLevel
                            )
                    );

            spawnDetonationPreview(
                    serverLevel,
                    target
            );

            if (currentGeneration
                    < MAX_PROPAGATION_GENERATION) {
                int nextGeneration =
                        currentGeneration + 1;

                List<LivingEntity> propagatedTargets =
                        detonationResult
                                .survivingSecondaryHits()
                                .stream()
                                .filter(candidate ->
                                        !castData.hasEverBeenMarked(
                                                candidate.getUUID()
                                        )
                                )
                                .toList();

                castData.advanceMarks(
                        propagatedTargets
                                .stream()
                                .map(LivingEntity::getUUID)
                                .toList(),
                        nextGeneration
                );

                for (LivingEntity propagatedTarget
                        : propagatedTargets) {
                    spawnMarkPreview(
                            serverLevel,
                            propagatedTarget
                    );
                }

                if (propagatedTargets.isEmpty()) {
                    finishChainAfterCurrentCast(
                            magicData,
                            recastInstance
                    );
                }
            } else {
                castData.retireCurrentMarks();
                finishChainAfterCurrentCast(
                        magicData,
                        recastInstance
                );
            }
        }

        super.onCast(
                level,
                spellLevel,
                caster,
                castSource,
                magicData
        );
    }

    /**
     * Collapses Iron's remaining recast count to one without removing
     * the recast inside onCast. AbstractSpell will perform the normal
     * final decrement immediately after this method returns, producing
     * exactly one normal recast-finished/cooldown transition.
     */
    private void finishChainAfterCurrentCast(
            MagicData magicData,
            RecastInstance recastInstance
    ) {
        var recasts = magicData.getPlayerRecasts();

        while (recastInstance.getRemainingRecasts() > 1
                && recasts.hasRecastForSpell(
                getSpellId()
        )) {
            recasts.decrementRecastCount(
                    getSpellId()
            );
        }
    }

    /*
     * Temporary checkpoint VFX. These make mark propagation and
     * detonation observable while final Quivering Palm presentation is
     * deferred to the polish pass.
     */
    private static void spawnMarkPreview(
            ServerLevel level,
            LivingEntity target
    ) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                target.getX(),
                target.getY()
                        + target.getBbHeight()
                        + 0.15D,
                target.getZ(),
                8,
                0.20D,
                0.08D,
                0.20D,
                0.01D
        );
    }

    private static void spawnDetonationPreview(
            ServerLevel level,
            LivingEntity target
    ) {
        level.sendParticles(
                ParticleTypes.CRIT,
                target.getX(),
                target.getY()
                        + target.getBbHeight() * 0.5D,
                target.getZ(),
                24,
                0.35D,
                0.35D,
                0.35D,
                0.08D
        );
    }

    @Override
    public void onRecastFinished(
            ServerPlayer serverPlayer,
            RecastInstance recastInstance,
            RecastResult recastResult,
            ICastDataSerializable castDataSerializable
    ) {
        super.onRecastFinished(
                serverPlayer,
                recastInstance,
                recastResult,
                castDataSerializable
        );
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new QuiveringPalmCastData();
    }

    @Nullable
    public static LivingEntity findTarget(
            ServerPlayer player
    ) {
        HitResult hitResult =
                RaycastBuilder
                        .begin(
                                player.level(),
                                player
                        )
                        .range(
                                TARGET_RANGE
                        )
                        .checkForBlocks(true)
                        .bbInflation(
                                RAYCAST_INFLATION
                        )
                        .filter(entity ->
                                entity
                                        instanceof LivingEntity living
                                        && living.isAlive()
                                        && !living.isSpectator()
                                        && !player.isAlliedTo(
                                        living
                                )
                        )
                        .build();

        if (hitResult
                instanceof EntityHitResult entityHit
                && entityHit.getEntity()
                instanceof LivingEntity target) {
            return target;
        }

        return null;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }
}
