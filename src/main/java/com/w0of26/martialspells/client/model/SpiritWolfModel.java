// Generated from frozen Archers Direwolf cuboids; positions and UVs unchanged.
package com.w0of26.martialspells.client.model;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.animation.SpiritWolfAnimations;
import com.w0of26.martialspells.entity.SpiritWolfEntity;
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
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class SpiritWolfModel extends HierarchicalModel<SpiritWolfEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MartialSpells.MOD_ID, "spirit_wolf"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private static final Vector3f TEMP = new Vector3f();

    public SpiritWolfModel(ModelPart baked) {
        this.root = baked.getChild("root");
        this.head = root.getChild("body").getChild("head");
    }

    @Override public ModelPart root() { return root; }

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition modelPartData = mesh.getRoot();
		PartDefinition root = modelPartData.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 16.0F, 0.0F));

		PartDefinition right_back_leg = root.addOrReplaceChild("right_back_leg", CubeListBuilder.create().texOffs(12, 42).mirror().addBox(-1.0F, -3.0F, -2.95F, 3.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 45).mirror().addBox(-1.0F, 1.0F, 3.05F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, -2.0F, 6.0F));

		PartDefinition left_back_leg = root.addOrReplaceChild("left_back_leg", CubeListBuilder.create().texOffs(12, 42).addBox(-2.0F, -3.0F, -2.95F, 3.0F, 7.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(0, 45).addBox(-2.0F, 1.0F, 3.05F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -2.0F, 6.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 24).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.0F, -6.0F, -16.0F, 10.0F, 11.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, 5.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 21).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 8.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 21).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 8.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 34).addBox(-2.0F, -1.0F, -10.0F, 4.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(11, 58).addBox(-2.0F, 2.0F, -10.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, -16.0F));

		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(47, 50).mirror().addBox(-5.0F, 0.0F, 0.0F, 5.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.0F, -4.0F, -1.0F, 0.0F, 0.7854F, 0.0F));

		PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(47, 50).addBox(0.0F, 0.0F, 0.0F, 5.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -4.0F, -1.0F, 0.0F, -0.7854F, 0.0F));

		PartDefinition mouith = head.addOrReplaceChild("mouith", CubeListBuilder.create().texOffs(30, 42).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(30, 48).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, -5.0F));

		PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(21, 22).mirror().addBox(-1.25F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.75F, -4.0F, -0.5F));

		PartDefinition left_ear = head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(21, 22).addBox(-0.75F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.75F, -4.0F, -0.5F));

		PartDefinition right_front_leg = body.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(50, 3).mirror().addBox(-0.25F, -2.0F, -2.0F, 3.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-5.0F, 0.0F, -12.0F));

		PartDefinition left_front_leg = body.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(50, 3).addBox(-2.75F, -2.0F, -2.0F, 3.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, 0.0F, -12.0F));

		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(28, 9).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.5F, 4.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

    @Override public void setupAnim(SpiritWolfEntity entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = Mth.clamp(netHeadYaw, -60F, 60F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -60F, 60F) * Mth.DEG_TO_RAD;
        // Exact keyframes. Spell Engine wraps the neighbours at the run-loop seam.
        SpiritWolfLoopAnimation.seamlessLoop(this, SpiritWolfAnimations.run,
                (long) (limbSwing * 50F), Math.min(limbSwingAmount, 1F), TEMP);
        int age = entity.lifeAge();
        if (entity.phase() == SpiritWolfEntity.SPAWNING) {
            KeyframeAnimations.animate(this, SpiritWolfAnimations.spawn,
                    (long) (age * 100L), 1F, TEMP); // source spawn playback speed 2
        } else if (entity.phase() == SpiritWolfEntity.DESPAWNING) {
            KeyframeAnimations.animate(this, SpiritWolfAnimations.spawn,
                    (long) ((SpiritWolfEntity.TOTAL_TICKS - age) * 100L), 1F, TEMP);
        } else {
            int biteAge = age - entity.attackStartAge();
            boolean biting = biteAge >= 0 && biteAge < SpiritWolfEntity.BITE_TICKS;
            if (biting) {
                KeyframeAnimations.animate(this, SpiritWolfAnimations.bite,
                        (long) (biteAge * 1000.0 / SpiritWolfEntity.BITE_TICKS), 1F, TEMP);
            }
            float idleWeight = biting ? 0F : Mth.clamp(1F - limbSwingAmount * 3F, 0F, 1F);
            if (idleWeight > 0F) {
                KeyframeAnimations.animate(this, SpiritWolfAnimations.idle,
                        (long) ((age - SpiritWolfEntity.SPAWN_TICKS) * 50L), idleWeight, TEMP);
            }
        }
    }
}
