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
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;

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

    private static final AnimationHolder MEDITATION_ANIMATION =
            new AnimationHolder(
                    SPELL_ID,
                    true,
                    true
            );

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

    private static final float[] HEALING_PERCENT_PER_PULSE = {
            0.020F,
            0.025F,
            0.030F,
            0.035F,
            0.040F
    };

    private static final float[] DAMAGE_REDUCTION = {
            0.00F,
            0.00F,
            0.00F,
            0.25F,
            0.30F
    };

    private static final int HEALING_PULSE_INTERVAL = 20;

    /*
     * The fourth pulse is applied in onCast when the channel succeeds.
     * onServerCastTick handles only the first three pulses.
     */
    private static final int TICKED_HEALING_PULSES = 3;

    private static final int EARLY_INTERRUPTION_COOLDOWN_TICKS =
            5 * 20;

    private static final int HEALED_INTERRUPTION_COOLDOWN_TICKS =
            15 * 20;

    private static final String CHANNEL_TICKS_TAG =
            MartialSpells.MOD_ID
                    + "_stillwater_meditation_channel_ticks";

    private static final String HEALING_PULSES_TAG =
            MartialSpells.MOD_ID
                    + "_stillwater_meditation_healing_pulses";

    private static final String START_X_TAG =
            MartialSpells.MOD_ID
                    + "_stillwater_meditation_start_x";

    private static final String START_Y_TAG =
            MartialSpells.MOD_ID
                    + "_stillwater_meditation_start_y";

    private static final String START_Z_TAG =
            MartialSpells.MOD_ID
                    + "_stillwater_meditation_start_z";

    /*
     * Allows tiny floating-point position differences without treating
     * them as deliberate movement.
     *
     * 0.05 blocks squared = 0.0025.
     */
    private static final double MOVEMENT_TOLERANCE_SQUARED =
            0.0025D;

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

    public static float getHealingPercentPerPulse(
            int spellLevel
    ) {
        return HEALING_PERCENT_PER_PULSE[
                getLevelIndex(spellLevel)
                ];
    }

    public static float getTotalHealingPercent(
            int spellLevel
    ) {
        return getHealingPercentPerPulse(spellLevel) * 4.0F;
    }

    public static float getDamageReduction(
            int spellLevel
    ) {
        return DAMAGE_REDUCTION[
                getLevelIndex(spellLevel)
                ];
    }

    public static int getChannelTicks() {
        return CHANNEL_TICKS;
    }
    /**
     * Returns the active Meditation level, or zero when the entity
     * is not currently channeling Stillwater Meditation.
     */
    public static int getActiveMeditationLevel(
            LivingEntity entity
    ) {
        if (!(entity instanceof Player player)) {
            return 0;
        }

        MagicData magicData =
                MagicData.getPlayerMagicData(player);

        if (!magicData.isCasting()) {
            return 0;
        }

        if (!SPELL_ID.toString().equals(
                magicData.getCastingSpellId()
        )) {
            return 0;
        }

        return clampLevel(
                magicData.getCastingSpellLevel()
        );
    }

    public static boolean isMeditating(
            LivingEntity entity
    ) {
        return getActiveMeditationLevel(entity) > 0;
    }

    public static boolean isMeditatingAtOrAbove(
            LivingEntity entity,
            int minimumLevel
    ) {
        return getActiveMeditationLevel(entity)
                >= minimumLevel;
    }


    private static void applyHealingPulse(
            ServerPlayer player,
            int spellLevel
    ) {
        if (!player.isAlive()
                || player.isDeadOrDying()) {
            return;
        }

        float healingAmount =
                player.getMaxHealth()
                        * getHealingPercentPerPulse(
                        spellLevel
                );

        player.heal(healingAmount);
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

    @Override
    public AnimationHolder getCastStartAnimation() {
        return MEDITATION_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
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
                ),
                Component.translatable(
                        "ui.martial_spells.movement_cancels_meditation"
                ),
                Component.translatable(
                        "ui.martial_spells.attacking_cancels_meditation"
                ),
                Component.translatable(
                        "ui.martial_spells.meditation_healing",
                        Utils.stringTruncation(
                                getHealingPercentPerPulse(spellLevel)
                                        * 100.0F,
                                1
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.meditation_total_healing",
                        Utils.stringTruncation(
                                getTotalHealingPercent(spellLevel)
                                        * 100.0F,
                                1
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.meditation_damage_reduction",
                        Utils.stringTruncation(
                                getDamageReduction(spellLevel)
                                        * 100.0F,
                                1
                        )
                ),
                Component.translatable(
                        "ui.martial_spells.interrupted_cooldown"
                ),
                Component.translatable(
                        "ui.martial_spells.monk_codex_only"
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

        /*
         * The fourth pulse occurs only after successfully completing
         * the full four-second channel.
         */
        applyHealingPulse(
                player,
                spellLevel
        );

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
        return true;
    }

    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData magicData
    ) {
        super.onServerPreCast(
                level,
                spellLevel,
                entity,
                magicData
        );

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        player.getPersistentData().putDouble(
                START_X_TAG,
                player.getX()
        );

        player.getPersistentData().putDouble(
                START_Y_TAG,
                player.getY()
        );

        player.getPersistentData().putDouble(
                START_Z_TAG,
                player.getZ()
        );

        player.getPersistentData().putInt(
                CHANNEL_TICKS_TAG,
                0
        );

        player.getPersistentData().putInt(
                HEALING_PULSES_TAG,
                0
        );
    }

    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData magicData
    ) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        if (!isMeditating(player)) {
            return;
        }

        if (!hasStoredStartPosition(player)) {
            storeStartPosition(player);
        }

        double startX =
                player.getPersistentData().getDouble(
                        START_X_TAG
                );

        double startY =
                player.getPersistentData().getDouble(
                        START_Y_TAG
                );

        double startZ =
                player.getPersistentData().getDouble(
                        START_Z_TAG
                );

        double deltaX = player.getX() - startX;
        double deltaY = player.getY() - startY;
        double deltaZ = player.getZ() - startZ;

        double distanceSquared =
                deltaX * deltaX
                        + deltaY * deltaY
                        + deltaZ * deltaZ;

        if (distanceSquared
                > MOVEMENT_TOLERANCE_SQUARED) {
            Utils.serverSideCancelCast(player);
            return;
        }

        int elapsedTicks =
                player.getPersistentData().getInt(
                        CHANNEL_TICKS_TAG
                ) + 1;

        player.getPersistentData().putInt(
                CHANNEL_TICKS_TAG,
                elapsedTicks
        );

        /*
         * Pulses occur at elapsed ticks 20, 40, and 60.
         *
         * The final pulse is applied in onCast because Iron's executes
         * the successful cast instead of onServerCastTick at tick 80.
         */
        boolean shouldHeal =
                elapsedTicks % HEALING_PULSE_INTERVAL == 0
                        && elapsedTicks
                        <= HEALING_PULSE_INTERVAL
                        * TICKED_HEALING_PULSES;

        if (!shouldHeal) {
            return;
        }

        applyHealingPulse(
                player,
                spellLevel
        );

        int completedPulses =
                player.getPersistentData().getInt(
                        HEALING_PULSES_TAG
                ) + 1;

        player.getPersistentData().putInt(
                HEALING_PULSES_TAG,
                completedPulses
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
        if (entity instanceof ServerPlayer player) {
            if (cancelled) {
                int completedHealingPulses =
                        player.getPersistentData().getInt(
                                HEALING_PULSES_TAG
                        );

                int cooldownTicks =
                        completedHealingPulses > 0
                                ? HEALED_INTERRUPTION_COOLDOWN_TICKS
                                : EARLY_INTERRUPTION_COOLDOWN_TICKS;

                /*
                 * Interrupted long casts do not receive Iron's normal
                 * successful-cast cooldown, so add the partial cooldown
                 * directly and synchronize it to the player.
                 */
                magicData.getPlayerCooldowns().addCooldown(
                        this,
                        cooldownTicks
                );

                magicData.getPlayerCooldowns()
                        .syncToPlayer(player);
            }

            clearMeditationState(player);
        }

        super.onServerCastComplete(
                level,
                spellLevel,
                entity,
                magicData,
                cancelled
        );
    }

    private static void storeStartPosition(
            ServerPlayer player
    ) {
        player.getPersistentData().putDouble(
                START_X_TAG,
                player.getX()
        );

        player.getPersistentData().putDouble(
                START_Y_TAG,
                player.getY()
        );

        player.getPersistentData().putDouble(
                START_Z_TAG,
                player.getZ()
        );
    }

    private static boolean hasStoredStartPosition(
            ServerPlayer player
    ) {
        return player.getPersistentData().contains(
                START_X_TAG
        )
                && player.getPersistentData().contains(
                START_Y_TAG
        )
                && player.getPersistentData().contains(
                START_Z_TAG
        );
    }

    private static void clearMeditationState(
            ServerPlayer player
    ) {
        player.getPersistentData().remove(START_X_TAG);
        player.getPersistentData().remove(START_Y_TAG);
        player.getPersistentData().remove(START_Z_TAG);

        player.getPersistentData().remove(
                CHANNEL_TICKS_TAG
        );

        player.getPersistentData().remove(
                HEALING_PULSES_TAG
        );
    }
}