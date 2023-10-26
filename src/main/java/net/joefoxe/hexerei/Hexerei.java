package net.joefoxe.hexerei;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateProvider;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.joefoxe.hexerei.block.CustomFlintAndSteelDispenserBehavior;
import net.joefoxe.hexerei.block.ModBlocks;
import net.joefoxe.hexerei.block.ModWoodType;
import net.joefoxe.hexerei.client.renderer.CrowPerchRenderer;
import net.joefoxe.hexerei.client.renderer.entity.BroomType;
import net.joefoxe.hexerei.client.renderer.entity.ModEntityTypes;
import net.joefoxe.hexerei.compat.CurioCompat;
import net.joefoxe.hexerei.compat.GlassesCurioRender;
import net.joefoxe.hexerei.config.HexConfig;
import net.joefoxe.hexerei.container.ModContainers;
import net.joefoxe.hexerei.data.books.PageDrawing;
import net.joefoxe.hexerei.data.datagen.ModRecipeProvider;
import net.joefoxe.hexerei.data.recipes.ModRecipeTypes;
import net.joefoxe.hexerei.data.tags.ModBiomeTagsProvider;
import net.joefoxe.hexerei.event.ClientEvents;
import net.joefoxe.hexerei.event.ModLootModifiers;
import net.joefoxe.hexerei.events.*;
import net.joefoxe.hexerei.fluid.ModFluidTypes;
import net.joefoxe.hexerei.fluid.ModFluids;
import net.joefoxe.hexerei.integration.HexereiModNameTooltipCompat;
import net.joefoxe.hexerei.item.ModItems;
import net.joefoxe.hexerei.light.LightManager;
import net.joefoxe.hexerei.particle.ModParticleTypes;
import net.joefoxe.hexerei.screen.*;
import net.joefoxe.hexerei.sounds.ModSounds;
import net.joefoxe.hexerei.tileentity.ModTileEntities;
import net.joefoxe.hexerei.util.*;
import net.joefoxe.hexerei.world.biome.ModBiomes;
import net.joefoxe.hexerei.world.biomemods.ModBiomeModifiers;
import net.joefoxe.hexerei.world.gen.ModFeatures;
import net.joefoxe.hexerei.world.gen.ModPlacedFeatures;
import net.joefoxe.hexerei.world.processor.DarkCovenLegProcessor;
import net.joefoxe.hexerei.world.processor.MangroveTreeLegProcessor;
import net.joefoxe.hexerei.world.processor.NatureCovenLegProcessor;
import net.joefoxe.hexerei.world.processor.WitchHutLegProcessor;
import net.joefoxe.hexerei.world.structure.ModStructures;
import net.joefoxe.hexerei.world.terrablender.ModRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.joefoxe.hexerei.util.ClientProxy.MODEL_SWAPPER;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Hexerei.MOD_ID)
public class Hexerei {

	public static final String MOD_ID = "hexerei";
    private static final Lazy<Registrate> REGISTRATE = Lazy.of(() -> new HexRegistrate(MOD_ID)
    );
    public static boolean curiosLoaded = false;

	static class HexRegistrate extends Registrate {
		protected HexRegistrate(String modid) {
			super(modid);
			this.registerEventListeners(FMLJavaModLoadingContext.get().getModEventBus());
		}

		//prevent blockstate and lang datagen
		@Override
		public <T extends RegistrateProvider> Registrate addDataGenerator(ProviderType<? extends T> type, NonNullConsumer<? extends T> cons) {
			if (type == ProviderType.LANG || type == ProviderType.BLOCKSTATE) return self();
			return super.addDataGenerator(type, cons);
		}
	}

	public static SidedProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);

	public static GlassesZoomKeyPressEvent glassesZoomKeyPressEvent;
	public static boolean entityClicked = false;

	@OnlyIn(Dist.CLIENT)
	public static Font font() {
		if (ClientProxy.fontIndex == 0)
			return Minecraft.getInstance().font;
		else {
			int index = ClientProxy.fontIndex % HexConfig.FONT_LIST.get().size();
			Font toReturn = ClientProxy.fontList.get(HexConfig.FONT_LIST.get().get(index));
			return toReturn == null ? Minecraft.getInstance().font : toReturn;
		}
//		if(clientTicks % 40 > 20)
//			return fontList.values().stream().toList().get(0);
//		return fontList.values().stream().toList().get(1);
//		return font;
	}

	public static Registrate registrate() {
		return REGISTRATE.get();
	}

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
					.disableHtmlEscaping()
					.create();

	// Directly reference a log4j logger.
	public static final Logger LOGGER = LogManager.getLogger();

	public static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder.named(HexereiConstants.CHANNEL_NAME)
					.clientAcceptedVersions(HexereiConstants.PROTOCOL_VERSION::equals)
					.serverAcceptedVersions(HexereiConstants.PROTOCOL_VERSION::equals)
					.networkProtocolVersion(HexereiConstants.PROTOCOL_VERSION::toString)
					.simpleChannel();

	public static StructureProcessorType<WitchHutLegProcessor> WITCH_HUT_LEG_PROCESSOR = () -> WitchHutLegProcessor.CODEC;
	public static StructureProcessorType<DarkCovenLegProcessor> DARK_COVEN_LEG_PROCESSOR = () -> DarkCovenLegProcessor.CODEC;
	public static StructureProcessorType<NatureCovenLegProcessor> NATURE_COVEN_LEG_PROCESSOR = () -> NatureCovenLegProcessor.CODEC;
	public static StructureProcessorType<MangroveTreeLegProcessor> MANGROVE_TREE_LEG_PROCESSOR = () -> MangroveTreeLegProcessor.CODEC;

	public static LinkedList<BlockPos> sageBurningPlateTileList = new LinkedList<>();

	public Hexerei() {


		// Register the setup method for modloading
		IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

//        eventBus.addListener(this::gatherData);

//        eventBus.addListener(HexereiDataGenerator::gatherData);
		//eventBus.addGenericListener(RecipeSerializer.class, ModItems::registerRecipeSerializers);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HexConfig.CLIENT_CONFIG, "Hexerei-client.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HexConfig.COMMON_CONFIG, "Hexerei-common.toml");
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ClientEvents.class));

		ModItems.register(eventBus);
		ModBlocks.register(eventBus);
		ModFluids.register(eventBus);
		ModFluidTypes.register(eventBus);
		ModTileEntities.register(eventBus);
		ModContainers.register(eventBus);
		ModRecipeTypes.register(eventBus);
		ModParticleTypes.PARTICLES.register(eventBus);
		ModFeatures.register(eventBus);
		ModPlacedFeatures.register(eventBus);
		ModStructures.DEFERRED_REGISTRY_STRUCTURE.register(eventBus);
		ModSounds.register(eventBus);
		ModEntityTypes.register(eventBus);
		ModBiomeModifiers.register(eventBus);
		//ModBiomes.register(eventBus);
		//ModBiomes.init();
		ModLootModifiers.init();
		HexereiModNameTooltipCompat.init();

//		BroomType.loadBroomTypes();

		IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

		eventBus.addListener(this::loadComplete);

		eventBus.addListener(this::setup);
		// Register the enqueueIMC method for modloading
		eventBus.addListener(this::enqueueIMC);
		// Register the processIMC method for modloading
		eventBus.addListener(this::processIMC);
		// Register the doClientStuff method for modloading
		eventBus.addListener(this::doClientStuff);


        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MODEL_SWAPPER.registerListeners(eventBus));

//        forgeEventBus.addListener(EventPriority.NORMAL, this::addDimensionalSpacing);
//        forgeEventBus.addListener(EventPriority.NORMAL, WitchHutStructure::setupStructureSpawns);


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.LOWEST, this::gatherData);

        curiosLoaded = ModList.get().isLoaded("curios");
    }

	public void gatherData(GatherDataEvent event) {
		DataGenerator gen = event.getGenerator();
		PackOutput output = event.getGenerator().getPackOutput();
		CompletableFuture<HolderLookup.Provider> provider = event.getLookupProvider();
		ExistingFileHelper helper = event.getExistingFileHelper();
		DatapackBuiltinEntriesProvider datapackProvider = new RegistryDataGenerator(output, provider);
		CompletableFuture<HolderLookup.Provider> lookupProvider = datapackProvider.getRegistryProvider();
		gen.addProvider(event.includeServer(), datapackProvider);

		gen.addProvider(true, new ModRecipeProvider(output));
		gen.addProvider(event.includeServer(), new ModBiomeTagsProvider(gen, provider, helper));
//		gen.addProvider(event.includeServer(), new HexereiRecipeProvider(gen));
	}


	@OnlyIn(Dist.CLIENT)
	public void setupCrowPerchRenderer() {
		MinecraftForge.EVENT_BUS.register(CrowPerchRenderer.class);
	}

	private void setup(final FMLCommonSetupEvent event) {
		// some preinit code

		event.enqueueWork(() -> {
			DispenserBlock.registerBehavior(Items.FLINT_AND_STEEL, new CustomFlintAndSteelDispenserBehavior(DispenserBlock.DISPENSER_REGISTRY.get(Items.FLINT_AND_STEEL)));

			AxeItem.STRIPPABLES = new ImmutableMap.Builder<Block, Block>().putAll(AxeItem.STRIPPABLES)
							.put(ModBlocks.MAHOGANY_LOG.get(), ModBlocks.STRIPPED_MAHOGANY_LOG.get())
							.put(ModBlocks.MAHOGANY_WOOD.get(), ModBlocks.STRIPPED_MAHOGANY_WOOD.get())
							.put(ModBlocks.WILLOW_LOG.get(), ModBlocks.STRIPPED_WILLOW_LOG.get())
							.put(ModBlocks.WILLOW_WOOD.get(), ModBlocks.STRIPPED_WILLOW_WOOD.get())
							.put(ModBlocks.WITCH_HAZEL_LOG.get(), ModBlocks.STRIPPED_WITCH_HAZEL_LOG.get())
							.put(ModBlocks.WITCH_HAZEL_WOOD.get(), ModBlocks.STRIPPED_WITCH_HAZEL_WOOD.get()).build();
//            ModStructures.setupStructures();
//            ModConfiguredStructures.registerConfiguredStructures();
			WoodType.register(ModWoodType.MAHOGANY);
			WoodType.register(ModWoodType.WILLOW);
			WoodType.register(ModWoodType.WITCH_HAZEL);
			WoodType.register(ModWoodType.POLISHED_MAHOGANY);
			WoodType.register(ModWoodType.POLISHED_WILLOW);
			WoodType.register(ModWoodType.POLISHED_WITCH_HAZEL);

			BroomType.create("mahogany", ModItems.MAHOGANY_BROOM.get(), 0.8f);
			BroomType.create("willow", ModItems.WILLOW_BROOM.get(), 0.4f);
			BroomType.create("witch_hazel", ModItems.WITCH_HAZEL_BROOM.get(), 0.6f);

			Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, new ResourceLocation(MOD_ID, "witch_hut_leg_processor"), WITCH_HUT_LEG_PROCESSOR);
			Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, new ResourceLocation(MOD_ID, "dark_coven_leg_processor"), DARK_COVEN_LEG_PROCESSOR);
			Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, new ResourceLocation(MOD_ID, "nature_coven_leg_processor"), NATURE_COVEN_LEG_PROCESSOR);
			Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, new ResourceLocation(MOD_ID, "mangrove_tree_leg_processor"), MANGROVE_TREE_LEG_PROCESSOR);

			HexereiPacketHandler.register();

			SpawnPlacements.register(ModEntityTypes.CROW.get(), SpawnPlacements.Type.ON_GROUND,
							Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
			LightManager.init();

			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(ModBlocks.MANDRAKE_FLOWER.getId(), ModBlocks.POTTED_MANDRAKE_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(ModBlocks.BELLADONNA_FLOWER.getId(), ModBlocks.POTTED_BELLADONNA_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(ModBlocks.YELLOW_DOCK_BUSH.getId(), ModBlocks.POTTED_YELLOW_DOCK_BUSH);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(ModBlocks.MUGWORT_BUSH.getId(), ModBlocks.POTTED_MUGWORT_BUSH);

			ComposterBlock.COMPOSTABLES.put(ModBlocks.WILLOW_VINES.get().asItem(), 0.5F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.WILLOW_LEAVES.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.MAHOGANY_LEAVES.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.WILLOW_SAPLING.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.MAHOGANY_SAPLING.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.MANDRAKE_FLOWER.get().asItem(), 1F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.BELLADONNA_FLOWER.get().asItem(), 1F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.MUGWORT_BUSH.get().asItem(), 1F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.YELLOW_DOCK_BUSH.get().asItem(), 1F);
			ComposterBlock.COMPOSTABLES.put(ModBlocks.LILY_PAD_BLOCK.get().asItem(), 1F);
			ComposterBlock.COMPOSTABLES.put(ModItems.BELLADONNA_BERRIES.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.BELLADONNA_FLOWERS.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.MANDRAKE_FLOWERS.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.MANDRAKE_ROOT.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.MUGWORT_FLOWERS.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.MUGWORT_LEAVES.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.YELLOW_DOCK_FLOWERS.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.YELLOW_DOCK_LEAVES.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.DRIED_BELLADONNA_FLOWERS.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.DRIED_MANDRAKE_FLOWERS.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.DRIED_MUGWORT_FLOWERS.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.DRIED_MUGWORT_LEAVES.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.DRIED_YELLOW_DOCK_FLOWERS.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.DRIED_YELLOW_DOCK_LEAVES.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.SAGE.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.SAGE_SEED.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.DRIED_SAGE.get().asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ModItems.TALLOW_IMPURITY.get().asItem(), 0.3F);
		});
		if (ModList.get().isLoaded("terrablender") && HexConfig.WILLOW_SWAMP_RARITY.get() > 0){
			event.enqueueWork(ModRegion::init);
		}
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		// do something that can only be done on the client

		setupCrowPerchRenderer();
		event.enqueueWork(() -> {
			Sheets.addWoodType(ModWoodType.MAHOGANY);
			Sheets.addWoodType(ModWoodType.WILLOW);
			Sheets.addWoodType(ModWoodType.WITCH_HAZEL);
			Sheets.addWoodType(ModWoodType.POLISHED_MAHOGANY);
			Sheets.addWoodType(ModWoodType.POLISHED_WILLOW);
			Sheets.addWoodType(ModWoodType.POLISHED_WITCH_HAZEL);

			ItemBlockRenderTypes.setRenderLayer(ModFluids.QUICKSILVER_FLUID.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ModFluids.QUICKSILVER_FLOWING.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ModFluids.BLOOD_FLUID.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ModFluids.BLOOD_FLOWING.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ModFluids.TALLOW_FLUID.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ModFluids.TALLOW_FLOWING.get(), RenderType.translucent());

			MenuScreens.register(ModContainers.MIXING_CAULDRON_CONTAINER.get(), MixingCauldronScreen::new);
			MenuScreens.register(ModContainers.COFFER_CONTAINER.get(), CofferScreen::new);
			MenuScreens.register(ModContainers.HERB_JAR_CONTAINER.get(), HerbJarScreen::new);
			MenuScreens.register(ModContainers.BROOM_CONTAINER.get(), BroomScreen::new);
			MenuScreens.register(ModContainers.CROW_CONTAINER.get(), CrowScreen::new);
			MenuScreens.register(ModContainers.CROW_FLUTE_CONTAINER.get(), CrowFluteScreen::new);
			MenuScreens.register(ModContainers.WOODCUTTER_CONTAINER.get(), WoodcutterScreen::new);


		});

        if (curiosLoaded) GlassesCurioRender.register();

	}

	static float clientTicks = 0;
	static float clientTicksPartial = 0;

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void onRenderLast(RenderLevelStageEvent event) {
		clientTicksPartial = event.getPartialTick();
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void clientTickEvent(TickEvent.ClientTickEvent event) {
		if (event.type == TickEvent.Type.CLIENT)
			clientTicks += 1;
		if(ClientProxy.fontList.isEmpty()) {
			List<? extends String> fonts = HexConfig.FONT_LIST.get();
			for (String str : fonts) {
				if (!ClientProxy.fontList.containsKey(str))
					ClientProxy.fontList.put(str, new Font((p_95014_) -> {
						return Minecraft.getInstance().fontManager.fontSets.getOrDefault(new ResourceLocation(str), Minecraft.getInstance().fontManager.missingFontSet);
					}, false));
			}
		}
	}


	@OnlyIn(Dist.CLIENT)
	public static float getClientTicks() {
		Minecraft mc = Minecraft.getInstance();
		return clientTicks + mc.getFrameTime();
	}

	public static float getClientTicksWithoutPartial() {
		return clientTicks;
	}

	public static float getPartial() {
		return clientTicksPartial;
	}

//    @SubscribeEvent
//    public static void recipes(final RegistryEvent.Register<RecipeSerializer<?>> event) {
//        register(new Serializer2(), "coffer_dyeing", event.getRegistry());
//    }
//
//    private static <T extends IForgeRegistryEntry<T>> void register(T obj, String name, IForgeRegistry<T> registry) {
//        registry.register(obj.setRegistryName(new ResourceLocation(MOD_ID, name)));
//    }

	private void enqueueIMC(final InterModEnqueueEvent event) {
        if (curiosLoaded) CurioCompat.sendIMC();
	}

	private void processIMC(final InterModProcessEvent event) {

	}

	private void loadComplete(final FMLLoadCompleteEvent event) {
        MinecraftForge.EVENT_BUS.register(new SageBurningPlateEvent());
        MinecraftForge.EVENT_BUS.register(new WitchArmorEvent());
        MinecraftForge.EVENT_BUS.register(new CrowFluteEvent());
        MinecraftForge.EVENT_BUS.register(new CrowWhitelistEvent());

        MinecraftForge.EVENT_BUS.register(new PageDrawing());
        glassesZoomKeyPressEvent = new GlassesZoomKeyPressEvent();
        MinecraftForge.EVENT_BUS.register(glassesZoomKeyPressEvent);

		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			if (ModList.get().isLoaded("ars_nouveau")) net.joefoxe.hexerei.compat.LightManagerCompat.fallbackToArs();
		});

    }


}
