package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.spells.PaladinVfx;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.fabric_extras.ranged_weapon.api.StatusEffects_RangedWeapon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** Frozen Paladins Battle Banner cloud translated to a standalone support entity. */
public final class BattleBannerEntity extends Entity {
    public static final int SPAWN_TICKS = 43;
    public static final int ACTIVE_TICKS = 10 * 20;
    public static final int DESPAWN_TICKS = 43;
    public static final int TOTAL_TICKS =
            SPAWN_TICKS
                    + ACTIVE_TICKS
                    + DESPAWN_TICKS;
    public static final int IMPACT_INTERVAL_TICKS = 10;
    public static final int EFFECT_DURATION_TICKS = 2 * 20;

    private UUID ownerId;
    private float radius = 3.0F;

    public BattleBannerEntity(
            EntityType<? extends BattleBannerEntity> type,
            Level level
    ) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public BattleBannerEntity(
            EntityType<? extends BattleBannerEntity> type,
            ServerLevel level,
            LivingEntity owner,
            float radius,
            float yaw
    ) {
        this(type, level);
        ownerId = owner.getUUID();
        this.radius = radius;
        setYRot(yaw);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide
                || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (tickCount >= TOTAL_TICKS) {
            discard();
            return;
        }

        if (tickCount >= SPAWN_TICKS
                && tickCount < SPAWN_TICKS + ACTIVE_TICKS) {
            if ((tickCount - SPAWN_TICKS)
                    % IMPACT_INTERVAL_TICKS == 0) {
                applyAura(serverLevel);
            }

            if ((tickCount - SPAWN_TICKS) % 40 == 0) {
                serverLevel.playSound(
                        null,
                        blockPosition(),
                        MartialSoundRegistry.BATTLE_BANNER_PRESENCE.get(),
                        SoundSource.PLAYERS,
                        0.8F,
                        1.0F
                );
            }

            if (tickCount % 4 == 0) {
                PaladinVfx.bannerPresence(
                        serverLevel,
                        position(),
                        radius
                );
            }
        }
    }

    private void applyAura(
            ServerLevel level
    ) {
        LivingEntity owner =
                resolveOwner(level);
        if (owner == null) {
            return;
        }

        double vertical =
                radius * 0.30D;
        AABB box =
                new AABB(
                        getX() - radius,
                        getY() - vertical,
                        getZ() - radius,
                        getX() + radius,
                        getY() + vertical,
                        getZ() + radius
                );

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        LivingEntity::isAlive
                )) {
            double dx =
                    target.getX() - getX();
            double dz =
                    target.getZ() - getZ();

            if (dx * dx + dz * dz
                    > radius * radius) {
                continue;
            }

            if (!Utils.shouldHealEntity(
                    owner,
                    target
            )) {
                continue;
            }

            target.addEffect(
                    new MobEffectInstance(
                            MartialEffectRegistry.BATTLE_BANNER.get(),
                            EFFECT_DURATION_TICKS,
                            0,
                            false,
                            true,
                            true
                    ),
                    owner
            );

            // Frozen Paladins applies +40% ranged haste. The Forge
            // RangedWeaponAPI port represents haste as a 10%-per-level
            // status effect, so amplifier 3 is the exact four-level mapping.
            target.addEffect(
                    new MobEffectInstance(
                            StatusEffects_RangedWeapon.HASTE.effect,
                            EFFECT_DURATION_TICKS,
                            3,
                            false,
                            false,
                            false
                    ),
                    owner
            );
        }
    }

    private LivingEntity resolveOwner(
            ServerLevel level
    ) {
        if (ownerId == null) {
            return null;
        }

        Entity entity =
                level.getEntity(ownerId);
        return entity instanceof LivingEntity living
                ? living
                : null;
    }

    public float getRadius() {
        return radius;
    }

    public float lifecycleScale(
            float partialTick
    ) {
        float age =
                tickCount + partialTick;

        if (age < SPAWN_TICKS) {
            return age / SPAWN_TICKS;
        }

        float despawnStart =
                SPAWN_TICKS + ACTIVE_TICKS;
        if (age >= despawnStart) {
            return Math.max(
                    0.0F,
                    1.0F
                            - (age - despawnStart)
                            / DESPAWN_TICKS
            );
        }

        return 1.0F;
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        ownerId =
                tag.hasUUID("Owner")
                        ? tag.getUUID("Owner")
                        : null;
        radius =
                tag.contains("Radius")
                        ? tag.getFloat("Radius")
                        : 3.0F;
        noPhysics = true;
        noCulling = true;
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        if (ownerId != null) {
            tag.putUUID(
                    "Owner",
                    ownerId
            );
        }
        tag.putFloat(
                "Radius",
                radius
        );
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks
                .getEntitySpawningPacket(this);
    }
}
