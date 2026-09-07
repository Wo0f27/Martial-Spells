package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.entity.BarrageArrow;
import com.w0of26.martialspells.entity.CaltropBundleProjectile;
import com.w0of26.martialspells.entity.CaltropFieldEntity;
import com.w0of26.martialspells.entity.EntanglingArrow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MartialEntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MartialSpells.MOD_ID);

    public static final RegistryObject<EntityType<BarrageArrow>> BARRAGE_ARROW =
            ENTITY_TYPES.register("barrage_arrow", () -> EntityType.Builder
                    .<BarrageArrow>of(BarrageArrow::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build(MartialSpells.MOD_ID + ":barrage_arrow"));

    public static final RegistryObject<EntityType<EntanglingArrow>> ENTANGLING_ARROW =
            ENTITY_TYPES.register("entangling_arrow", () -> EntityType.Builder
                    .<EntanglingArrow>of(EntanglingArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build(MartialSpells.MOD_ID + ":entangling_arrow"));

    public static final RegistryObject<EntityType<CaltropBundleProjectile>> CALTROP_BUNDLE =
            ENTITY_TYPES.register("caltrop_bundle", () -> EntityType.Builder
                    .<CaltropBundleProjectile>of(CaltropBundleProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build(MartialSpells.MOD_ID + ":caltrop_bundle"));

    public static final RegistryObject<EntityType<CaltropFieldEntity>> CALTROP_FIELD =
            ENTITY_TYPES.register("caltrop_field", () -> EntityType.Builder
                    .<CaltropFieldEntity>of(CaltropFieldEntity::new, MobCategory.MISC)
                    .sized(5.0F, 0.55F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build(MartialSpells.MOD_ID + ":caltrop_field"));

    private MartialEntityRegistry() {}

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
