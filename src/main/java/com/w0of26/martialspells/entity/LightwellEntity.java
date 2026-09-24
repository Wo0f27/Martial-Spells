package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.PaladinVfx;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.Comparator;
import java.util.UUID;

/**
 * Stationary Lightwell support summon.
 *
 * <p>Source lifespan: 20 spawn ticks + 12 active seconds + 20 despawn ticks.
 * It continually reacquires wounded friendlies within Holy Mote's 12-block
 * range and clears that target after every cast.</p>
 */
public final class LightwellEntity extends Entity {
    public static final int SPAWN_TICKS = 20;
    public static final int ACTIVE_TICKS = 12 * 20;
    public static final int DESPAWN_TICKS = 20;
    public static final int TOTAL_TICKS =
            SPAWN_TICKS
                    + ACTIVE_TICKS
                    + DESPAWN_TICKS;
    public static final double HEAL_RANGE = 12.0D;
    public static final int BASE_MOTE_COOLDOWN_TICKS = 30;

    private UUID ownerId;
    private float healingPower = 1.0F;
    private int moteCooldown;

    public LightwellEntity(
            EntityType<? extends LightwellEntity> type,
            Level level
    ) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public LightwellEntity(
            EntityType<? extends LightwellEntity> type,
            ServerLevel level,
            LivingEntity owner,
            float healingPower
    ) {
        this(type, level);
        ownerId = owner.getUUID();
        this.healingPower = healingPower;
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

        if (tickCount == TOTAL_TICKS - DESPAWN_TICKS) {
            serverLevel.playSound(
                    null,
                    blockPosition(),
                    MartialSoundRegistry.LIGHTWELL_DESPAWN.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        if (tickCount >= TOTAL_TICKS) {
            discard();
            return;
        }

        if (tickCount % 6 == 0) {
            PaladinVfx.lightwellAura(
                    serverLevel,
                    position()
            );
        }

        if (tickCount % 60 == 0) {
            serverLevel.playSound(
                    null,
                    blockPosition(),
                    MartialSoundRegistry.LIGHTWELL_AMBIENT.get(),
                    SoundSource.PLAYERS,
                    0.7F,
                    1.0F
            );
        }

        if (!isActive()) {
            return;
        }

        if (moteCooldown > 0) {
            moteCooldown--;
            return;
        }

        LivingEntity owner =
                resolveOwner(serverLevel);
        if (owner == null) {
            return;
        }

        LivingEntity target =
                findWoundedTarget(
                        serverLevel,
                        owner
                );
        if (target == null) {
            return;
        }

        launchMote(
                serverLevel,
                owner,
                target
        );
        moteCooldown =
                effectiveMoteCooldown(owner);
    }

    private LivingEntity findWoundedTarget(
            ServerLevel level,
            LivingEntity owner
    ) {
        AABB search =
                getBoundingBox().inflate(
                        HEAL_RANGE
                );

        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        target ->
                                target.isAlive()
                                        && target.getHealth()
                                        < target.getMaxHealth()
                                        && Utils.shouldHealEntity(
                                                owner,
                                                target
                                        )
                )
                .stream()
                .filter(target ->
                        target.distanceToSqr(this)
                                <= HEAL_RANGE * HEAL_RANGE
                )
                .min(
                        Comparator.comparingDouble(
                                target ->
                                        target.distanceToSqr(this)
                        )
                )
                .orElse(null);
    }

    private void launchMote(
            ServerLevel level,
            LivingEntity owner,
            LivingEntity target
    ) {
        float healAmount =
                healingPower * 0.35F;

        HolyMoteProjectile mote =
                new HolyMoteProjectile(
                        MartialEntityRegistry
                                .HOLY_MOTE
                                .get(),
                        level,
                        this,
                        owner,
                        target,
                        healAmount
                );

        mote.setPos(
                getX(),
                getY() + 0.9D,
                getZ()
        );
        mote.launchToward(
                target
        );
        level.addFreshEntity(mote);
    }

    private int effectiveMoteCooldown(
            LivingEntity owner
    ) {
        double reduction =
                owner.getAttributeValue(
                        AttributeRegistry
                                .COOLDOWN_REDUCTION
                                .get()
                );

        double multiplier =
                2.0D
                        - Utils.softCapFormula(
                                reduction
                        );

        return Math.max(
                1,
                (int) Math.round(
                        BASE_MOTE_COOLDOWN_TICKS
                                * multiplier
                )
        );
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

    public boolean isActive() {
        return tickCount >= SPAWN_TICKS
                && tickCount
                < SPAWN_TICKS + ACTIVE_TICKS;
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
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(
            net.minecraft.world.damagesource.DamageSource source,
            float amount
    ) {
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
        healingPower =
                tag.contains("HealingPower")
                        ? tag.getFloat("HealingPower")
                        : 1.0F;
        moteCooldown =
                tag.getInt("MoteCooldown");
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
                "HealingPower",
                healingPower
        );
        tag.putInt(
                "MoteCooldown",
                moteCooldown
        );
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks
                .getEntitySpawningPacket(this);
    }
}
