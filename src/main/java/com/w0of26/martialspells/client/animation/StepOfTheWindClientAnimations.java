package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.StepOfTheWindClientSurfaceState;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class StepOfTheWindClientAnimations {

    private static final ResourceLocation
            WALL_RUN_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "step_of_the_wind_wall_run"
            );

    /*
     * Minimum movement along the wall before the legs
     * begin cycling.
     *
     * This lets an actual wall cling remain mostly still
     * instead of constantly running in place.
     */
    private static final double
            MIN_WALL_MOVEMENT_SQR =
            0.0025D;

    private static final Set<UUID>
            PLAYING =
            new HashSet<>();

    private StepOfTheWindClientAnimations() {
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            PLAYING.clear();
            return;
        }

        for (Player rawPlayer
                : minecraft.level.players()) {

            if (!(rawPlayer
                    instanceof AbstractClientPlayer player)) {
                continue;
            }

            updatePlayerAnimation(
                    player
            );
        }
    }

    private static void updatePlayerAnimation(
            AbstractClientPlayer player
    ) {
        UUID playerId =
                player.getUUID();

        Direction wallDirection =
                StepOfTheWindClientSurfaceState
                        .getWallDirection(
                                playerId
                        );

        /*
         * No wall = no wall-running animation.
         */
        if (wallDirection == null) {
            stop(
                    player
            );
            return;
        }

        Vec3 velocity =
                player.getDeltaMovement();

        /*
         * Remove velocity directly toward/away from
         * the wall.
         *
         * Importantly, Y remains intact.
         *
         * That means climbing vertically now counts as
         * locomotion just as much as running sideways.
         */
        Vec3 wallNormal =
                new Vec3(
                        wallDirection.getStepX(),
                        0.0D,
                        wallDirection.getStepZ()
                );

        double normalVelocity =
                velocity.dot(
                        wallNormal
                );

        Vec3 alongWallVelocity =
                velocity.subtract(
                        wallNormal.scale(
                                normalVelocity
                        )
                );

        if (alongWallVelocity.lengthSqr()
                < MIN_WALL_MOVEMENT_SQR) {

            stop(
                    player
            );
            return;
        }

        play(
                player
        );
    }

    @SuppressWarnings("unchecked")
    private static void play(
            AbstractClientPlayer player
    ) {
        UUID playerId =
                player.getUUID();

        /*
         * Do not restart the loop every client tick.
         */
        if (PLAYING.contains(
                playerId
        )) {
            return;
        }

        ModifierLayer<IAnimation> layer =
                (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess
                                .getPlayerAssociatedData(
                                        player
                                )
                                .get(
                                        StepOfTheWindAnimationLayer
                                                .LAYER_ID
                                );

        if (layer == null) {
            return;
        }

        var animation =
                PlayerAnimationRegistry
                        .getAnimation(
                                WALL_RUN_ANIMATION
                        );

        if (animation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Step of the Wind "
                            + "wall-run animation {}",
                    WALL_RUN_ANIMATION
            );

            return;
        }

        layer.setAnimation(
                new KeyframeAnimationPlayer(
                        animation
                )
        );

        PLAYING.add(
                playerId
        );
    }

    @SuppressWarnings("unchecked")
    private static void stop(
            AbstractClientPlayer player
    ) {
        UUID playerId =
                player.getUUID();

        if (!PLAYING.remove(
                playerId
        )) {
            return;
        }

        ModifierLayer<IAnimation> layer =
                (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess
                                .getPlayerAssociatedData(
                                        player
                                )
                                .get(
                                        StepOfTheWindAnimationLayer
                                                .LAYER_ID
                                );

        if (layer != null) {
            layer.setAnimation(
                    null
            );
        }
    }
}