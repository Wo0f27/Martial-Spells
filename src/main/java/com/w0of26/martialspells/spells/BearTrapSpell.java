package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.combat.DualMeleePower;
import com.w0of26.martialspells.entity.BearTrapEntity;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncBearTrapAnimationPacket;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Forge/Iron's translation of frozen Rogues Bear Trap. */
public final class BearTrapSpell extends AbstractSpell implements MartialTechnique {
    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "bear_trap");

    /** PlayerAnimator registers by the authored internal name, not file name. */
    public static final ResourceLocation RELEASE_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "dual_handed_ground_release");

    public static final int MAX_LEVEL = 1;
    public static final int TRAP_COUNT = 3;
    public static final float PLACEMENT_DISTANCE = 2.0F;
    public static final float PLACEMENT_YAW_STEP = 120.0F;
    public static final int PLACEMENT_DELAY_STEP_TICKS = 3;
    public static final int BASE_COOLDOWN_SECONDS = 15;
    public static final float CONTROL_HEALTH_BASE = 100.0F;
    public static final float CONTROL_POWER_MULTIPLIER = 2.0F;
    private static final double GROUND_SEARCH_UP = 2.0D;
    private static final double GROUND_SEARCH_DOWN = 3.0D;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(MartialSchoolRegistry.MARTIAL_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(BASE_COOLDOWN_SECONDS)
            .build();

    public BearTrapSpell() {
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
        return new AnimationHolder(RELEASE_ANIMATION, true, false);
    }

    public static float getControlHealthLimit(ServerPlayer player) {
        return CONTROL_HEALTH_BASE + CONTROL_POWER_MULTIPLIER * DualMeleePower.calculate(player);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.martial_spells.bear_trap_count", TRAP_COUNT),
                Component.translatable("ui.martial_spells.bear_trap_duration", BearTrapEntity.ACTIVE_TICKS / 20.0F),
                Component.translatable("ui.martial_spells.bear_trap_root", BearTrapEntity.ROOT_DURATION_TICKS / 20.0F),
                Component.translatable("ui.martial_spells.base_cooldown", BASE_COOLDOWN_SECONDS)
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (!(level instanceof ServerLevel serverLevel) || !(caster instanceof ServerPlayer player)) {
            return;
        }

        float power = DualMeleePower.calculate(player);
        float controlLimit = CONTROL_HEALTH_BASE + CONTROL_POWER_MULTIPLIER * power;
        float baseYaw = player.getYRot();
        Vec3 castOrigin = player.position();

        for (int index = 0; index < TRAP_COUNT; index++) {
            float yaw = baseYaw + index * PLACEMENT_YAW_STEP;
            Vec3 offset = new Vec3(0.0D, 0.0D, PLACEMENT_DISTANCE)
                    .yRot((float) Math.toRadians(-yaw));
            Vec3 requested = castOrigin.add(offset);
            Vec3 grounded = groundTrapPosition(serverLevel, player, requested);

            BearTrapEntity trap = new BearTrapEntity(MartialEntityRegistry.BEAR_TRAP.get(), serverLevel);
            trap.configure(
                    player,
                    grounded,
                    yaw,
                    index * PLACEMENT_DELAY_STEP_TICKS,
                    power,
                    controlLimit
            );
            serverLevel.addFreshEntity(trap);
            trap.beginImmediatePlacementIfNeeded();
        }

        serverLevel.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                MartialSoundRegistry.BEAR_TRAP_RELEASE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        MartialNetwork.sendToTrackingAndSelf(new SyncBearTrapAnimationPacket(player.getUUID()), player);
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    /**
     * Resolve each radial placement against its own local collision surface. This keeps all three
     * traps sitting on slabs, stairs and uneven terrain instead of inheriting the caster's Y value.
     */
    private static Vec3 groundTrapPosition(ServerLevel level, ServerPlayer player, Vec3 requested) {
        Vec3 start = requested.add(0.0D, GROUND_SEARCH_UP, 0.0D);
        Vec3 end = requested.add(0.0D, -GROUND_SEARCH_DOWN, 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        return hit.getType() == HitResult.Type.MISS ? requested : hit.getLocation();
    }
}
