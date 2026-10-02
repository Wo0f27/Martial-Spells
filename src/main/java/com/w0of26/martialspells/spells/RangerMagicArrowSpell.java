package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.NominalPhysicalRangedPower;
import com.w0of26.martialspells.combat.RangerAim;
import com.w0of26.martialspells.client.visual.RangerSourceVfx;
import com.w0of26.martialspells.entity.RangerMagicArrowProjectile;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialParticleRegistry;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.technique.MartialTechnique;
import com.w0of26.martialspells.technique.MartialTechniqueClass;
import com.w0of26.martialspells.technique.ReleaseChargedTechnique;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Source one-second held CHARGE translated through Iron's opted-in release bridge. */
public final class RangerMagicArrowSpell extends AbstractSpell implements MartialTechnique, ReleaseChargedTechnique {
    public static final ResourceLocation SPELL_ID = ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "magic_arrow");
    public static final int COOLDOWN_SECONDS = 8;
    public static final int REFERENCE_CHARGE_TICKS = 20;
    public static final float MIN_RELEASE_RATIO = 0.2F;
    public static final float BASE_RANGE = 16.0F;
    public static final float RANGE_BONUS = 48.0F;
    public static final float VELOCITY = 1.5F;
    public static final float HOMING_DEGREES_PER_TICK = 2.0F;
    public static final int PIERCE = 99999;
    public static final double DAMAGE_COEFFICIENT = 1.2D;
    public static final double KNOCKBACK_COEFFICIENT = 2.0D;
    private static final ResourceLocation CAST_ANIMATION = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "archery_pull");
    private static final ResourceLocation RELEASE_ANIMATION = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "archery_release");
    private static final Map<UUID, Float> RELEASE_CHARGE = new HashMap<>();

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC).setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(1).setCooldownSeconds(COOLDOWN_SECONDS).build();

    public RangerMagicArrowSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = REFERENCE_CHARGE_TICKS;
    }

    @Override public MartialTechniqueClass getTechniqueClass() { return MartialTechniqueClass.RANGER; }
    @Override public ResourceLocation getSpellResource() { return SPELL_ID; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getMaxLevel() { return 1; }
    @Override public int getSpellCooldown() { return COOLDOWN_SECONDS * 20; }
    @Override public boolean allowLooting() { return false; }
    // Spell Engine uses a client-side repeat-until-release sound; the local client hook owns it.
    @Override public Optional<SoundEvent> getCastStartSound() { return Optional.empty(); }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.empty(); }
    @Override public AnimationHolder getCastStartAnimation() { return new AnimationHolder(CAST_ANIMATION, true, false); }
    @Override public AnimationHolder getCastFinishAnimation() { return new AnimationHolder(RELEASE_ANIMATION, true, false); }

    @Override public int getEffectiveCastTime(int level, @Nullable LivingEntity caster) {
        // Iron's Cast Time Reduction is translated to an effective castHaste. The source
        // reference is round(20 / castHaste), and a full charge may be held indefinitely.
        double modifier = caster == null ? 1.0D
                : 2.0D - Utils.softCapFormula(caster.getAttributeValue(AttributeRegistry.CAST_TIME_REDUCTION.get()));
        double castHaste = modifier <= 0.0D ? 1.0D : 1.0D / modifier;
        return Math.max(1, (int) Math.round(REFERENCE_CHARGE_TICKS / castHaste));
    }

    @Override public CastResult canBeCastedBy(int level, CastSource source, MagicData data, Player player) {
        CastResult result = super.canBeCastedBy(level, source, data, player);
        if (!result.isSuccess()) return result;
        return hasCost(player) ? result : new CastResult(CastResult.Type.FAILURE,
                Component.translatable("ui.martial_spells.magic_arrow_requires_arrow").withStyle(ChatFormatting.RED));
    }

    @Override public boolean releaseOnUseStop(ServerPlayer player, MagicData data) {
        if (!data.isCasting() || data.getCastingSpell().getSpell() != this) return false;
        float ratio = Mth.clamp(data.getCastCompletionPercent(), 0.0F, 1.0F);
        // Returning false delegates the fizzle to Iron's normal LONG cancellation: no spell
        // cost, cooldown, or projectile. A missing release-time arrow cancels the same way.
        if (ratio < MIN_RELEASE_RATIO || !hasCost(player)) return false;
        RELEASE_CHARGE.put(player.getUUID(), ratio);
        int level = data.getCastingSpellLevel();
        CastSource source = data.getCastSource();
        castSpell(player.level(), level, player, source, true);
        if (source == CastSource.SCROLL) Scroll.attemptRemoveScrollAfterCast(player);
        onServerCastComplete(player.level(), level, player, data, false);
        player.stopUsingItem();
        return true;
    }

    @Override public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (!(level instanceof ServerLevel server)) return;
        RangerSourceVfx.magicCharge(server, caster);
    }

    private static boolean freeCost(Player player) {
        ItemStack weapon = NominalPhysicalRangedPower.selectedWeapon(player);
        return player.getAbilities().instabuild || !weapon.isEmpty()
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, weapon) > 0;
    }

    private static ItemStack arrowCost(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            // Archers' spell cost id is exactly minecraft:arrow, not an arrow-item tag.
            if (stack.is(Items.ARROW)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasCost(Player player) { return freeCost(player) || !arrowCost(player).isEmpty(); }

    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (level instanceof ServerLevel server && caster instanceof ServerPlayer player) {
            float ratio = RELEASE_CHARGE.getOrDefault(player.getUUID(), 1.0F);
            if (ratio >= MIN_RELEASE_RATIO && hasCost(player)) {
                if (!freeCost(player)) arrowCost(player).shrink(1);
                double sampledPower = NominalPhysicalRangedPower.get(player); // source ImpactContext at release
                double range = BASE_RANGE + RANGE_BONUS * ratio;
                UUID target = RangerAim.entity(player, range);
                RangerMagicArrowProjectile arrow = new RangerMagicArrowProjectile(
                        MartialEntityRegistry.RANGER_MAGIC_ARROW.get(), server, player,
                        target, sampledPower, ratio);
                Vec3 look = player.getLookAngle().normalize();
                Vec3 origin = player.getEyePosition().add(look.scale(0.5D));
                arrow.setPos(origin.x, origin.y, origin.z);
                arrow.setDeltaMovement(look.scale(VELOCITY));
                server.addFreshEntity(arrow);
                server.playSound(null, player.getX(), player.getY(), player.getZ(),
                        MartialSoundRegistry.MAGIC_ARROW_RELEASE.get(), SoundSource.PLAYERS, 1, 1);
                RangerSourceVfx.magicRelease(server, player);
            }
        }
        super.onCast(level, spellLevel, caster, source, data);
    }

    @Override public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster,
                                               MagicData data, boolean cancelled) {
        RELEASE_CHARGE.remove(caster.getUUID());
        super.onServerCastComplete(level, spellLevel, caster, data, cancelled);
    }
}
