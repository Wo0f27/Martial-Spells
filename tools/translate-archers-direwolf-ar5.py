"""Map frozen Archers Direwolf cuboids/keyframes from Yarn names to Mojmap names."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ARCHERS = Path(r"C:\Modding\RPG-Ports\upstream\Archers")
REVISION = "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74"
PREFIX = "common/src/main/java/net/archers/client/entity/"
TARGET = ROOT / "src/main/java/com/w0of26/martialspells/client"


def source(name: str) -> str:
    return subprocess.check_output(
        ["git", "-c", f"safe.directory={ARCHERS.as_posix()}", "show",
         f"{REVISION}:{PREFIX}{name}"], cwd=ARCHERS,
    ).decode("utf-8")


animation = source("DirewolfEntityAnimations.java")
animation = animation.replace("package net.archers.client.entity;",
                              "package com.w0of26.martialspells.client.animation;")
animation = animation.replace("import net.minecraft.client.render.entity.animation.Animation;",
                              "import net.minecraft.client.animation.AnimationDefinition;")
animation = animation.replace("import net.minecraft.client.render.entity.animation.AnimationHelper;",
                              "import net.minecraft.client.animation.KeyframeAnimations;")
animation = animation.replace("import net.minecraft.client.render.entity.animation.Keyframe;",
                              "import net.minecraft.client.animation.Keyframe;")
animation = animation.replace("import net.minecraft.client.render.entity.animation.Transformation;",
                              "import net.minecraft.client.animation.AnimationChannel;")
animation = animation.replace("DirewolfEntityAnimations", "SpiritWolfAnimations")
animation = animation.replace("Animation.Builder.create", "AnimationDefinition.Builder.withLength")
animation = animation.replace("public static final Animation ", "public static final AnimationDefinition ")
animation = animation.replace(".addBoneAnimation(", ".addAnimation(")
animation = animation.replace("new Transformation(", "new AnimationChannel(")
animation = animation.replace("Transformation.Targets.ROTATE", "AnimationChannel.Targets.ROTATION")
animation = animation.replace("Transformation.Targets.TRANSLATE", "AnimationChannel.Targets.POSITION")
animation = animation.replace("Transformation.Targets.SCALE", "AnimationChannel.Targets.SCALE")
animation = animation.replace("Transformation.Interpolations.CUBIC", "AnimationChannel.Interpolations.CATMULLROM")
animation = animation.replace("Transformation.Interpolations.LINEAR", "AnimationChannel.Interpolations.LINEAR")
animation = animation.replace("AnimationHelper.createRotationalVector", "KeyframeAnimations.degreeVec")
animation = animation.replace("AnimationHelper.createTranslationalVector", "KeyframeAnimations.posVec")
animation = animation.replace("AnimationHelper.createScalingVector", "KeyframeAnimations.scaleVec")
animation = "// Generated from frozen Archers Direwolf keyframes; coordinates and timings unchanged.\n" + animation
output = TARGET / "animation/SpiritWolfAnimations.java"
output.write_text(animation, encoding="utf-8", newline="\n")

model = source("DirewolfEntityModel.java")
geometry = model[model.index("\tpublic static TexturedModelData getTexturedModelData() {"):
                 model.index("\n\t// HAND-WRITTEN CODE")]
for old, new in (
    ("TexturedModelData getTexturedModelData()", "LayerDefinition createBodyLayer()"),
    ("ModelData modelData = new ModelData()", "MeshDefinition mesh = new MeshDefinition()"),
    ("modelData.getRoot()", "mesh.getRoot()"),
    ("ModelPartData", "PartDefinition"),
    ("ModelPartBuilder", "CubeListBuilder"),
    (".addChild(", ".addOrReplaceChild("),
    (".uv(", ".texOffs("),
    ("ModelTransform.pivot", "PartPose.offset"),
    ("ModelTransform.of", "PartPose.offsetAndRotation"),
    ("Dilation", "CubeDeformation"),
    (".cuboid(", ".addBox("),
    (".mirrored()", ".mirror()"),
    (".mirrored(false)", ".mirror(false)"),
    ("TexturedModelData.of(modelData,", "LayerDefinition.create(mesh,"),
):
    geometry = geometry.replace(old, new)

model_head = '''// Generated from frozen Archers Direwolf cuboids; positions and UVs unchanged.
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

'''
model_tail = '''
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
'''
output = TARGET / "model/SpiritWolfModel.java"
output.write_text(model_head + geometry + model_tail, encoding="utf-8", newline="\n")
print("Translated exact Direwolf geometry and four keyframe clips into MartialSpells")
