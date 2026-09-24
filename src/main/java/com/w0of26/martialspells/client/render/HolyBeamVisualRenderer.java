package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.w0of26.martialspells.entity.HolyBeamVisualEntity;
import com.w0of26.martialspells.spells.PaladinHolyBeamSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Source-faithful Spell Engine 1.20.1 beam renderer for frozen Paladins Holy
 * Light.
 *
 * <p>The upstream spell uses Minecraft's beacon-beam texture, a 0.1-wide
 * white center, two wider #FFCC66 outer layers, flow 1.5 and a continuously
 * rotating rectangular prism. This renderer mirrors BeamRenderer's geometry
 * and UV math locally so Spell Engine remains absent at runtime.</p>
 */
public final class HolyBeamVisualRenderer
        extends EntityRenderer<HolyBeamVisualEntity> {
    private static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft",
                    "textures/entity/beacon_beam.png"
            );

    private static final float WIDTH = 0.10F;
    private static final float FLOW = 1.50F;

    private static final int INNER_RED = 255;
    private static final int INNER_GREEN = 255;
    private static final int INNER_BLUE = 255;
    private static final int INNER_ALPHA = 255;

    private static final int OUTER_RED = 255;
    private static final int OUTER_GREEN = 204;
    private static final int OUTER_BLUE = 102;
    private static final int OUTER_ALPHA = 255;

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
        Entity resolvedOwner =
                beam.level().getEntity(
                        beam.getOwnerEntityId()
                );
        if (!(resolvedOwner instanceof LivingEntity owner)) {
            return;
        }

        Vec3 look =
                owner == Minecraft.getInstance().player
                        ? owner.getLookAngle().normalize()
                        : owner.getViewVector(partialTick).normalize();

        Vec3 launchPoint =
                sourceLaunchPoint(
                        owner,
                        look
                );

        Vec3 maxEnd =
                launchPoint.add(
                        look.scale(
                                PaladinHolyBeamSpell.RANGE
                        )
                );

        HitResult blockHit =
                beam.level().clip(
                        new ClipContext(
                                launchPoint,
                                maxEnd,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                owner
                        )
                );

        float length =
                blockHit.getType()
                        == HitResult.Type.BLOCK
                        ? (float) launchPoint.distanceTo(
                                blockHit.getLocation()
                        )
                        : PaladinHolyBeamSpell.RANGE;

        if (length <= 1.0E-5F) {
            return;
        }

        /*
         * BeamRenderer translates to the caster's interpolated position, then
         * to shoulder height. Its final +0.5 forward launch offset is applied
         * after the beam-direction rotation as a local-Y translation.
         */
        Vec3 ownerPosition =
                owner.getPosition(partialTick);
        Vec3 beamPosition =
                beam.getPosition(partialTick);
        float launchHeight =
                owner.getEyeHeight()
                        - owner.getBbHeight() * 0.15F;

        poseStack.pushPose();
        poseStack.translate(
                ownerPosition.x - beamPosition.x,
                ownerPosition.y - beamPosition.y
                        + launchHeight,
                ownerPosition.z - beamPosition.z
        );

        float inclination =
                (float) Math.acos(
                        Mth.clamp(
                                look.y,
                                -1.0D,
                                1.0D
                        )
                );
        float azimuth =
                (float) Math.atan2(
                        look.z,
                        look.x
                );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        (1.5707964F - azimuth)
                                * 57.295776F
                )
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        inclination
                                * 57.295776F
                )
        );
        poseStack.translate(
                0.0D,
                0.50D,
                0.0D
        );

        float absoluteTime =
                (float) Math.floorMod(
                        beam.level().getGameTime(),
                        40L
                )
                        + partialTick;

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        absoluteTime * 2.25F
                                - 45.0F
                )
        );

        renderBeam(
                poseStack,
                bufferSource,
                beam.level().getGameTime(),
                partialTick,
                length
        );

        poseStack.popPose();

        super.render(
                beam,
                entityYaw,
                partialTick,
                poseStack,
                bufferSource,
                LightTexture.FULL_BRIGHT
        );
    }

    private static Vec3 sourceLaunchPoint(
            LivingEntity owner,
            Vec3 look
    ) {
        return owner.position()
                .add(
                        0.0D,
                        owner.getEyeHeight()
                                - owner.getBbHeight()
                                * 0.15D,
                        0.0D
                )
                .add(
                        look.scale(0.50D)
                );
    }

    private static void renderBeam(
            PoseStack poseStack,
            MultiBufferSource buffers,
            long gameTime,
            float partialTick,
            float height
    ) {
        float shift =
                (float) Math.floorMod(
                        gameTime,
                        40L
                )
                        + partialTick;

        float offset =
                Mth.frac(
                        shift * 0.20F
                                - Mth.floor(
                                        shift * 0.10F
                                )
                )
                        * -FLOW;

        VertexConsumer inner =
                buffers.getBuffer(
                        PaladinSpellModelRenderTypes
                                .holyBeamInner(
                                        BEAM_TEXTURE
                                )
                );
        VertexConsumer outer =
                buffers.getBuffer(
                        PaladinSpellModelRenderTypes
                                .holyBeamOuter(
                                        BEAM_TEXTURE
                                )
                );

        renderBeamLayer(
                poseStack,
                inner,
                INNER_RED,
                INNER_GREEN,
                INNER_BLUE,
                INNER_ALPHA,
                height,
                WIDTH,
                offset
        );

        renderBeamLayer(
                poseStack,
                outer,
                OUTER_RED,
                OUTER_GREEN,
                OUTER_BLUE,
                Math.round(
                        OUTER_ALPHA * 0.75F
                ),
                height,
                WIDTH * 1.50F,
                offset * 0.90F
        );

        renderBeamLayer(
                poseStack,
                outer,
                OUTER_RED,
                OUTER_GREEN,
                OUTER_BLUE,
                OUTER_ALPHA / 3,
                height,
                WIDTH * 2.0F,
                offset * 0.80F
        );
    }

    private static void renderBeamLayer(
            PoseStack poseStack,
            VertexConsumer vertices,
            int red,
            int green,
            int blue,
            int alpha,
            float height,
            float width,
            float offset
    ) {
        PoseStack.Pose matrix =
                poseStack.last();

        renderBeamFace(
                matrix,
                vertices,
                red,
                green,
                blue,
                alpha,
                height,
                0.0F,
                width,
                width,
                0.0F,
                0.0F,
                1.0F,
                height,
                offset
        );
        renderBeamFace(
                matrix,
                vertices,
                red,
                green,
                blue,
                alpha,
                height,
                0.0F,
                -width,
                -width,
                0.0F,
                0.0F,
                1.0F,
                height,
                offset
        );
        renderBeamFace(
                matrix,
                vertices,
                red,
                green,
                blue,
                alpha,
                height,
                width,
                0.0F,
                0.0F,
                -width,
                0.0F,
                1.0F,
                height,
                offset
        );
        renderBeamFace(
                matrix,
                vertices,
                red,
                green,
                blue,
                alpha,
                height,
                -width,
                0.0F,
                0.0F,
                width,
                0.0F,
                1.0F,
                height,
                offset
        );
    }

    private static void renderBeamFace(
            PoseStack.Pose matrix,
            VertexConsumer vertices,
            int red,
            int green,
            int blue,
            int alpha,
            float height,
            float x1,
            float z1,
            float x2,
            float z2,
            float u1,
            float u2,
            float v1,
            float v2
    ) {
        renderBeamVertex(
                matrix,
                vertices,
                red,
                green,
                blue,
                alpha,
                height,
                x1,
                z1,
                u2,
                v1
        );
        renderBeamVertex(
                matrix,
                vertices,
                red,
                green,
                blue,
                alpha,
                0.0F,
                x1,
                z1,
                u2,
                v2
        );
        renderBeamVertex(
                matrix,
                vertices,
                red,
                green,
                blue,
                alpha,
                0.0F,
                x2,
                z2,
                u1,
                v2
        );
        renderBeamVertex(
                matrix,
                vertices,
                red,
                green,
                blue,
                alpha,
                height,
                x2,
                z2,
                u1,
                v1
        );
    }

    private static void renderBeamVertex(
            PoseStack.Pose matrix,
            VertexConsumer vertices,
            int red,
            int green,
            int blue,
            int alpha,
            float y,
            float x,
            float z,
            float u,
            float v
    ) {
        vertices.vertex(
                        matrix.pose(),
                        x,
                        y,
                        z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .uv(
                        u,
                        v
                )
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(
                        LightTexture.FULL_BRIGHT
                )
                .normal(
                        matrix.normal(),
                        0.0F,
                        1.0F,
                        0.0F
                )
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(
            HolyBeamVisualEntity entity
    ) {
        return BEAM_TEXTURE;
    }
}
