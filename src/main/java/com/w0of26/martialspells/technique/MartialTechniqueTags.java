package com.w0of26.martialspells.technique;

import com.w0of26.martialspells.registry.MartialSpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.tags.TagKey;

/**
 * Spell-registry tags used to expose Martial technique
 * classifications to datapacks and compatibility systems.
 *
 * Java's MartialTechnique interface remains the authoritative
 * gameplay classification for this mod.
 */
public final class MartialTechniqueTags {

    public static final TagKey<AbstractSpell>
            MARTIAL_TECHNIQUES =
            MartialSpellRegistry.SPELLS.createTagKey(
                    "martial_techniques"
            );

    public static final TagKey<AbstractSpell>
            MONK_TECHNIQUES =
            MartialSpellRegistry.SPELLS.createTagKey(
                    "monk_techniques"
            );

    public static final TagKey<AbstractSpell>
            FIGHTER_TECHNIQUES =
            MartialSpellRegistry.SPELLS.createTagKey(
                    "fighter_techniques"
            );

    public static final TagKey<AbstractSpell>
            BARBARIAN_TECHNIQUES =
            MartialSpellRegistry.SPELLS.createTagKey(
                    "barbarian_techniques"
            );

    private MartialTechniqueTags() {
    }
}