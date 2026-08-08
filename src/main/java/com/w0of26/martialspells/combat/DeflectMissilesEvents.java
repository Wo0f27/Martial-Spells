package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
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
     * Current Phase-I behavior:
     *
     * While Deflect Missiles is actively being channeled,
     * supported vanilla missiles are intercepted.
     *
     * Normal and spectral arrows are temporarily returned
     * to their attacker 100% of the time so that return
     * trajectory and ownership can be tested reliably.
     *
     * Tridents and fireballs continue to scatter sideways.
     *
     * Final level-based return chance comes later.
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

        redirectProjectile(
                projectile,
                player
        );

        /*
         * Cancel the original impact only after the
         * projectile has been moved away from the
         * defender.
         *
         * This prevents normal collision damage and
         * reduces the risk of an immediate repeat
         * collision.
         */
        event.setCanceled(true);

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

    private static boolean isDeflectableVanillaMissile(
            Projectile projectile
    ) {
        /*
         * AbstractArrow covers:
         * - normal arrows
         * - spectral arrows
         * - thrown tridents
         *
         * Fireball covers the vanilla fireballs already
         * proven by runtime testing.
         */
        return projectile instanceof AbstractArrow
                || projectile instanceof Fireball;
    }

    /*
     * Only actual arrows are returned during this
     * checkpoint.
     *
     * Thrown tridents remain on the existing scatter
     * behavior until arrow returning is proven reliable.
     */
    private static boolean isReturnableArrow(
            Projectile projectile
    ) {
        /*
         * Eligible for return-to-attacker:
         * - normal arrows
         * - spectral arrows
         * - vanilla small fireballs
         * - vanilla large fireballs
         *
         * Tridents deliberately do NOT qualify even though
         * they are AbstractArrow instances.
         */
        return projectile instanceof Arrow
                || projectile instanceof SpectralArrow
                || projectile instanceof Fireball;
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
                                magicData.getCastingSpellLevel(),
                                RETURN_CHANCES.length
                        )
                );

        float returnChance =
                RETURN_CHANCES[
                        spellLevel - 1
                        ];

        /*
         * Only normal and spectral arrows participate in
         * return-to-attacker behavior for this checkpoint.
         *
         * Deflection itself remains guaranteed.
         */
        if (isReturnableArrow(projectile)
                && projectile.getOwner()
                instanceof LivingEntity attacker
                && attacker.isAlive()
                && attacker != defender
                && defender.getRandom().nextFloat()
                < returnChance) {

            returnProjectileToAttacker(
                    projectile,
                    defender,
                    attacker
            );

            return;
        }

        /*
         * Failed return roll does NOT mean failed defense.
         *
         * The projectile is still safely scattered away.
         */
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
         * Aim near the center of the attacker's hitbox.
         */
        Vec3 target =
                attacker
                        .getBoundingBox()
                        .getCenter();

        Vec3 direction =
                target.subtract(
                        projectile.position()
                );

        if (direction.lengthSqr() < 1.0E-6D) {
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
         * Transfer ownership to the Monk.
         *
         * This allows the returned projectile to collide with
         * its original shooter and attributes the returned
         * projectile to the defender.
         */
        projectile.setOwner(
                defender
        );

        projectile.setDeltaMovement(
                returnedVelocity
        );

        /*
         * Fireballs have persistent acceleration in addition
         * to their current velocity.
         *
         * If we only reverse their velocity, their original
         * xPower/yPower/zPower can cause them to curve back
         * toward their old trajectory.
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
         * Move the projectile outside the defender's immediate
         * collision area before the original impact is
         * canceled.
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
         * Create a vector perpendicular to the projectile's
         * incoming horizontal direction.
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
        if (sideways.lengthSqr() < 1.0E-6D) {
            sideways =
                    new Vec3(
                            1.0D,
                            0.0D,
                            0.0D
                    );
        }

        /*
         * Alternate left/right based on entity ID so every
         * projectile does not scatter in exactly the same
         * direction.
         */
        if ((projectile.getId() & 1) == 0) {
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
         * Fireballs continuously accelerate using
         * xPower/yPower/zPower.
         *
         * Their acceleration therefore has to be redirected
         * along with their current velocity, otherwise they
         * can curve back toward their original trajectory.
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