package com.w0of26.martialspells.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StepOfTheWindPlayerRenderEvents {

    /*
     * Move the rendered player toward the wall so their
     * feet visually meet the surface.
     *
     * The real player hitbox and physics remain upright.
     */
    private static final double WALL_VISUAL_OFFSET =
            0.30D;

    /*
     * Tracks which RenderPlayerEvent.Pre calls pushed the
     * PoseStack so the corresponding Post event can safely
     * restore it.
     */
    private static final Set<UUID>
            TRANSFORMED_PLAYERS =
            new HashSet<>();

    private StepOfTheWindPlayerRenderEvents() {
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST,
            receiveCanceled = true
    )
    public static void onRenderPlayerPre(
            RenderPlayerEvent.Pre event
    ) {
        /*
         * Do not modify the PoseStack if another renderer
         * has already cancelled player rendering.
         */
        if (event.isCanceled()) {
            return;
        }

        UUID playerId =
                event.getEntity()
                        .getUUID();

        Direction wallDirection =
                StepOfTheWindClientSurfaceState
                        .getWallDirection(
                                playerId
                        );

        if (wallDirection == null
                || wallDirection
                .getAxis()
                .isVertical()) {
            return;
        }

        PoseStack poseStack =
                event.getPoseStack();

        poseStack.pushPose();

        /*
         * Shift the rendered model toward the wall so the
         * feet appear planted against it.
         */
        poseStack.translate(
                wallDirection.getStepX()
                        * WALL_VISUAL_OFFSET,
                0.0D,
                wallDirection.getStepZ()
                        * WALL_VISUAL_OFFSET
        );

        /*
         * Rotate the rendered player by exactly 90 degrees.
         *
         * This makes the wall act as the visual floor while
         * leaving server physics and collision unchanged.
         */
        switch (wallDirection) {

            case NORTH ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    90.0F
                            )
                    );

            case SOUTH ->
                    poseStack.mulPose(
                            Axis.XP.rotationDegrees(
                                    -90.0F
                            )
                    );

            case WEST ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(
                                    -90.0F
                            )
                    );

            case EAST ->
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(
                                    90.0F
                            )
                    );

            default -> {
                poseStack.popPose();
                return;
            }
        }

        TRANSFORMED_PLAYERS.add(
                playerId
        );
    }

    @SubscribeEvent(
            priority = EventPriority.HIGHEST
    )
    public static void onRenderPlayerPost(
            RenderPlayerEvent.Post event
    ) {
        UUID playerId =
                event.getEntity()
                        .getUUID();

        if (!TRANSFORMED_PLAYERS.remove(
                playerId
        )) {
            return;
        }

        event.getPoseStack()
                .popPose();
    }

    /*
     * Never keep Step surface rendering state when leaving
     * a world or server.
     */
    @SubscribeEvent
    public static void onClientLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        StepOfTheWindClientSurfaceState.clear();
        TRANSFORMED_PLAYERS.clear();
    }
}