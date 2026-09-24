package com.w0of26.martialspells.spells;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Source-style look-relative ground placement for P4 constructs. */
final class PaladinP4Placement {
    private PaladinP4Placement() {
    }

    static Vec3 aheadOnGround(
            ServerLevel level,
            LivingEntity caster,
            double forwardDistance
    ) {
        Vec3 look =
                caster.getLookAngle();
        Vec3 horizontal =
                new Vec3(
                        look.x,
                        0.0D,
                        look.z
                );

        if (horizontal.lengthSqr() <= 1.0E-8D) {
            horizontal =
                    new Vec3(
                            0.0D,
                            0.0D,
                            1.0D
                    );
        } else {
            horizontal =
                    horizontal.normalize();
        }

        Vec3 requested =
                caster.position()
                        .add(
                                horizontal.scale(
                                        forwardDistance
                                )
                        );

        Vec3 top =
                requested.add(
                        0.0D,
                        3.0D,
                        0.0D
                );
        Vec3 bottom =
                requested.add(
                        0.0D,
                        -5.0D,
                        0.0D
                );

        HitResult hit =
                level.clip(
                        new ClipContext(
                                top,
                                bottom,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                caster
                        )
                );

        if (hit.getType()
                == HitResult.Type.BLOCK) {
            return hit.getLocation()
                    .add(
                            0.0D,
                            0.02D,
                            0.0D
                    );
        }

        return requested;
    }
}
