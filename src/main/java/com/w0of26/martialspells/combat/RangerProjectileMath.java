package com.w0of26.martialspells.combat;

import net.minecraft.world.phys.Vec3;

/** Angular turn limit used by the frozen Spell Engine projectile deliveries. */
public final class RangerProjectileMath {
    private RangerProjectileMath() {}

    public static Vec3 turnTowards(Vec3 velocity, Vec3 desired, double degrees) {
        if (velocity.lengthSqr() < 1.0E-10 || desired.lengthSqr() < 1.0E-10) return velocity;
        double speed = velocity.length();
        Vec3 from = velocity.normalize();
        Vec3 to = desired.normalize();
        double angle = Math.acos(Math.max(-1.0D, Math.min(1.0D, from.dot(to))));
        double cap = Math.toRadians(degrees);
        if (angle <= cap) return to.scale(speed);
        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < 1.0E-10) axis = from.cross(new Vec3(0, 1, 0));
        if (axis.lengthSqr() < 1.0E-10) axis = from.cross(new Vec3(1, 0, 0));
        axis = axis.normalize();
        // Rodrigues rotation preserves speed and caps the turn by an angle, not a lerp fraction.
        Vec3 turned = from.scale(Math.cos(cap))
                .add(axis.cross(from).scale(Math.sin(cap)))
                .add(axis.scale(axis.dot(from) * (1 - Math.cos(cap))));
        return turned.normalize().scale(speed);
    }
}
