package com.w0of26.martialspells.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** Exact synchronous hit identity, never an attribute mutation or a crit roll. */
public final class PenanceCritContext {
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();
    private PenanceCritContext() {}

    public static Scope enter(DamageSource source, LivingEntity target, LivingEntity owner) {
        if (target.level().isClientSide || source.getEntity() != owner) {
            throw new IllegalArgumentException("Penance context requires a server hit and matching owner");
        }
        Scope scope = new Scope(Objects.requireNonNull(source), Objects.requireNonNull(target),
                Objects.requireNonNull(owner), ACTIVE.get());
        ACTIVE.set(scope);
        return scope;
    }

    public static boolean matches(DamageSource source, LivingEntity target, LivingEntity attacker) {
        Scope scope = ACTIVE.get();
        return scope != null && scope.source == source && scope.target == target
                && scope.owner == attacker && source.getEntity() == attacker
                && !target.level().isClientSide;
    }

    public static final class Scope implements AutoCloseable {
        private final DamageSource source;
        private final LivingEntity target;
        private final LivingEntity owner;
        private final Scope previous;
        private boolean closed;

        private Scope(DamageSource source, LivingEntity target, LivingEntity owner, Scope previous) {
            this.source = source;
            this.target = target;
            this.owner = owner;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) return;
            if (ACTIVE.get() != this) {
                throw new IllegalStateException("Penance scopes must close on their owning thread in reverse order");
            }
            closed = true;
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }
}
