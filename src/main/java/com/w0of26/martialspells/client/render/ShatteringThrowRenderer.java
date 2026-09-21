package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.entity.ShatteringThrowProjectile;
import com.w0of26.martialspells.spells.ShatteringThrowSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Frozen Spell Engine ALONG_MOTION held-item projectile renderer.
 */
public final class ShatteringThrowRenderer
        extends EntityRenderer<ShatteringThrowProjectile> {

    public ShatteringThrowRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            ShatteringThrowProjectile entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() <= 1.0E-8D) {
            return;
        }

        Entity cameraEntity =
                Minecraft.getInstance().getCameraEntity();
        if (entity.tickCount < 2
                && cameraEntity != null
                && cameraEntity.distanceToSqr(entity) < 12.25D) {
            return;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(
                entity.getItemModelId()
        );
        if (itemId == null) {
            return;
        }

        ItemStack stack = BuiltInRegistries.ITEM
                .get(itemId)
                .getDefaultInstance();
        if (stack.isEmpty()) {
            return;
        }

        Vec3 direction = velocity.normalize();
        float directionYaw =
                (float) Math.toDegrees(
                        Math.atan2(direction.x, direction.z)
                ) + 180.0F;
        float directionPitch =
                (float) Math.toDegrees(
                        Math.asin(
                                Mth.clamp(
                                        direction.y,
                                        -1.0D,
                                        1.0D
                                )
                        )
                );

        float age = entity.tickCount + partialTick;

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft
                .getItemRenderer()
                .getModel(
                        stack,
                        entity.level(),
                        null,
                        entity.getId()
                );

        poseStack.pushPose();

        // Spell Engine Orientation.ALONG_MOTION.
        poseStack.mulPose(
                Axis.YP.rotationDegrees(directionYaw)
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(directionPitch)
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(90.0F)
        );

        // Source spin: -36 degrees every tick.
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        age
                                * ShatteringThrowSpell
                                        .SPIN_DEGREES_PER_TICK
                )
        );

        model.getTransforms()
                .getTransform(ItemDisplayContext.FIXED)
                .apply(false, poseStack);

        VertexConsumer vertices = bufferSource.getBuffer(
                RenderType.entityTranslucentCull(
                        TextureAtlas.LOCATION_BLOCKS
                )
        );

        poseStack.translate(-0.5D, -0.5D, -0.5D);
        minecraft.getItemRenderer().renderModelLists(
                model,
                ItemStack.EMPTY,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                vertices
        );

        poseStack.popPose();

        super.render(
                entity,
                entityYaw,
                partialTick,
                poseStack,
                bufferSource,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            ShatteringThrowProjectile entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
