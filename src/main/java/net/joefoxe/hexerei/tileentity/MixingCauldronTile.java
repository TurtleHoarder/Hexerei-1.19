package net.joefoxe.hexerei.tileentity;

import net.joefoxe.hexerei.Hexerei;
import net.joefoxe.hexerei.block.custom.MixingCauldron;
import net.joefoxe.hexerei.container.MixingCauldronContainer;
import net.joefoxe.hexerei.data.recipes.FluidMixingRecipe;
import net.joefoxe.hexerei.data.recipes.MixingCauldronRecipe;
import net.joefoxe.hexerei.data.recipes.MoonPhases;
import net.joefoxe.hexerei.fluid.ModFluids;
import net.joefoxe.hexerei.fluid.PotionMixingRecipes;
import net.joefoxe.hexerei.item.ModItems;
import net.joefoxe.hexerei.particle.ModParticleTypes;
import net.joefoxe.hexerei.tileentity.renderer.MixingCauldronRenderer;
import net.joefoxe.hexerei.util.HexereiPacketHandler;
import net.joefoxe.hexerei.util.HexereiTags;
import net.joefoxe.hexerei.util.HexereiUtil;
import net.joefoxe.hexerei.util.message.EmitParticlesPacket;
import net.joefoxe.hexerei.util.message.TESyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

public class MixingCauldronTile extends RandomizableContainerBlockEntity implements WorldlyContainer, Clearable, MenuProvider, IFluidHandler {

//    private final ItemStackHandler itemHandler = createHandler();
//    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    public boolean crafting;
    public int craftDelay;
    public int craftDelayOld;
    public int emitParticles = 0;
    public boolean emitParticleSpout = false;
    public float degrees;
    private boolean crafted;
    private boolean extracted = false;
    private int isColliding = 0;  // 15 is colliding, 0 is no longer colliding
    public static final int craftDelayMax = 100;
    private long tickedGameTime;
    public int dyeColor = 0x422F1E;

    public Component customName;
    public NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);
    private FluidStack fluidStack = FluidStack.EMPTY;

    private static final int[] SLOTS_INPUT = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] SLOTS_OUTPUT = new int[]{8};

    VoxelShape BLOOD_SIGIL_SHAPE = Block.box(2.0D, 3.0D, 2.0D, 14.0D, 6.0D, 14.0D);
    VoxelShape HOPPER_SHAPE = Block.box(2.0D, 3.0D, 2.0D, 14.0D, 6.0D, 14.0D);

    public float fluidRenderLevel = 0;
    public FluidStack renderedFluid;

    boolean checkCraft = true;


    public MixingCauldronTile(BlockEntityType<?> tileEntityTypeIn, BlockPos blockPos, BlockState blockState) {
        super(tileEntityTypeIn, blockPos, blockState);
    }

    public MixingCauldronTile(BlockPos blockPos, BlockState blockState) {
        this(ModTileEntities.MIXING_CAULDRON_TILE.get(),blockPos, blockState);
    }

    public FluidStack getFluidStack(){
        return this.fluidStack;
    }

    public void setFluidStack(FluidStack fluidStack){
        this.fluidStack = fluidStack;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn) {
        this.items = itemsIn;
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName
                : Component.literal("");
    }

    @Override
    public Component getCustomName() {
        return this.customName;
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    public void setDyeColor(int dyeColor){
        this.dyeColor = dyeColor;
    }

    public int getDyeColor(){
        DyeColor dye = HexereiUtil.getDyeColorNamed(this.getDisplayName().getString());
        if(dye != null)
            return HexereiUtil.getColorValue(dye);
        return this.dyeColor;
    }
    @Override
    public void setChanged() {
        super.setChanged();
        this.checkCraft = true;
        sync();
    }

    public void sync() {
        if(level != null){
            if (!level.isClientSide)
                HexereiPacketHandler.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new TESyncPacket(worldPosition, save(new CompoundTag())));

            if (this.level != null)
                this.level.sendBlockUpdated(this.getPos(), this.level.getBlockState(this.getPos()), this.level.getBlockState(this.getPos()),
                        Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Hexerei.MOD_ID + ".mixing_cauldron");
    }



    public void setContents(List<ItemStack> stacks, Player player) {
        for (int i = 0; i < stacks.size(); i++) {
            if(i < 8 || !stacks.get(i).isEmpty()) {
                if(!items.get(i).sameItemStackIgnoreDurability(stacks.get(i))){
                    int slot = player.inventory.findSlotMatchingItem(stacks.get(i));
                    ItemStack stack = ContainerHelper.removeItem(player.inventory.items, slot, 1);
                    player.inventory.placeItemBackInInventory(items.get(i));
                    items.set(i, stack);
                }
            }
        }
        setChanged();
    }


    
    /**
     * Returns the stack in the given slot.
     */
    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < this.items.size() ? this.items.get(index) : ItemStack.EMPTY;
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    @Override
    public ItemStack removeItem(int index, int count) {
//        setChanged();
        ItemStack itemStack = ContainerHelper.removeItem(this.items, index, count);
        if(itemStack.getCount() < 1)
            itemStack.setCount(1);
        return itemStack;
    }

    /**
     * Removes a stack from the given slot and returns it.
     */
    @Override
    public ItemStack removeItemNoUpdate(int index) {

        return ContainerHelper.takeItem(this.items, index);
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < this.items.size()) {
            ItemStack itemStack = stack.copy();
//            itemStack.setCount(1);
            this.items.set(index, itemStack);
        }

        setChanged();
    }

    public boolean canPlaceItem(int index, ItemStack stack) {

        if (index == 8)
            return false;
        if (index == 9 && !stack.is(HexereiTags.Items.SIGILS))
            return false;
        return this.items.get(index).isEmpty();

    }

    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_OUTPUT;
        } else {
            return SLOTS_INPUT;
        }
    }

    /**
     * Returns true if automation can insert the given item in the given slot from the given side.
     */
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, @Nullable Direction direction) {
        return this.canPlaceItem(index, itemStackIn);
    }

    /**
     * Returns true if automation can extract the given item in the given slot from the given side.
     */
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
//        if (index == 3) {
//            return stack.getItem() == Items.GLASS_BOTTLE;
//        } else {
//            return true;
//        }
        return true;
    }

    public int getCraftDelay() {
        return this.craftDelay;
    }
    public void setCraftDelay(int delay) {
        this.craftDelay =  delay;
    }



    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory player) {
        return new MixingCauldronContainer(id, this.level, this.getPos(), player, player.player);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.items.clear();
    }

//    @Override
//    public double getMaxRenderDistanceSquared() {
//        return 4096D;
//    }

    @Override
    public AABB getRenderBoundingBox() {
        return super.getRenderBoundingBox().inflate(5, 5, 5);
    }

    @Override
    public void requestModelDataUpdate() {
        super.requestModelDataUpdate();
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        return super.getModelData();
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);

        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        this.fluidStack = FluidStack.loadFluidStackFromNBT(compoundTag.getCompound("fluid"));
        if (compoundTag.contains("CustomName", 8))
            this.customName = Component.Serializer.fromJson(compoundTag.getString("CustomName"));

        if(compoundTag.contains("DyeColor"))
            this.dyeColor = compoundTag.getInt("DyeColor");
        if(compoundTag.contains("delay"))
            this.craftDelay = compoundTag.getInt("delay");
        if(compoundTag.contains("delayOld"))
            this.craftDelayOld = compoundTag.getInt("delayOld");
        if (!this.tryLoadLootTable(compoundTag)) {
            ContainerHelper.loadAllItems(compoundTag, this.items);
        }
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        ContainerHelper.saveAllItems(compound, this.items);
        compound.put("fluid", this.fluidStack.writeToNBT(new CompoundTag()));
        compound.putInt("delay", this.craftDelay);
        compound.putInt("delayOld", this.craftDelayOld);
        compound.putInt("DyeColor", this.dyeColor);
        if (this.customName != null)
            compound.putString("CustomName", Component.Serializer.toJson(this.customName));
    }

//    @Override
    public CompoundTag save(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.put("fluid", this.fluidStack.writeToNBT(new CompoundTag()));
        tag.putInt("delay", this.craftDelay);
        tag.putInt("delayOld", this.craftDelayOld);
        tag.putInt("DyeColor", this.dyeColor);
        if (this.customName != null)
            tag.putString("CustomName", Component.Serializer.toJson(this.customName));
        return tag;
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        return this.save(new CompoundTag());
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this, (tag) -> this.getUpdateTag());
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket pkt)
    {
        this.deserializeNBT(pkt.getTag());
    }




    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
    }

    @Override
    public CompoundTag serializeNBT() {
        return super.serializeNBT();
    }

    /*
     * This method gets called on the client when it receives the packet that was
     * sent in getUpdatePacket(). And here we just read the data from the packet
     * that was recieved.
     */
//    @Override
//    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
//        this.load(pkt.getTag());
//    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }


    LazyOptional<? extends IItemHandler>[] handlers =
            SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.extracted && facing != null && capability == ForgeCapabilities.ITEM_HANDLER) {
            if (facing == Direction.UP)
                return handlers[0].cast();
            else if (facing == Direction.DOWN)
                return handlers[1].cast();
            else
                return handlers[2].cast();
        }
        if (capability == ForgeCapabilities.FLUID_HANDLER)
            return ForgeCapabilities.FLUID_HANDLER.orEmpty(capability, LazyOptional.of(() -> this));
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<? extends IItemHandler> handler : handlers) handler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.handlers = net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    }

    public Item getItemInSlot(int slot) {
        return this.items.get(slot).getItem();
    }



    public ItemStack getItemStackInSlot(int slot) {
        return this.items.get(slot);
    }

    public int getCraftMaxDelay() {
        return this.craftDelayMax;
    }

    public boolean getCrafted() {
        return this.crafted;
    }

    public int getNumberOfItems() {

        int num = 0;
        for(int i = 0; i < 8; i++)
        {
            if(this.items.get(i) != ItemStack.EMPTY)
                num++;
        }
        return num;

    }

    private void strikeLightning() {
        if(!this.level.isClientSide()) {
            EntityType.LIGHTNING_BOLT.spawn((ServerLevel)level, null, null,
                    worldPosition, MobSpawnType.TRIGGERED, true, true);
        }
    }

    public void entityInside(Entity entity) {
        BlockPos blockpos = this.getPos();
        if (entity instanceof ItemEntity) {
            if (Shapes.joinIsNotEmpty(Shapes.create(entity.getBoundingBox().move(-blockpos.getX(), -blockpos.getY(), -blockpos.getZ())), HOPPER_SHAPE, BooleanOp.AND)) {
                if (captureItem((ItemEntity) entity) && !level.isClientSide) {
                    HexereiPacketHandler.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new EmitParticlesPacket(worldPosition, 2, true));
                }
            }
        }else
        {
            if (Shapes.joinIsNotEmpty(Shapes.create(entity.getBoundingBox().move(-blockpos.getX(), -blockpos.getY(), -blockpos.getZ())), BLOOD_SIGIL_SHAPE, BooleanOp.AND)) {
                if (this.isColliding <= 1 && this.getItemInSlot(9).asItem() == ModItems.BLOOD_SIGIL.get()) {
                    Random random = new Random();
                    entity.hurt(DamageSource.MAGIC, 3.0f);

                    if (fluidStack.isEmpty() || (fluidStack.containsFluid(new FluidStack(ModFluids.BLOOD_FLUID.get(), 1)) && this.getFluidStack().getAmount() < this.getTankCapacity(0))) {

                        if (fluidStack.isEmpty())
                            this.fill(new FluidStack(ModFluids.BLOOD_FLUID.get(), 111), IFluidHandler.FluidAction.EXECUTE);
                        else {
                            this.getFluidStack().grow(111);
                            if (this.getFluidStack().getAmount() % 1000 == 1)
                                this.getFluidStack().shrink(1);
                            if (this.getFluidStack().getAmount() % 1000 == 999)
                                this.getFluidStack().grow(1);
                            setChanged();
                        }
                        entity.level().playSound(null, entity.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.BLOCKS, 1.0F, 1.0F);
                        if(!level.isClientSide)
                            HexereiPacketHandler.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new EmitParticlesPacket(worldPosition, 2, true));

                    }
                }

                this.isColliding = 6; // little cooldown so you don't constantly take damage, you must jump on the nails to take damage

            }

        }

    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {

        return super.getCapability(cap);
    }

    public boolean captureItem(ItemEntity itemEntity) {
        boolean flag = false;
        ItemStack itemstack = itemEntity.getItem().copy();

        //check if there is a slot open  getFirstOpenSlot
        if (getFirstOpenSlot() >= 0)
        {
            ItemStack temp = itemstack.copy();
            temp.setCount(1);
            this.setItem(getFirstOpenSlot(), temp);
//            this.itemHandler.insertItem(getFirstOpenSlot(), itemstack, false);
            itemEntity.getItem().shrink(1);
            //((MixingCauldron)this.getBlockState().getBlock()).emitCraftCompletedParticles();
            return true;
        }
        return false;
    }



    public int getFirstOpenSlot(){
        for(int i = 0; i < 8; i++) {
            if(this.items.get(i).isEmpty())
                return i;
        }
        return -1;
    }


    public void craft(){

        this.crafting = false;
        SimpleContainer inv = new SimpleContainer(10);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, this.items.get(i));
        }

        if (PotionMixingRecipes.ALL == null || PotionMixingRecipes.ALL.isEmpty()) {
            PotionMixingRecipes.ALL = PotionMixingRecipes.createRecipes();
            PotionMixingRecipes.BY_ITEM = PotionMixingRecipes.sortRecipesByItem(PotionMixingRecipes.ALL);
        }

        Optional<MixingCauldronRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(MixingCauldronRecipe.Type.INSTANCE, inv, level);
        List<FluidMixingRecipe> recipe2 = PotionMixingRecipes.ALL.stream().filter((potionRecipe) -> {
            if (potionRecipe.getIngredients().isEmpty()) {
                PotionMixingRecipes.ALL = null;
                return false;

            }
            return potionRecipe.matches(inv, level);
//            return inv.getItem(0).is(potionRecipe.getIngredients().get(0).getItems()[0].getItem());

        }).toList();
        boolean matchesRecipe = false;

        ResourceLocation fl2 = ForgeRegistries.FLUIDS.getKey(this.fluidStack.getFluid());
        CompoundTag tag = this.fluidStack.isEmpty() ? new CompoundTag() : this.fluidStack.copy().getOrCreateTag();


        for (FluidMixingRecipe fluidMixingRecipe : recipe2) {
            boolean fluidEqual = fluidMixingRecipe.getLiquid().isFluidEqual(this.fluidStack);
            ResourceLocation fl1 = ForgeRegistries.FLUIDS.getKey(fluidMixingRecipe.getLiquid().getFluid());
            if (!fluidEqual && fl1 != null && fl2 != null && fl1.getPath().equals(fl2.getPath())) {
                boolean flag = NbtUtils.compareNbt(fluidMixingRecipe.getLiquid().copy().getOrCreateTag(), tag, true);
                if (flag) {
                    fluidEqual = true;
                }
            }

            if (fluidEqual) {
                matchesRecipe = true;
                break;
            }
        }

//        List<FluidMixingRecipe> list = PotionMixingRecipes.BY_ITEM.get(inv.getItem(0).getItem());
//        Optional<FluidMixingRecipe> recipe2 = list != null ?list.stream().findFirst().filter((fluidMixingRecipe -> {
//            return fluidMixingRecipe.matches(inv, level);
//        })) : Optional.empty();


        if (!matchesRecipe)
            recipe2 = level.getRecipeManager().getRecipeFor(FluidMixingRecipe.Type.INSTANCE, inv, level).stream().toList();


        AtomicBoolean firstRecipe = new AtomicBoolean(false);
        recipe.ifPresent(iRecipe -> {
            ItemStack output = iRecipe.getResultItem();
            //ask for delay
            FluidStack recipeFluid = iRecipe.getLiquid();
            FluidStack containerFluid = this.getFluidStack();

            boolean fluidEqual = recipeFluid.isFluidEqual(containerFluid);
            boolean outputClear = (inv.getItem(8) == ItemStack.EMPTY || inv.getItem(8).getCount() == 0) || (inv.getItem(8).sameItem(output) && inv.getItem(8).getCount() + output.getCount() <= inv.getItem(8).getMaxStackSize());
            boolean hasEnoughFluid = iRecipe.getFluidLevelsConsumed() <= this.getFluidStack().getAmount();
            boolean needsHeat = iRecipe.getHeatCondition() != FluidMixingRecipe.HeatCondition.NONE;
            boolean needsMoonPhase = iRecipe.getMoonCondition() != MoonPhases.MoonCondition.NONE;
            if (!needsMoonPhase || MoonPhases.MoonCondition.getMoonPhase(this.level) == iRecipe.getMoonCondition()) {
                if (fluidEqual && !this.crafted && hasEnoughFluid && outputClear) {
                    BlockState heatSource = level.getBlockState(getPos().below());
                    if (!needsHeat || heatSource.is(HexereiTags.Blocks.HEAT_SOURCES)) {
                        if (!heatSource.hasProperty(LIT) || heatSource.getValue(LIT)) {
                            firstRecipe.set(true);
                            this.crafting = true;
                            if (this.craftDelay >= craftDelayMax) {
                                craftTheItem(output);
                                int temp = this.getFluidStack().getAmount();
                                this.getFluidStack().shrink(this.getTankCapacity(0));
                                this.fill(new FluidStack(iRecipe.getLiquidOutput(), temp), FluidAction.EXECUTE);

                                //for setting a cooldown on crafting so the animations can take place
                                this.crafted = true;
                                HexereiPacketHandler.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new EmitParticlesPacket(worldPosition, 10, true));
                                this.getFluidStack().shrink(iRecipe.getFluidLevelsConsumed());
                                if (this.getFluidStack().getAmount() % 10 == 1)
                                    this.getFluidStack().shrink(1);
                                if (this.getFluidStack().getAmount() % 10 == 9)
                                    this.getFluidStack().grow(1);
                                setChanged();
                            }
                        }
                    }
                }
            } else {
                //potentially add particles or something to say it cant craft without the moon phase
            }


        });


        if (!firstRecipe.get() && !recipe2.isEmpty()) {
            for (FluidMixingRecipe fluidMixingRecipe : recipe2) {
                ItemStack output = fluidMixingRecipe.getResultItem();
                //ask for delay
                FluidStack recipeFluid = fluidMixingRecipe.getLiquid();
                FluidStack containerFluid = this.getFluidStack();

                boolean fluidEqual = recipeFluid.isFluidEqual(containerFluid);

                ResourceLocation fl1 = ForgeRegistries.FLUIDS.getKey(fluidMixingRecipe.getLiquid().getFluid());
                if (!fluidEqual && fl1 != null && fl2 != null && fl1.getPath().equals(fl2.getPath())) {
                    boolean flag = NbtUtils.compareNbt(fluidMixingRecipe.getLiquid().copy().getOrCreateTag(), tag, true);
                    if (flag) {
                        fluidEqual = true;
                    }
                }


                boolean hasEnoughFluid = this.getFluidStack().getAmount() >= 50;
                boolean needsHeat = fluidMixingRecipe.getHeatCondition() != FluidMixingRecipe.HeatCondition.NONE;
                if (fluidEqual && !this.crafted && hasEnoughFluid) {
                    BlockState heatSource = level.getBlockState(getPos().below());
                    if (!needsHeat || heatSource.is(HexereiTags.Blocks.HEAT_SOURCES)) {
                        if (!heatSource.hasProperty(LIT) || heatSource.getValue(LIT)) {
                            this.crafting = true;
                            if (this.craftDelay >= craftDelayMax) {
                                craftTheItem(output);
                                int temp = this.getFluidStack().getAmount();
                                this.getFluidStack().shrink(this.getTankCapacity(0));
                                this.fill(new FluidStack(fluidMixingRecipe.getLiquidOutput(), temp), FluidAction.EXECUTE);

                                //for setting a cooldown on crafting so the animations can take place
                                this.crafted = true;
                                HexereiPacketHandler.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new EmitParticlesPacket(worldPosition, 10, true));
                                setChanged();

                            }
                        }
                    }
                }
            }
        }
    }

    private void craftTheItem(ItemStack output) {
//        itemHandler.extractItem(0, 1, false);
//        itemHandler.extractItem(1, 1, false);
//        itemHandler.extractItem(2, 1, false);
//        itemHandler.extractItem(3, 1, false);
//        itemHandler.extractItem(4, 1, false);
//        itemHandler.extractItem(5, 1, false);
//        itemHandler.extractItem(6, 1, false);
//        itemHandler.extractItem(7, 1, false);

        if(output.getItem() == ModItems.TALLOW_IMPURITY.get())
        {
            Random random = new Random();
            if(random.nextInt(5) != 0)
                output = ItemStack.EMPTY;
        }
        this.setItem(0, ItemStack.EMPTY);
        this.setItem(1, ItemStack.EMPTY);
        this.setItem(2, ItemStack.EMPTY);
        this.setItem(3, ItemStack.EMPTY);
        this.setItem(4, ItemStack.EMPTY);
        this.setItem(5, ItemStack.EMPTY);
        this.setItem(6, ItemStack.EMPTY);
        this.setItem(7, ItemStack.EMPTY);
        if(ItemStack.isSameItem(this.getItem(8), output)){
            this.getItem(8).setCount(this.getItem(8).getCount() + output.getCount());
        } else
            this.setItem(8, output);
//        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
//        itemHandler.setStackInSlot(1, ItemStack.EMPTY);
//        itemHandler.setStackInSlot(2, ItemStack.EMPTY);
//        itemHandler.setStackInSlot(3, ItemStack.EMPTY);
//        itemHandler.setStackInSlot(4, ItemStack.EMPTY);
//        itemHandler.setStackInSlot(5, ItemStack.EMPTY);
//        itemHandler.setStackInSlot(6, ItemStack.EMPTY);
//        itemHandler.setStackInSlot(7, ItemStack.EMPTY);
//        itemHandler.setStackInSlot(8, output);


    }




//    @Override
    public void tick() {


        this.craftDelayOld = this.craftDelay;
        if(level.isClientSide) {
            float dist = Math.abs(fluidRenderLevel - fluidStack.getAmount()) / 1000f;
            if(!fluidStack.isEmpty())
                renderedFluid = fluidStack.copy();
            if(renderedFluid != null)
                renderedFluid.setAmount((int)fluidRenderLevel);
            renderParticles();
            return;
        }

        this.tickedGameTime = this.level.getGameTime();
        if(this.checkCraft) {
            craft();
            this.checkCraft = false;
        }

        if(this.crafting)
            this.craftDelay += 2;
        if(this.craftDelay >= 1)
            this.craftDelay--;
        if(this.craftDelay > 0 && !level.isClientSide) {
            this.sync();
        }
        if(this.craftDelay > craftDelayMax)
            craft();
        if(this.craftDelay < 10) {
            if(this.crafted){
                this.checkCraft = true;
                this.crafted = false;
            }
        }

        if(extracted)
        {
            extracted = false;
            setChanged();
        }
        this.isColliding--;
    }

    private void renderParticles() {
        float fillPercentage = 0;
        FluidStack fluidStack = getFluidInTank(0);
        if(!fluidStack.isEmpty())
            fillPercentage = Math.min(1, (float) fluidStack.getAmount() / getTankCapacity(0));
        float height = MixingCauldronRenderer.MIN_Y + (MixingCauldronRenderer.MAX_Y - MixingCauldronRenderer.MIN_Y) * fillPercentage;
        Random rand = new Random();

        if (this.emitParticles > 0 && level != null) {
            for (int i = 0; i < 3; i++) {
                if (this.emitParticleSpout) {
                    if (rand.nextInt(3) == 0)
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(worldPosition)), worldPosition.getX() + 0.5f, worldPosition.getY() + height, worldPosition.getZ() + 0.5f, (rand.nextDouble() - 0.5d) / 20d, (rand.nextDouble() + 0.5d) * 2d, (rand.nextDouble() - 0.5d) / 20d);
                    if (rand.nextInt(3) == 0)
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(worldPosition)), worldPosition.getX() + 0.5f, worldPosition.getY() + height, worldPosition.getZ() + 0.5f, (rand.nextDouble() - 0.5d) / 20d, (rand.nextDouble() + 0.5d) * 2d, (rand.nextDouble() - 0.5d) / 20d);
                    if (rand.nextInt(3) == 0)
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(worldPosition)), worldPosition.getX() + 0.5f, worldPosition.getY() + height, worldPosition.getZ() + 0.5f, (rand.nextDouble() - 0.5d) / 20d, (rand.nextDouble() + 0.5d) * 2d, (rand.nextDouble() - 0.5d) / 20d);
                    if (rand.nextInt(3) == 0)
                        level.addParticle(ParticleTypes.SMOKE, worldPosition.getX() + 0.5f, worldPosition.getY() + height, worldPosition.getZ() + 0.5f, (rand.nextDouble() - 0.5d) / 50d, (rand.nextDouble() + 0.5d) * 0.045d, (rand.nextDouble() - 0.5d) / 50d);
                    if (rand.nextInt(3) == 0)
                        level.addParticle(ParticleTypes.SMOKE, worldPosition.getX() + 0.5f, worldPosition.getY() + height, worldPosition.getZ() + 0.5f, (rand.nextDouble() - 0.5d) / 50d, (rand.nextDouble() + 0.5d) * 0.045d, (rand.nextDouble() - 0.5d) / 50d);
                }
                if(rand.nextInt(3) == 0)
                    level.addParticle(ModParticleTypes.CAULDRON.get(), worldPosition.getX() + 0.2d + (0.6d * rand.nextDouble()), worldPosition.getY() + height, worldPosition.getZ() + 0.2d + (0.6d * rand.nextDouble()), (rand.nextDouble() - 0.5d) / 50d, (rand.nextDouble() + 0.5d) * 0.024d, (rand.nextDouble() - 0.5d) / 50d);
                if(rand.nextInt(3) == 0)
                    level.addParticle(ModParticleTypes.CAULDRON.get(), worldPosition.getX() + 0.2d + (0.6d * rand.nextDouble()), worldPosition.getY() + height, worldPosition.getZ() + 0.2d + (0.6d * rand.nextDouble()), (rand.nextDouble() - 0.5d) / 50d, (rand.nextDouble() + 0.5d) * 0.024d, (rand.nextDouble() - 0.5d) / 50d);
                if(rand.nextInt(3) == 0)
                    level.addParticle(ModParticleTypes.CAULDRON.get(), worldPosition.getX() + 0.2d + (0.6d * rand.nextDouble()), worldPosition.getY() + height, worldPosition.getZ() + 0.2d + (0.6d * rand.nextDouble()), (rand.nextDouble() - 0.5d) / 50d, (rand.nextDouble() + 0.5d) * 0.024d, (rand.nextDouble() - 0.5d) / 50d);
                if(rand.nextInt(3) == 0)
                    level.addParticle(ModParticleTypes.CAULDRON.get(), worldPosition.getX() + 0.2d + (0.6d * rand.nextDouble()), worldPosition.getY() + height, worldPosition.getZ() + 0.2d + (0.6d * rand.nextDouble()), (rand.nextDouble() - 0.5d) / 50d, (rand.nextDouble() + 0.5d) * 0.024d, (rand.nextDouble() - 0.5d) / 50d);
                if(rand.nextInt(3) == 0)
                    level.addParticle(ModParticleTypes.CAULDRON.get(), worldPosition.getX() + 0.2d + (0.6d * rand.nextDouble()), worldPosition.getY() + height, worldPosition.getZ() + 0.2d + (0.6d * rand.nextDouble()), (rand.nextDouble() - 0.5d) / 50d, (rand.nextDouble() + 0.5d) * 0.024d, (rand.nextDouble() - 0.5d) / 50d);
            }
            this.emitParticles--;
        }


    }

    //    @Override
    public BlockPos getPos() {
        return worldPosition;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    public boolean isEmpty() {
        for(ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }


    @Override
    public int getTanks() {
        return 1;
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.fluidStack.copy();
    }

    @Override
    public int getTankCapacity(int tank) {
        return 2000;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !(this.fluidStack.isEmpty() || this.fluidStack.isFluidEqual(resource)))
            return 0;
        int amount = Math.min(resource.getAmount(), this.getTankCapacity(0) - this.fluidStack.getAmount());
        if (action.execute()) {
            FluidStack newStack = resource.copy();
            newStack.setAmount(this.fluidStack.getAmount() + amount);
            this.fluidStack = newStack;
            this.setChanged();
        }
        return amount;
    }


    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || this.fluidStack.isEmpty() || !this.fluidStack.getFluid().isSame(resource.getFluid()))
            return FluidStack.EMPTY;
        int amount = Math.min(resource.getAmount(), this.fluidStack.getAmount());
        FluidStack returnStack = this.fluidStack.copy();
        returnStack.setAmount(amount);
        if (action.execute()) {
            this.fluidStack.shrink(amount);
            this.setChanged();
        }
        return returnStack;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || this.fluidStack.isEmpty())
            return FluidStack.EMPTY;
        int amount = Math.min(maxDrain, this.fluidStack.getAmount());
        FluidStack returnStack = this.fluidStack.copy();
        returnStack.setAmount(amount);
        if (action.execute()) {
            this.fluidStack.shrink(amount);
            this.setChanged();
        }
        return returnStack;
    }

    public boolean interactWithFluid(IFluidHandlerItem fluidHandler) {
        if (fluidHandler.getTanks() == 0)
            return false;
        FluidStack cauldronFluid = fluidHandler.getFluidInTank(0);
        Random random = new Random();
        if (cauldronFluid.isEmpty()) {
            if (!this.fluidStack.isEmpty() && fluidHandler.isFluidValid(0, this.fluidStack)) {
                int amount = fluidHandler.fill(this.fluidStack.copy(), FluidAction.EXECUTE);
                if (amount > 0) {
                    if(getLevel() != null)
                        getLevel().playSound(null, getPos().getX() + 0.5f, getPos().getY() + 0.5f, getPos().getZ() + 0.5f, fluidHandler.getFluidInTank(1).getFluid().getPickupSound().isPresent() ? fluidHandler.getFluidInTank(1).getFluid().getPickupSound().get() : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F + 0.4F * random.nextFloat());
                    this.fluidStack.shrink(amount);
                    this.setChanged();
                    return true;
                }

            }
        } else if (this.fluidStack.isEmpty() || this.fluidStack.isFluidEqual(cauldronFluid)) {
            cauldronFluid = cauldronFluid.copy();
            cauldronFluid.setAmount(this.getTankCapacity(0) - this.fluidStack.getAmount());
            FluidStack amount = fluidHandler.drain(cauldronFluid, FluidAction.SIMULATE);
            if (!amount.isEmpty() && (this.fluidStack.isEmpty() || this.fluidStack.isFluidEqual(amount))) {
                amount = fluidHandler.drain(cauldronFluid, FluidAction.EXECUTE);
                amount.grow(this.fluidStack.getAmount());
                this.fluidStack = amount;
                if(getLevel() != null)
                    getLevel().playSound(null, getPos().getX() + 0.5f, getPos().getY() + 0.5f, getPos().getZ() + 0.5f, amount.getFluid().getPickupSound().isPresent() ? amount.getFluid().getPickupSound().get() : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 0.8F + 0.4F * random.nextFloat());
                this.setChanged();
                return true;
            }
        }
        return false;
    }
}
