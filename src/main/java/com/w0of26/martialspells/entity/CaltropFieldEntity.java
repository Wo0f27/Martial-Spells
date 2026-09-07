package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.combat.MartialPowerHelper;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.spells.CaltropsSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

public final class CaltropFieldEntity extends Entity {
    private static final EntityDataAccessor<Integer>
            SPELL_LEVEL =
            SynchedEntityData.defineId(
                    CaltropFieldEntity.class,
                    EntityDataSerializers.INT
            );

    private static final int SLOW_REFRESH_TICKS = 12;
    private static final double EFFECT_HEIGHT = 0.55D;

    private UUID ownerUuid;
    private int remainingTicks = 1;

    public CaltropFieldEntity(
            EntityType<? extends CaltropFieldEntity> type,
            Level level
    ) {
        super(type, level);
        noPhysics = true;
    }

    public CaltropFieldEntity(
            Level level,
            ServerPlayer owner,
            int spellLevel
    ) {
        this(
                MartialEntityRegistry.CALTROP_FIELD.get(),
                level
        );

        ownerUuid = owner.getUUID();
        setSpellLevel(spellLevel);
        remainingTicks =
                CaltropsSpell.getDurationTicks(spellLevel);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SPELL_LEVEL, 1);
    }

    public int getSpellLevel() {
        return CaltropsSpell.clampLevel(
                entityData.get(SPELL_LEVEL)
        );
    }

    private void setSpellLevel(int spellLevel) {
        entityData.set(
                SPELL_LEVEL,
                CaltropsSpell.clampLevel(spellLevel)
        );
    }

    public int getFieldSize() {
        return CaltropsSpell.getFieldSize(
                getSpellLevel()
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (remainingTicks <= 0) {
            discard();
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)
                || ownerUuid == null) {
            discard();
            return;
        }

        ServerPlayer owner =
                serverLevel
                        .getServer()
                        .getPlayerList()
                        .getPlayer(ownerUuid);

        if (owner == null) {
            discard();
            return;
        }

        List<LivingEntity> targets =
                level().getEntitiesOfClass(
                        LivingEntity.class,
                        getEffectBox(),
                        target ->
                                canAffect(owner, target)
                                        && isFeetInsideField(target)
                );

        int spellLevel = getSpellLevel();
        int slowAmplifier =
                CaltropsSpell.getSlownessAmplifier(
                        spellLevel
                );

        boolean damageTick =
                tickCount
                        % CaltropsSpell.DAMAGE_INTERVAL_TICKS
                        == 0;

        float damage =
                CaltropsSpell.getDamagePerTick(
                        spellLevel
                )
                        * MartialPowerHelper
                        .getMartialPower(owner);

        for (LivingEntity target : targets) {
            target.setSprinting(false);

            target.addEffect(
                    new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN,
                            SLOW_REFRESH_TICKS,
                            slowAmplifier,
                            false,
                            false
                    )
            );

            if (damageTick && damage > 0.0F) {
                target.hurt(
                        MartialDamageTypes.caltrops(
                                owner,
                                this
                        ),
                        damage
                );
            }
        }

        remainingTicks--;

        if (remainingTicks <= 0) {
            discard();
        }
    }

    private AABB getEffectBox() {
        double halfSize =
                getFieldSize() / 2.0D;

        return new AABB(
                getX() - halfSize,
                getY() - 0.10D,
                getZ() - halfSize,
                getX() + halfSize,
                getY() + EFFECT_HEIGHT,
                getZ() + halfSize
        );
    }

    private boolean isFeetInsideField(
            LivingEntity target
    ) {
        double halfSize =
                getFieldSize() / 2.0D;

        return target.getX()
                >= getX() - halfSize
                && target.getX()
                <= getX() + halfSize
                && target.getZ()
                >= getZ() - halfSize
                && target.getZ()
                <= getZ() + halfSize
                && target.getY()
                >= getY() - 0.10D
                && target.getY()
                <= getY() + EFFECT_HEIGHT;
    }

    private static boolean canAffect(
            ServerPlayer owner,
            LivingEntity target
    ) {
        if (target == owner
                || !target.isAlive()
                || target.isDeadOrDying()
                || target.isSpectator()
                || !target.onGround()) {
            return false;
        }

        return !owner.isAlliedTo(target)
                && !target.isAlliedTo(owner);
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }

        tag.putInt(
                "SpellLevel",
                getSpellLevel()
        );

        tag.putInt(
                "RemainingTicks",
                remainingTicks
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        ownerUuid =
                tag.hasUUID("Owner")
                        ? tag.getUUID("Owner")
                        : null;

        setSpellLevel(
                tag.getInt("SpellLevel")
        );

        remainingTicks =
                Math.max(
                        1,
                        tag.getInt("RemainingTicks")
                );
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
