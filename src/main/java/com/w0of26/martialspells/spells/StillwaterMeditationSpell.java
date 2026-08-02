package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.ki.KiHelper;
import com.w0of26.martialspells.registry.MartialItemRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

import java.util.List;

/**
 * The Monk's foundational Ki-generating technique.
 *
 * The first implementation proves the complete casting-to-Ki loop.
 * Custom animation and explicit interruption rules are added separately.
 */
public final class StillwaterMeditationSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "stillwater_meditation"
            );

    private static final int MAX_LEVEL = 5;

    private static final int CHANNEL_SECONDS = 4;
    private static final int CHANNEL_TICKS =
            CHANNEL_SECONDS * 20;

    /**
     * Level IV represents the Epic Codex tier.
     */
    public static final int UNINTERRUPTIBLE_LEVEL = 4;


    private static final int COOLDOWN_SECONDS = 50;

    private static final int[] KI_GENERATED = {
            2,
            3,
            3,
            4,
            5
    };

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    /*
                     * Holy is temporary until the dedicated Monk
                     * spell school is introduced.
                     */
                    .setSchoolResource(
                            SchoolRegistry.HOLY_RESOURCE
                    )
                    .setMinRarity(SpellRarity.COMMON)
                    .setMaxLevel(MAX_LEVEL)
                    .setCooldownSeconds(COOLDOWN_SECONDS)
                    .build();

    public StillwaterMeditationSpell() {
        /*
         * Monk techniques use Ki rather than mana.
         */
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;

        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;

        this.castTime = CHANNEL_TICKS;
    }

    private static int clampLevel(int spellLevel) {
        return Math.max(
                1,
                Math.min(spellLevel, MAX_LEVEL)
        );
    }

    private static int getLevelIndex(int spellLevel) {
        return clampLevel(spellLevel) - 1;
    }

    public static int getKiGenerated(
            int spellLevel
    ) {
        return KI_GENERATED[
                getLevelIndex(spellLevel)
                ];
    }

    public static int getChannelTicks() {
        return CHANNEL_TICKS;
    }

    /**
     * Checks whether an entity is actively channeling Stillwater
     * Meditation at or above the requested spell level.
     */
    public static boolean isMeditatingAtOrAbove(
            LivingEntity entity,
            int minimumLevel
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        MagicData magicData =
                MagicData.getPlayerMagicData(player);

        return magicData.isCasting()
                && SPELL_ID.toString().equals(
                magicData.getCastingSpellId()
        )
                && magicData.getCastingSpellLevel()
                >= minimumLevel;
    }

    /**
     * Level IV and V Meditation cannot be interrupted by incoming damage.
     *
     * Lower levels retain Iron's normal interruption rules, including
     * compatibility with the Concentration Amulet.
     */
    @Override
    public boolean canBeInterrupted(
            @Nullable Player player
    ) {
        if (player != null
                && isMeditatingAtOrAbove(
                player,
                UNINTERRUPTIBLE_LEVEL
        )) {
            return false;
        }

        return super.canBeInterrupted(player);
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

    /**
     * Meditation always requires four seconds.
     *
     * Generic cast-time-reduction equipment must not shorten the
     * vulnerable meditation window.
     */
    @Override
    public int getEffectiveCastTime(
            int spellLevel,
            LivingEntity entity
    ) {
        return CHANNEL_TICKS;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return 0;
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

        /*
         * Allow command casting for development and administration.
         * Normal gameplay requires the technique to come from an
         * equipped Monk Codex.
         */
        if (castSource != CastSource.COMMAND) {
            boolean validSource =
                    castSource == CastSource.SPELLBOOK;

            boolean codexEquipped =
                    MartialItemRegistry.MONK_CODEX
                            .get()
                            .isEquippedBy(player);

            if (!validSource || !codexEquipped) {
                return new CastResult(
                        CastResult.Type.FAILURE,
                        Component.translatable(
                                        "ui.martial_spells.requires_monk_codex"
                                )
                                .withStyle(ChatFormatting.RED)
                );
            }
        }

        int maximumKi =
                KiHelper.getMaximumKi(player);

        if (maximumKi <= 0) {
            return new CastResult(
                    CastResult.Type.FAILURE,
                    Component.translatable(
                                    "ui.martial_spells.requires_monk_codex"
                            )
                            .withStyle(ChatFormatting.RED)
            );
        }

        if (KiHelper.getCurrentKi(player)
                >= maximumKi) {
            return new CastResult(
                    CastResult.Type.FAILURE,
                    Component.translatable(
                                    "ui.martial_spells.ki_full"
                            )
                            .withStyle(ChatFormatting.RED)
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
        return List.of(
                Component.translatable(
                        "ui.martial_spells.ki_generated",
                        getKiGenerated(spellLevel)
                ),
                Component.translatable(
                        "ui.martial_spells.fixed_channel_time",
                        CHANNEL_SECONDS
                ),
                Component.translatable(
                        "ui.martial_spells.fixed_cooldown",
                        COOLDOWN_SECONDS
                ),
                Component.translatable(
                        "ui.martial_spells.epic_meditation_mastery"
                )
        );
    }

    /**
     * Called after the full long cast succeeds.
     */
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
         * Revalidate at completion. Removing the Codex during the
         * four-second channel must not grant Ki.
         */
        boolean codexEquipped =
                MartialItemRegistry.MONK_CODEX
                        .get()
                        .isEquippedBy(player);

        if (!codexEquipped) {
            return;
        }

        KiHelper.addKi(
                player,
                getKiGenerated(spellLevel)
        );
    }

    /**
     * This is a permanent core Codex technique rather than random loot.
     */
    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean allowCrafting() {
        return false;
    }
}