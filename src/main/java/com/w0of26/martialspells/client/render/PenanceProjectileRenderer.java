package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.PenanceProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Frozen Paladins Penance Lightwell-orb renderer.
 *
 * <p>The orb is offset 0.6 blocks from the projectile center, then spun at
 * 15 degrees/tick around the travel axis. This recreates the source orbiting
 * composite-model presentation without taking Spell Engine as a dependency.</p>
 */
public final class PenanceProjectileRenderer
        extends EntityRenderer<PenanceProjectile> {
    public static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_projectile/lightwell_orb"
            );

    private static final float SCALE = 0.9F;
    private static final float ORBIT_RADIUS = 0.6F;
    private static final float ORBIT_DEGREES_PER_TICK = 15.0F;

    public PenanceProjectileRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            PenanceProjectile entity,
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
                        Math.asin(
                                Mth.clamp(
                                        direction.y,
                                        -1.0D,
                                        1.0D
                                )
                        )
                );
        float age = entity.tickCount + partialTick;

        poseStack.pushPose();

        poseStack.mulPose(
                Axis.YP.rotationDegrees(directionYaw)
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(directionPitch)
        );

        // Source rotation happens before the x-offset, producing an orbit.
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        age * ORBIT_DEGREES_PER_TICK
                )
        );
        poseStack.translate(
                ORBIT_RADIUS,
                0.0D,
                0.0D
        );
        poseStack.scale(
                SCALE,
                SCALE,
                SCALE
        );

        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(MODEL);

        VertexConsumer vertices =
                bufferSource.getBuffer(
                        Sheets.translucentCullBlockSheet()
                );

        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        int effectiveLight =
                LightTexture.FULL_BRIGHT;

        Minecraft.getInstance()
                .getItemRenderer()
                .renderModelLists(
                        model,
                        ItemStack.EMPTY,
                        effectiveLight,
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
                effectiveLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            PenanceProjectile entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
