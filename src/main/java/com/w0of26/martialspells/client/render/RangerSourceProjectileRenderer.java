package com.w0of26.martialspells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.w0of26.martialspells.MartialSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;

/** Exact Archers baked projectile models, with Spell Engine block-atlas emission layers. */
public final class RangerSourceProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    public static final ResourceLocation RAIN_MODEL = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "spell_projectile/volley_arrow");
    public static final ResourceLocation MAGIC_MODEL = ResourceLocation.fromNamespaceAndPath(
            MartialSpells.MOD_ID, "spell_projectile/magic_arrow");
    private final ResourceLocation modelId;
    private final float scale;
    private final boolean radiate;

    public RangerSourceProjectileRenderer(EntityRendererProvider.Context context,
                                          ResourceLocation modelId, float scale, boolean radiate) {
        super(context);
        this.modelId = modelId;
        this.scale = scale;
        this.radiate = radiate;
    }

    @Override public void render(T entity, float yaw, float partialTick, PoseStack stack,
                                 MultiBufferSource buffer, int light) {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (entity.tickCount < 2 && cameraEntity != null && cameraEntity.distanceToSqr(entity) < 12.25D)
            return;
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 direction = velocity.lengthSqr() > 1.0E-8D ? velocity.normalize() : new Vec3(0, 0, 1);
        float directionYaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z)) + 180F;
        float directionPitch = (float) Math.toDegrees(Math.asin(direction.y));
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(directionYaw));
        stack.mulPose(Axis.XP.rotationDegrees(directionPitch));
        stack.scale(scale, scale, scale);
        stack.translate(-0.5D, -0.5D, -0.5D);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelId);
        VertexConsumer vertices = buffer.getBuffer(radiate
                ? PaladinSpellModelRenderTypes.radiate() : PaladinSpellModelRenderTypes.glow());
        int modelLight = radiate ? (light & 0xFFFF0000) | Math.max(light & 0xFFFF, 10 * 16)
                : LightTexture.FULL_BRIGHT;
        Minecraft.getInstance().getItemRenderer().renderModelLists(model, ItemStack.EMPTY,
                modelLight,
                OverlayTexture.NO_OVERLAY, stack, vertices);
        stack.popPose();
        super.render(entity, yaw, partialTick, stack, buffer, light);
    }

    @Override public ResourceLocation getTextureLocation(T entity) { return InventoryMenu.BLOCK_ATLAS; }
}
