package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.RangedTechniqueHelper;
import com.w0of26.martialspells.entity.EntanglingArrow;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class EntanglingArrowSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "entangling_arrow");

    public static final int CAST_TIME_TICKS = 20;
    private static final float DAMAGE_MULTIPLIER = 0.75F;
    private static final float INACCURACY = 0.0F;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(12)
            .build();

    public EntanglingArrowSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = CAST_TIME_TICKS;
    }

    @Override public MartialTechniqueClass getTechniqueClass() { return MartialTechniqueClass.RANGER; }
    @Override public ResourceLocation getSpellResource() { return SPELL_ID; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) { return CAST_TIME_TICKS; }
    @Override public boolean allowLooting() { return false; }
    @Override public Optional<SoundEvent> getCastStartSound() { return Optional.empty(); }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.empty(); }
    @Override public AnimationHolder getCastStartAnimation() { return AnimationHolder.none(); }
    @Override public AnimationHolder getCastFinishAnimation() { return AnimationHolder.none(); }

    @Override
    public CastResult canBeCastedBy(int spellLevel, CastSource source, MagicData magicData, Player player) {
        CastResult result = super.canBeCastedBy(spellLevel, source, magicData, player);
        if (!result.isSuccess()) return result;
        if (RangedTechniqueHelper.findHeldRangedWeapon(player).isEmpty()) {
            return new CastResult(CastResult.Type.FAILURE,
                    Component.translatable("ui.martial_spells.entangling_arrow_requires_ranged_weapon")
                            .withStyle(ChatFormatting.RED));
        }
        return result;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.martial_spells.entangling_arrow_damage", 75),
                Component.translatable("ui.martial_spells.entangling_arrow_root_duration",
                        EntanglingArrow.ROOT_DURATION_TICKS / 20.0F),
                Component.translatable("ui.martial_spells.scales_with_martial_power")
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster,
                       CastSource castSource, MagicData magicData) {
        if (!level.isClientSide && caster instanceof ServerPlayer player) {
            fireShot(player);
        }
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private static void fireShot(ServerPlayer player) {
        ItemStack weapon = RangedTechniqueHelper.findHeldRangedWeapon(player);
        if (weapon.isEmpty()) return;

        EntanglingArrow arrow = RangedTechniqueHelper.createEntanglingArrow(
                player.level(), player, weapon, DAMAGE_MULTIPLIER);
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
