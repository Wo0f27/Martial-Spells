package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.RangedTechniqueHelper;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A physical Ranger technique that launches a three-arrow burst using
 * the held bow/crossbow as the authoritative damage and velocity base.
 */
public final class BarrageSpell extends AbstractSpell {
    private static final int PROJECTILE_COUNT = 3;
    private static final float DAMAGE_MULTIPLIER = 0.75F;
    private static final int CAST_TIME_TICKS = 10;
    private static final float INACCURACY = 1.0F;

    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "barrage"
            );

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.RARE)
                    .setSchoolResource(
                            MartialSchoolRegistry.MARTIAL_RESOURCE
                    )
                    .setMaxLevel(1)
                    .setCooldownSeconds(10)
                    .build();

    public BarrageSpell() {
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = CAST_TIME_TICKS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
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
     * Barrage's half-second wind-up is a weapon-technique timing, not a
     * generic spell cast. Iron's cast-time-reduction attribute therefore
     * does not shorten it.
     */
    @Override
    public int getEffectiveCastTime(
            int spellLevel,
            @Nullable LivingEntity entity
    ) {
        return CAST_TIME_TICKS;
    }

    @Override
    public CastResult canBeCastedBy(
            int spellLevel,
            CastSource castSource,
            MagicData playerMagicData,
            Player player
    ) {
        CastResult baseResult = super.canBeCastedBy(
                spellLevel,
                castSource,
                playerMagicData,
                player
        );

        if (!baseResult.isSuccess()) {
            return baseResult;
        }

        if (RangedTechniqueHelper
                .findHeldRangedWeapon(player)
                .isEmpty()) {
            return new CastResult(
                    CastResult.Type.FAILURE,
                    Component.translatable(
                            "ui.martial_spells.barrage_requires_ranged_weapon"
                    ).withStyle(ChatFormatting.RED)
            );
        }

        if (RangedTechniqueHelper
                .findArrowAmmo(player)
                .isEmpty()) {
            return new CastResult(
                    CastResult.Type.FAILURE,
                    Component.translatable(
                            "ui.martial_spells.barrage_requires_arrow"
                    ).withStyle(ChatFormatting.RED)
            );
        }

        return baseResult;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(
            int spellLevel,
            LivingEntity caster
    ) {
        return List.of(
                Component.translatable(
                        "ui.martial_spells.barrage_projectiles",
                        PROJECTILE_COUNT
                ),
                Component.translatable(
                        "ui.martial_spells.barrage_damage_per_arrow",
                        Math.round(DAMAGE_MULTIPLIER * 100.0F)
                ),
                Component.translatable(
                        "ui.martial_spells.barrage_ammo_behavior"
                )
        );
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
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData magicData
    ) {
        if (!(entity instanceof Player player)
                || level.isClientSide) {
            return;
        }

        ItemStack weapon =
                RangedTechniqueHelper.findHeldRangedWeapon(player);
        ItemStack ammo =
                RangedTechniqueHelper.findArrowAmmo(player);

        /*
         * Revalidate at release because the player can swap equipment
         * during the wind-up. The pre-cast checks remain authoritative
         * for beginning the cast; this prevents an invalid projectile
         * from being created after an equipment swap.
         */
        if (weapon.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(
                            "ui.martial_spells.barrage_requires_ranged_weapon"
                    ).withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        if (ammo.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(
                            "ui.martial_spells.barrage_requires_arrow"
                    ).withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        float velocity =
                RangedTechniqueHelper.getProjectileVelocity(weapon);

        for (int shot = 0; shot < PROJECTILE_COUNT; shot++) {
            AbstractArrow arrow =
                    RangedTechniqueHelper.createTechniqueArrow(
                            level,
                            player,
                            weapon,
                            ammo,
                            DAMAGE_MULTIPLIER
                    );

            RangedTechniqueHelper.markBarrageArrow(arrow);

            arrow.shootFromRotation(
                    player,
                    player.getXRot(),
                    player.getYRot(),
                    0.0F,
                    velocity,
                    INACCURACY
            );

            level.addFreshEntity(arrow);
        }

        SoundEvent releaseSound =
                weapon.getItem() instanceof CrossbowItem
                        ? SoundEvents.CROSSBOW_SHOOT
                        : SoundEvents.ARROW_SHOOT;

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                releaseSound,
                SoundSource.PLAYERS,
                1.0F,
                0.95F + level.random.nextFloat() * 0.10F
        );

        super.onCast(
                level,
                spellLevel,
                entity,
                castSource,
                magicData
        );
    }
}
