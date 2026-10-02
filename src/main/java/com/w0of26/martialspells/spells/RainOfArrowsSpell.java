package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.NominalPhysicalRangedPower;
import com.w0of26.martialspells.combat.RainOfArrowsPending;
import com.w0of26.martialspells.combat.RangerAim;
import com.w0of26.martialspells.client.visual.RangerSourceVfx;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Sticky AIM meteor release with a saved 0/5/.../100-tick server sequence. */
public final class RainOfArrowsSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID = ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "rain_of_arrows");
    public static final int CAST_TICKS = 10;
    public static final int COOLDOWN_SECONDS = 15;
    public static final int LAUNCH_COUNT = 21;
    public static final int LAUNCH_DELAY_TICKS = 5;
    public static final double LAUNCH_RADIUS = 2.0D;
    public static final double LAUNCH_HEIGHT = 10.0D;
    public static final double VELOCITY = 1.5D;
    public static final double IMPACT_RADIUS = 1.5D;
    public static final double DAMAGE_COEFFICIENT = 0.4D;
    public static final double KNOCKBACK = 0.25D;
    private static final ResourceLocation CAST_ANIMATION = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "archery_upwards_pull");
    private static final ResourceLocation RELEASE_ANIMATION = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "archery_upwards_release");
    private static final Map<UUID, UUID> STICKY_AIM = new HashMap<>();

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC).setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(1).setCooldownSeconds(COOLDOWN_SECONDS).build();

    public RainOfArrowsSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = CAST_TICKS;
    }

    @Override public MartialTechniqueClass getTechniqueClass() { return MartialTechniqueClass.RANGER; }
    @Override public ResourceLocation getSpellResource() { return SPELL_ID; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getMaxLevel() { return 1; }
    @Override public int getSpellCooldown() { return COOLDOWN_SECONDS * 20; }
    @Override public int getEffectiveCastTime(int level, @Nullable LivingEntity caster) { return CAST_TICKS; }
    @Override public boolean allowLooting() { return false; }
    // Spell Engine loops this cast sound on clients until the 10-tick cast ends.
    @Override public Optional<SoundEvent> getCastStartSound() { return Optional.empty(); }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.empty(); }
    @Override public AnimationHolder getCastStartAnimation() { return new AnimationHolder(CAST_ANIMATION, true, false); }
    @Override public AnimationHolder getCastFinishAnimation() { return new AnimationHolder(RELEASE_ANIMATION, true, false); }

    @Override public CastResult canBeCastedBy(int level, CastSource source, MagicData data, Player player) {
        CastResult result = super.canBeCastedBy(level, source, data, player);
        if (!result.isSuccess()) return result;
        return RangedWeaponClassifier.isSupported(NominalPhysicalRangedPower.selectedWeapon(player))
                ? result : new CastResult(CastResult.Type.FAILURE,
                Component.translatable("ui.martial_spells.rain_requires_ranged_weapon").withStyle(ChatFormatting.RED));
    }

    @Override public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        super.onServerPreCast(level, spellLevel, caster, data);
        if (caster instanceof ServerPlayer player) updateSticky(player);
    }

    @Override public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (caster instanceof ServerPlayer player) updateSticky(player);
    }

    private static void updateSticky(ServerPlayer caster) {
        UUID current = RangerAim.entity(caster, 32.0D);
        if (current != null) { STICKY_AIM.put(caster.getUUID(), current); return; }
        UUID prior = STICKY_AIM.get(caster.getUUID());
        if (prior != null && caster.level() instanceof ServerLevel server) {
            net.minecraft.world.entity.Entity entity = server.getEntity(prior);
            if (entity == null || !RangerAim.visible(caster, entity)) STICKY_AIM.remove(caster.getUUID());
        }
    }

    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (level instanceof ServerLevel server && caster instanceof ServerPlayer player) {
            updateSticky(player);
            UUID targetId = STICKY_AIM.remove(player.getUUID());
            net.minecraft.world.entity.Entity target = targetId == null ? null : server.getEntity(targetId);
            Vec3 anchor = target != null ? target.position() : RangerAim.location(player, 32.0D);
            if (target == null) targetId = null;
            RainOfArrowsPending.begin(server, player, targetId, anchor,
                    NominalPhysicalRangedPower.get(player));
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    MartialSoundRegistry.RAIN_OF_ARROWS_RELEASE.get(), SoundSource.PLAYERS, 1, 1);
            RangerSourceVfx.rainRelease(server, player);
        }
        super.onCast(level, spellLevel, caster, source, data);
    }

    @Override public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster,
                                               MagicData data, boolean cancelled) {
        STICKY_AIM.remove(caster.getUUID());
        super.onServerCastComplete(level, spellLevel, caster, data, cancelled);
    }

    public static boolean canAreaDamage(LivingEntity caster, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || target == caster || caster.isAlliedTo(target)) return false;
        if (target instanceof net.minecraft.world.entity.player.Player
                || target instanceof net.minecraft.world.entity.npc.Villager
                || target instanceof net.minecraft.world.entity.animal.allay.Allay
                || target instanceof net.minecraft.world.entity.animal.IronGolem
                || target instanceof net.minecraft.world.entity.animal.Cat) return false;
        return !(target instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame());
    }
}
