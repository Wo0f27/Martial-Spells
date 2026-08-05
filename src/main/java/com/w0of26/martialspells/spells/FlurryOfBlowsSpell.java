package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import com.w0of26.martialspells.combat.FlurrySequenceManager;
import net.minecraft.resources.ResourceLocation;
import com.w0of26.martialspells.combat.MonkWeaponHelper;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import com.w0of26.martialspells.client.animation.FlurryClientAnimations;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import javax.annotation.Nullable;
import java.util.List;
import com.w0of26.martialspells.combat.MonkEncumbranceHelper;
import net.minecraft.world.InteractionHand;

public final class FlurryOfBlowsSpell
        extends AbstractMonkTechniqueSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "flurry_of_blows"
            );

    private static final int MAX_LEVEL = 5;
    private static final int KI_COST = 1;
    private static final int COOLDOWN_SECONDS = 3;
    private static final float TARGET_RANGE = 4.0F;
    private static final float RAYCAST_INFLATION = 0.30F;

    /*
     * Prevents empty-hand Flurry from scaling from only one point
     * of vanilla attack damage.
     */
    private static final float
            MINIMUM_EFFECTIVE_ATTACK_DAMAGE = 4.0F;

    private static final int[] STRIKE_COUNTS = {
            2,
            3,
            4,
            5,
            6
    };

    private static final float[]
            DAMAGE_MULTIPLIERS = {
            0.60F,
            0.65F,
            0.675F,
            0.64F,
            0.60F
    };


    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    /*
                     * Temporary until the dedicated Monk school
                     * is implemented.
                     */
                    .setSchoolResource(
                            SchoolRegistry.HOLY_RESOURCE
                    )
                    .setMinRarity(SpellRarity.COMMON)
                    .setMaxLevel(MAX_LEVEL)
                    .setCooldownSeconds(
                            COOLDOWN_SECONDS
                    )
                    .build();

    public FlurryOfBlowsSpell() {
        super(MAX_LEVEL);
    }

    private static int clampLevel(
            int spellLevel
    ) {
        return Math.max(
                1,
                Math.min(
                        spellLevel,
                        MAX_LEVEL
                )
        );
    }

    private static int getLevelIndex(
            int spellLevel
    ) {
        return clampLevel(spellLevel) - 1;
    }

    public static float getDamagePerStrikeMultiplier(
            int spellLevel
    ) {
        return DAMAGE_MULTIPLIERS[
                getLevelIndex(
                        spellLevel
                )
                ];
    }

    public static int getStrikeCount(
            int spellLevel
    ) {
        return STRIKE_COUNTS[
                getLevelIndex(
                        spellLevel
                )
                ];
    }

    public static int getKiCost() {
        return KI_COST;
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
                    "ui.martial_spells.server_player_required"
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

        if (!MonkWeaponHelper
                .hasValidMainHand(player)) {
            return failure(
                    "ui.martial_spells."
                            + "requires_monk_weapon"
            );
        }

        if (!hasTechniqueKi(
                serverPlayer,
                KI_COST
        )) {
            return failure(
                    "ui.martial_spells.not_enough_ki"
            );
        }

        return new CastResult(
                CastResult.Type.SUCCESS
        );
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {

        int displayedKiCost =
                caster == null
                        ? KI_COST
                        : getEffectiveTechniqueKiCost(
                        KI_COST,
                        caster
                );

        int percentage =
                Math.round(
                        getDamagePerStrikeMultiplier(
                                spellLevel
                        ) * 100.0F
                );

        return List.of(
                Component.translatable(
                        "ui.martial_spells.ki_cost",
                        displayedKiCost
                ),
                Component.translatable(
                        "ui.martial_spells.flurry_strikes",
                        getStrikeCount(spellLevel)
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "flurry_damage_per_strike",
                        percentage
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "flurry_range",
                        TARGET_RANGE
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "requires_monk_weapon"
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "heavy_armor_penalty"
                )
        );
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        if (!(caster instanceof ServerPlayer player)) {
            return;
        }

        /*
         * Command casting is allowed for development testing.
         * Normal gameplay still requires the Monk Codex.
         */
        if (!isValidMonkTechniqueSource(
                castSource,
                player
        )) {
            return;
        }

        /*
         * Validate the weapon when the technique executes.
         */
        if (!MonkWeaponHelper
                .hasValidMainHand(player)) {
            return;
        }

        /*
         * Ki is consumed immediately when the technique executes,
         * even when every subsequent punch misses.
         */
        if (!consumeTechniqueKi(
                player,
                KI_COST
        )) {
            return;
        }                MonkEncumbranceHelper
                        .getEffectiveKiCost(
                                KI_COST,
                                player
                        );

        if (!consumeTechniqueKi(
                player,
                KI_COST
        )) {
            return;
        }

        float attackDamage =
                (float) player.getAttributeValue(
                        Attributes.ATTACK_DAMAGE
                );

        float effectiveAttackDamage =
                Math.max(
                        MINIMUM_EFFECTIVE_ATTACK_DAMAGE,
                        attackDamage
                );

        float baseDamagePerStrike =
                effectiveAttackDamage
                        * getDamagePerStrikeMultiplier(
                        spellLevel
                );

        float damagePerStrike =
                applyTechniqueDamagePenalty(
                        baseDamagePerStrike,
                        player
                );

        FlurrySequenceManager.begin(
                player,
                clampTechniqueLevel(
                        spellLevel
                ),
                damagePerStrike
        );
    }

    @Nullable
    public static LivingEntity findTarget(
            ServerPlayer player
    ) {
        Level level = player.level();

        HitResult hitResult =
                RaycastBuilder
                        .begin(level, player)
                        .range(TARGET_RANGE)
                        .checkForBlocks(true)
                        .bbInflation(
                                RAYCAST_INFLATION
                        )
                        .filter(entity ->
                                entity
                                        instanceof LivingEntity
                                        living
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
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean allowCrafting() {
        return true;
    }

    /*
     * Temporary animation reference.
     *
     * This uses Better Combat's existing fist animation only to verify
     * that Flurry correctly enters Iron's PlayerAnimator pipeline.
     *
     * It does not invoke Better Combat's damage or attack system.
     */

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }


    @Override
    public void onClientPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            InteractionHand hand,
            @Nullable MagicData magicData
    ) {
        super.onClientPreCast(
                level,
                spellLevel,
                entity,
                hand,
                magicData
        );

        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () ->
                        FlurryClientAnimations.play(
                                entity,
                                spellLevel
                        )
        );
    }
}