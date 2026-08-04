package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.item.MonkCodexItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers items belonging to Martial Spells.
 */
public final class MartialItemRegistry {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    MartialSpells.MOD_ID
            );

    public static final RegistryObject<MonkCodexItem> MONK_CODEX =
            ITEMS.register(
                    "monk_codex",
                    MonkCodexItem::new
            );

    private MartialItemRegistry() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}