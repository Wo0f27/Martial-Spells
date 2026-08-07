package com.w0of26.martialspells.technique;

/**
 * Marker API for spells that represent Martial techniques.
 *
 * Implementations declare their owning martial archetype
 * independently from the spell school itself.
 */
public interface MartialTechnique {

    MartialTechniqueClass getTechniqueClass();
}