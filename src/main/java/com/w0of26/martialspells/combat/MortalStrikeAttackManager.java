package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.mixin.EntityInvulnerabilityAccessor;
import com.w0of26.martialspells.mixin.LivingEntityAttackStrengthAccessor;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.spells.MortalStrikeSpell;
import com.w0of26.martialspells.visual.ShatterBloodVfx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Executes Mortal Strike's delayed source-shaped vanilla melee contact frame.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MortalStrikeAttackManager {
    private static final UUID DAMAGE_MODIFIER_ID =
            UUID.fromString(
                    "f6bb3db3-c966-4d09-9e14-c8519db7cb59"
            );

    private static final double HALF_WIDTH =
            MortalStrikeSpell.RANGE
                    * MortalStrikeSpell.HITBOX_WIDTH_FACTOR
                    * 0.5D;

    private static final double HALF_HEIGHT =
            MortalStrikeSpell.RANGE
                    * MortalStrikeSpell.HITBOX_HEIGHT_FACTOR
                    * 0.5D;

    private static final double HALF_LENGTH =
            MortalStrikeSpell.RANGE
                    * MortalStrikeSpell.HITBOX_LENGTH_FACTOR
                    * 0.5D;

    private static final double ARC_COSINE =
            Math.cos(
                    Math.toRadians(
                            MortalStrikeSpell.ARC_DEGREES * 0.5D
                    )
            );

    private static final double EPSILON = 1.0E-7D;

    private static final Map<UUID, PendingAttack>
            PENDING_ATTACKS = new HashMap<>();

    private MortalStrikeAttackManager() {}

    public static void begin(ServerPlayer player) {
        PENDING_ATTACKS.put(
                player.getUUID(),
                new PendingAttack(
                        player.level().dimension(),
                        player.serverLevel().getGameTime(),
                        getImpactDelayTicks(player)
                )
        );

        // Source melee swing sound begins when delivery starts.
        player.serverLevel().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                MartialSoundRegistry.MORTAL_STRIKE_WHOOSH.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    /**
     * Source attack.delay=0.3 is a ratio of the resolved vanilla melee attack
     * duration. On 1.20.1 that duration is 20 / Attack Speed ticks.
     */
    public static int getImpactDelayTicks(
            ServerPlayer player
    ) {
        double attackSpeed =
                player.getAttributeValue(Attributes.ATTACK_SPEED);

        double attackDurationTicks =
                attackSpeed > EPSILON
                        ? 20.0D / attackSpeed
                        : 20.0D;

        return Math.max(
                1,
                Math.round(
                        (float) (
                                attackDurationTicks
                                        * MortalStrikeSpell
                                        .ATTACK_DELAY_FRACTION
                        )
                )
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        PendingAttack pending =
                PENDING_ATTACKS.get(player.getUUID());
        if (pending == null) {
            return;
        }

        if (!player.isAlive()
                || player.isDeadOrDying()
                || player.isRemoved()
                || !player.level()
                .dimension()
                .equals(pending.startDimension)) {
            PENDING_ATTACKS.remove(player.getUUID());
            return;
        }

        long elapsed =
                player.serverLevel().getGameTime()
                        - pending.startTick;

        if (elapsed < pending.delayTicks) {
            return;
        }

        PENDING_ATTACKS.remove(player.getUUID());
        performAttack(player);
    }

    private static void performAttack(
            ServerPlayer player
    ) {
        ServerLevel level = player.serverLevel();

        Vec3 origin = targetingOrigin(player);
        Vec3 forward =
                player.getViewVector(1.0F).normalize();

        List<Entity> targets =
                findTargets(
                        level,
                        player,
                        origin,
                        forward
                );

        if (targets.isEmpty()) {
            return;
        }

        AttributeInstance attackDamage =
                player.getAttribute(Attributes.ATTACK_DAMAGE);

        AttributeModifier damageModifier = null;

        if (attackDamage != null) {
            attackDamage.removeModifier(DAMAGE_MODIFIER_ID);

            damageModifier =
                    new AttributeModifier(
                            DAMAGE_MODIFIER_ID,
                            MartialSpells.MOD_ID
                                    + ".mortal_strike_damage",
                            MortalStrikeSpell.DAMAGE_BONUS,
                            AttributeModifier.Operation
                                    .MULTIPLY_TOTAL
                    );

            attackDamage.addTransientModifier(
                    damageModifier
            );
        }

        LivingEntityAttackStrengthAccessor attackStrength =
                (LivingEntityAttackStrengthAccessor) player;

        int originalAttackStrengthTicker =
                attackStrength
                        .martialSpells$getAttackStrengthTicker();

        int impactSoundsRemaining =
                MortalStrikeSpell.IMPACT_SOUND_CAP;

        try {
            for (Entity target : targets) {
                if (!target.isAlive()
                        || target.isRemoved()
                        || !target.isAttackable()) {
                    continue;
                }

                EntityInvulnerabilityAccessor invulnerability =
                        (EntityInvulnerabilityAccessor) target;

                int originalInvulnerableTime =
                        invulnerability
                                .martialSpells$getInvulnerableTime();

                try {
                    // Spell Engine forces this skill's vanilla weapon swing
                    // fully charged and bypasses target iframes.
                    attackStrength
                            .martialSpells$setAttackStrengthTicker(
                                    100
                            );

                    invulnerability
                            .martialSpells$setInvulnerableTime(0);

                    player.attack(target);

                    if (impactSoundsRemaining > 0) {
                        level.playSound(
                                null,
                                target.getX(),
                                target.getY(),
                                target.getZ(),
                                MartialSoundRegistry
                                        .MORTAL_STRIKE_IMPACT
                                        .get(),
                                SoundSource.PLAYERS,
                                1.0F,
                                1.0F
                        );
                        impactSoundsRemaining--;
                    }

                    if (target instanceof LivingEntity living) {
                        applyBleed(player, living);
                    }
                } finally {
                    invulnerability
                            .martialSpells$setInvulnerableTime(
                                    originalInvulnerableTime
                            );
                }
            }
        } finally {
            attackStrength
                    .martialSpells$setAttackStrengthTicker(
                            originalAttackStrengthTicker
                    );

            if (attackDamage != null
                    && damageModifier != null) {
                attackDamage.removeModifier(
                        DAMAGE_MODIFIER_ID
                );
            }
        }
    }

    /**
     * Spell Engine applies Mortal Strike impacts while its +50% temporary
     * melee modifier is still present. PHYSICAL_MELEE therefore observes the
     * boosted Attack Damage for amplifier_power_multiplier.
     */
    private static void applyBleed(
            ServerPlayer player,
            LivingEntity target
    ) {
        double impactPhysicalMeleePower =
                player.getAttributeValue(
                        Attributes.ATTACK_DAMAGE
                );

        int amplifier =
                MortalStrikeSpell.BLEED_BASE_AMPLIFIER
                        + (int) (
                                MortalStrikeSpell
                                        .BLEED_POWER_MULTIPLIER
                                        * impactPhysicalMeleePower
                        );

        target.addEffect(
                new MobEffectInstance(
                        MartialEffectRegistry.BLEED.get(),
                        MortalStrikeSpell.BLEED_DURATION_TICKS,
                        amplifier,
                        false,
                        false,
                        true
                ),
                player
        );

        // Source STATUS_EFFECT impact visuals: 40 blood particles,
        // entity-centered sphere, speed 0.2-0.4.
        ShatterBloodVfx.spawn(
                player.serverLevel(),
                target,
                MortalStrikeSpell.BLEED_IMPACT_PARTICLES,
                0.20D,
                0.40D
        );
    }

    private static List<Entity> findTargets(
            ServerLevel level,
            ServerPlayer player,
            Vec3 origin,
            Vec3 forward
    ) {
        AABB search =
                new AABB(origin, origin)
                        .expandTowards(
                                forward.scale(
                                        MortalStrikeSpell.RANGE
                                )
                        )
                        .inflate(
                                MortalStrikeSpell.RANGE,
                                MortalStrikeSpell.RANGE,
                                MortalStrikeSpell.RANGE
                        );

        List<Entity> result = new ArrayList<>();

        for (Entity target :
                level.getEntities(
                        player,
                        search,
                        Entity::isAttackable
                )) {
            if (!isEligibleTarget(player, target)
                    || !intersectsSourceHitbox(
                            target,
                            origin,
                            forward
                    )
                    || !withinRangeAndArc(
                            target,
                            origin,
                            forward
                    )
                    || !hasLineOfSight(
                            level,
                            player,
                            target,
                            origin
                    )) {
                continue;
            }

            result.add(target);
        }

        return result;
    }

    private static boolean isEligibleTarget(
            ServerPlayer player,
            Entity target
    ) {
        return target != player
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && target.isAttackable()
                && !player.isAlliedTo(target);
    }

    private static boolean intersectsSourceHitbox(
            Entity target,
            Vec3 origin,
            Vec3 forward
    ) {
        Vec3 right =
                new Vec3(
                        -forward.z,
                        0.0D,
                        forward.x
                );

        if (right.lengthSqr() <= EPSILON) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        Vec3 up = right.cross(forward);
        if (up.lengthSqr() <= EPSILON) {
            up = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            up = up.normalize();
        }

        Vec3 obbCenter =
                origin.add(
                        forward.scale(HALF_LENGTH)
                );

        AABB box = target.getBoundingBox();
        Vec3 boxCenter = box.getCenter();
        Vec3 difference =
                boxCenter.subtract(obbCenter);

        double extentX =
                (box.maxX - box.minX) * 0.5D;
        double extentY =
                (box.maxY - box.minY) * 0.5D;
        double extentZ =
                (box.maxZ - box.minZ) * 0.5D;

        return overlapsAxis(
                difference,
                right,
                HALF_WIDTH,
                extentX,
                extentY,
                extentZ
        ) && overlapsAxis(
                difference,
                up,
                HALF_HEIGHT,
                extentX,
                extentY,
                extentZ
        ) && overlapsAxis(
                difference,
                forward,
                HALF_LENGTH,
                extentX,
                extentY,
                extentZ
        );
    }

    private static boolean overlapsAxis(
            Vec3 centerDifference,
            Vec3 axis,
            double sourceHalfExtent,
            double targetExtentX,
            double targetExtentY,
            double targetExtentZ
    ) {
        double targetRadius =
                targetExtentX * Math.abs(axis.x)
                        + targetExtentY
                        * Math.abs(axis.y)
                        + targetExtentZ
                        * Math.abs(axis.z);

        double separation =
                Math.abs(
                        centerDifference.dot(axis)
                );

        return separation
                <= sourceHalfExtent + targetRadius;
    }

    private static boolean withinRangeAndArc(
            Entity target,
            Vec3 origin,
            Vec3 forward
    ) {
        AABB box = target.getBoundingBox();

        Vec3 nearest =
                nearestPoint(origin, box);

        Vec3 closestVector =
                nearest.subtract(origin);

        if (closestVector.lengthSqr()
                > MortalStrikeSpell.RANGE
                * MortalStrikeSpell.RANGE) {
            return false;
        }

        Vec3 centerVector =
                box.getCenter().subtract(origin);

        return withinArc(
                closestVector,
                forward
        ) || withinArc(
                centerVector,
                forward
        );
    }

    private static boolean withinArc(
            Vec3 vector,
            Vec3 forward
    ) {
        if (vector.lengthSqr() <= EPSILON) {
            return true;
        }

        return vector.normalize().dot(forward)
                >= ARC_COSINE;
    }

    private static boolean hasLineOfSight(
            ServerLevel level,
            ServerPlayer player,
            Entity target,
            Vec3 origin
    ) {
        AABB box = target.getBoundingBox();

        Vec3 nearest =
                nearestPoint(origin, box);

        if (unobstructed(
                level,
                player,
                origin,
                nearest
        )) {
            return true;
        }

        return unobstructed(
                level,
                player,
                origin,
                box.getCenter()
        );
    }

    private static boolean unobstructed(
            ServerLevel level,
            ServerPlayer player,
            Vec3 start,
            Vec3 end
    ) {
        return level.clip(
                new ClipContext(
                        start,
                        end,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        ).getType() != HitResult.Type.BLOCK;
    }

    private static Vec3 nearestPoint(
            Vec3 point,
            AABB box
    ) {
        return new Vec3(
                clamp(
                        point.x,
                        box.minX,
                        box.maxX
                ),
                clamp(
                        point.y,
                        box.minY,
                        box.maxY
                ),
                clamp(
                        point.z,
                        box.minZ,
                        box.maxZ
                )
        );
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(
                min,
                Math.min(max, value)
        );
    }

    private static Vec3 targetingOrigin(
            ServerPlayer player
    ) {
        return player.getEyePosition().add(
                0.0D,
                -player.getBbHeight() * 0.15D,
                0.0D
        );
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        PENDING_ATTACKS.remove(
                event.getEntity().getUUID()
        );
    }

    @SubscribeEvent
    public static void onChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        PENDING_ATTACKS.remove(
                event.getEntity().getUUID()
        );
    }

    @SubscribeEvent
    public static void onClone(
            PlayerEvent.Clone event
    ) {
        PENDING_ATTACKS.remove(
                event.getEntity().getUUID()
        );
    }

    private static final class PendingAttack {
        private final ResourceKey<Level> startDimension;
        private final long startTick;
        private final int delayTicks;

        private PendingAttack(
                ResourceKey<Level> startDimension,
                long startTick,
                int delayTicks
        ) {
            this.startDimension = startDimension;
            this.startTick = startTick;
            this.delayTicks = delayTicks;
        }
    }
}
