package com.w0of26.martialspells.registry;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.recipe.MonkCodexSmithingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers Martial Spells recipe serializers.
 */
public final class MartialRecipeRegistry {
    private static final DeferredRegister<
            RecipeSerializer<?>>
            RECIPE_SERIALIZERS =
            DeferredRegister.create(
                    ForgeRegistries
                            .RECIPE_SERIALIZERS,
                    MartialSpells.MOD_ID
            );

    public static final RegistryObject<
            RecipeSerializer<
                    MonkCodexSmithingRecipe>>
            MONK_CODEX_UPGRADE =
            RECIPE_SERIALIZERS.register(
                    "monk_codex_upgrade",
                    MonkCodexSmithingRecipe
                            .Serializer::new
            );

    private MartialRecipeRegistry() {
    }

    public static void register(
            IEventBus eventBus
    ) {
        RECIPE_SERIALIZERS.register(
                eventBus
        );
    }
}