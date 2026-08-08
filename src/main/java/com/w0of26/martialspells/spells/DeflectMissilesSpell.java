package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.w0of26.martialspells.combat.MonkWeaponHelper;

import java.util.List;

public final class DeflectMissilesSpell
        extends AbstractMonkTechniqueSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "deflect_missiles"
            );

    private static final int MAX_LEVEL = 5;

    /*
     * TEST VALUE ONLY.
     *
     * 100 ticks = 5 seconds maximum channel.
     * Final duration is still undecided.
     */
    private static final int KI_COST = 1;

    private static final int[] CHANNEL_TICKS = {
            50,   // 2.5 seconds
            60,   // 3.0 seconds
            70,   // 3.5 seconds
            80,   // 4.0 seconds
            100   // 5.0 seconds
    };

    private static final int COOLDOWN_SECONDS = 10;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setSchoolResource(
                            MartialSchoolRegistry
                                    .MARTIAL_RESOURCE
                    )
                    /*
                     * Provisional rarity.
                     * Final Deflect Missiles rarity is still open.
                     */
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

    public DeflectMissilesSpell() {
        super(MAX_LEVEL);
    }
    @Override
    public int getCastTime(
            int spellLevel
    ) {
        return CHANNEL_TICKS[
                getTechniqueLevelIndex(
                        spellLevel
                )
                ];
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
        return CastType.CONTINUOUS;
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

        /*
         * Preserve Iron's normal casting restrictions.
         */
        if (!normalResult.isSuccess()) {
            return normalResult;
        }

        /*
         * Explicit cooldown enforcement is required for
         * Deflect Missiles because of its continuous-cast
         * lifecycle.
         */
        if (castSource != CastSource.COMMAND
                && magicData
                .getPlayerCooldowns()
                .isOnCooldown(this)) {

            return failure(
                    "ui.martial_spells."
                            + "deflect_missiles_on_cooldown"
            );
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
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

        /*
         * Deflect Missiles only supports the weapon states
         * represented by its defensive animations:
         *
         * - empty hand
         * - gauntlets
         * - quarterstaff
         */
        if (!MonkWeaponHelper
                .hasValidDeflectMissilesMainHand(
                        player
                )) {

            return failure(
                    "ui.martial_spells."
                            + "requires_deflect_missiles_weapon"
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
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity caster,
            MagicData magicData
    ) {
        if (!(caster instanceof ServerPlayer player)) {
            return;
        }

        consumeTechniqueKi(
                player,
                KI_COST
        );
    }

    public static float getChannelSeconds(
            int spellLevel
    ) {
        int level =
                Math.max(
                        1,
                        Math.min(
                                spellLevel,
                                MAX_LEVEL
                        )
                );

        return CHANNEL_TICKS[level - 1]
                / 20.0F;
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
                        "ui.martial_spells.deflect_missiles_channel",
                        getChannelSeconds(
                                spellLevel
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.deflect_missiles_deflection"
                ),
                Component.translatable(
                        "ui.martial_spells."
                                + "requires_deflect_missiles_weapon"
                )
        );
    }
}