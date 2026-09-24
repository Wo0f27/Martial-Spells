package com.w0of26.martialspells.client.render;

import com.w0of26.martialspells.entity.PenanceProjectile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * P3 safety renderer for Penance.
 *
 * <p>The bolt's visible P3 presentation is its synchronized Holy particle
 * trail. The frozen orbiting Lightwell-orb composite model is intentionally
 * restored in P5, so this renderer emits no placeholder cube or missing
 * texture geometry.</p>
 */
public final class PenanceProjectileRenderer
        extends EntityRenderer<PenanceProjectile> {
    public PenanceProjectileRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            PenanceProjectile entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
