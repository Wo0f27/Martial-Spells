package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.api.RangerCompatibility;
import com.w0of26.martialspells.combat.ArchersPhysicalArrow;
import com.w0of26.martialspells.combat.RangedTechniqueHelper;
import com.w0of26.martialspells.combat.RangerArrowMetadata;
import com.w0of26.martialspells.combat.RangerTickScheduler;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The one existing Barrage ID: ten-tick draw, then critical ammo shots at 0/2/4 ticks. */
public final class BarrageSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "barrage");
    public static final int MAX_LEVEL = 1;
    public static final int CAST_TIME_TICKS = 10;
    public static final int COOLDOWN_SECONDS = 10;
    public static final int EXTRA_LAUNCH_COUNT = 2;
    public static final int EXTRA_LAUNCH_DELAY_TICKS = 2;
    public static final float DAMAGE_MULTIPLIER = 0.75F;
    public static final float VELOCITY = 3.15F;
    public static final float FOLLOW_UP_DIVERGENCE = 5.0F;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public BarrageSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = CAST_TIME_TICKS;
    }

    @Override public MartialTechniqueClass getTechniqueClass() { return MartialTechniqueClass.RANGER; }
    @Override public ResourceLocation getSpellResource() { return SPELL_ID; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public int getMinRarity() { return SpellRarity.RARE.getValue(); }
    /** Old persisted server config may still declare the prior five-level maximum. */
    @Override public int getMaxLevel() { return MAX_LEVEL; }
    /** Old persisted server config may still declare the prior six-second base. */
    @Override public int getSpellCooldown() { return COOLDOWN_SECONDS * 20; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) { return CAST_TIME_TICKS; }
    @Override public boolean allowLooting() { return false; }
    @Override public Optional<SoundEvent> getCastStartSound() { return Optional.of(MartialSoundRegistry.BOW_PULL.get()); }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.empty(); }
    @Override public AnimationHolder getCastStartAnimation() { return AnimationHolder.none(); }
    @Override public AnimationHolder getCastFinishAnimation() { return AnimationHolder.none(); }

    @Override
    public CastResult canBeCastedBy(int spellLevel, CastSource source, MagicData magicData, Player player) {
        CastResult result = super.canBeCastedBy(spellLevel, source, magicData, player);
        if (!result.isSuccess()) return result;
        ItemStack weapon = RangedTechniqueHelper.findHeldRangedWeapon(player);
        if (weapon.isEmpty()) {
            return new CastResult(CastResult.Type.FAILURE,
                    Component.translatable("ui.martial_spells.barrage_requires_ranged_weapon")
                            .withStyle(ChatFormatting.RED));
        }
        if (!player.getAbilities().instabuild && !isArrow(ArchersPhysicalArrow.findAmmunition(player, weapon))) {
            return new CastResult(CastResult.Type.FAILURE,
                    Component.translatable("ui.martial_spells.barrage_requires_arrow")
                            .withStyle(ChatFormatting.RED));
        }
        return result;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.martial_spells.barrage_projectiles", 1 + EXTRA_LAUNCH_COUNT),
                Component.translatable("ui.martial_spells.barrage_damage_per_arrow", 75),
                Component.translatable("ui.martial_spells.barrage_uses_ammo"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData magicData) {
        if (level instanceof ServerLevel server && caster instanceof ServerPlayer player
                && launch(server, player, 0)) {
            UUID casterId = player.getUUID();
            for (int index = 1; index <= EXTRA_LAUNCH_COUNT; index++) {
                final int launchIndex = index;
                RangerTickScheduler.schedule(server, casterId, index * EXTRA_LAUNCH_DELAY_TICKS,
                        (scheduledLevel, scheduledCasterId) -> {
                            ServerPlayer scheduledPlayer = scheduledLevel.getServer().getPlayerList()
                                    .getPlayer(scheduledCasterId);
                            if (scheduledPlayer == null || scheduledPlayer.isRemoved()
                                    || !scheduledPlayer.isAlive()
                                    || scheduledPlayer.level() != scheduledLevel) return;
                            launch(scheduledLevel, scheduledPlayer, launchIndex);
                        });
            }
        }
        super.onCast(level, spellLevel, caster, source, magicData);
    }

    /** Each invocation performs its own weapon/ammo lookup, as source ArrowHelper.shootArrow does. */
    private static boolean launch(ServerLevel level, ServerPlayer player, int index) {
        ItemStack weapon = RangedTechniqueHelper.findHeldRangedWeapon(player);
        if (weapon.isEmpty() || !RangedWeaponClassifier.isSupported(weapon)) return false;
        ItemStack ammunition = ArchersPhysicalArrow.findAmmunition(player, weapon);
        if (ammunition.isEmpty() && player.getAbilities().instabuild) ammunition = new ItemStack(Items.ARROW);
        if (!isArrow(ammunition)) return false;

        Optional<AbstractArrow> prepared = ArchersPhysicalArrow.prepare(level, player, weapon,
                ammunition.copyWithCount(1), DAMAGE_MULTIPLIER, 0,
                RangerCompatibility.extraBarragePenetrableTargets(player), true, true);
        if (prepared.isEmpty()) return false;
        AbstractArrow arrow = prepared.get();
        RangerArrowMetadata.markBarrage(arrow);
        ArchersPhysicalArrow.launch(arrow, player, VELOCITY,
                index == 0 ? 0.0F : FOLLOW_UP_DIVERGENCE);

        // Spell Engine ArrowHelper.load pays before spawning; the separate spell cost has consume=false.
        boolean free = player.getAbilities().instabuild
                || ammunition.is(Items.ARROW) && EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.INFINITY_ARROWS, weapon) > 0;
        if (free) arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        else ammunition.shrink(1);
        level.addFreshEntity(arrow);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F,
                index == 0 ? 1.0F : 1.0F / (player.getRandom().nextFloat() * 0.5F + 1.8F)
                        + (index == 1 ? 0.63F : 0.43F));
        return true;
    }

    private static boolean isArrow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ArrowItem;
    }
}
