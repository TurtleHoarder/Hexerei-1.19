package net.joefoxe.hexerei.data.tags;

import net.joefoxe.hexerei.Hexerei;
import net.joefoxe.hexerei.world.biome.ModBiomes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.tags.BiomeTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBiomeTagsProvider extends BiomeTagsProvider {

	public ModBiomeTagsProvider(DataGenerator dataGen, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
		super(dataGen.getPackOutput(),lookupProvider, Hexerei.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider pProvider) {
		/*tag(BiomeTags.IS_OVERWORLD).addTag(ModBiomes.WILLOW_SWAMP);
		tag(Tags.Biomes.IS_WET_OVERWORLD).add(ModBiomes.WILLOW_SWAMP.get());
		tag(Tags.Biomes.IS_SWAMP).add(ModBiomes.WILLOW_SWAMP.get());

		tag(BiomeTags.WATER_ON_MAP_OUTLINES).add(ModBiomes.WILLOW_SWAMP.get());
		tag(BiomeTags.HAS_SWAMP_HUT).add(ModBiomes.WILLOW_SWAMP.get());
		tag(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS).add(ModBiomes.WILLOW_SWAMP.get());
		tag(BiomeTags.HAS_CLOSER_WATER_FOG).add(ModBiomes.WILLOW_SWAMP.get()); */

	}
}
