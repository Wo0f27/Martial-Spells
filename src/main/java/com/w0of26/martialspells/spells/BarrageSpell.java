package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.RangedTechniqueHelper;
import com.w0of26.martialspells.entity.BarrageArrow;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class BarrageSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "barrage");

    public static final int MAX_LEVEL = 5;
    public static final int SHOT_INTERVAL_TICKS = 4;
    public static final int PREPARE_TICKS = 10;
    public static final int COOLDOWN_SECONDS = 6;

    private static final float INACCURACY = 0.0F;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public BarrageSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = getCastTimeTicks(MAX_LEVEL);
    }

    @Override public MartialTechniqueClass getTechniqueClass() { return MartialTechniqueClass.RANGER; }
    @Override public ResourceLocation getSpellResource() { return SPELL_ID; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) { return getCastTimeTicks(spellLevel); }
    @Override public boolean allowLooting() { return false; }
    @Override public Optional<SoundEvent> getCastStartSound() { return Optional.empty(); }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.empty(); }
    @Override public AnimationHolder getCastStartAnimation() { return AnimationHolder.none(); }
    @Override public AnimationHolder getCastFinishAnimation() { return AnimationHolder.none(); }

    public static int getProjectileCount(int spellLevel) {
        return switch (clampLevel(spellLevel)) {
            case 1 -> 2;
            case 2, 3 -> 3;
            case 4 -> 4;
            default -> 5;
        };
    }

    public static float getDamageMultiplier(int spellLevel) {
        return switch (clampLevel(spellLevel)) {
            case 1 -> 0.50F;
            case 2 -> 0.625F;
            case 3 -> 0.75F;
            case 4 -> 0.875F;
            default -> 1.00F;
        };
    }

    public static int getCastTimeTicks(int spellLevel) {
        return PREPARE_TICKS + (getProjectileCount(spellLevel) - 1) * SHOT_INTERVAL_TICKS + 1;
    }

    private static int clampLevel(int spellLevel) {
        return Mth.clamp(spellLevel, 1, MAX_LEVEL);
    }

    @Override
    public CastResult canBeCastedBy(int spellLevel, CastSource source, MagicData magicData, Player player) {
        CastResult result = super.canBeCastedBy(spellLevel, source, magicData, player);
        if (!result.isSuccess()) return result;
        if (RangedTechniqueHelper.findHeldRangedWeapon(player).isEmpty()) {
            return new CastResult(CastResult.Type.FAILURE,
                    Component.translatable("ui.martial_spells.barrage_requires_ranged_weapon")
                            .withStyle(ChatFormatting.RED));
        }
        return result;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.martial_spells.barrage_projectiles", getProjectileCount(spellLevel)),
                Component.translatable("ui.martial_spells.barrage_damage_per_arrow",
                        Math.round(getDamageMultiplier(spellLevel) * 100.0F)),
                Component.translatable("ui.martial_spells.barrage_interval", SHOT_INTERVAL_TICKS),
                Component.translatable("ui.martial_spells.fixed_cooldown", COOLDOWN_SECONDS),
                Component.translatable("ui.martial_spells.cooldown_reduction_immune"),
                Component.translatable("ui.martial_spells.scales_with_martial_power")
        );
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, @Nullable MagicData magicData) {
        super.onServerCastTick(level, spellLevel, caster, magicData);
        if (level.isClientSide || !(caster instanceof ServerPlayer player) || magicData == null) return;

        int remaining = magicData.getCastDurationRemaining();
        int castTime = getCastTimeTicks(spellLevel);
        int firstShotRemaining = castTime - PREPARE_TICKS;
        int projectileCount = getProjectileCount(spellLevel);

        for (int shotIndex = 0; shotIndex < projectileCount; shotIndex++) {
            if (remaining == firstShotRemaining - shotIndex * SHOT_INTERVAL_TICKS) {
                fireShot(player, getDamageMultiplier(spellLevel));
                break;
            }
        }
    }

    private static void fireShot(ServerPlayer player, float damageMultiplier) {
        ItemStack weapon = RangedTechniqueHelper.findHeldRangedWeapon(player);
        if (weapon.isEmpty()) return;

        BarrageArrow arrow = RangedTechniqueHelper.createBarrageArrow(
                player.level(), player, weapon, damageMultiplier);
        float velocity = RangedTechniqueHelper.getProjectileVelocity(weapon);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, INACCURACY);
        player.level().addFreshEntity(arrow);

        SoundEvent sound = RangedWeaponClassifier.isCrossbow(weapon)
                ? SoundEvents.CROSSBOW_SHOOT
                : SoundEvents.ARROW_SHOOT;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 1.0F,
                0.95F + player.getRandom().nextFloat() * 0.10F);
    }
}
