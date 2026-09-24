package com.w0of26.martialspells.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * Presentation-only anchor for Holy Light's continuous beam renderer.
 */
public final class HolyBeamVisualEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(
                    HolyBeamVisualEntity.class,
                    EntityDataSerializers.INT
            );

    private static final int SAFETY_LIFETIME_TICKS = 120;

    public HolyBeamVisualEntity(
            EntityType<? extends HolyBeamVisualEntity> type,
            Level level
    ) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public HolyBeamVisualEntity(
            EntityType<? extends HolyBeamVisualEntity> type,
            Level level,
            Entity owner
    ) {
        this(type, level);
        entityData.set(
                OWNER_ID,
                owner.getId()
        );
        setPos(
                owner.getX(),
                owner.getY(),
                owner.getZ()
        );
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(
                OWNER_ID,
                -1
        );
    }

    public int getOwnerEntityId() {
        return entityData.get(
                OWNER_ID
        );
    }

    @Override
    public void tick() {
        super.tick();

        Entity owner =
                level().getEntity(
                        getOwnerEntityId()
                );

        if (owner == null
                || owner.isRemoved()
                || tickCount > SAFETY_LIFETIME_TICKS) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        setPos(
                owner.getX(),
                owner.getY(),
                owner.getZ()
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        if (tag.contains("OwnerId")) {
            entityData.set(
                    OWNER_ID,
                    tag.getInt("OwnerId")
            );
        }
        noPhysics = true;
        noCulling = true;
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putInt(
                "OwnerId",
                getOwnerEntityId()
        );
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks
                .getEntitySpawningPacket(this);
    }
}
