package com.w0of26.martialspells.quivering;

import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serializable state carried by Iron's native recast system for one
 * Quivering Palm chain.
 *
 * The recast instance is the owner of this data. That keeps the mark
 * state tied to the same lifecycle that drives Iron's recast bar,
 * including logout persistence and timeout handling, without a global
 * per-tick entity scan.
 */
public final class QuiveringPalmCastData
        implements ICastDataSerializable {

    private static final String ACTIVE_GENERATION_TAG =
            "activeGeneration";
    private static final String MARKS_TAG = "marks";
    private static final String TARGET_TAG = "target";
    private static final String GENERATION_TAG = "generation";

    private final Map<UUID, Integer> marks =
            new LinkedHashMap<>();

    private int activeGeneration;

    public QuiveringPalmCastData() {
    }

    public int getActiveGeneration() {
        return activeGeneration;
    }

    public boolean isActiveMark(UUID targetUuid) {
        if (targetUuid == null) {
            return false;
        }

        Integer generation = marks.get(targetUuid);
        return generation != null
                && generation == activeGeneration;
    }

    public int getGeneration(UUID targetUuid) {
        return marks.getOrDefault(targetUuid, -1);
    }

    public Set<UUID> getMarkedTargets() {
        return Set.copyOf(marks.keySet());
    }

    /**
     * Replaces the currently selectable mark set with a new generation.
     *
     * Checkpoint 1 uses this with the same target while the native
     * recast lifecycle is validated. The detonation checkpoint will
     * replace that target with the actual AOE propagation candidates.
     */
    public void replaceMarks(
            Collection<UUID> targetUuids,
            int generation
    ) {
        marks.clear();
        activeGeneration = Math.max(0, generation);

        if (targetUuids == null) {
            return;
        }

        for (UUID targetUuid : targetUuids) {
            if (targetUuid != null) {
                marks.put(targetUuid, activeGeneration);
            }
        }
    }

    @Override
    public void reset() {
        marks.clear();
        activeGeneration = 0;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buffer) {
        buffer.writeVarInt(activeGeneration);
        buffer.writeVarInt(marks.size());

        for (Map.Entry<UUID, Integer> entry
                : marks.entrySet()) {
            buffer.writeUUID(entry.getKey());
            buffer.writeVarInt(entry.getValue());
        }
    }

    @Override
    public void readFromBuffer(FriendlyByteBuf buffer) {
        marks.clear();
        activeGeneration = buffer.readVarInt();

        int markCount = buffer.readVarInt();
        for (int i = 0; i < markCount; i++) {
            UUID targetUuid = buffer.readUUID();
            int generation = buffer.readVarInt();
            marks.put(targetUuid, generation);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag result = new CompoundTag();
        result.putInt(
                ACTIVE_GENERATION_TAG,
                activeGeneration
        );

        ListTag serializedMarks = new ListTag();
        for (Map.Entry<UUID, Integer> entry
                : marks.entrySet()) {
            CompoundTag serializedMark =
                    new CompoundTag();

            serializedMark.putUUID(
                    TARGET_TAG,
                    entry.getKey()
            );
            serializedMark.putInt(
                    GENERATION_TAG,
                    entry.getValue()
            );
            serializedMarks.add(serializedMark);
        }

        result.put(MARKS_TAG, serializedMarks);
        return result;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        reset();

        if (nbt == null) {
            return;
        }

        activeGeneration = Math.max(
                0,
                nbt.getInt(ACTIVE_GENERATION_TAG)
        );

        ListTag serializedMarks =
                nbt.getList(
                        MARKS_TAG,
                        Tag.TAG_COMPOUND
                );

        for (Tag rawTag : serializedMarks) {
            if (!(rawTag
                    instanceof CompoundTag serializedMark)) {
                continue;
            }

            if (!serializedMark.hasUUID(TARGET_TAG)) {
                continue;
            }

            marks.put(
                    serializedMark.getUUID(TARGET_TAG),
                    Math.max(
                            0,
                            serializedMark.getInt(
                                    GENERATION_TAG
                            )
                    )
            );
        }
    }
}
