package net.joefoxe.hexerei.data.recipes;

import com.google.gson.JsonObject;
import net.joefoxe.hexerei.item.custom.CrowFluteItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nonnull;


public class CrowFluteRecipe extends ShapedRecipe {

    NonNullList<Ingredient> inputs;
    ItemStack output;

    public CrowFluteRecipe(ShapedRecipe compose) {
        super(compose.getId(), compose.getGroup(), CraftingBookCategory.MISC,compose.getWidth(), compose.getHeight(), compose.getIngredients(), compose.getResultItem(null));


        this.inputs = compose.getIngredients();
        this.output = compose.getResultItem(null);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
    @Nonnull
    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        int first = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            Item item = stack.getItem();

            int colorId;
            if (item instanceof DyeItem dye) {
                colorId = dye.getDyeColor().getId();
            } else {
                continue;
            }
            if (first == -1) {
                first = colorId;
            } else {
                return CrowFluteItem.withColors(first, colorId);
            }
        }
        return CrowFluteItem.withColors(first != -1 ? first : 0, 0);
    }





    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return getOutput();
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public NonNullList<Ingredient> getInputs() {
        return inputs;
    }

    public static class Serializer implements RecipeSerializer<CrowFluteRecipe> {
        @Nonnull
        @Override
        public CrowFluteRecipe fromJson(@Nonnull ResourceLocation recipeId, @Nonnull JsonObject json) {
            return new CrowFluteRecipe(SHAPED_RECIPE.fromJson(recipeId, json));
        }

        @Nonnull
        @Override
        public CrowFluteRecipe fromNetwork(@Nonnull ResourceLocation recipeId, @Nonnull FriendlyByteBuf buffer) {
            return new CrowFluteRecipe(SHAPED_RECIPE.fromNetwork(recipeId, buffer));
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull CrowFluteRecipe recipe) {
            SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.CROW_FLUTE_DYE_SERIALIZER.get();
    }
}