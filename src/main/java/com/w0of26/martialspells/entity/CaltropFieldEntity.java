package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.combat.MartialPowerHelper;
import com.w0of26.martialspells.damage.MartialDamageTypes;
import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.spells.CaltropsSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
    private static final double SURFACE_EPSILON = 0.01D;

    /*
     * Every horizontal field cell searches exactly three vertical
     * support layers: one block above the deployment floor, the
     * deployment floor itself, and one block below it.
     *
     * This makes the effective placement volume 3x3x3, 4x3x4, or
     * 5x3x5 depending on spell level without creating floating
     * caltrops over uneven terrain.
     */
    private static final int VERTICAL_SEARCH_RADIUS = 1;

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

    /**
     * Returns the local X/Z offset of one field cell from this
     * entity's center. Even-sized fields remain symmetrically
     * centered between their four middle cells.
     */
    public double getCellOffset(int cellIndex) {
        return -(
                getFieldSize() - 1
        ) / 2.0D + cellIndex;
    }

    /**
     * Finds the world-space Y position where a caltrop should sit in
     * the requested horizontal field cell.
     *
     * The search checks three possible support blocks from highest to
     * lowest. A cell with no sturdy top surface and clear space above
     * it returns NaN, so neither a visual caltrop nor its hazard cell
     * exists there.
     */
    public double getCaltropSurfaceY(
            int cellX,
            int cellZ
    ) {
        double worldX =
                getX() + getCellOffset(cellX);
        double worldZ =
                getZ() + getCellOffset(cellZ);

        int baseSupportY =
                Mth.floor(getY()) - 1;

        for (int yOffset = VERTICAL_SEARCH_RADIUS;
             yOffset >= -VERTICAL_SEARCH_RADIUS;
             yOffset--) {
            BlockPos support =
                    BlockPos.containing(
                            worldX,
                            baseSupportY + yOffset,
                            worldZ
                    );

            if (!level()
                    .getBlockState(support)
                    .isFaceSturdy(
                            level(),
                            support,
                            Direction.UP
                    )) {
                continue;
            }

            BlockPos above = support.above();

            if (!level()
                    .getBlockState(above)
                    .getCollisionShape(
                            level(),
                            above
                    )
                    .isEmpty()) {
                continue;
            }

            return support.getY()
                    + 1.0D
                    + SURFACE_EPSILON;
        }

        return Double.NaN;
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
                                        && isFeetInsideActiveCell(target)
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

        /*
         * Broad-phase box covers the complete three-layer terrain
         * search. Fine collision is then checked per active caltrop
         * cell in isFeetInsideActiveCell.
         */
        return new AABB(
                getX() - halfSize,
                getY() - 1.10D,
                getZ() - halfSize,
                getX() + halfSize,
                getY() + 1.0D + EFFECT_HEIGHT,
                getZ() + halfSize
        );
    }

    private boolean isFeetInsideActiveCell(
            LivingEntity target
    ) {
        int size = getFieldSize();

        for (int cellX = 0; cellX < size; cellX++) {
            double centerX =
                    getX() + getCellOffset(cellX);

            if (target.getX() < centerX - 0.5D
                    || target.getX() > centerX + 0.5D) {
                continue;
            }

            for (int cellZ = 0; cellZ < size; cellZ++) {
                double centerZ =
                        getZ() + getCellOffset(cellZ);

                if (target.getZ() < centerZ - 0.5D
                        || target.getZ() > centerZ + 0.5D) {
                    continue;
                }

                double surfaceY =
                        getCaltropSurfaceY(
                                cellX,
                                cellZ
                        );

                if (Double.isNaN(surfaceY)) {
                    return false;
                }

                return target.getY()
                        >= surfaceY - 0.10D
                        && target.getY()
                        <= surfaceY + EFFECT_HEIGHT;
            }
        }

        return false;
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
