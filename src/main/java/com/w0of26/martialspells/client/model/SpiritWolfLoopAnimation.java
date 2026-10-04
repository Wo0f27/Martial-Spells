package com.w0of26.martialspells.client.model;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/** Narrow Mojmap translation of Spell Engine's loop-seam interpolation for the direwolf run clip. */
final class SpiritWolfLoopAnimation {
    private SpiritWolfLoopAnimation() {}

    static void seamlessLoop(HierarchicalModel<?> model, AnimationDefinition animation,
                             long runningTime, float scale, Vector3f temp) {
        float time = runningTime / 1000.0F;
        if (animation.looping()) time %= animation.lengthInSeconds();
        final float clipTime = time;
        for (Map.Entry<String, List<AnimationChannel>> entry : animation.boneAnimations().entrySet()) {
            model.getAnyDescendantWithName(entry.getKey()).ifPresent(part -> {
                for (AnimationChannel channel : entry.getValue()) {
                    apply(part, channel, clipTime, animation.looping(), scale, temp);
                }
            });
        }
    }

    private static void apply(ModelPart part, AnimationChannel channel, float time,
                              boolean looping, float scale, Vector3f temp) {
        Keyframe[] frames = channel.keyframes();
        int last = frames.length - 1;
        int i = Math.max(0, Mth.binarySearch(0, frames.length,
                index -> time <= frames[index].timestamp()) - 1);
        int j = Math.min(last, i + 1);
        Keyframe from = frames[i];
        Keyframe to = frames[j];
        float delta = i == j ? 0F : Mth.clamp(
                (time - from.timestamp()) / (to.timestamp() - from.timestamp()), 0F, 1F);
        if (looping && last >= 2 && (i == 0 || j == last)) {
            Vector3f p0 = frames[i == 0 ? last - 1 : i - 1].target();
            Vector3f p1 = from.target();
            Vector3f p2 = to.target();
            Vector3f p3 = frames[j == last ? 1 : j + 1].target();
            temp.set(Mth.catmullrom(delta, p0.x(), p1.x(), p2.x(), p3.x()) * scale,
                    Mth.catmullrom(delta, p0.y(), p1.y(), p2.y(), p3.y()) * scale,
                    Mth.catmullrom(delta, p0.z(), p1.z(), p2.z(), p3.z()) * scale);
        } else {
            to.interpolation().apply(temp, delta, frames, i, j, scale);
        }
        channel.target().apply(part, temp);
    }
}
