package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.MutilateAttackManager;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncMutilateAnimationPacket;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Forge/Iron's translation of frozen Rogues Mutilate.
 *
 * <p>The original Spell Engine delivery is a single physical dual-melee attack
 * with a 160-degree forward hitbox. The contact frame occurs at 50% of the
 * caster's current melee attack cycle, then vanilla player attacks are used so
 * weapon enchantments, crits, knockback, fire aspect and other normal melee
 * hooks remain part of the hit.</p>
 */
public final class MutilateSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "mutilate");

    /**
     * Exact PlayerAnimator pose used by frozen Rogues/Spell Engine, copied into
     * the Martial Spells namespace by the R6 asset sync script so Spell Engine
     * itself does not become a runtime dependency.
     */
    public static final ResourceLocation MUTILATE_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "mutilate_dual_slash_cross"
            );

    public static final int MAX_LEVEL = 1;
    public static final float RANGE = 3.0F;
    public static final float ARC_DEGREES = 160.0F;
    public static final float HITBOX_WIDTH_FACTOR = 0.5F;
    public static final float HITBOX_HEIGHT_FACTOR = 0.2F;
    public static final float ATTACK_DELAY_FRACTION = 0.5F;
    public static final int BASE_COOLDOWN_SECONDS = 12;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public MutilateSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override
    public MartialTechniqueClass getTechniqueClass() {
        return MartialTechniqueClass.ROGUE;
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
    public boolean allowLooting() {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        /*
         * Retain the native Iron's finish-animation declaration as a fallback.
         * The authoritative R6 visual path is additionally synchronized by
         * SyncMutilateAnimationPacket because this instant spell was observed
         * to complete mechanically without Iron's finish callback producing a
         * visible PlayerAnimator pose.
         */
        return new AnimationHolder(MUTILATE_ANIMATION, true, false);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.martial_spells.mutilate_range", RANGE),
                Component.translatable("ui.martial_spells.mutilate_arc", Math.round(ARC_DEGREES)),
                Component.translatable("ui.martial_spells.mutilate_dual_damage"),
                Component.translatable(
                        "ui.martial_spells.mutilate_delay",
                        Math.round(ATTACK_DELAY_FRACTION * 100.0F)
                ),
                Component.translatable(
                        "ui.martial_spells.base_cooldown",
                        BASE_COOLDOWN_SECONDS
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
        if (!level.isClientSide && caster instanceof ServerPlayer player) {
            /*
             * Use the same explicit server -> tracking-client animation route
             * that already works for Stunning Strike. This guarantees that the
             * authored Mutilate pose is requested even when Iron's instant
             * spell finish-animation path does not visibly fire.
             */
            MartialNetwork.sendToTrackingAndSelf(
                    new SyncMutilateAnimationPacket(player.getUUID()),
                    player
            );
            MutilateAttackManager.begin(player);
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }
}
