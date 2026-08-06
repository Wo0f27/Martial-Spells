package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import io.redspace.ironsspellbooks.api.attribute.MagicRangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers the attributes used by the Martial spell school.
 *
 * Both attributes use 1.0 as their neutral value, matching Iron's
 * school-power and school-resistance attribute conventions.
 */
public final class MartialAttributeRegistry {

    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(
                    ForgeRegistries.ATTRIBUTES,
                    MartialSpells.MOD_ID
            );

    public static final RegistryObject<Attribute>
            MARTIAL_SPELL_POWER =
            ATTRIBUTES.register(
                    "martial_spell_power",
                    () -> new MagicRangedAttribute(
                            "attribute.martial_spells."
                                    + "martial_spell_power",
                            1.0D,
                            -100.0D,
                            100.0D
                    ).setSyncable(true)
            );

    public static final RegistryObject<Attribute>
            MARTIAL_MAGIC_RESIST =
            ATTRIBUTES.register(
                    "martial_magic_resist",
                    () -> new MagicRangedAttribute(
                            "attribute.martial_spells."
                                    + "martial_magic_resist",
                            1.0D,
                            -100.0D,
                            100.0D
                    ).setSyncable(true)
            );

    private MartialAttributeRegistry() {
    }

    public static void register(
            IEventBus modEventBus
    ) {
        ATTRIBUTES.register(modEventBus);

        modEventBus.addListener(
                MartialAttributeRegistry
                        ::addAttributesToEntities
        );
    }

    /**
     * School attributes must be attached to entities before they can
     * be queried through LivingEntity#getAttributeValue.
     */
    private static void addAttributesToEntities(
            EntityAttributeModificationEvent event
    ) {
        event.getTypes().forEach(entityType -> {
            event.add(
                    entityType,
                    MARTIAL_SPELL_POWER.get()
            );

            event.add(
                    entityType,
                    MARTIAL_MAGIC_RESIST.get()
            );
        });
    }
}