package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.HolyMoteProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

public final class HolyMoteRenderer
        extends EntityRenderer<HolyMoteProjectile> {
    public static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "spell_projectile/lightwell_orb"
            );

    public HolyMoteRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            HolyMoteProjectile entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.scale(
                1.0F,
                1.0F,
                1.0F
        );
        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(MODEL);
        VertexConsumer vertices =
                bufferSource.getBuffer(
                        PaladinSpellModelRenderTypes.glow()
                );

        Minecraft.getInstance()
                .getItemRenderer()
                .renderModelLists(
                        model,
                        ItemStack.EMPTY,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        vertices
                );

        poseStack.popPose();

        super.render(
                entity,
                entityYaw,
                partialTick,
                poseStack,
                bufferSource,
                LightTexture.FULL_BRIGHT
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            HolyMoteProjectile entity
    ) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
