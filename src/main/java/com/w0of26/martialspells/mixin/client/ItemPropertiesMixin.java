package com.w0of26.martialspells.mixin.client;

import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Render-only item-property bridge for Magic Arrow.
 *
 * The spell never starts real item use. Instead, recognized bows and
 * crossbows expose the same pulling/pull model predicates they normally
 * use while being prepared, driven by Iron's spell-cast progress.
 */
@Mixin(ItemProperties.class)
public abstract class ItemPropertiesMixin {
    private static final ResourceLocation PULLING =
            ResourceLocation.fromNamespaceAndPath("minecraft", "pulling");
    private static final ResourceLocation PULL =
            ResourceLocation.fromNamespaceAndPath("minecraft", "pull");
    private static final ResourceLocation CHARGED =
            ResourceLocation.fromNamespaceAndPath("minecraft", "charged");
    private static final ResourceLocation FIREWORK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "firework");

    @Inject(
            method = "getProperty",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void martialSpells$magicArrowRangedVisuals(
            Item item,
            ResourceLocation propertyId,
            CallbackInfoReturnable<ItemPropertyFunction> cir
    ) {
        boolean bow = RangedWeaponClassifier.isBow(item);
        boolean crossbow = RangedWeaponClassifier.isCrossbow(item);

        if (!bow && !crossbow) {
            return;
        }

        boolean relevant = bow
                ? PULLING.equals(propertyId) || PULL.equals(propertyId)
                : PULLING.equals(propertyId)
                        || PULL.equals(propertyId)
                        || CHARGED.equals(propertyId)
                        || FIREWORK.equals(propertyId);

        if (!relevant) {
            return;
        }

        ItemPropertyFunction original = cir.getReturnValue();

        cir.setReturnValue((stack, level, entity, seed) -> {
            if (MagicArrowClientVisuals.isMagicArrowCasting(entity)
                    && MagicArrowClientVisuals.isSelectedRangedStack(
                    entity,
                    stack
            )) {
                if (PULLING.equals(propertyId)) {
                    return 1.0F;
                }

                if (PULL.equals(propertyId)) {
                    return MagicArrowClientVisuals
                            .getCastProgress(entity);
                }

                if (crossbow && (CHARGED.equals(propertyId)
                        || FIREWORK.equals(propertyId))) {
                    return 0.0F;
                }
            }

            return original == null
                    ? 0.0F
                    : original.call(stack, level, entity, seed);
        });
    }
}
