package com.w0of26.martialspells.combat;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Fireball;
import com.w0of26.martialspells.spells.DeflectMissilesSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DeflectMissilesEvents {

    /*
     * Temporary Phase-I behavior:
     *
     * While Deflect Missiles is active, every vanilla
     * AbstractArrow impact against the player is deflected.
     *
     * Level-based deflection chance comes later after the
     * underlying projectile behavior is proven reliable.
     */
    private static final double DEFLECTED_SPEED_MULTIPLIER =
            0.80D;

    private static final double COLLISION_PUSH_DISTANCE =
            0.75D;

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

        /*
         * Phase I-A deliberately handles only vanilla-style
         * arrows first.
         *
         * AbstractArrow includes normal arrows, spectral
         * arrows, and thrown tridents.
         */
        if (!isDeflectableVanillaMissile(projectile)) {
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
         * Never deflect the player's own projectile.
         */
        if (projectile.getOwner() == player) {
            return;
        }

        if (!event.isCancelable()) {
            return;
        }

        scatterProjectile(
                projectile
        );

        /*
         * Cancel the original hit after moving the projectile.
         *
         * This prevents the arrow from dealing its normal
         * collision damage to the player.
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
         * - Arrow
         * - Spectral Arrow
         * - Trident
         *
         * Fireball covers the vanilla small and large
         * fireballs used by Blazes and Ghasts.
         */
        return projectile instanceof AbstractArrow
                || projectile instanceof Fireball;
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
         *
         * This makes the first implementation scatter the
         * projectile sideways instead of sending it straight
         * back to the shooter.
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
         * Fireballs continuously accelerate using xPower/yPower/zPower.
         *
         * Redirect that acceleration along with the velocity or the
         * projectile can curve back toward its original trajectory
         * after being deflected.
         */
        if (projectile
                instanceof AbstractHurtingProjectile hurtingProjectile) {

            double originalPower =
                    Math.sqrt(
                            hurtingProjectile.xPower
                                    * hurtingProjectile.xPower
                                    + hurtingProjectile.yPower
                                    * hurtingProjectile.yPower
                                    + hurtingProjectile.zPower
                                    * hurtingProjectile.zPower
                    );

            /*
             * Vanilla fireballs normally have a small non-zero
             * acceleration. Keep a sensible fallback in case a
             * projectile reaches us with effectively zero power.
             */
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
         * Physically move it outside the player's immediate
         * collision area.
         *
         * Canceling the impact without displacement can allow
         * the same projectile to collide again immediately.
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