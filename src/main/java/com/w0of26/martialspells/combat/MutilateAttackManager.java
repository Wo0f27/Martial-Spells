package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.mixin.EntityInvulnerabilityAccessor;
import com.w0of26.martialspells.mixin.LivingEntityAttackStrengthAccessor;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import com.w0of26.martialspells.spells.MutilateSpell;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
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
 * Executes the delayed contact frame for frozen Rogues Mutilate.
 *
 * <p>This deliberately lands through {@link ServerPlayer#attack(Entity)} rather
 * than raw damage. Frozen Spell Engine does the same, temporarily making the
 * attack fully charged and clearing each target's invulnerability timer while
 * the Mutilate contact is processed.</p>
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MutilateAttackManager {
    private static final UUID DUAL_WIELD_MODIFIER_ID =
            UUID.fromString("439a9308-cf77-4fe8-8e16-8aa1ef771dd1");

    private static final double HALF_WIDTH =
            MutilateSpell.RANGE * MutilateSpell.HITBOX_WIDTH_FACTOR * 0.5D;
    private static final double HALF_HEIGHT =
            MutilateSpell.RANGE * MutilateSpell.HITBOX_HEIGHT_FACTOR * 0.5D;
    private static final double HALF_LENGTH = MutilateSpell.RANGE * 0.5D;
    private static final double ARC_COSINE =
            Math.cos(Math.toRadians(MutilateSpell.ARC_DEGREES * 0.5D));
    private static final double EPSILON = 1.0E-7D;

    private static final Map<UUID, PendingAttack> PENDING_ATTACKS = new HashMap<>();

    private MutilateAttackManager() {
    }

    public static void begin(ServerPlayer player) {
        int delayTicks = getImpactDelayTicks(player);
        PENDING_ATTACKS.put(
                player.getUUID(),
                new PendingAttack(
                        player.level().dimension(),
                        player.serverLevel().getGameTime(),
                        delayTicks
                )
        );

        /*
         * Frozen Spell Engine broadcasts the melee animation and swing sound
         * immediately when the melee attack starts, before the delayed contact
         * frame. Iron's synchronizes the animation via MutilateSpell's finish
         * AnimationHolder; this is the dependency-free translation of the
         * generic Spell Engine weapon_sword_swing audio cue.
         *
         * Play it even if the attack ultimately whiffs so the cast always has
         * immediate audiovisual feedback.
         */
        player.serverLevel().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.8F,
                1.05F
        );
    }

    /**
     * Frozen JSON uses delay=0.5, where delay is a fraction of the resolved
     * melee attack duration rather than seconds. Vanilla's attack cycle is
     * 20 / Attack Speed ticks, so Mutilate contacts halfway through that cycle.
     */
    public static int getImpactDelayTicks(ServerPlayer player) {
        double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double attackDurationTicks = attackSpeed > EPSILON
                ? 20.0D / attackSpeed
                : 20.0D;
        return Math.max(
                1,
                Math.round((float) (attackDurationTicks * MutilateSpell.ATTACK_DELAY_FRACTION))
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        PendingAttack pending = PENDING_ATTACKS.get(player.getUUID());
        if (pending == null) {
            return;
        }

        if (!player.isAlive()
                || player.isDeadOrDying()
                || player.isRemoved()
                || !player.level().dimension().equals(pending.startDimension)) {
            PENDING_ATTACKS.remove(player.getUUID());
            return;
        }

        long elapsed = player.serverLevel().getGameTime() - pending.startTick;
        if (elapsed < pending.delayTicks) {
            return;
        }

        PENDING_ATTACKS.remove(player.getUUID());
        performAttack(player);
    }

    private static void performAttack(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 origin = targetingOrigin(player);
        Vec3 forward = player.getViewVector(1.0F).normalize();

        List<Entity> targets = findTargets(level, player, origin, forward);
        if (targets.isEmpty()) {
            return;
        }

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeModifier dualWieldModifier = null;

        if (attackDamage != null) {
            attackDamage.removeModifier(DUAL_WIELD_MODIFIER_ID);
            double bonusFraction = dualWieldBonusFraction(player, attackDamage);
            if (Math.abs(bonusFraction) > EPSILON) {
                dualWieldModifier = new AttributeModifier(
                        DUAL_WIELD_MODIFIER_ID,
                        MartialSpells.MOD_ID + ".mutilate_dual_wield",
                        bonusFraction,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                );
                attackDamage.addTransientModifier(dualWieldModifier);
            }
        }

        LivingEntityAttackStrengthAccessor attackStrength =
                (LivingEntityAttackStrengthAccessor) player;
        int originalAttackStrengthTicker =
                attackStrength.martialSpells$getAttackStrengthTicker();

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
                        invulnerability.martialSpells$getInvulnerableTime();

                try {
                    // Frozen Spell Engine sets the vanilla melee swing to fully
                    // charged for every selected Mutilate target.
                    attackStrength.martialSpells$setAttackStrengthTicker(100);
                    invulnerability.martialSpells$setInvulnerableTime(0);
                    player.attack(target);

                    level.playSound(
                            null,
                            target.getX(),
                            target.getY(),
                            target.getZ(),
                            MartialSoundRegistry.MUTILATE_IMPACT.get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F
                    );
                } finally {
                    invulnerability.martialSpells$setInvulnerableTime(
                            originalInvulnerableTime
                    );
                }
            }
        } finally {
            attackStrength.martialSpells$setAttackStrengthTicker(
                    originalAttackStrengthTicker
            );
            if (attackDamage != null && dualWieldModifier != null) {
                attackDamage.removeModifier(DUAL_WIELD_MODIFIER_ID);
            }
        }
    }

    /**
     * Mirrors frozen Spell Power's physical_melee_dual value:
     *
     * dual = current main-hand Attack Damage +
     *        (base Attack Damage + flat offhand weapon bonus) *
     *        attack-damage multipliers.
     *
     * The result is expressed as a temporary MULTIPLY_TOTAL bonus before
     * vanilla player.attack(...) is called, exactly as Spell Engine 1.20.1 does.
     */
    private static double dualWieldBonusFraction(
            ServerPlayer player,
            AttributeInstance attackDamage
    ) {
        double singleHanded = attackDamage.getValue();
        if (Math.abs(singleHanded) <= EPSILON) {
            return 0.0D;
        }

        double offhandDamage = calculateOffhandAttackDamage(player, attackDamage);
        double dualWielded = singleHanded + offhandDamage;
        return dualWielded / singleHanded - 1.0D;
    }

    private static double calculateOffhandAttackDamage(
            ServerPlayer player,
            AttributeInstance attackDamage
    ) {
        double weaponDamage = attackDamage.getBaseValue()
                + flatAttackDamageFrom(player.getOffhandItem());

        if (Math.abs(weaponDamage) <= EPSILON) {
            return 0.0D;
        }

        double multiplyBase = 1.0D;
        double multiplyTotal = 1.0D;

        for (AttributeModifier modifier : attackDamage.getModifiers()) {
            switch (modifier.getOperation()) {
                case ADDITION -> {
                    // Frozen helper intentionally ignores additive entity
                    // modifiers here; only the offhand stack's flat bonus is
                    // added to the attribute base above.
                }
                case MULTIPLY_BASE -> multiplyBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> multiplyTotal += modifier.getAmount();
            }
        }

        return weaponDamage * multiplyBase * multiplyTotal;
    }

    /**
     * Frozen Spell Engine reads the offhand stack's ADDITION modifiers across
     * every equipment slot. This matters because ordinary weapons expose their
     * Attack Damage modifier for MAINHAND even while the stack is physically in
     * the player's offhand.
     */
    private static double flatAttackDamageFrom(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }

        double total = 0.0D;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (AttributeModifier modifier :
                    stack.getAttributeModifiers(slot).get(Attributes.ATTACK_DAMAGE)) {
                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    total += modifier.getAmount();
                }
            }
        }
        return total;
    }

    private static List<Entity> findTargets(
            ServerLevel level,
            ServerPlayer player,
            Vec3 origin,
            Vec3 forward
    ) {
        AABB search = new AABB(origin, origin)
                .expandTowards(forward.scale(MutilateSpell.RANGE))
                .inflate(MutilateSpell.RANGE, 1.5D, MutilateSpell.RANGE);

        List<Entity> result = new ArrayList<>();
        for (Entity target : level.getEntities(player, search, Entity::isAttackable)) {
            if (!isEligibleTarget(player, target)
                    || !intersectsSourceHitbox(target, origin, forward)
                    || !withinRangeAndArc(target, origin, forward)
                    || !hasLineOfSight(level, player, target, origin)) {
                continue;
            }
            result.add(target);
        }
        return result;
    }

    private static boolean isEligibleTarget(ServerPlayer player, Entity target) {
        return target != player
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && target.isAttackable()
                && !player.isAlliedTo(target);
    }

    /**
     * Source hitbox dimensions at three-block melee range are 1.5 wide,
     * 0.6 high and 3.0 long. The box is oriented along the caster's look
     * direction and starts at the source shoulder-height origin.
     *
     * This projection test is the Forge-side OBB/AABB overlap gate; the
     * source's separate radius, arc and LOS tests are applied afterwards.
     */
    private static boolean intersectsSourceHitbox(
            Entity target,
            Vec3 origin,
            Vec3 forward
    ) {
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
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

        Vec3 obbCenter = origin.add(forward.scale(HALF_LENGTH));
        AABB box = target.getBoundingBox();
        Vec3 boxCenter = box.getCenter();
        Vec3 difference = boxCenter.subtract(obbCenter);

        double extentX = (box.maxX - box.minX) * 0.5D;
        double extentY = (box.maxY - box.minY) * 0.5D;
        double extentZ = (box.maxZ - box.minZ) * 0.5D;

        return overlapsAxis(difference, right, HALF_WIDTH, extentX, extentY, extentZ)
                && overlapsAxis(difference, up, HALF_HEIGHT, extentX, extentY, extentZ)
                && overlapsAxis(difference, forward, HALF_LENGTH, extentX, extentY, extentZ);
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
                        + targetExtentY * Math.abs(axis.y)
                        + targetExtentZ * Math.abs(axis.z);
        double separation = Math.abs(centerDifference.dot(axis));
        return separation <= sourceHalfExtent + targetRadius;
    }

    private static boolean withinRangeAndArc(
            Entity target,
            Vec3 origin,
            Vec3 forward
    ) {
        AABB box = target.getBoundingBox();
        Vec3 nearest = nearestPoint(origin, box);
        Vec3 closestVector = nearest.subtract(origin);
        if (closestVector.lengthSqr()
                > MutilateSpell.RANGE * MutilateSpell.RANGE) {
            return false;
        }

        Vec3 centerVector = box.getCenter().subtract(origin);
        return withinArc(closestVector, forward)
                || withinArc(centerVector, forward);
    }

    private static boolean withinArc(Vec3 vector, Vec3 forward) {
        if (vector.lengthSqr() <= EPSILON) {
            return true;
        }
        return vector.normalize().dot(forward) >= ARC_COSINE;
    }

    private static boolean hasLineOfSight(
            ServerLevel level,
            ServerPlayer player,
            Entity target,
            Vec3 origin
    ) {
        AABB box = target.getBoundingBox();
        Vec3 nearest = nearestPoint(origin, box);
        if (unobstructed(level, player, origin, nearest)) {
            return true;
        }
        return unobstructed(level, player, origin, box.getCenter());
    }

    private static boolean unobstructed(
            ServerLevel level,
            ServerPlayer player,
            Vec3 start,
            Vec3 end
    ) {
        return level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        )).getType() != HitResult.Type.BLOCK;
    }

    private static Vec3 nearestPoint(Vec3 point, AABB box) {
        return new Vec3(
                clamp(point.x, box.minX, box.maxX),
                clamp(point.y, box.minY, box.maxY),
                clamp(point.z, box.minZ, box.maxZ)
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Vec3 targetingOrigin(ServerPlayer player) {
        return player.getEyePosition().add(
                0.0D,
                -player.getBbHeight() * 0.15D,
                0.0D
        );
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_ATTACKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PENDING_ATTACKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        PENDING_ATTACKS.remove(event.getEntity().getUUID());
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
