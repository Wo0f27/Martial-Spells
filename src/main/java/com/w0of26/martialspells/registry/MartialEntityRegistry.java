package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.BarrageArrow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MartialEntityRegistry {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MartialSpells.MOD_ID);

    public static final RegistryObject<EntityType<BarrageArrow>> BARRAGE_ARROW =
            ENTITY_TYPES.register("barrage_arrow", () -> EntityType.Builder
                    .<BarrageArrow>of(BarrageArrow::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build(MartialSpells.MOD_ID + ":barrage_arrow"));

    private MartialEntityRegistry() {}

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
