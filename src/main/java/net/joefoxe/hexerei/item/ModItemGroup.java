package net.joefoxe.hexerei.item;

import net.joefoxe.hexerei.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroup {
    public static final CreativeModeTab HEXEREI_GROUP = CreativeModeTab.builder().title(Component.literal("hexereiModTab")).build();
    {
        //@Override
        //public ItemStack makeIcon() {
        //    return new ItemStack(ModBlocks.MIXING_CAULDRON.get().asItem());
       // }

    };
}
