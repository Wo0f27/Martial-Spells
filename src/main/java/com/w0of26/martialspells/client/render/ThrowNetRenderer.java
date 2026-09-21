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
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the frozen Rogues Throw Net projectile through Forge's standalone
 * baked-model path. Orientation and centering mirror Spell Engine's
 * TOWARDS_MOTION composite renderer.
 */
public final class ThrowNetRenderer extends EntityRenderer<ThrowNetProjectile> {
    public static final ResourceLocation MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_projectile/throw_net"
            );

    public static final ModelResourceLocation MODEL =
            new ModelResourceLocation(MODEL_ID, "standalone");

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
        if (velocity.lengthSqr() <= 1.0E-8D) {
            return;
        }

        Vec3 direction = velocity.normalize();
        float directionYaw =
                (float) Math.toDegrees(
                        Math.atan2(direction.x, direction.z)
                ) + 180.0F;
        float directionPitch =
                (float) Math.toDegrees(
                        Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D))
                );
        float age = entity.tickCount + partialTick;

        poseStack.pushPose();

        // Spell Engine ProjectileModelComposite.Orientation.TOWARDS_MOTION.
        poseStack.mulPose(Axis.YP.rotationDegrees(directionYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(directionPitch));

        // Frozen Rogues composite-model spin.
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                age * ThrowNetSpell.SPIN_DEGREES_PER_TICK
        ));

        BakedModel model = Minecraft.getInstance()
                .getModelManager()
                .getModel(MODEL);
        VertexConsumer vertices =
                bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

        // Spell Engine CustomModels.renderModel centers raw block-model geometry.
        poseStack.translate(-0.5D, -0.5D, -0.5D);
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
