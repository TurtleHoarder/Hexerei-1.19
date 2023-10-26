package net.joefoxe.hexerei.data.recipes;

import net.joefoxe.hexerei.item.custom.KeychainItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;


public class KeychainRecipe extends CustomRecipe {
    public static final SimpleCraftingRecipeSerializer<KeychainRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(KeychainRecipe::new);

    public KeychainRecipe(ResourceLocation registryName, CraftingBookCategory pCategory) {
        super(registryName, pCategory);


    }

    @Override
    public boolean isSpecial() {
        return true;
    }
    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        int keychain = 0;
        int other = 0;

        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof KeychainItem) {
                    CompoundTag tag = stack.getOrCreateTag();

                    if(!tag.contains("Items")){
                        ++keychain;
                    }
                } else {
                    ++other;
                }

                if (other > 1 || keychain > 1) {
                    return false;
                }
            }
        }

        return keychain == 1 && other == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess access) {
        ItemStack keychain = ItemStack.EMPTY;
        ItemStack other = ItemStack.EMPTY;

        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof KeychainItem) {
                    keychain = stack.copy();
                    keychain.setCount(1);
                } else {
                    other = stack.copy();
                    other.setCount(1);
                }
            }
        }
        if (keychain.getItem() instanceof KeychainItem && !other.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            if(keychain.hasTag())
                tag = keychain.getTag();

            ListTag listtag = new ListTag();

            if (!other.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte)0);
                other.save(compoundtag);
                listtag.add(compoundtag);

            }

            tag.put("Items", listtag);

            keychain.setTag(tag);
        }

        return keychain;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 1;
    }
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.KEYCHAIN_APPLY_SERIALIZER.get();
    }
}