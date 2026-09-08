package com.w0of26.martialspells.quivering;

import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final String RETIRED_TARGETS_TAG =
            "retiredTargets";
    private static final String TARGET_TAG = "target";
    private static final String GENERATION_TAG = "generation";

    private final Map<UUID, Integer> marks =
            new LinkedHashMap<>();

    /*
     * Targets from earlier generations are retired permanently for the
     * current chain. This prevents Quivering Palm from bouncing back to
     * an entity that was already part of an earlier generation.
     */
    private final Set<UUID> retiredTargets =
            new LinkedHashSet<>();

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

    public boolean hasEverBeenMarked(UUID targetUuid) {
        if (targetUuid == null) {
            return false;
        }

        return marks.containsKey(targetUuid)
                || retiredTargets.contains(targetUuid);
    }

    public int getGeneration(UUID targetUuid) {
        return marks.getOrDefault(targetUuid, -1);
    }

    public Set<UUID> getMarkedTargets() {
        return Set.copyOf(marks.keySet());
    }

    /**
     * Replaces the currently selectable mark set with a new generation.
     * Used for the initial Generation 0 mark.
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
            if (targetUuid != null
                    && !retiredTargets.contains(targetUuid)) {
                marks.put(targetUuid, activeGeneration);
            }
        }
    }

    /**
     * Advances the chain to the next selectable generation.
     *
     * Every candidate from the previous generation is retired, not only
     * the chosen detonation target. This deliberately makes each
     * generation a single branch choice and prevents exponential or
     * ping-pong chains.
     */
    public void advanceMarks(
            Collection<UUID> targetUuids,
            int generation
    ) {
        retiredTargets.addAll(marks.keySet());
        replaceMarks(targetUuids, generation);
    }

    /**
     * Retires the final active generation when no further propagation is
     * allowed.
     */
    public void retireCurrentMarks() {
        retiredTargets.addAll(marks.keySet());
        marks.clear();
    }

    @Override
    public void reset() {
        marks.clear();
        retiredTargets.clear();
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

        buffer.writeVarInt(retiredTargets.size());
        for (UUID targetUuid : retiredTargets) {
            buffer.writeUUID(targetUuid);
        }
    }

    @Override
    public void readFromBuffer(FriendlyByteBuf buffer) {
        marks.clear();
        retiredTargets.clear();
        activeGeneration = buffer.readVarInt();

        int markCount = buffer.readVarInt();
        for (int i = 0; i < markCount; i++) {
            UUID targetUuid = buffer.readUUID();
            int generation = buffer.readVarInt();
            marks.put(targetUuid, generation);
        }

        int retiredCount = buffer.readVarInt();
        for (int i = 0; i < retiredCount; i++) {
            retiredTargets.add(buffer.readUUID());
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

        ListTag serializedRetiredTargets =
                new ListTag();
        for (UUID targetUuid : retiredTargets) {
            CompoundTag serializedTarget =
                    new CompoundTag();
            serializedTarget.putUUID(
                    TARGET_TAG,
                    targetUuid
            );
            serializedRetiredTargets.add(
                    serializedTarget
            );
        }

        result.put(
                RETIRED_TARGETS_TAG,
                serializedRetiredTargets
        );
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

        ListTag serializedRetiredTargets =
                nbt.getList(
                        RETIRED_TARGETS_TAG,
                        Tag.TAG_COMPOUND
                );

        for (Tag rawTag : serializedRetiredTargets) {
            if (!(rawTag
                    instanceof CompoundTag serializedTarget)) {
                continue;
            }

            if (serializedTarget.hasUUID(TARGET_TAG)) {
                retiredTargets.add(
                        serializedTarget.getUUID(
                                TARGET_TAG
                        )
                );
            }
        }
    }
}
