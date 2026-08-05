package com.w0of26.martialspells.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.w0of26.martialspells.item.MonkCodexItem;
import com.w0of26.martialspells.item.MonkCodexTier;
import com.w0of26.martialspells.item.MonkCodexUpgradeHelper;
import com.w0of26.martialspells.registry.MartialItemRegistry;
import com.w0of26.martialspells.registry.MartialRecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Dynamically upgrades a Monk Codex while preserving the complete
 * ItemStack and its existing Iron's spell container.
 *
 * Smithing slots:
 * 0 = advancement template
 * 1 = existing Monk Codex
 * 2 = advancement material
 */
public final class MonkCodexSmithingRecipe
        implements SmithingRecipe {

    private static final int TEMPLATE_SLOT = 0;
    private static final int BASE_SLOT = 1;
    private static final int ADDITION_SLOT = 2;
    private static final int REQUIRED_SLOT_COUNT = 3;

    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final MonkCodexTier targetTier;
    private final ItemStack displayResult;

    public MonkCodexSmithingRecipe(
            ResourceLocation id,
            Ingredient template,
            Ingredient base,
            Ingredient addition,
            MonkCodexTier targetTier
    ) {
        this.id = id;
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.targetTier = targetTier;
        this.displayResult =
                createDisplayResult(targetTier);
    }

    @Override
    public boolean matches(
            Container container,
            Level level
    ) {
        if (!hasRequiredSlots(container)) {
            return false;
        }

        ItemStack templateStack =
                container.getItem(TEMPLATE_SLOT);

        ItemStack baseStack =
                container.getItem(BASE_SLOT);

        ItemStack additionStack =
                container.getItem(ADDITION_SLOT);

        return template.test(templateStack)
                && base.test(baseStack)
                && addition.test(additionStack)
                && isCorrectTransition(baseStack);
    }

    @Override
    public ItemStack assemble(
            Container container,
            RegistryAccess registryAccess
    ) {
        if (!hasRequiredSlots(container)) {
            return ItemStack.EMPTY;
        }

        ItemStack templateStack =
                container.getItem(TEMPLATE_SLOT);

        ItemStack baseStack =
                container.getItem(BASE_SLOT);

        ItemStack additionStack =
                container.getItem(ADDITION_SLOT);

        /*
         * Revalidate every input. The recipe must never upgrade an
         * invalid Codex if assemble is called independently.
         */
        if (!template.test(templateStack)
                || !base.test(baseStack)
                || !addition.test(additionStack)
                || !isCorrectTransition(baseStack)) {
            return ItemStack.EMPTY;
        }

        /*
         * Never mutate the smithing-table input directly.
         */
        ItemStack result =
                baseStack.copy();

        result.setCount(1);

        MonkCodexUpgradeHelper.UpgradeResult
                upgradeResult =
                MonkCodexUpgradeHelper
                        .upgradeToNextTier(result);

        if (upgradeResult
                != MonkCodexUpgradeHelper
                .UpgradeResult.SUCCESS) {
            return ItemStack.EMPTY;
        }

        /*
         * This guards against an unexpected mismatch between the
         * recipe target and the helper's resulting tier.
         */
        if (MonkCodexItem.getTier(result)
                != targetTier) {
            return ItemStack.EMPTY;
        }

        return result;
    }

    private boolean isCorrectTransition(
            ItemStack baseStack
    ) {
        if (baseStack.isEmpty()
                || !(baseStack.getItem()
                instanceof MonkCodexItem)) {
            return false;
        }

        MonkCodexTier currentTier =
                MonkCodexItem.getTier(baseStack);

        if (currentTier == MonkCodexTier.TIER_V) {
            return false;
        }

        return currentTier.getNextTier()
                == targetTier;
    }

    private static boolean hasRequiredSlots(
            Container container
    ) {
        return container != null
                && container.getContainerSize()
                >= REQUIRED_SLOT_COUNT;
    }

    @Override
    public boolean isTemplateIngredient(
            ItemStack itemStack
    ) {
        return template.test(itemStack);
    }

    @Override
    public boolean isBaseIngredient(
            ItemStack itemStack
    ) {
        return base.test(itemStack);
    }

    @Override
    public boolean isAdditionIngredient(
            ItemStack itemStack
    ) {
        return addition.test(itemStack);
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return true;
    }

    @Override
    public NonNullList<Ingredient>
    getIngredients() {
        NonNullList<Ingredient> ingredients =
                NonNullList.create();

        ingredients.add(template);
        ingredients.add(base);
        ingredients.add(addition);

        return ingredients;
    }

    @Override
    public ItemStack getResultItem(
            RegistryAccess registryAccess
    ) {
        return displayResult.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?>
    getSerializer() {
        return MartialRecipeRegistry
                .MONK_CODEX_UPGRADE
                .get();
    }

    /**
     * Produces a generic target-tier Codex for recipe viewers.
     *
     * The actual smithing output is always assembled from the
     * player's real Codex.
     */
    private static ItemStack createDisplayResult(
            MonkCodexTier targetTier
    ) {
        ItemStack result =
                new ItemStack(
                        MartialItemRegistry
                                .MONK_CODEX
                                .get()
                );

        if (!(result.getItem()
                instanceof MonkCodexItem codexItem)) {
            return result;
        }

        codexItem.initializeSpellContainer(result);

        while (MonkCodexItem.getTier(result)
                != targetTier) {

            MonkCodexUpgradeHelper.UpgradeResult
                    upgradeResult =
                    MonkCodexUpgradeHelper
                            .upgradeToNextTier(result);

            if (upgradeResult
                    != MonkCodexUpgradeHelper
                    .UpgradeResult.SUCCESS) {
                break;
            }
        }

        return result;
    }

    public static final class Serializer
            implements RecipeSerializer<
            MonkCodexSmithingRecipe> {

        @Override
        public MonkCodexSmithingRecipe fromJson(
                ResourceLocation recipeId,
                JsonObject json
        ) {
            Ingredient template =
                    readIngredient(
                            json,
                            "template"
                    );

            Ingredient base =
                    readIngredient(
                            json,
                            "base"
                    );

            Ingredient addition =
                    readIngredient(
                            json,
                            "addition"
                    );

            int targetTierId =
                    GsonHelper.getAsInt(
                            json,
                            "target_tier"
                    );

            MonkCodexTier targetTier =
                    readTargetTier(
                            targetTierId
                    );

            return new MonkCodexSmithingRecipe(
                    recipeId,
                    template,
                    base,
                    addition,
                    targetTier
            );
        }

        @Override
        @Nullable
        public MonkCodexSmithingRecipe fromNetwork(
                ResourceLocation recipeId,
                FriendlyByteBuf buffer
        ) {
            Ingredient template =
                    Ingredient.fromNetwork(buffer);

            Ingredient base =
                    Ingredient.fromNetwork(buffer);

            Ingredient addition =
                    Ingredient.fromNetwork(buffer);

            MonkCodexTier targetTier =
                    readTargetTier(
                            buffer.readVarInt()
                    );

            return new MonkCodexSmithingRecipe(
                    recipeId,
                    template,
                    base,
                    addition,
                    targetTier
            );
        }

        @Override
        public void toNetwork(
                FriendlyByteBuf buffer,
                MonkCodexSmithingRecipe recipe
        ) {
            recipe.template.toNetwork(buffer);
            recipe.base.toNetwork(buffer);
            recipe.addition.toNetwork(buffer);

            buffer.writeVarInt(
                    recipe.targetTier
                            .getSerializedId()
            );
        }

        private static Ingredient readIngredient(
                JsonObject json,
                String memberName
        ) {
            JsonElement element =
                    json.get(memberName);

            if (element == null) {
                throw new JsonSyntaxException(
                        "Missing smithing ingredient: "
                                + memberName
                );
            }

            return Ingredient.fromJson(element);
        }

        private static MonkCodexTier readTargetTier(
                int serializedId
        ) {
            MonkCodexTier tier =
                    MonkCodexTier
                            .fromSerializedId(
                                    serializedId
                            );

            /*
             * Reject Tier I and invalid values that fell back to
             * Tier I.
             */
            if (tier == MonkCodexTier.TIER_I
                    || tier.getSerializedId()
                    != serializedId) {
                throw new JsonSyntaxException(
                        "Invalid Monk Codex target tier: "
                                + serializedId
                );
            }

            return tier;
        }
    }
}