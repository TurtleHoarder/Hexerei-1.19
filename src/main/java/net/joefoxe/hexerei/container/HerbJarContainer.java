package net.joefoxe.hexerei.container;

import net.joefoxe.hexerei.block.ModBlocks;
import net.joefoxe.hexerei.items.JarSlot;
import net.joefoxe.hexerei.tileentity.HerbJarTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;


public class HerbJarContainer extends AbstractContainerMenu {
    private final BlockEntity tileEntity;
    private final Player playerEntity;
    private final IItemHandler playerInventory;
    public final ItemStack stack;

    public static final int OFFSET = 28;


    public HerbJarContainer(int windowId, ItemStack itemStack, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super(ModContainers.HERB_JAR_CONTAINER.get(), windowId);
        this.stack = itemStack;
        this.tileEntity = world.getBlockEntity(pos);
        playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        layoutPlayerInventorySlots(11, 147 - OFFSET);

        if(tileEntity != null) {
            tileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> addSlot(new JarSlot(h, 0, 83, 74 - OFFSET)));



            addDataSlot(new DataSlot() {
                @Override
                public void set(int value) {
                    ((HerbJarTile)tileEntity).setButtonToggled(value);
                }
                @Override
                public int get() {
                    return ((HerbJarTile)tileEntity).getButtonToggled();
                }
            });
        }

    }

    public int getToggled() {
        return ((HerbJarTile)tileEntity).getButtonToggled();
    }

    public void setToggled(int value) {
        ((HerbJarTile)tileEntity).setButtonToggled(value);
    }


    public static boolean canAddItemToSlot(@Nullable Slot slot, @Nonnull ItemStack stack, boolean stackSizeMatters) {
        boolean flag = slot == null || !slot.hasItem();
        if (slot != null) {
            ItemStack slotStack = slot.getItem();

            if (!flag && stack.is(slotStack.getItem()) && ItemStack.isSameItemSameTags(slotStack, stack)) {
                return slotStack.getCount() + (stackSizeMatters ? 0 : stack.getCount()) <= slot.getMaxStackSize(slotStack);
            }
        }
        return flag;
    }

    public void clicked(int p_150400_, int p_150401_, ClickType p_150402_, Player p_150403_) {
            this.doClick(p_150400_, p_150401_, p_150402_, p_150403_);
            this.tileEntity.setChanged();
    }

    private void doClick(int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_) {
        if (p_150433_ == ClickType.QUICK_CRAFT) {
            int i = this.quickcraftStatus;
            this.quickcraftStatus = getQuickcraftHeader(p_150432_);
            if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.quickcraftStatus == 0) {
                this.quickcraftType = getQuickcraftType(p_150432_);
                if (isValidQuickcraftType(this.quickcraftType, p_150434_)) {
                    this.quickcraftStatus = 1;
                    this.quickcraftSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.quickcraftStatus == 1) {
                Slot slot = this.slots.get(p_150431_);
                ItemStack itemstack = this.getCarried();
                if (canItemQuickReplace(slot, itemstack, true) && slot.mayPlace(itemstack) && (this.quickcraftType == 2 || itemstack.getCount() > this.quickcraftSlots.size()) && this.canDragTo(slot)) {
                    this.quickcraftSlots.add(slot);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (this.quickcraftSlots.size() == 1) {
                        int l = (this.quickcraftSlots.iterator().next()).index;
                        this.resetQuickCraft();
                        this.doClick(l, this.quickcraftType, ClickType.PICKUP, p_150434_);
                        return;
                    }

                    ItemStack itemstack3 = this.getCarried().copy();
                    int j1 = this.getCarried().getCount();

                    for(Slot slot1 : this.quickcraftSlots) {
                        ItemStack itemstack1 = this.getCarried();
                        if (slot1 != null && canItemQuickReplace(slot1, itemstack1, true) && slot1.mayPlace(itemstack1) && (this.quickcraftType == 2 || itemstack1.getCount() >= this.quickcraftSlots.size()) && this.canDragTo(slot1)) {
                            ItemStack itemstack2 = itemstack3.copy();
                            int j = slot1.hasItem() ? slot1.getItem().getCount() : 0;
                            //getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, itemstack2, j);
                            int k = Math.min(itemstack2.getMaxStackSize(), slot1.getMaxStackSize(itemstack2));
                            if (itemstack2.getCount() > k) {
                                itemstack2.setCount(k);
                            }

                            j1 -= itemstack2.getCount() - j;
                            slot1.set(itemstack2);
                        }
                    }

                    itemstack3.setCount(j1);
                    this.setCarried(itemstack3);
                }

                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((p_150433_ == ClickType.PICKUP || p_150433_ == ClickType.QUICK_MOVE) && (p_150432_ == 0 || p_150432_ == 1)) {
            ClickAction clickaction = p_150432_ == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (p_150431_ == -999) {
                if (!this.getCarried().isEmpty()) {
                    if (clickaction == ClickAction.PRIMARY) {
                        p_150434_.drop(this.getCarried(), true);
                        this.setCarried(ItemStack.EMPTY);
                    } else {
                        p_150434_.drop(this.getCarried().split(1), true);
                    }
                }
            } else if (p_150433_ == ClickType.QUICK_MOVE) {
                if (p_150431_ < 0) {
                    return;
                }

                Slot slot6 = this.slots.get(p_150431_);
                if (!slot6.mayPickup(p_150434_)) {
                    return;
                }

                for(ItemStack itemstack9 = this.quickMoveStack(p_150434_, p_150431_); !itemstack9.isEmpty() && ItemStack.isSameItem(slot6.getItem(), itemstack9); itemstack9 = this.quickMoveStack(p_150434_, p_150431_)) {
                }
            } else {
                if (p_150431_ < 0) {
                    return;
                }

                Slot slot7 = this.slots.get(p_150431_);
                ItemStack itemstack10 = slot7.getItem();
                ItemStack itemstack11 = this.getCarried();
                p_150434_.updateTutorialInventoryAction(itemstack11, slot7.getItem(), clickaction);
                if (!itemstack11.overrideStackedOnOther(slot7, clickaction, p_150434_) && !itemstack10.overrideOtherStackedOnMe(itemstack11, slot7, clickaction, p_150434_, this.createCarriedSlotAccess())) {
                    if (itemstack10.isEmpty()) {
                        if (!itemstack11.isEmpty()) {
                            int l2 = clickaction == ClickAction.PRIMARY ? itemstack11.getCount() : 1;
                            this.setCarried(slot7.safeInsert(itemstack11, l2));
                        }
                    } else if (slot7.mayPickup(p_150434_)) {
                        if (itemstack11.isEmpty()) {


                            int toMove;
                            if (slot7 instanceof JarSlot) {
                                if (itemstack10.getMaxStackSize() < itemstack10.getCount())
                                    toMove = p_150432_ == 0 ? itemstack10.getMaxStackSize() : (itemstack10.getMaxStackSize() + 1) / 2;
                                else toMove = p_150432_ == 0 ? itemstack10.getCount() : (itemstack10.getCount() + 1) / 2;
                            } else {
                                toMove = p_150432_ == 0 ? itemstack10.getCount() : (itemstack10.getCount() + 1) / 2;
                            }
                            //int toMove = p_150432_ == 0 ? itemstack10.getCount() : (itemstack10.getCount() + 1) / 2;
                            this.setCarried(slot7.remove(toMove));

                            if (itemstack10.isEmpty()) {
                                slot7.set(ItemStack.EMPTY);
                            }

                            slot7.onTake(p_150434_, this.getCarried());

                            /////
//                            int i3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : (itemstack10.getCount() + 1) / 2;
//                            Optional<ItemStack> optional1 = slot7.tryRemove(i3, Integer.MAX_VALUE, p_150434_);
//                            optional1.ifPresent((p_150421_) -> {
//                                this.setCarried(p_150421_);
//                                slot7.onTake(p_150434_, p_150421_);
//                            });
                        } else if (slot7.mayPlace(itemstack11)) {
                            if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) {
                                int j3 = clickaction == ClickAction.PRIMARY ? itemstack11.getCount() : 1;
                                this.setCarried(slot7.safeInsert(itemstack11, j3));
                            } else if (itemstack11.getCount() <= slot7.getMaxStackSize(itemstack11)) {
                                slot7.set(itemstack11);
                                this.setCarried(itemstack10);
                            }
                        } else if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) {
                            Optional<ItemStack> optional = slot7.tryRemove(itemstack10.getCount(), itemstack11.getMaxStackSize() - itemstack11.getCount(), p_150434_);
                            optional.ifPresent((p_150428_) -> {
                                itemstack11.grow(p_150428_.getCount());
                                slot7.onTake(p_150434_, p_150428_);
                            });
                        }
                    }
                }

                slot7.setChanged();
            }
        }
//        else if (p_150433_ == ClickType.SWAP) {
//            Slot slot2 = this.slots.get(p_150431_);
//            ItemStack itemstack4 = inventory.getItem(p_150432_);
//            ItemStack itemstack7 = slot2.getItem();
//            if (!itemstack4.isEmpty() || !itemstack7.isEmpty()) {
//                if (itemstack4.isEmpty()) {
//                    if (slot2.mayPickup(p_150434_)) {
//                        inventory.setItem(p_150432_, itemstack7);
//                        slot2.onSwapCraft(itemstack7.getCount());
//                        slot2.set(ItemStack.EMPTY);
//                        slot2.onTake(p_150434_, itemstack7);
//                    }
//                } else if (itemstack7.isEmpty()) {
//                    if (slot2.mayPlace(itemstack4)) {
//                        int l1 = slot2.getMaxStackSize(itemstack4);
//                        if (itemstack4.getCount() > l1) {
//                            slot2.set(itemstack4.split(l1));
//                        } else {
//                            inventory.setItem(p_150432_, ItemStack.EMPTY);
//                            slot2.set(itemstack4);
//                        }
//                    }
//                } else if (slot2.mayPickup(p_150434_) && slot2.mayPlace(itemstack4)) {
//                    int i2 = slot2.getMaxStackSize(itemstack4);
//                    if (itemstack4.getCount() > i2) {
//                        slot2.set(itemstack4.split(i2));
//                        slot2.onTake(p_150434_, itemstack7);
//                        if (!inventory.add(itemstack7)) {
//                            p_150434_.drop(itemstack7, true);
//                        }
//                    } else {
//                        inventory.setItem(p_150432_, itemstack7);
//                        slot2.set(itemstack4);
//                        slot2.onTake(p_150434_, itemstack7);
//                    }
//                }
//            }
//        }
        else if (p_150433_ == ClickType.CLONE && p_150434_.getAbilities().instabuild && this.getCarried().isEmpty() && p_150431_ >= 0) {
            Slot slot5 = this.slots.get(p_150431_);
            if (slot5.hasItem()) {
                ItemStack itemstack6 = slot5.getItem().copy();
                itemstack6.setCount(itemstack6.getMaxStackSize());
                this.setCarried(itemstack6);
            }
        } else if (p_150433_ == ClickType.THROW && this.getCarried().isEmpty() && p_150431_ >= 0) {
            Slot slot4 = this.slots.get(p_150431_);
            int i1 = p_150432_ == 0 ? 1 : slot4.getItem().getCount();
            ItemStack itemstack8 = slot4.safeTake(i1, Integer.MAX_VALUE, p_150434_);
            p_150434_.drop(itemstack8, true);
        } else if (p_150433_ == ClickType.PICKUP_ALL && p_150431_ >= 0) {
            Slot slot3 = this.slots.get(p_150431_);
            ItemStack itemstack5 = this.getCarried();
            if (!itemstack5.isEmpty() && (!slot3.hasItem() || !slot3.mayPickup(p_150434_))) {
                int k1 = p_150432_ == 0 ? 0 : this.slots.size() - 1;
                int j2 = p_150432_ == 0 ? 1 : -1;

                for(int k2 = 0; k2 < 2; ++k2) {
                    for(int k3 = k1; k3 >= 0 && k3 < this.slots.size() && itemstack5.getCount() < itemstack5.getMaxStackSize(); k3 += j2) {
                        Slot slot8 = this.slots.get(k3);
                        if (slot8.hasItem() && canItemQuickReplace(slot8, itemstack5, true) && slot8.mayPickup(p_150434_) && this.canTakeItemForPickAll(itemstack5, slot8)) {
                            ItemStack itemstack12 = slot8.getItem();
                            if (k2 != 0 || itemstack12.getCount() != itemstack12.getMaxStackSize()) {
                                ItemStack itemstack13 = slot8.safeTake(itemstack12.getCount(), itemstack5.getMaxStackSize() - itemstack5.getCount(), p_150434_);
                                itemstack5.grow(itemstack13.getCount());
                            }
                        }
                    }
                }
            }
        }

    }


    private SlotAccess createCarriedSlotAccess() {
        return new SlotAccess() {
            public ItemStack get() {
                return HerbJarContainer.this.getCarried();
            }

            public boolean set(ItemStack p_150452_) {
                HerbJarContainer.this.setCarried(p_150452_);
                return true;
            }
        };
    }


    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = startIndex;

        if (reverseDirection) {
            i = endIndex - 1;
        }

        while (!stack.isEmpty()) {
            if (reverseDirection) {
                if (i < startIndex) break;
            } else {
                if (i >= endIndex) break;
            }

            Slot slot = this.slots.get(i);
            ItemStack itemstack = slot.getItem();

            if (!itemstack.isEmpty() && itemstack.getItem() == stack.getItem() && ItemStack.isSameItemSameTags(stack, itemstack)) {
                int j = itemstack.getCount() + stack.getCount();
                int maxSize = slot.getMaxStackSize(itemstack);

                if (j <= maxSize) {
                    stack.setCount(0);
                    itemstack.setCount(j);
                    slot.setChanged();
                    flag = true;
                } else if (itemstack.getCount() < maxSize) {
                    stack.shrink(maxSize - itemstack.getCount());
                    itemstack.setCount(maxSize);
                    slot.setChanged();
                    flag = true;
                }
            }

            i += (reverseDirection) ? -1 : 1;
        }

        if (!stack.isEmpty()) {
            if (reverseDirection) i = endIndex - 1;
            else i = startIndex;

            while (true) {
                if (reverseDirection) {
                    if (i < startIndex) break;
                } else {
                    if (i >= endIndex) break;
                }

                Slot slot1 = this.slots.get(i);
                ItemStack itemstack1 = slot1.getItem();

                if (itemstack1.isEmpty() && slot1.mayPlace(stack)) {
                    if (stack.getCount() > slot1.getMaxStackSize(stack)) {
                        slot1.set(stack.split(slot1.getMaxStackSize(stack)));
                    } else {
                        slot1.set(stack.split(stack.getCount()));
                    }

                    slot1.setChanged();
                    flag = true;
                    break;
                }

                i += (reverseDirection) ? -1 : 1;
            }
        }

        return flag;
    }

//
//    @Override
//    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
//        boolean flag = false;
//        int i = startIndex;
//        if (reverseDirection) {
//            i = endIndex - 1;
//        }
//
//        if (stack.isStackable()) {
//            while(!stack.isEmpty()) {
//                if (reverseDirection) {
//                    if (i < startIndex) {
//                        break;
//                    }
//                } else if (i >= endIndex) {
//                    break;
//                }
//
//                Slot slot = this.slots.get(i);
//                ItemStack itemstack = slot.getItem();
//                if (!itemstack.isEmpty() && areItemsAndTagsEqual(stack, itemstack)) {
//                    int j = itemstack.getCount() + stack.getCount();
//                    int maxSize = 1024;
//                    if (j <= maxSize) {
//                        stack.setCount(0);
//                        itemstack.setCount(j);
//                        slot.setChanged();
//                        flag = true;
//                    } else if (itemstack.getCount() < maxSize) {
//                        stack.shrink(maxSize - itemstack.getCount());
//                        itemstack.setCount(maxSize);
//                        slot.setChanged();
//                        flag = true;
//                    }
//                }
//
//                if (reverseDirection) {
//                    --i;
//                } else {
//                    ++i;
//                }
//            }
//        }
//
//        if (!stack.isEmpty()) {
//            if (reverseDirection) {
//                i = endIndex - 1;
//            } else {
//                i = startIndex;
//            }
//
//            while(true) {
//                if (reverseDirection) {
//                    if (i < startIndex) {
//                        break;
//                    }
//                } else if (i >= endIndex) {
//                    break;
//                }
//
//                Slot slot1 = this.slots.get(i);
//                ItemStack itemstack1 = slot1.getItem();
//                if (itemstack1.isEmpty() && slot1.mayPlace(stack)) {
//                    if (stack.getCount() > 1024) {
//                        slot1.set(stack.split(1024));
//                    } else {
//                        slot1.set(stack.split(stack.getCount()));
//                    }
//
//                    slot1.setChanged();
//                    flag = true;
//                    break;
//                }
//
//                if (reverseDirection) {
//                    --i;
//                } else {
//                    ++i;
//                }
//            }
//        }
//
//        return flag;
//    }


    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos()),
                playerIn, ModBlocks.HERB_JAR.get());
    }

    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }

        return index;
    }

    private int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0; j < verAmount; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }

        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);

        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our BlockEntity slot numbers 0 - 8)

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 1;  // must match TileEntityInventoryBasic.NUMBER_OF_SLOTS

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();


        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + index);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerEntity, sourceStack);
        return copyOfSourceStack;
    }
}
