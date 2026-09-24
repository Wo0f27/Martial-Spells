package com.w0of26.martialspells.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.animation.BattleBannerAnimations;
import com.w0of26.martialspells.entity.BattleBannerEntity;
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
import net.minecraft.util.Mth;

/**
 * Frozen Paladins Battle Banner geometry translated to Mojmap.
 *
 * <p>P4 keeps the exact source cuboids and frozen Blockbench place/idle
 * keyframe definitions. Spawn plays place forward; despawn samples the same
 * clip in reverse, matching the upstream summoned-entity lifecycle.</p>
 */
public final class BattleBannerModel
        extends HierarchicalModel<BattleBannerEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            MartialSpells.MOD_ID,
                            "battle_banner"
                    ),
                    "main"
            );

    private final ModelPart root;
    private final ModelPart flagPart;
    private final ModelPart flagPart2;
    private final ModelPart flagPart3;
    private final ModelPart flagPart4;

    public BattleBannerModel(
            ModelPart root
    ) {
        this.root = root;
        ModelPart battleFlag =
                root.getChild("battle_flag");
        this.flagPart =
                battleFlag.getChild("flag_part");
        this.flagPart2 =
                flagPart.getChild("flag_part_2");
        this.flagPart3 =
                flagPart2.getChild("flag_part_3");
        this.flagPart4 =
                flagPart3.getChild("flag_part_4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh =
                new MeshDefinition();
        PartDefinition root =
                mesh.getRoot();

        PartDefinition battleFlag =
                root.addOrReplaceChild(
                        "battle_flag",
                        CubeListBuilder.create()
                                .texOffs(24, 38)
                                .addBox(
                                        -8.0F, -36.0F, -3.0F,
                                        16.0F, 2.0F, 2.0F,
                                        new CubeDeformation(0.2F)
                                )
                                .texOffs(28, 0)
                                .addBox(
                                        -8.0F, -36.0F, -3.0F,
                                        16.0F, 2.0F, 2.0F
                                )
                                .texOffs(29, 23)
                                .addBox(
                                        -1.5F, -6.0F, -1.5F,
                                        3.0F, 6.0F, 3.0F
                                )
                                .texOffs(45, 4)
                                .addBox(
                                        -1.0F, -38.0F, -1.0F,
                                        2.0F, 32.0F, 2.0F
                                ),
                        PartPose.offset(
                                0.0F,
                                24.0F,
                                0.0F
                        )
                );

        PartDefinition flagPart =
                battleFlag.addOrReplaceChild(
                        "flag_part",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -7.0F, 0.0F, 0.0F,
                                        14.0F, 8.0F, 0.0F
                                ),
                        PartPose.offset(
                                0.0F,
                                -34.0F,
                                -2.0F
                        )
                );

        PartDefinition flagPart2 =
                flagPart.addOrReplaceChild(
                        "flag_part_2",
                        CubeListBuilder.create()
                                .texOffs(0, 8)
                                .addBox(
                                        -7.0F, 0.0F, 0.0F,
                                        14.0F, 8.0F, 0.0F
                                ),
                        PartPose.offset(
                                0.0F,
                                8.0F,
                                0.0F
                        )
                );

        PartDefinition flagPart3 =
                flagPart2.addOrReplaceChild(
                        "flag_part_3",
                        CubeListBuilder.create()
                                .texOffs(0, 16)
                                .addBox(
                                        -7.0F, 0.0F, 0.0F,
                                        14.0F, 8.0F, 0.0F
                                ),
                        PartPose.offset(
                                0.0F,
                                8.0F,
                                0.0F
                        )
                );

        flagPart3.addOrReplaceChild(
                "flag_part_4",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(
                                -7.0F, 0.0F, 0.0F,
                                14.0F, 8.0F, 0.0F
                        ),
                PartPose.offset(
                        0.0F,
                        8.0F,
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
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(
            BattleBannerEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        root.getAllParts()
                .forEach(ModelPart::resetPose);

        this.animate(
                entity.spawnAnimationState,
                BattleBannerAnimations.place,
                ageInTicks,
                1.0F
        );
        this.animate(
                entity.idleAnimationState,
                BattleBannerAnimations.idle,
                ageInTicks,
                1.0F
        );
        this.animate(
                entity.despawnAnimationState,
                BattleBannerAnimations.place,
                ageInTicks,
                -1.0F
        );
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
