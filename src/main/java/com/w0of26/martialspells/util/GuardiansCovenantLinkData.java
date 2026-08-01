package com.w0of26.martialspells.util;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class GuardiansCovenantLinkData {
    public static final String CASTER_UUID_TAG =
            MartialSpells.MOD_ID
                    + "_guardians_covenant_caster";

    public static final String SPELL_LEVEL_TAG =
            MartialSpells.MOD_ID
                    + "_guardians_covenant_level";

    public static final String RADIUS_TAG =
            MartialSpells.MOD_ID
                    + "_guardians_covenant_radius";

    private GuardiansCovenantLinkData() {
    }

    public static void setLink(
            ServerPlayer ally,
            UUID casterUuid,
            int spellLevel,
            double radius
    ) {
        CompoundTag data = ally.getPersistentData();

        data.putUUID(
                CASTER_UUID_TAG,
                casterUuid
        );

        data.putInt(
                SPELL_LEVEL_TAG,
                spellLevel
        );

        data.putDouble(
                RADIUS_TAG,
                radius
        );
    }

    public static boolean hasLink(Entity entity) {
        return entity.getPersistentData()
                .hasUUID(CASTER_UUID_TAG);
    }

    public static UUID getCasterUuid(Entity entity) {
        return entity.getPersistentData()
                .getUUID(CASTER_UUID_TAG);
    }

    public static int getSpellLevel(Entity entity) {
        return entity.getPersistentData()
                .getInt(SPELL_LEVEL_TAG);
    }

    public static double getRadius(Entity entity) {
        return entity.getPersistentData()
                .getDouble(RADIUS_TAG);
    }

    public static void clearLink(Entity entity) {
        CompoundTag data = entity.getPersistentData();

        data.remove(CASTER_UUID_TAG);
        data.remove(SPELL_LEVEL_TAG);
        data.remove(RADIUS_TAG);
    }
}