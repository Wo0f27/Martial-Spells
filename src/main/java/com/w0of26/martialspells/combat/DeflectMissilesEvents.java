package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.network.MartialNetwork;
import com.w0of26.martialspells.network.SyncDeflectMissilesAnimationPacket;
import com.w0of26.martialspells.registry.MartialTags;
import com.w0of26.martialspells.spells.DeflectMissilesSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DeflectMissilesEvents {

    /*
     * Deflect Missiles vanilla projectile handling:
     *
     * While the technique is actively channeled, supported
     * vanilla missiles are always prevented from hitting
     * the defending player.
     *
     * Arrows and fireballs have a level-scaled chance to be
     * redirected toward their original attacker.
     *
     * Tridents are always scattered and never returned.
     *
     * Empty-handed and gauntlet users play one randomized
     * defensive swipe whenever a projectile is successfully
     * intercepted.
     *
     * Quarterstaff users retain their continuous staff-spin
     * visual instead.
     */

    private static final double DEFLECTED_SPEED_MULTIPLIER =
            0.80D;

    private static final double COLLISION_PUSH_DISTANCE =
            0.75D;

    private static final float[] RETURN_CHANCES = {
            0.20F,
            0.30F,
            0.40F,
            0.50F,
            0.75F
    };

    private DeflectMissilesEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(
            ProjectileImpactEvent event
    ) {
        Projectile projectile =
                event.getProjectile();

        if (projectile.level().isClientSide) {
            return;
        }

        if (!isDeflectableVanillaMissile(
                projectile
        )) {
            return;
        }

        if (!(event.getRayTraceResult()
                instanceof EntityHitResult entityHit)) {
            return;
        }

        if (!(entityHit.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        if (!isActivelyChannelingDeflectMissiles(
                player
        )) {
            return;
        }

        /*
         * Never deflect a projectile already owned
         * by the defending player.
         */
        if (projectile.getOwner() == player) {
            return;
        }

        if (!event.isCancelable()) {
            return;
        }

        /*
         * Redirect the projectile before canceling the
         * original collision.
         */
        redirectProjectile(
                projectile,
                player
        );

        /*
         * Cancel the original impact after displacement.
         *
         * The projectile therefore cannot deal its normal
         * collision damage to the defending player.
         */
        event.setCanceled(true);

        /*
         * Empty-hand and gauntlet Deflect Missiles uses an
         * impact-triggered hand animation.
         *
         * Quarterstaff users do not receive this packet
         * because their defensive visual is the continuous
         * staff spin.
         */
        triggerHandDeflectAnimation(
                player
        );

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                0.8F,
                1.25F
        );
    }

    private static boolean
    isActivelyChannelingDeflectMissiles(
            ServerPlayer player
    ) {
        MagicData magicData =
                MagicData.getPlayerMagicData(
                        player
                );

        if (!magicData.isCasting()) {
            return false;
        }

        return DeflectMissilesSpell
                .SPELL_ID
                .toString()
                .equals(
                        magicData
                                .getCastingSpellId()
                );
    }

    private static boolean
    isDeflectableVanillaMissile(
            Projectile projectile
    ) {
        /*
         * AbstractArrow covers:
         *
         * - normal arrows
         * - spectral arrows
         * - thrown tridents
         *
         * Fireball covers the supported vanilla small
         * and large fireball family.
         */
        return projectile instanceof AbstractArrow
                || projectile instanceof Fireball;
    }

    private static boolean
    isReturnableMissile(
            Projectile projectile
    ) {
        /*
         * Missiles eligible for return-to-attacker:
         *
         * - normal arrows
         * - spectral arrows
         * - vanilla fireballs
         *
         * Tridents deliberately do not qualify.
         */
        return projectile instanceof Arrow
                || projectile instanceof SpectralArrow
                || projectile instanceof Fireball;
    }

    /*
     * Trigger one hand-deflection animation after a
     * projectile has actually been intercepted.
     *
     * The random base/mirrored choice is made on the
     * server. Every tracking client therefore receives
     * the same result and sees the same hand move.
     */
    private static void triggerHandDeflectAnimation(
            ServerPlayer player
    ) {
        ItemStack mainHand =
                player.getMainHandItem();

        /*
         * Only these two equipment states use the swipe:
         *
         * - empty hand
         * - gauntlets
         *
         * Quarterstaffs use the continuous spinning-item
         * renderer instead.
         */
        boolean usesHandAnimation =
                mainHand.isEmpty()
                        || mainHand.is(
                        MartialTags.Items.GAUNTLETS
                );

        if (!usesHandAnimation) {
            return;
        }

        boolean mirrored =
                player.getRandom()
                        .nextBoolean();

        MartialNetwork.sendToTrackingAndSelf(
                new SyncDeflectMissilesAnimationPacket(
                        player.getUUID(),
                        mirrored
                ),
                player
        );
    }

    private static void redirectProjectile(
            Projectile projectile,
            ServerPlayer defender
    ) {
        MagicData magicData =
                MagicData.getPlayerMagicData(
                        defender
                );

        int spellLevel =
                Math.max(
                        1,
                        Math.min(
                                magicData
                                        .getCastingSpellLevel(),
                                RETURN_CHANCES.length
                        )
                );

        float returnChance =
                RETURN_CHANCES[
                        spellLevel - 1
                        ];

        /*
         * Returnable missiles use the technique's
         * level-scaled return chance.
         *
         * A failed return roll does NOT mean a failed
         * defensive deflection. The projectile is simply
         * scattered instead.
         */
        if (isReturnableMissile(
                projectile
        )
                && projectile.getOwner()
                instanceof LivingEntity attacker
                && attacker.isAlive()
                && attacker != defender
                && defender.getRandom()
                .nextFloat()
                < returnChance) {

            returnProjectileToAttacker(
                    projectile,
                    defender,
                    attacker
            );

            return;
        }

        scatterProjectile(
                projectile
        );
    }

    private static void returnProjectileToAttacker(
            Projectile projectile,
            ServerPlayer defender,
            LivingEntity attacker
    ) {
        Vec3 incoming =
                projectile.getDeltaMovement();

        double originalSpeed =
                incoming.length();

        if (originalSpeed < 0.05D) {
            originalSpeed = 0.75D;
        }

        /*
         * Aim approximately at the middle of the
         * attacker's hitbox.
         */
        Vec3 target =
                attacker
                        .getBoundingBox()
                        .getCenter();

        Vec3 direction =
                target.subtract(
                        projectile.position()
                );

        if (direction.lengthSqr()
                < 1.0E-6D) {

            scatterProjectile(
                    projectile
            );

            return;
        }

        direction =
                direction.normalize();

        Vec3 returnedVelocity =
                direction.scale(
                        originalSpeed
                );

        /*
         * Transfer projectile ownership to the Monk so
         * the returned projectile can collide with its
         * original attacker.
         */
        projectile.setOwner(
                defender
        );

        projectile.setDeltaMovement(
                returnedVelocity
        );

        /*
         * Fireballs maintain persistent acceleration
         * through xPower/yPower/zPower.
         *
         * Redirect that acceleration along with their
         * velocity so they continue along the new path.
         */
        if (projectile
                instanceof AbstractHurtingProjectile
                hurtingProjectile) {

            double originalPower =
                    Math.sqrt(
                            hurtingProjectile.xPower
                                    * hurtingProjectile.xPower
                                    + hurtingProjectile.yPower
                                    * hurtingProjectile.yPower
                                    + hurtingProjectile.zPower
                                    * hurtingProjectile.zPower
                    );

            if (originalPower < 1.0E-6D) {
                originalPower = 0.1D;
            }

            Vec3 redirectedPower =
                    direction.scale(
                            originalPower
                    );

            hurtingProjectile.xPower =
                    redirectedPower.x;

            hurtingProjectile.yPower =
                    redirectedPower.y;

            hurtingProjectile.zPower =
                    redirectedPower.z;
        }

        /*
         * Move the projectile outside the defender's
         * immediate collision area before the original
         * impact is canceled.
         */
        Vec3 displacement =
                direction.scale(
                        COLLISION_PUSH_DISTANCE
                );

        projectile.setPos(
                projectile.getX()
                        + displacement.x,
                projectile.getY()
                        + displacement.y,
                projectile.getZ()
                        + displacement.z
        );
    }

    private static void scatterProjectile(
            Projectile projectile
    ) {
        Vec3 incoming =
                projectile.getDeltaMovement();

        double originalSpeed =
                incoming.length();

        if (originalSpeed < 0.05D) {
            originalSpeed = 0.75D;
        }

        /*
         * Create a vector perpendicular to the incoming
         * horizontal trajectory.
         */
        Vec3 sideways =
                new Vec3(
                        -incoming.z,
                        0.0D,
                        incoming.x
                );

        /*
         * A nearly vertical projectile has no useful
         * horizontal perpendicular vector.
         */
        if (sideways.lengthSqr()
                < 1.0E-6D) {

            sideways =
                    new Vec3(
                            1.0D,
                            0.0D,
                            0.0D
                    );
        }

        /*
         * Alternate left/right scatter direction using
         * the projectile entity ID.
         */
        if ((projectile.getId() & 1)
                == 0) {

            sideways =
                    sideways.scale(
                            -1.0D
                    );
        }

        Vec3 scattered =
                sideways
                        .normalize()
                        .add(
                                0.0D,
                                0.15D,
                                0.0D
                        )
                        .normalize()
                        .scale(
                                originalSpeed
                                        * DEFLECTED_SPEED_MULTIPLIER
                        );

        projectile.setDeltaMovement(
                scattered
        );

        /*
         * Redirect fireball acceleration along with
         * current movement.
         */
        if (projectile
                instanceof AbstractHurtingProjectile
                hurtingProjectile) {

            double originalPower =
                    Math.sqrt(
                            hurtingProjectile.xPower
                                    * hurtingProjectile.xPower
                                    + hurtingProjectile.yPower
                                    * hurtingProjectile.yPower
                                    + hurtingProjectile.zPower
                                    * hurtingProjectile.zPower
                    );

            if (originalPower < 1.0E-6D) {
                originalPower = 0.1D;
            }

            Vec3 redirectedPower =
                    scattered
                            .normalize()
                            .scale(
                                    originalPower
                            );

            hurtingProjectile.xPower =
                    redirectedPower.x;

            hurtingProjectile.yPower =
                    redirectedPower.y;

            hurtingProjectile.zPower =
                    redirectedPower.z;
        }

        /*
         * Physically move the projectile outside the
         * player's immediate collision area.
         */
        Vec3 displacement =
                scattered
                        .normalize()
                        .scale(
                                COLLISION_PUSH_DISTANCE
                        );

        projectile.setPos(
                projectile.getX()
                        + displacement.x,
                projectile.getY()
                        + displacement.y,
                projectile.getZ()
                        + displacement.z
        );
    }
}