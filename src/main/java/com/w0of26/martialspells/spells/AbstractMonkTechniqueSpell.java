package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.combat.MonkEncumbranceHelper;
import com.w0of26.martialspells.ki.KiHelper;
import com.w0of26.martialspells.registry.MartialItemRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;

/**
 * Shared foundation for Monk techniques.
 *
 * This class centralizes common Monk rules without controlling
 * the individual technique's cast lifecycle.
 *
 * Subclasses remain responsible for:
 * - weapon requirements;
 * - target validation;
 * - when Ki is consumed or generated;
 * - animations;
 * - channel behavior;
 * - technique-specific execution.
 */
public abstract class AbstractMonkTechniqueSpell
        extends AbstractSpell
        implements MartialTechnique {

    private final int maximumTechniqueLevel;

    protected AbstractMonkTechniqueSpell(
            int maximumTechniqueLevel
    ) {
        if (maximumTechniqueLevel < 1) {
            throw new IllegalArgumentException(
                    "A Monk technique must have "
                            + "at least one level."
            );
        }

        this.maximumTechniqueLevel =
                maximumTechniqueLevel;

        /*
         * Monk techniques use Ki rather than Iron's mana.
         */
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;

        /*
         * Technique scaling is handled explicitly by subclasses.
         */
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
    }

    @Override
    public final MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.MONK;
    }

    protected final int clampTechniqueLevel(
            int spellLevel
    ) {
        return Math.max(
                1,
                Math.min(
                        spellLevel,
                        maximumTechniqueLevel
                )
        );
    }

    protected final int getTechniqueLevelIndex(
            int spellLevel
    ) {
        return clampTechniqueLevel(
                spellLevel
        ) - 1;
    }

    @Override
    public final int getManaCost(
            int spellLevel
    ) {
        return 0;
    }

    protected final boolean hasEquippedMonkCodex(
            Player player
    ) {
        return player != null
                && MartialItemRegistry
                .MONK_CODEX
                .get()
                .isEquippedBy(player);
    }

    /**
     * Command casting remains available for development and
     * administration.
     *
     * All normal casting must originate from an equipped Monk Codex.
     */
    protected final boolean isValidMonkTechniqueSource(
            CastSource castSource,
            Player player
    ) {
        if (castSource == CastSource.COMMAND) {
            return true;
        }

        return castSource == CastSource.SPELLBOOK
                && hasEquippedMonkCodex(player);
    }

    protected final CastResult
    validateMonkTechniqueSource(
            CastSource castSource,
            Player player
    ) {
        if (!isValidMonkTechniqueSource(
                castSource,
                player
        )) {
            return failure(
                    "ui.martial_spells."
                            + "requires_monk_codex"
            );
        }

        return success();
    }

    protected final int getEffectiveTechniqueKiCost(
            int baseKiCost,
            LivingEntity caster
    ) {
        return MonkEncumbranceHelper
                .getEffectiveKiCost(
                        baseKiCost,
                        caster
                );
    }

    protected final boolean hasTechniqueKi(
            ServerPlayer player,
            int baseKiCost
    ) {
        int effectiveCost =
                getEffectiveTechniqueKiCost(
                        baseKiCost,
                        player
                );

        return KiHelper.hasKi(
                player,
                effectiveCost
        );
    }

    protected final boolean consumeTechniqueKi(
            ServerPlayer player,
            int baseKiCost
    ) {
        int effectiveCost =
                getEffectiveTechniqueKiCost(
                        baseKiCost,
                        player
                );

        return KiHelper.consumeKi(
                player,
                effectiveCost
        );
    }

    protected final float applyTechniqueDamagePenalty(
            float baseDamage,
            LivingEntity caster
    ) {
        return MonkEncumbranceHelper
                .applyDamagePenalty(
                        baseDamage,
                        caster
                );
    }

    protected final CastResult success() {
        return new CastResult(
                CastResult.Type.SUCCESS
        );
    }

    protected final CastResult failure(
            String translationKey
    ) {
        return new CastResult(
                CastResult.Type.FAILURE,
                Component.translatable(
                        translationKey
                ).withStyle(
                        ChatFormatting.RED
                )
        );
    }

    /**
     * Monk techniques are learned through the Monk Codex rather
     * than appearing as random spell loot.
     */
    @Override
    public final boolean allowLooting() {
        return false;
    }

    /**
     * Crafting remains enabled for administrative, datapack, and
     * progression-system compatibility.
     *
     * Technique-scroll restrictions are enforced separately.
     */
    @Override
    public final boolean allowCrafting() {
        return true;
    }
}