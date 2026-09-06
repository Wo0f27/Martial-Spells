package com.w0of26.martialspells.entity;

import com.w0of26.martialspells.registry.MartialEntityRegistry;
import com.w0of26.martialspells.registry.MartialParticleRegistry;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class BarrageArrow extends AbstractArrow {
    public BarrageArrow(EntityType<? extends BarrageArrow> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
    }

    public BarrageArrow(Level level, LivingEntity owner) {
        super(MartialEntityRegistry.BARRAGE_ARROW.get(), owner, level);
        pickup = Pickup.DISALLOWED;
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
    public void tick() {
        super.tick();
        if (!level().isClientSide || isRemoved() || inGround) return;

        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() <= 1.0E-7D) return;

        for (int i = 0; i < 2; i++) {
            double offset = 0.12D + i * 0.14D;
            level().addParticle(
                    MartialParticleRegistry.BARRAGE_TRAIL.get(),
                    getX() - motion.x * offset,
                    getY() - motion.y * offset,
                    getZ() - motion.z * offset,
                    -motion.x * 0.025D,
                    -motion.y * 0.025D,
                    -motion.z * 0.025D
            );
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        LivingEntity target = result.getEntity() instanceof LivingEntity living ? living : null;
        int arrowsBefore = target == null ? 0 : target.getArrowCount();
        super.onHitEntity(result);
        if (!level().isClientSide && target != null) {
            target.setArrowCount(arrowsBefore);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) discard();
    }
}
