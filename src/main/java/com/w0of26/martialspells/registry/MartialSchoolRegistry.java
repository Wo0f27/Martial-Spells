package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers the shared Martial school used by Monk, Fighter,
 * Barbarian, and other physical-technique classes.
 */
public final class MartialSchoolRegistry {

    private static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(
                    SchoolRegistry.SCHOOL_REGISTRY_KEY,
                    MartialSpells.MOD_ID
            );

    public static final ResourceLocation MARTIAL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "martial"
            );

    public static final TagKey<Item> MARTIAL_FOCUS =
            ItemTags.create(
                    ResourceLocation.fromNamespaceAndPath(
                            MartialSpells.MOD_ID,
                            "martial_focus"
                    )
            );

    public static final ResourceKey<DamageType>
            MARTIAL_DAMAGE_TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    MARTIAL_RESOURCE
            );

    public static final RegistryObject<SchoolType> MARTIAL =
            SCHOOLS.register(
                    "martial",
                    () -> new SchoolType(
                            MARTIAL_RESOURCE,
                            MARTIAL_FOCUS,
                            Component.translatable(
                                    "school.martial_spells.martial"
                            ).withStyle(
                                    Style.EMPTY.withColor(
                                            0x8C3FD1
                                    )
                            ),
                            LazyOptional.of(
                                    MartialAttributeRegistry
                                            .MARTIAL_SPELL_POWER
                                            ::get
                            ),
                            LazyOptional.of(
                                    MartialAttributeRegistry
                                            .MARTIAL_MAGIC_RESIST
                                            ::get
                            ),
                            LazyOptional.of(
                                    SoundRegistry
                                            .EVOCATION_CAST
                                            ::get
                            ),
                            MARTIAL_DAMAGE_TYPE
                    )
            );

    private MartialSchoolRegistry() {
    }

    public static void register(
            IEventBus modEventBus
    ) {
        SCHOOLS.register(modEventBus);
    }
}