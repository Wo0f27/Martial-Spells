#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float GlintAlpha;

in float vertexDistance;
in vec2 itemTexCoord;
in vec2 glowTexCoord;

out vec4 fragColor;

void main() {
    // Forge safety mask: preserve the real generated-item silhouette.
    vec4 itemColor = texture(Sampler0, itemTexCoord);
    if (itemColor.a < 0.1) {
        discard;
    }

    vec4 glowColor = texture(Sampler1, glowTexCoord);
    if (glowColor.a < 0.1) {
        discard;
    }

    float fade =
            linear_fog_fade(
                    vertexDistance,
                    FogStart,
                    FogEnd
            ) * GlintAlpha;

    fragColor = vec4(
            glowColor.rgb
                    * ColorModulator.rgb
                    * fade,
            glowColor.a
    );
}
