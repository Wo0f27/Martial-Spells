package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.item.MonkCodexItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Rarity;

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

    public static final RegistryObject<Item>
            MONK_CODEX_ADVANCEMENT_II =
            ITEMS.register(
                    "monk_codex_advancement_ii",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(16)
                                    .rarity(Rarity.UNCOMMON)
                    )
            );

    public static final RegistryObject<Item>
            MONK_CODEX_ADVANCEMENT_III =
            ITEMS.register(
                    "monk_codex_advancement_iii",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(16)
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final RegistryObject<Item>
            MONK_CODEX_ADVANCEMENT_IV =
            ITEMS.register(
                    "monk_codex_advancement_iv",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(16)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item>
            MONK_CODEX_ADVANCEMENT_V =
            ITEMS.register(
                    "monk_codex_advancement_v",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(16)
                                    .rarity(Rarity.EPIC)
                    )
            );

    private MartialItemRegistry() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}