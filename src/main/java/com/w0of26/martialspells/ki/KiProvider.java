package com.w0of26.martialspells.ki;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes KiData as a player capability and handles its persistent NBT.
 */
public final class KiProvider
        implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "ki"
            );

    private final KiData data = new KiData();

    private final LazyOptional<KiData> optional =
            LazyOptional.of(() -> data);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability,
            @Nullable Direction side
    ) {
        if (capability == MartialCapabilities.KI) {
            return optional.cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.deserializeNBT(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}