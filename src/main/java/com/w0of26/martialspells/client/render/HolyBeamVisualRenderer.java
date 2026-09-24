package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.entity.HolyBeamVisualEntity;
import com.w0of26.martialspells.spells.PaladinHolyBeamSpell;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Continuous source-style golden beam for Holy Light.
 *
 * <p>Spell Engine's original beam used color 0xFFCC66FF and flow 1.5. The
 * Forge translation renders a fullbright additive crossed beam in the same
 * gold palette, while PaladinVfx supplies the flowing spark layer.</p>
 */
public final class HolyBeamVisualRenderer
        extends EntityRenderer<HolyBeamVisualEntity> {
    private static final double OUTER_WIDTH = 0.055D;
    private static final double INNER_WIDTH = 0.020D;

    public HolyBeamVisualRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            HolyBeamVisualEntity beam,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Entity owner =
                beam.level().getEntity(
                        beam.getOwnerEntityId()
                );
        if (owner == null) {
            return;
        }

        Vec3 start =
                owner.getEyePosition(partialTick);
        Vec3 look =
                owner.getViewVector(partialTick)
                        .normalize();
        Vec3 maxEnd =
                start.add(
                        look.scale(
                                PaladinHolyBeamSpell.RANGE
                        )
                );

        HitResult blockHit =
                beam.level().clip(
                        new ClipContext(
                                start,
                                maxEnd,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                owner
                        )
                );

        Vec3 end =
                blockHit.getType()
                        == HitResult.Type.BLOCK
                        ? blockHit.getLocation()
                        : maxEnd;

        Vec3 beamOrigin =
                beam.getPosition(partialTick);
        Vec3 localStart =
                start.subtract(beamOrigin);
        Vec3 localEnd =
                end.subtract(beamOrigin);
        Vec3 delta =
                localEnd.subtract(localStart);
        if (delta.lengthSqr() <= 1.0E-8D) {
            return;
        }

        Vec3 direction =
                delta.normalize();
        Vec3 reference =
                Math.abs(direction.y) < 0.95D
                        ? new Vec3(0.0D, 1.0D, 0.0D)
                        : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side =
                direction.cross(reference)
                        .normalize();
        Vec3 up =
                side.cross(direction)
                        .normalize();

        VertexConsumer vertices =
                bufferSource.getBuffer(
                        RenderType.lightning()
                );
        Matrix4f matrix =
                poseStack.last().pose();

        renderCross(
                matrix,
                vertices,
                localStart,
                localEnd,
                side,
                up,
                OUTER_WIDTH,
                255,
                204,
                102,
                145
        );
        renderCross(
                matrix,
                vertices,
                localStart,
                localEnd,
                side,
                up,
                INNER_WIDTH,
                255,
                255,
                214,
                230
        );

        super.render(
                beam,
                entityYaw,
                partialTick,
                poseStack,
                bufferSource,
                packedLight
        );
    }

    private static void renderCross(
            Matrix4f matrix,
            VertexConsumer vertices,
            Vec3 start,
            Vec3 end,
            Vec3 side,
            Vec3 up,
            double width,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        renderQuad(
                matrix,
                vertices,
                start,
                end,
                side.scale(width),
                red,
                green,
                blue,
                alpha
        );
        renderQuad(
                matrix,
                vertices,
                start,
                end,
                up.scale(width),
                red,
                green,
                blue,
                alpha
        );
    }

    private static void renderQuad(
            Matrix4f matrix,
            VertexConsumer vertices,
            Vec3 start,
            Vec3 end,
            Vec3 offset,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertex(
                matrix,
                vertices,
                start.add(offset),
                red,
                green,
                blue,
                alpha
        );
        vertex(
                matrix,
                vertices,
                end.add(offset),
                red,
                green,
                blue,
                alpha
        );
        vertex(
                matrix,
                vertices,
                end.subtract(offset),
                red,
                green,
                blue,
                alpha
        );
        vertex(
                matrix,
                vertices,
                start.subtract(offset),
                red,
                green,
                blue,
                alpha
        );
    }

    private static void vertex(
            Matrix4f matrix,
            VertexConsumer vertices,
            Vec3 point,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertices.vertex(
                        matrix,
                        (float) point.x,
                        (float) point.y,
                        (float) point.z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(
            HolyBeamVisualEntity entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
