package com.w0of26.martialspells.tag;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class MartialEntityTypeTags {
    public static final TagKey<EntityType<?>>
            GUARDIANS_CRY_EFFECT_IMMUNE_COMPATIBLE =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "guardians_cry_effect_immune_compatible"
                    )
            );

    private MartialEntityTypeTags() {
    }
}