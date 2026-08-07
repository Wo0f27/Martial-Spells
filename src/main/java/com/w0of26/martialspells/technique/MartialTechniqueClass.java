package com.w0of26.martialspells.technique;

/**
 * Identifies the martial archetype that owns a technique.
 *
 * The Martial spell school is shared by multiple archetypes,
 * while this classification preserves their class identity.
 */
public enum MartialTechniqueClass {
    MONK,
    FIGHTER,
    BARBARIAN
}