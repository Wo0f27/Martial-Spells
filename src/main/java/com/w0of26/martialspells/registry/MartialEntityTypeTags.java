package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class MartialEntityTypeTags {

    /*
     * Entities in this tag receive half of Stunning Strike's
     * normal stun duration.
     */
    public static final TagKey<EntityType<?>>
            STUN_RESISTANT =
            create(
                    "stun_resistant"
            );

    /*
     * Entities in this tag never receive Stunning Strike's
     * Stunned effect.
     *
     * Rend and damage are unaffected.
     */
    public static final TagKey<EntityType<?>>
            STUN_IMMUNE =
            create(
                    "stun_immune"
            );

    /*
     * Quivering Palm bosses use capped percentage-health damage
     * plus a fixed Martial component instead of unrestricted scaling.
     * Datapacks and modpack integrations may extend this tag.
     */
    public static final TagKey<EntityType<?>>
            QUIVERING_PALM_BOSS =
            create(
                    "quivering_palm_bosses"
            );

    private MartialEntityTypeTags() {
    }

    private static TagKey<EntityType<?>> create(
            String path
    ) {
        return TagKey.create(
                Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(
                        MartialSpells.MOD_ID,
                        path
                )
        );
    }
}
