package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.HeavenfallClientTargetState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HeavenfallTargetRenderer {

    private HeavenfallTargetRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        int entityId =
                HeavenfallClientTargetState
                        .getTargetEntityId();

        if (entityId < 0) {
            return;
        }

        Entity entity =
                minecraft.level
                        .getEntity(
                                entityId
                        );

        if (!(entity
                instanceof LivingEntity target)) {
            return;
        }

        if (!target.isAlive()) {
            return;
        }

        PoseStack poseStack =
                event.getPoseStack();

        Vec3 cameraPosition =
                event.getCamera()
                        .getPosition();

        MultiBufferSource.BufferSource
                bufferSource =
                minecraft.renderBuffers()
                        .bufferSource();

        VertexConsumer lines =
                bufferSource.getBuffer(
                        RenderType.lines()
                );

        AABB box =
                target.getBoundingBox()
                        .inflate(
                                0.12D
                        );

        poseStack.pushPose();

        poseStack.translate(
                -cameraPosition.x,
                -cameraPosition.y,
                -cameraPosition.z
        );

        LevelRenderer.renderLineBox(
                poseStack,
                lines,
                box,
                1.0F,
                0.80F,
                0.10F,
                1.0F
        );

        poseStack.popPose();

        bufferSource.endBatch(
                RenderType.lines()
        );
    }

    @SubscribeEvent
    public static void onLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        HeavenfallClientTargetState
                .clear();
    }
}