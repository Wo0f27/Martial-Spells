package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Local Forge 1.20.1 equivalents of Spell Engine 1.10.5 spell-object
 * LightEmission layers used by frozen Paladins.
 */
public final class PaladinSpellModelRenderTypes
        extends RenderType {
    private static RenderType glow;
    private static RenderType radiate;

    private PaladinSpellModelRenderTypes(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            Runnable setupState,
            Runnable clearState
    ) {
        super(
                name,
                format,
                mode,
                bufferSize,
                affectsCrumbling,
                sortOnUpload,
                setupState,
                clearState
        );
    }

    /**
     * Spell Engine LightEmission.GLOW:
     * beacon-beam shader, block atlas, solid/additive-looking emissive output,
     * no culling, ordinary overlay, particle target.
     */
    public static RenderType glow() {
        if (glow == null) {
            glow =
                    create(
                            "martial_spells_paladin_spell_glow",
                            DefaultVertexFormat.NEW_ENTITY,
                            VertexFormat.Mode.QUADS,
                            256,
                            true,
                            true,
                            CompositeState.builder()
                                    .setShaderState(
                                            RENDERTYPE_BEACON_BEAM_SHADER
                                    )
                                    .setTextureState(
                                            new TextureStateShard(
                                                    InventoryMenu.BLOCK_ATLAS,
                                                    false,
                                                    false
                                            )
                                    )
                                    .setTransparencyState(
                                            NO_TRANSPARENCY
                                    )
                                    .setCullState(
                                            NO_CULL
                                    )
                                    .setWriteMaskState(
                                            COLOR_DEPTH_WRITE
                                    )
                                    .setOverlayState(
                                            OVERLAY
                                    )
                                    .setOutputState(
                                            PARTICLES_TARGET
                                    )
                                    .createCompositeState(false)
                    );
        }
        return glow;
    }

    /**
     * Spell Engine LightEmission.RADIATE:
     * translucent-emissive entity shader on the block atlas with no culling.
     */
    public static RenderType radiate() {
        if (radiate == null) {
            radiate =
                    create(
                            "martial_spells_paladin_spell_radiate",
                            DefaultVertexFormat.NEW_ENTITY,
                            VertexFormat.Mode.QUADS,
                            256,
                            true,
                            true,
                            CompositeState.builder()
                                    .setShaderState(
                                            RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER
                                    )
                                    .setTextureState(
                                            new TextureStateShard(
                                                    InventoryMenu.BLOCK_ATLAS,
                                                    false,
                                                    false
                                            )
                                    )
                                    .setTransparencyState(
                                            NO_TRANSPARENCY
                                    )
                                    .setCullState(
                                            NO_CULL
                                    )
                                    .setWriteMaskState(
                                            COLOR_DEPTH_WRITE
                                    )
                                    .setOverlayState(
                                            OVERLAY
                                    )
                                    .setOutputState(
                                            PARTICLES_TARGET
                                    )
                                    .createCompositeState(false)
                    );
        }
        return radiate;
    }
}
