package com.w0of26.martialspells.client.prone;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ProneClientAnimationRegistry {

    private static final Map<
            EntityType<?>,
            ProneAnimationFamily
            > ENTITY_FAMILIES =
            new IdentityHashMap<>();


    static {
        registerDefaults();
    }


    private ProneClientAnimationRegistry() {
    }


    public static void registerEntity(
            EntityType<?> entityType,
            ProneAnimationFamily family
    ) {
        if (entityType == null
                || family == null) {
            throw new IllegalArgumentException(
                    "Prone animation registration "
                            + "cannot contain null values."
            );
        }

        ENTITY_FAMILIES.put(
                entityType,
                family
        );
    }


    public static ProneAnimationFamily getFamily(
            LivingEntity entity
    ) {
        return ENTITY_FAMILIES.getOrDefault(
                entity.getType(),
                ProneAnimationFamily.NONE
        );
    }


    public static boolean usesBipedAnimator(
            LivingEntity entity
    ) {
        return getFamily(entity)
                == ProneAnimationFamily.BIPED;
    }


    private static void registerDefaults() {

        registerEntity(
                EntityType.PLAYER,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.ZOMBIE,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.ZOMBIE_VILLAGER,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.HUSK,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.DROWNED,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.SKELETON,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.STRAY,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.WITHER_SKELETON,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.ZOMBIFIED_PIGLIN,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.PIGLIN,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.PIGLIN_BRUTE,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.PILLAGER,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.VINDICATOR,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.EVOKER,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.WITCH,
                ProneAnimationFamily.BIPED
        );

        registerEntity(
                EntityType.ENDERMAN,
                ProneAnimationFamily.ENDERMAN
        );

        registerEntity(
                EntityType.SPIDER,
                ProneAnimationFamily.SPIDER
        );

        registerEntity(
                EntityType.CAVE_SPIDER,
                ProneAnimationFamily.SPIDER
        );
    }
}