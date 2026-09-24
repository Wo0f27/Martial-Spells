package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Feeds held-item quads to the Blessed Strikes glow overlay with the same
 * scrolling streak UV transform used by Spell Engine's item glow.
 */
public final class BlessedStrikesGlowVertexConsumer
        implements VertexConsumer {
    private final VertexConsumer delegate;
    private final Matrix4f textureMatrix;
    private final Vector3f scratch =
            new Vector3f();

    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    public BlessedStrikesGlowVertexConsumer(
            VertexConsumer delegate,
            float opacity
    ) {
        this.delegate = delegate;
        this.textureMatrix =
                BlessedStrikesItemGlow.textureMatrix();

        float intensity =
                Math.min(
                        1.0F,
                        opacity * 3.0F
                );

        this.red =
                Math.round(
                        255.0F * intensity
                );
        this.green =
                Math.round(
                        255.0F * intensity
                );
        this.blue =
                Math.round(
                        204.0F * intensity
                );
        this.alpha = 255;
    }

    @Override
    public VertexConsumer vertex(
            double x,
            double y,
            double z
    ) {
        delegate.vertex(
                x,
                y,
                z
        );
        return this;
    }

    @Override
    public VertexConsumer color(
            int red,
            int green,
            int blue,
            int alpha
    ) {
        delegate.color(
                this.red,
                this.green,
                this.blue,
                this.alpha
        );
        return this;
    }

    @Override
    public VertexConsumer uv(
            float u,
            float v
    ) {
        Vector3f transformed =
                textureMatrix.transformPosition(
                        scratch.set(
                                u,
                                v,
                                0.0F
                        )
                );

        delegate.uv(
                transformed.x(),
                transformed.y()
        );
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(
            int u,
            int v
    ) {
        delegate.overlayCoords(
                u,
                v
        );
        return this;
    }

    @Override
    public VertexConsumer uv2(
            int u,
            int v
    ) {
        delegate.uv2(
                u,
                v
        );
        return this;
    }

    @Override
    public VertexConsumer normal(
            float x,
            float y,
            float z
    ) {
        delegate.normal(
                x,
                y,
                z
        );
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(
            int red,
            int green,
            int blue,
            int alpha
    ) {
        delegate.defaultColor(
                this.red,
                this.green,
                this.blue,
                this.alpha
        );
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}
