package com.w0of26.martialspells.ranged;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared Ranger-facing classification for bow and crossbow items.
 *
 * The Martial Spells tags are the primary extension point. Their JSON
 * definitions include Forge's standard tool tags and can be extended by
 * any datapack. Vanilla-style class inheritance remains as a fallback so
 * ordinary BowItem/CrossbowItem subclasses work even when a third-party
 * mod forgets to tag them.
 */
public final class RangedWeaponClassifier {
    public static final TagKey<Item> BOWS =
            ItemTags.create(
                    ResourceLocation.fromNamespaceAndPath(
                            MartialSpells.MOD_ID,
                            "ranged/bows"
                    )
            );

    public static final TagKey<Item> CROSSBOWS =
            ItemTags.create(
                    ResourceLocation.fromNamespaceAndPath(
                            MartialSpells.MOD_ID,
                            "ranged/crossbows"
                    )
            );

    private RangedWeaponClassifier() {
    }

    public static Type classify(ItemStack stack) {
        if (stack.isEmpty()) {
            return Type.NONE;
        }

        return classify(stack.getItem());
    }

    @SuppressWarnings("deprecation")
    public static Type classify(Item item) {
        /*
         * Datapack classification intentionally wins over Java class
         * inheritance. This lets an unusual third-party weapon opt into
         * the correct Ranger presentation without a code dependency.
         * If an item somehow appears in both tags, crossbow wins because
         * its casting presentation is more specialized.
         */
        if (item.builtInRegistryHolder().is(CROSSBOWS)) {
            return Type.CROSSBOW;
        }

        if (item.builtInRegistryHolder().is(BOWS)) {
            return Type.BOW;
        }

        if (item instanceof CrossbowItem) {
            return Type.CROSSBOW;
        }

        if (item instanceof BowItem) {
            return Type.BOW;
        }

        return Type.NONE;
    }

    public static boolean isBow(ItemStack stack) {
        return classify(stack) == Type.BOW;
    }

    public static boolean isBow(Item item) {
        return classify(item) == Type.BOW;
    }

    public static boolean isCrossbow(ItemStack stack) {
        return classify(stack) == Type.CROSSBOW;
    }

    public static boolean isCrossbow(Item item) {
        return classify(item) == Type.CROSSBOW;
    }

    public static boolean isSupported(ItemStack stack) {
        return classify(stack) != Type.NONE;
    }

    public enum Type {
        NONE,
        BOW,
        CROSSBOW
    }
}
