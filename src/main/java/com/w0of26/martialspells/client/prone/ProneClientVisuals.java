package com.w0of26.martialspells.client.prone;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ProneClientVisuals {

    private ProneClientVisuals() {
    }

    public static void setProne(
            int entityId,
            boolean active
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Entity entity =
                minecraft.level.getEntity(
                        entityId
                );

        if (!(entity
                instanceof LivingEntity living)) {

            MartialSpells.LOGGER.warn(
                    "Could not resolve Prone visual entity "
                            + "with id {} on the client.",
                    entityId
            );

            return;
        }

        ProneAnimationFamily family =
                ProneClientAnimationRegistry
                        .getFamily(
                                living
                        );

        switch (family) {

            case BIPED -> {
                if (active) {
                    ProneBipedAnimator.play(
                            living
                    );
                } else {
                    ProneBipedAnimator.stop(
                            living
                    );
                }
            }

            /*
             * Dedicated visual implementations later.
             *
             * Gameplay is completely unaffected.
             */
            case ENDERMAN,
                 SPIDER,
                 NONE -> {
            }
        }
    }
}