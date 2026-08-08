package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.MonkWeaponHelper;
import com.w0of26.martialspells.combat.StunningStrikeWeaponStyle;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncStunningStrikeAnimationPacket;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import com.w0of26.martialspells.combat.MartialPowerHelper;
import com.w0of26.martialspells.combat.StunningStrikeImpactManager;

import javax.annotation.Nullable;
import java.util.List;

public final class StunningStrikeSpell
        extends AbstractMonkTechniqueSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "stunning_strike"
            );

    private static final int MAX_LEVEL = 5;

    private static final int KI_COST = 2;

    private static final int COOLDOWN_SECONDS = 15;

    private static final float TARGET_RANGE = 4.0F;

    private static final float RAYCAST_INFLATION = 0.30F;

    private static final float
            MINIMUM_EFFECTIVE_ATTACK_DAMAGE = 4.0F;

    private static final float[]
            DAMAGE_MULTIPLIERS = {
            1.10F,
            1.15F,
            1.20F,
            1.25F,
            1.30F
    };

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

    public static float getDamageMultiplier(
            int spellLevel
    ) {
        return DAMAGE_MULTIPLIERS[
                getLevelIndex(spellLevel)
                ];
    }

    public static float getStunDurationSeconds(
            int spellLevel
    ) {
        int level = clampLevel(spellLevel);

        return 5.0F
                + ((level - 1) * 0.75F);
    }

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setSchoolResource(
                            MartialSchoolRegistry
                                    .MARTIAL_RESOURCE
                    )
                    .setMinRarity(
                            SpellRarity.RARE
                    )
                    .setMaxLevel(
                            MAX_LEVEL
                    )
                    .setCooldownSeconds(
                            COOLDOWN_SECONDS
                    )
                    .build();

    public StunningStrikeSpell() {
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

        if (!MonkWeaponHelper
                .hasValidStunningStrikeMainHand(
                        player
                )) {
            return failure(
                    "ui.martial_spells."
                            + "requires_stunning_strike_weapon"
            );
        }

        if (findTarget(serverPlayer) == null) {
            return failure(
                    "ui.martial_spells."
                            + "no_stunning_strike_target"
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

        StunningStrikeWeaponStyle weaponStyle =
                MonkWeaponHelper
                        .getStunningStrikeWeaponStyle(
                                player
                        );

        if (weaponStyle == null) {
            return;
        }

        /*
         * Revalidate the target when the server actually
         * executes the technique.
         */
        LivingEntity target =
                findTarget(player);

        if (target == null) {
            return;
        }

        /*
         * Heavy armor automatically changes the base
         * cost of 2 Ki to 3 through the shared Monk
         * encumbrance framework.
         */
        if (!consumeTechniqueKi(
                player,
                KI_COST
        )) {
            return;
        }

        float baseDamage =
                MartialPowerHelper
                        .calculateTechniqueDamage(
                                player,
                                MINIMUM_EFFECTIVE_ATTACK_DAMAGE,
                                getDamageMultiplier(
                                        spellLevel
                                )
                        );

        float finalDamage =
                applyTechniqueDamagePenalty(
                        baseDamage,
                        player
                );
        /*
         * Animation only.
         *
         * Stunning Strike damage is deliberately NOT
         * performed by Better Combat.
         */
        MartialNetwork.sendToTrackingAndSelf(
                new SyncStunningStrikeAnimationPacket(
                        player.getUUID(),
                        weaponStyle
                ),
                player
        );


        StunningStrikeImpactManager.begin(
                player,
                finalDamage,
                spellLevel
        );
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

        return List.of(
                Component.translatable(
                        "ui.martial_spells.ki_cost",
                        displayedKiCost
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "stunning_strike_range",
                        TARGET_RANGE
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "requires_stunning_strike_weapon"
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "heavy_armor_penalty"
                ),

                Component.translatable(
                        "ui.martial_spells.stunning_strike_stun_duration",
                        getStunDurationSeconds(
                                spellLevel
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.stunning_strike_rend"
                )
        );
    }

    /*
     * Suppress Iron's generic casting animation.
     * The weapon-specific animation is synchronized
     * through MartialNetwork instead.
     */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

}