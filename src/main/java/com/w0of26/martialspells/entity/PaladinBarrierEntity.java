package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * Frozen Paladins Barrier gameplay entity.
 *
 * <p>The source is an 8x4 collidable volume for ten seconds. Protected allies
 * inside receive a five-tick immunity refresh every four ticks; intruding
 * hostiles are knocked back at 0.3 strength. Hostile projectiles entering the
 * volume are removed here because Martial Spells intentionally does not depend
 * on Spell Engine's reverse-collision mixin.</p>
 */
public final class PaladinBarrierEntity extends Entity {
    public static final int LIFE_TICKS = 10 * 20;
    public static final int CHECK_INTERVAL_TICKS = 4;
    public static final int PROTECTION_TICKS = CHECK_INTERVAL_TICKS + 1;
    public static final float KNOCKBACK_STRENGTH = 0.30F;
    public static final int EXPIRATION_TICKS = 20;

    private UUID ownerId;

    public PaladinBarrierEntity(
            EntityType<? extends PaladinBarrierEntity> type,
            Level level
    ) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PaladinBarrierEntity(
            EntityType<? extends PaladinBarrierEntity> type,
            ServerLevel level,
            LivingEntity owner
    ) {
        this(type, level);
        ownerId = owner.getUUID();
        setPos(
                owner.getX(),
                owner.getY(),
                owner.getZ()
        );
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (tickCount == LIFE_TICKS - EXPIRATION_TICKS) {
            serverLevel.playSound(
                    null,
                    blockPosition(),
                    MartialSoundRegistry.HOLY_BARRIER_DEACTIVATE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        if (tickCount > LIFE_TICKS) {
            discard();
            return;
        }

        LivingEntity owner = resolveOwner(serverLevel);
        if (owner == null) {
            return;
        }

        if (tickCount % CHECK_INTERVAL_TICKS == 0) {
            refreshProtection(owner);
            blockHostileProjectiles(owner);
        }
    }

    private void refreshProtection(
            LivingEntity owner
    ) {
        for (LivingEntity living :
                level().getEntitiesOfClass(
                        LivingEntity.class,
                        getBoundingBox().inflate(0.10D),
                        LivingEntity::isAlive
                )) {
            if (Utils.shouldHealEntity(owner, living)) {
                living.addEffect(
                        new MobEffectInstance(
                                MartialEffectRegistry.BARRIER_PROTECTED.get(),
                                PROTECTION_TICKS,
                                0,
                                false,
                                false,
                                false
                        ),
                        owner
                );
                continue;
            }

            living.knockback(
                    KNOCKBACK_STRENGTH,
                    getX() - living.getX(),
                    getZ() - living.getZ()
            );
            living.hurtMarked = true;
        }
    }

    private void blockHostileProjectiles(
            LivingEntity owner
    ) {
        for (Projectile projectile :
                level().getEntitiesOfClass(
                        Projectile.class,
                        getBoundingBox().inflate(0.10D),
                        Projectile::isAlive
                )) {
            Entity projectileOwner =
                    projectile.getOwner();

            if (projectileOwner instanceof LivingEntity livingOwner
                    && Utils.shouldHealEntity(owner, livingOwner)) {
                continue;
            }

            level().playSound(
                    null,
                    projectile.blockPosition(),
                    MartialSoundRegistry.HOLY_BARRIER_IMPACT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
            projectile.discard();
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

    public boolean isExpiring() {
        return tickCount
                >= LIFE_TICKS - EXPIRATION_TICKS;
    }

    public float lifeProgress(
            float partialTick
    ) {
        return Math.min(
                1.0F,
                (tickCount + partialTick)
                        / (float) LIFE_TICKS
        );
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        ownerId =
                tag.hasUUID("Owner")
                        ? tag.getUUID("Owner")
                        : null;
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
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks
                .getEntitySpawningPacket(this);
    }
}
