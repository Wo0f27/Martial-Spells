package com.w0of26.martialspells.combat;

import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.UUID;

/** Server-side AIM selection shared only by the two AR4 spell projectiles. */
public final class RangerAim {
    private RangerAim() {}

    public static UUID entity(ServerPlayer caster, double range) {
        HitResult hit = RaycastBuilder.begin(caster.level(), caster).range((float) range)
                .checkForBlocks(true).bbInflation(0.3F)
                .filter(candidate -> candidate instanceof LivingEntity living
                        && living.isAlive() && !living.isSpectator()
                        && candidate != caster && !caster.isAlliedTo(candidate))
                .build();
        return hit instanceof EntityHitResult entityHit ? entityHit.getEntity().getUUID() : null;
    }

    public static Vec3 location(ServerPlayer caster, double range) {
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(range));
        HitResult hit = caster.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    public static boolean visible(ServerPlayer caster, Entity target) {
        return !target.isRemoved() && caster.hasLineOfSight(target);
    }
}
