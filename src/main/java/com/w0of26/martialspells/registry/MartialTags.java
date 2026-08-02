package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class MartialTags {
    private MartialTags() {
    }

    public static final class Items {
        public static final TagKey<Item> MONK_WEAPONS =
                create("monk_weapons");

        public static final TagKey<Item> GAUNTLETS =
                create("gauntlets");

        public static final TagKey<Item> QUARTERSTAFFS =
                create("quarterstaffs");

        private Items() {
        }

        private static TagKey<Item> create(String path) {
            return TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            MartialSpells.MOD_ID,
                            path
                    )
            );
        }
    }
}