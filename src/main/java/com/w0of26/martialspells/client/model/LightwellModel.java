package com.w0of26.martialspells.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.LightwellEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class LightwellModel
        extends EntityModel<LightwellEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            MartialSpells.MOD_ID,
                            "lightwell"
                    ),
                    "main"
            );

    private final ModelPart root;
    private final ModelPart light;

    public LightwellModel(
            ModelPart bakedRoot
    ) {
        root =
                bakedRoot.getChild("root");
        light =
                root.getChild("light");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh =
                new MeshDefinition();
        PartDefinition root =
                mesh.getRoot();

        PartDefinition well =
                root.addOrReplaceChild(
                        "root",
                        CubeListBuilder.create()
                                .texOffs(0, 23)
                                .addBox(
                                        -6.0F, -3.0F, -6.0F,
                                        12.0F, 6.0F, 12.0F
                                )
                                .texOffs(24, 5)
                                .addBox(
                                        5.0F, -5.0F, -5.0F,
                                        2.0F, 3.0F, 10.0F
                                )
                                .texOffs(0, 0)
                                .addBox(
                                        -7.0F, -5.0F, 5.0F,
                                        14.0F, 3.0F, 2.0F
                                )
                                .texOffs(0, 5)
                                .addBox(
                                        -7.0F, -5.0F, -5.0F,
                                        2.0F, 3.0F, 10.0F
                                )
                                .texOffs(0, 18)
                                .addBox(
                                        -7.0F, -5.0F, -7.0F,
                                        14.0F, 3.0F, 2.0F
                                ),
                        PartPose.offset(
                                0.0F,
                                21.0F,
                                0.0F
                        )
                );

        well.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(
                                -1.0F, 0.0F, -2.0F,
                                4.0F, 5.0F, 4.0F
                        )
                        .texOffs(48, 0)
                        .addBox(
                                -1.0F, 0.0F, -2.0F,
                                4.0F, 5.0F, 4.0F
                        ),
                PartPose.offsetAndRotation(
                        7.0F,
                        -5.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.3927F
                )
        );

        well.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create()
                        .texOffs(48, 0)
                        .mirror()
                        .addBox(
                                -3.0F, 0.0F, -2.0F,
                                4.0F, 5.0F, 4.0F
                        )
                        .mirror(false),
                PartPose.offsetAndRotation(
                        -7.0F,
                        -5.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        -0.3927F
                )
        );

        well.addOrReplaceChild(
                "light",
                CubeListBuilder.create()
                        .texOffs(3, 44)
                        .addBox(
                                -5.0F, -10.0F, -5.0F,
                                10.0F, 10.0F, 10.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offset(
                        0.0F,
                        -5.0F,
                        0.0F
                )
        );

        return LayerDefinition.create(
                mesh,
                64,
                64
        );
    }

    @Override
    public void setupAnim(
            LightwellEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        root.getAllParts()
                .forEach(ModelPart::resetPose);

        light.yRot =
                ageInTicks * 0.035F;
        light.xRot =
                Mth.sin(ageInTicks * 0.08F) * 0.04F;
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertices,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        root.render(
                poseStack,
                vertices,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
    }
}
