package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.HolyMoteProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class HolyMoteRenderer
        extends EntityRenderer<HolyMoteProjectile> {
    public static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_projectile/lightwell_orb"
            );

    public HolyMoteRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            HolyMoteProjectile entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Vec3 velocity =
                entity.getDeltaMovement();
        if (velocity.lengthSqr() <= 1.0E-8D) {
            return;
        }

        Vec3 direction =
                velocity.normalize();
        float directionYaw =
                (float) Math.toDegrees(
                        Math.atan2(
                                direction.x,
                                direction.z
                        )
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
        float age =
                entity.tickCount + partialTick;

        poseStack.pushPose();
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        directionYaw
                )
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        directionPitch
                )
        );
        // Spell.ProjectileModelComposite.Model defaults to 2 deg/tick.
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        age * 2.0F
                )
        );
        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(MODEL);
        VertexConsumer vertices =
                bufferSource.getBuffer(
                        PaladinSpellModelRenderTypes.glow()
                );

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
    public ResourceLocation getTextureLocation(
            HolyMoteProjectile entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
