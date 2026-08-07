package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.MonkEncumbranceHelper;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DiamondBodySpell
        extends AbstractMonkTechniqueSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "diamond_body"
            );

    private static final int MAX_LEVEL = 5;
    private static final int DURATION_TICKS = 20 * 20;
    private static final int BASE_COOLDOWN_SECONDS = 30;

    private static final int[] KI_COSTS = {
            1,
            2,
            3,
            4,
            5
    };

    private static final int[] DAMAGE_REDUCTION_PERCENT = {
            20,
            25,
            30,
            35,
            40
    };

    private static final int[] ARMOR_VALUES = {
            4,
            8,
            12,
            16,
            20
    };

    private static final int[] TOUGHNESS_VALUES = {
            2,
            4,
            6,
            9,
            12
    };

    private static final int[] KNOCKBACK_PERCENT = {
            8,
            16,
            24,
            32,
            40
    };

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setSchoolResource(
                            MartialSchoolRegistry.MARTIAL_RESOURCE
                    )
                    .setMinRarity(
                            SpellRarity.COMMON
                    )
                    .setMaxLevel(MAX_LEVEL)
                    .setCooldownSeconds(
                            BASE_COOLDOWN_SECONDS
                    )
                    .build();

    public DiamondBodySpell() {
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

    private int getKiCostForLevel(
            int spellLevel
    ) {
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
        CastResult baseResult =
                super.canBeCastedBy(
                        spellLevel,
                        castSource,
                        magicData,
                        player
                );

        if (!baseResult.isSuccess()) {
            return baseResult;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
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

        if (MonkEncumbranceHelper
                .getHeavyArmorPieceCount(player) > 0) {
            return failure(
                    "ui.martial_spells."
                            + "diamond_body_heavy_armor"
            );
        }

        if (!hasTechniqueKi(
                serverPlayer,
                getKiCostForLevel(spellLevel)
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
        if (!(caster instanceof ServerPlayer player)) {
            return;
        }

        if (!isValidMonkTechniqueSource(
                castSource,
                player
        )) {
            return;
        }

        if (MonkEncumbranceHelper
                .getHeavyArmorPieceCount(player) > 0) {
            return;
        }

        int clampedLevel =
                clampTechniqueLevel(spellLevel);

        int kiCost =
                getKiCostForLevel(clampedLevel);

        /*
         * Do not use consumeTechniqueKi here because heavy armor is
         * prohibited rather than applying the shared Ki multiplier.
         */
        if (!com.w0of26.martialspells.ki.KiHelper
                .consumeKi(
                        player,
                        kiCost
                )) {
            return;
        }

        player.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry
                                .DIAMOND_BODY
                                .get(),
                        DURATION_TICKS,
                        clampedLevel - 1,
                        false,
                        false,
                        true
                )
        );

        super.onCast(
                level,
                spellLevel,
                caster,
                castSource,
                magicData
        );
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        int index =
                getTechniqueLevelIndex(spellLevel);

        return List.of(
                Component.translatable(
                        "ui.martial_spells.ki_cost",
                        KI_COSTS[index]
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "diamond_body_reduction",
                        DAMAGE_REDUCTION_PERCENT[index]
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "diamond_body_armor",
                        ARMOR_VALUES[index]
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "diamond_body_toughness",
                        TOUGHNESS_VALUES[index]
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "diamond_body_knockback",
                        KNOCKBACK_PERCENT[index]
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "diamond_body_duration",
                        20
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "diamond_body_heavy_restriction"
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "diamond_body_oakskin_stacking"
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "diamond_heart_description"
                )
        );
    }
}