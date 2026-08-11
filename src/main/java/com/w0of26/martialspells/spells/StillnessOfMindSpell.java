package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
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

public final class StillnessOfMindSpell
        extends AbstractMonkTechniqueSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "stillness_of_mind"
            );

    private static final int MAX_LEVEL = 1;

    public static final int DURATION_SECONDS = 20;

    public static final int DURATION_TICKS =
            DURATION_SECONDS * 20;

    public static final int COOLDOWN_SECONDS = 180;

    public static final int COOLDOWN_TICKS =
            COOLDOWN_SECONDS * 20;

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
                    /*
                     * Iron's still requires a configured value.
                     * StillnessOfMindCooldownEvents replaces
                     * the effective value with the fixed value.
                     */
                    .setCooldownSeconds(
                            COOLDOWN_SECONDS
                    )
                    .build();

    public StillnessOfMindSpell() {
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

        if (!(player instanceof ServerPlayer)) {
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

        if (player.hasEffect(
                MartialEffectRegistry
                        .STILLNESS_OF_MIND
                        .get()
        )) {
            return failure(
                    "ui.martial_spells."
                            + "stillness_already_active"
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

        player.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry
                                .STILLNESS_OF_MIND
                                .get(),
                        DURATION_TICKS,
                        0,
                        false,
                        false,
                        true
                )
        );
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.stillness_infinite_ki",
                        DURATION_SECONDS
                ),
                Component.translatable(
                        "ui.martial_spells.stillness_defense_penalty"
                ),
                Component.translatable(
                        "ui.martial_spells.stillness_preserves_ki"
                ),
                Component.translatable(
                        "ui.martial_spells.stillness_aftereffect"
                ),
                Component.translatable(
                        "ui.martial_spells.stillness_requires_codex_active"
                ),
                Component.translatable(
                        "ui.martial_spells.fixed_cooldown",
                        COOLDOWN_SECONDS
                ),
                Component.translatable(
                        "ui.martial_spells.cooldown_reduction_immune"
                )
        );
    }
}