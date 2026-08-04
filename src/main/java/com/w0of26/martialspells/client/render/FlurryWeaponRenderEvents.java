package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.ClientFlurryVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders visually stowed Monk weapons during Flurry.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class FlurryWeaponRenderEvents {
    private FlurryWeaponRenderEvents() {
    }

    /**
     * Initial first-person behavior:
     * hide the normal main-hand render while a weapon is stowed.
     *
     * This also hides the vanilla first-person arm for that hand.
     * A custom first-person fist renderer can be added after the
     * third-person stowing system is stable.
     */
    @SubscribeEvent
    public static void onRenderHand(
            RenderHandEvent event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || event.getHand()
                != InteractionHand.MAIN_HAND) {
            return;
        }

        if (ClientFlurryVisuals
                .shouldStowWeapon(
                        minecraft.player
                                .getUUID()
                )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(
            RenderPlayerEvent.Post event
    ) {
        Player player =
                event.getEntity();

        if (!ClientFlurryVisuals
                .shouldStowWeapon(
                        player.getUUID()
                )) {
            return;
        }

        ItemStack displayedWeapon =
                ClientFlurryVisuals
                        .getDisplayedWeapon(
                                player.getUUID()
                        );

        if (displayedWeapon.isEmpty()) {
            return;
        }

        PoseStack poseStack =
                event.getPoseStack();

        poseStack.pushPose();

        /*
         * Initial diagonal-back placement.
         *
         * These values will likely need visual tuning for long
         * weapons after the first runtime test.
         */
        poseStack.translate(
                -0.12D,
                1.05D,
                0.22D
        );

        if (player.isCrouching()) {
            poseStack.translate(
                    0.0D,
                    -0.12D,
                    0.05D
            );

            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            25.0F
                    )
            );
        }

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F
                )
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        45.0F
                )
        );

        poseStack.scale(
                0.85F,
                0.85F,
                0.85F
        );

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        player,
                        displayedWeapon,
                        ItemDisplayContext
                                .THIRD_PERSON_RIGHT_HAND,
                        false,
                        poseStack,
                        event.getMultiBufferSource(),
                        player.level(),
                        event.getPackedLight(),
                        OverlayTexture.NO_OVERLAY,
                        player.getId()
                );

        poseStack.popPose();
    }
}