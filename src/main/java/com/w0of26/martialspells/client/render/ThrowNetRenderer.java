package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.ThrowNetProjectile;
import com.w0of26.martialspells.spells.ThrowNetSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Renders the exact frozen Rogues Throw Net projectile model and 12-degree spin. */
public final class ThrowNetRenderer extends EntityRenderer<ThrowNetProjectile> {
    public static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_projectile/throw_net"
            );

    public ThrowNetRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            ThrowNetProjectile entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Vec3 velocity = entity.getDeltaMovement();
        double horizontal = Math.sqrt(
                velocity.x * velocity.x + velocity.z * velocity.z
        );
        float yaw = (float) Math.toDegrees(
                Math.atan2(velocity.x, velocity.z)
        );
        float pitch = (float) Math.toDegrees(
                Math.atan2(velocity.y, horizontal)
        );

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                (entity.tickCount + partialTick)
                        * ThrowNetSpell.SPIN_DEGREES_PER_TICK
        ));

        // Frozen model is authored in block-model 1/16 units around this center.
        poseStack.translate(-0.5D, -0.375D, -0.5D);

        BakedModel model = Minecraft.getInstance()
                .getModelManager()
                .getModel(MODEL);
        VertexConsumer vertices =
                bufferSource.getBuffer(Sheets.cutoutBlockSheet());

        Minecraft.getInstance()
                .getItemRenderer()
                .renderModelLists(
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
    public ResourceLocation getTextureLocation(ThrowNetProjectile entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
