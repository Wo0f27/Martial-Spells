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
 * Render-only predicate overrides for Magic Arrow.
 *
 * Vanilla item models normally decide whether a bow is pulling or a
 * crossbow is charged from normal item-use state/NBT. Iron's spell
 * casting does not use those states, so Magic Arrow needs a visual-only
 * bridge while it is being channeled.
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
                        || CHARGED.equals(propertyId)
                        || FIREWORK.equals(propertyId);

        if (!relevant) {
            return;
        }

        ItemPropertyFunction original = cir.getReturnValue();

        cir.setReturnValue((stack, level, entity, seed) -> {
            if (MagicArrowClientVisuals.isMagicArrowCasting(entity)
                    && MagicArrowClientVisuals.isHeldRangedStack(
                    entity,
                    stack
            )) {
                if (bow) {
                    if (PULLING.equals(propertyId)) {
                        return 1.0F;
                    }

                    if (PULL.equals(propertyId)) {
                        return MagicArrowClientVisuals
                                .getBowPullProgress(entity);
                    }
                }

                if (crossbow) {
                    if (CHARGED.equals(propertyId)) {
                        return 1.0F;
                    }

                    if (FIREWORK.equals(propertyId)
                            || PULLING.equals(propertyId)) {
                        return 0.0F;
                    }
                }
            }

            return original == null
                    ? 0.0F
                    : original.call(stack, level, entity, seed);
        });
    }
}
