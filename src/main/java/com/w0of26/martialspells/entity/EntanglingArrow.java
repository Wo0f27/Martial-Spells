package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.registry.MartialEntityRegistry;
import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * Physical Ranger projectile that applies Iron's real RootEntity on a valid hit.
 */
public final class EntanglingArrow extends AbstractArrow {
    private static final int DEFAULT_ROOT_DURATION_TICKS = 60;
    private int rootDurationTicks = DEFAULT_ROOT_DURATION_TICKS;

    public EntanglingArrow(EntityType<? extends EntanglingArrow> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
    }

    public EntanglingArrow(Level level, LivingEntity owner) {
        super(MartialEntityRegistry.ENTANGLING_ARROW.get(), owner, level);
        pickup = Pickup.DISALLOWED;
    }

    public void setRootDurationTicks(int rootDurationTicks) {
        this.rootDurationTicks = Math.max(1, rootDurationTicks);
    }

    public int getRootDurationTicks() {
        return rootDurationTicks;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        LivingEntity target = result.getEntity() instanceof LivingEntity living ? living : null;
        int arrowsBefore = target == null ? 0 : target.getArrowCount();

        super.onHitEntity(result);

        if (!level().isClientSide && target != null) {
            target.setArrowCount(arrowsBefore);
            tryApplyRoot(target);
        }
    }

    private void tryApplyRoot(LivingEntity target) {
        if (!target.isAlive() || target.isDeadOrDying()) return;
        if (target.getType().is(ModTags.CANT_ROOT)) return;
        if (target.getVehicle() instanceof RootEntity) return;

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        if (owner == null) return;

        RootEntity root = new RootEntity(level(), owner);
        root.setDuration(rootDurationTicks);
        root.setTarget(target);
        root.moveTo(target.position());
        level().addFreshEntity(root);

        target.stopRiding();
        target.startRiding(root, true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RootDuration", rootDurationTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RootDuration")) {
            rootDurationTicks = Math.max(1, tag.getInt("RootDuration"));
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) discard();
    }
}
