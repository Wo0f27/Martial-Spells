package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.spells.CaltropsSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class CaltropBundleProjectile
        extends ThrowableItemProjectile {
    private static final int MAX_LIFETIME_TICKS = 200;
    private static final int MAX_GROUND_SEARCH_DEPTH = 6;

    private int spellLevel = 1;

    public CaltropBundleProjectile(
            EntityType<? extends CaltropBundleProjectile> type,
            Level level
    ) {
        super(type, level);
    }

    public CaltropBundleProjectile(
            Level level,
            ServerPlayer owner,
            int spellLevel
    ) {
        super(
                MartialEntityRegistry.CALTROP_BUNDLE.get(),
                owner,
                level
        );

        this.spellLevel =
                CaltropsSpell.clampLevel(spellLevel);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.BUNDLE;
    }

    /**
     * Caltrops are a ground-placement technique. The thrown bundle
     * deliberately passes through entities and only deploys when it
     * collides with a block.
     */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide
                && tickCount > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (level().isClientSide) {
            return;
        }

        if (!(getOwner() instanceof ServerPlayer owner)) {
            discard();
            return;
        }

        BlockPos support = findGroundSupport(result);

        if (support == null) {
            discard();
            return;
        }

        Vec3 impact = result.getLocation();
        Direction face = result.getDirection();

        double centerX =
                impact.x + face.getStepX() * 0.25D;
        double centerZ =
                impact.z + face.getStepZ() * 0.25D;

        CaltropFieldEntity field =
                new CaltropFieldEntity(
                        level(),
                        owner,
                        spellLevel
                );

        field.setPos(
                centerX,
                support.getY() + 1.01D,
                centerZ
        );

        level().addFreshEntity(field);
        discard();
    }

    private BlockPos findGroundSupport(
            BlockHitResult result
    ) {
        Vec3 impact = result.getLocation();
        Direction face = result.getDirection();

        double outsideX =
                impact.x + face.getStepX() * 0.25D;
        double outsideY =
                impact.y + face.getStepY() * 0.25D;
        double outsideZ =
                impact.z + face.getStepZ() * 0.25D;

        BlockPos cursor =
                BlockPos.containing(
                        outsideX,
                        outsideY,
                        outsideZ
                );

        for (int depth = 0;
             depth <= MAX_GROUND_SEARCH_DEPTH;
             depth++) {
            BlockPos support = cursor.below();

            if (level()
                    .getBlockState(support)
                    .isFaceSturdy(
                            level(),
                            support,
                            Direction.UP
                    )) {
                return support;
            }

            cursor = cursor.below();
        }

        if (face == Direction.UP) {
            BlockPos hitBlock = result.getBlockPos();

            if (level()
                    .getBlockState(hitBlock)
                    .isFaceSturdy(
                            level(),
                            hitBlock,
                            Direction.UP
                    )) {
                return hitBlock;
            }
        }

        return null;
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpellLevel", spellLevel);
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(tag);

        spellLevel =
                CaltropsSpell.clampLevel(
                        tag.getInt("SpellLevel")
                );
    }

    @Override
    public Packet<ClientGamePacketListener>
    getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
