package com.w0of26.martialspells.client.model;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.BearTrapEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Exact 64x32 frozen Rogues Bear Trap model translated from Yarn to Mojmap. */
public final class BearTrapModel extends HierarchicalModel<BearTrapEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "bear_trap"),
            "main"
    );

    private static final Vector3f ANIMATION_VECTOR = new Vector3f();
    private final ModelPart root;

    public BearTrapModel(ModelPart layerRoot) {
        this.root = layerRoot.getChild("root");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition modelRoot = mesh.getRoot();

        PartDefinition root = modelRoot.addOrReplaceChild(
                "root",
                CubeListBuilder.create()
                        .texOffs(28, 28)
                        .addBox(-8.0F, -2.0F, -1.0F, 16.0F, 2.0F, 2.0F)
                        .texOffs(0, 22)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "beartrap_part_1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -9.0F, -3.2F, -6.0F,
                                15.0F, 4.0F, 7.0F,
                                new CubeDeformation(0.01F)
                        ),
                PartPose.offset(1.5F, -1.0F, -1.0F)
        );

        root.addOrReplaceChild(
                "beartrap_part_2",
                CubeListBuilder.create()
                        .texOffs(0, 11)
                        .mirror()
                        .addBox(-7.0F, -3.2F, -1.0F, 15.0F, 4.0F, 7.0F)
                        .mirror(false),
                PartPose.offset(-0.5F, -1.0F, 1.0F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(
            BearTrapEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        root.getAllParts().forEach(ModelPart::resetPose);
        long timeMs = Math.max(0L, (long) (ageInTicks * 50.0F));

        switch (entity.getPhase()) {
            case BearTrapEntity.PHASE_SPAWNING -> KeyframeAnimations.animate(
                    this,
                    BearTrapAnimations.SPAWN,
                    timeMs,
                    1.0F,
                    ANIMATION_VECTOR
            );
            case BearTrapEntity.PHASE_ACTIVE -> KeyframeAnimations.animate(
                    this,
                    BearTrapAnimations.IDLE,
                    timeMs,
                    1.0F,
                    ANIMATION_VECTOR
            );
            case BearTrapEntity.PHASE_DESPAWNING -> KeyframeAnimations.animate(
                    this,
                    entity.isSprung() ? BearTrapAnimations.ATTACK : BearTrapAnimations.DESPAWN,
                    timeMs,
                    1.0F,
                    ANIMATION_VECTOR
            );
            default -> {
            }
        }
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
