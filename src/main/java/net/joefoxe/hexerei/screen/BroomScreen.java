package net.joefoxe.hexerei.screen;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.joefoxe.hexerei.Hexerei;
import net.joefoxe.hexerei.client.renderer.entity.custom.BroomEntity;
import net.joefoxe.hexerei.container.BroomContainer;
import net.joefoxe.hexerei.util.HexereiTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.joefoxe.hexerei.container.BroomContainer.OFFSET;
import static net.joefoxe.hexerei.util.HexereiUtil.moveTo;

public class BroomScreen extends AbstractContainerScreen<BroomContainer> {
    private final ResourceLocation GUI = new ResourceLocation(Hexerei.MOD_ID,
            "textures/gui/broom_gui.png");
    private final ResourceLocation INVENTORY = new ResourceLocation(Hexerei.MOD_ID,
            "textures/gui/inventory.png");

    public final BroomEntity broomEntity;
    public float dropdownOffset = 0;
    public int offset = 0;
    public boolean dropdownClicked = false;

    public BroomScreen(BroomContainer broomContainer, Inventory inv, Component titleIn) {
        super(broomContainer, inv, titleIn);
        broomEntity = broomContainer.broomEntity;
        titleLabelY = 1 - OFFSET;
        titleLabelX = 4;
        inventoryLabelY = 94 - OFFSET;
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (dropdownClicked)
            dropdownOffset = moveTo(dropdownOffset, 58, 4f);
        else
            dropdownOffset = moveTo(dropdownOffset, 0, 4f);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {


        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
        this.renderButtonTooltip(g, mouseX, mouseY);
    }

    @Override
    public Component getTitle() {
        return super.getTitle();
    }

    public boolean isHovering(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }


    public void renderButtonTooltip(GuiGraphics g, int mouseX, int mouseY) {

        int offset = 0;
        if (broomEntity.getModule(BroomEntity.BroomSlot.SATCHEL).is(HexereiTags.Items.SMALL_SATCHELS))
            offset = 21;
        if (broomEntity.getModule(BroomEntity.BroomSlot.SATCHEL).is(HexereiTags.Items.MEDIUM_SATCHELS))
            offset = 42;
        if (broomEntity.getModule(BroomEntity.BroomSlot.SATCHEL).is(HexereiTags.Items.LARGE_SATCHELS))
            offset = 63;
//        if(dropdownOffset > 29){
//            if (x > this.leftPos + 188.25f && x < this.leftPos + 188.25f + 18 && y > this.topPos + 88 + offset + ((int)dropdownOffset) && y < this.topPos + 88 + offset + ((int)dropdownOffset) + 18) {
//                this.menu.setFloatMode(false);
//                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//
//            }
//            if (x > this.leftPos + 188.25f && x < this.leftPos + 188.25f + 18 && y > this.topPos + 60 + offset + ((int)dropdownOffset) && y < this.topPos + 60 + offset + ((int)dropdownOffset) + 18) {
//                this.menu.setFloatMode(true);
//                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//
//            }
//
//        }
//        if(x > this.leftPos + 188.25f && x < this.leftPos + 188.25f + 18 &&  y > this.topPos + 89 + offset && y < this.topPos + 89 + 18 + offset){
//            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//            dropdownClicked = !dropdownClicked;
//        }

        List<Component> components = new ArrayList<>();
        if (isHovering(mouseX, mouseY, 188.25D, 89 + offset - OFFSET, 18D, 18D)) {
            components.add(Component.translatable("tooltip.hexerei.broom_settings"));
            g.renderTooltip(Minecraft.getInstance().font, components, Optional.empty(), mouseX, mouseY);
            //this.renderTooltip(g, components, Optional.empty(), mouseX, mouseY, Minecraft.getInstance().font, ItemStack.EMPTY);
        } else if (dropdownOffset > 29) {
            if (isHovering(mouseX, mouseY, 188.25D, 88 + offset + ((int) dropdownOffset) - OFFSET, 18D, 18D)) {
                components.add(Component.translatable("tooltip.hexerei.broom_float_mode_off"));
                if (Screen.hasShiftDown()) {
                    components.add(Component.translatable("<%s>", Component.translatable("tooltip.hexerei.shift").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAA6600)))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x999999))));
                    components.add(Component.translatable("tooltip.hexerei.broom_float_mode_off_0").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x999999))));
                } else {
                    components.add(Component.translatable("[%s]", Component.translatable("tooltip.hexerei.shift").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAA00)))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x999999))));
                }
                g.renderTooltip(Minecraft.getInstance().font, components, Optional.empty(), mouseX, mouseY);
            }

            if (isHovering(mouseX, mouseY, 188.25D, 60 + offset + ((int) dropdownOffset) - OFFSET, 18D, 18D)) {
                components.add(Component.translatable("tooltip.hexerei.broom_float_mode_on"));
                if (Screen.hasShiftDown()) {
                    components.add(Component.translatable("<%s>", Component.translatable("tooltip.hexerei.shift").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAA6600)))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x999999))));
                    components.add(Component.translatable("tooltip.hexerei.broom_float_mode_on_0").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x999999))));
                    components.add(Component.translatable("tooltip.hexerei.broom_float_mode_on_1").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x999999))));
                } else {
                    components.add(Component.translatable("[%s]", Component.translatable("tooltip.hexerei.shift").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAA00)))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x999999))));
                }
                g.renderTooltip(Minecraft.getInstance().font, components, Optional.empty(), mouseX, mouseY);
            }
        }


    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI);


        int i = this.leftPos;
        int j = this.topPos;
        this.offset = 0;
        if (broomEntity.getModule(BroomEntity.BroomSlot.SATCHEL).is(HexereiTags.Items.SMALL_SATCHELS))
            offset = 21;
        if (broomEntity.getModule(BroomEntity.BroomSlot.SATCHEL).is(HexereiTags.Items.MEDIUM_SATCHELS))
            offset = 42;
        if (broomEntity.getModule(BroomEntity.BroomSlot.SATCHEL).is(HexereiTags.Items.LARGE_SATCHELS))
            offset = 63;
        inventoryLabelY = 94 + offset - OFFSET;

        g.blit(GUI, i + 184, j + 55 + offset + ((int) dropdownOffset) - OFFSET, 230, 164, 26, 58);
        if (this.menu.getFloatMode()) {
            g.blit(GUI, i + 188, j + 60 + offset + ((int) dropdownOffset) - OFFSET, 238, 106, 18, 18);
        } else {
            g.blit(GUI, i + 188, j + 88 + offset + ((int) dropdownOffset) - OFFSET, 238, 70, 18, 18);
        }

        g.blit(GUI, i, j - 3 - OFFSET, 0, 0, 214, 82);
        g.blit(GUI, i, j + 79 + offset - OFFSET, 0, 82, 214, 34);


        if (!broomEntity.getModule(BroomEntity.BroomSlot.MISC).isEmpty())
            g.blit(GUI, i + 37, j + 47 - OFFSET, 235, 31, 16, 16);
        if (!broomEntity.getModule(BroomEntity.BroomSlot.SATCHEL).isEmpty())
            g.blit(GUI, i + 99, j + 47 - OFFSET, 235, 31, 16, 16);
        if (!broomEntity.getModule(BroomEntity.BroomSlot.BRUSH).isEmpty())
            g.blit(GUI, i + 160, j + 47 - OFFSET, 235, 31, 16, 16);

        if (!broomEntity.isEnder()) {
            ItemStack satchel = broomEntity.getModule(BroomEntity.BroomSlot.SATCHEL);
            if (satchel.is(HexereiTags.Items.SMALL_SATCHELS)) {
                g.blit(GUI, i, j + 79 - OFFSET, 0, 116, 214, 21);
            }

            if (satchel.is(HexereiTags.Items.MEDIUM_SATCHELS)) {
                g.blit(GUI, i, j + 79 - OFFSET, 0, 116, 214, 21);
                g.blit(GUI, i, j + 79 + 21 - OFFSET, 0, 116, 214, 21);
            }

            if (satchel.is(HexereiTags.Items.LARGE_SATCHELS)) {
                g.blit(GUI, i, j + 79 - OFFSET, 0, 116, 214, 21);
                g.blit(GUI, i, j + 79 + 21 - OFFSET, 0, 116, 214, 21);
                g.blit(GUI, i, j + 79 + 42 - OFFSET, 0, 116, 214, 21);
            }
        } else {

            g.blit(GUI, i, j + 79 - OFFSET, 0, 200, 214, 21);
            g.blit(GUI, i, j + 79 + 21 - OFFSET, 0, 200, 214, 21);
            g.blit(GUI, i, j + 79 + 42 - OFFSET, 0, 200, 214, 21);
            g.blit(GUI, i, j + 79 + 42 - OFFSET + 21, 0, 221, 214, 5);
            g.blit(GUI, i, j + 75 - OFFSET, 0, 137, 214, 72);
        }


        g.blit(GUI, i + 94, j - 30 - OFFSET, 230, 0, 26, 26);

        if (this.dropdownClicked)
            g.blit(GUI, i + 188, j + 89 + offset - OFFSET, 238, 124, 18, 18);

        RenderSystem.setShaderTexture(0, INVENTORY);
        g.blit(GUI, i + 3, j + 88 + offset - OFFSET, 0, 0, 176, 100);

        RenderSystem.setShaderTexture(0, GUI);
        int extraBrush = broomEntity.getExtraBrush();
        if (broomEntity.isReplacer() && extraBrush != -1) {

            g.blit(GUI, i + 12 + 21 * ((extraBrush - 3) % 9), j + 21 * ((extraBrush - 3) / 9) + 79 - OFFSET, 234, 142, 22, 22);
        }


        //Rendering the coffer item at the top of the screen
        Minecraft minecraft = Minecraft.getInstance();


        ItemRenderer itemRenderer = minecraft.getItemRenderer();


        RenderSystem.disableDepthTest();
        g.renderItem(broomEntity.getPickResult(),
                this.leftPos + 99,
                this.topPos - 25 - OFFSET);

//        matrixStack.translate(this.leftPos + 42*1.666f, this.topPos + 14*1.666f, 0f);
        MutableComponent misc = Component.translatable("tooltip.hexerei.broom_misc");
        MutableComponent satchel = Component.translatable("tooltip.hexerei.broom_satchel");
        MutableComponent brush = Component.translatable("tooltip.hexerei.broom_brush");

        g.renderTooltip(Minecraft.getInstance().font, misc, this.leftPos + 34, this.topPos + 29 - OFFSET);
        g.renderTooltip(Minecraft.getInstance().font, satchel, this.leftPos + 34, this.topPos + 29 - OFFSET);
        g.renderTooltip(Minecraft.getInstance().font, brush, this.leftPos + 34, this.topPos + 29 - OFFSET);

        /*minecraft.font.draw(matrixStack, misc, this.leftPos + 34, this.topPos + 29 - OFFSET, 0xFF606060);
        minecraft.font.draw(matrixStack, satchel, this.leftPos + 89, this.topPos + 29 - OFFSET, 0xFF606060);
        minecraft.font.draw(matrixStack, brush, this.leftPos + 154, this.topPos + 29 - OFFSET, 0xFF606060); */
//        InventoryScreen.renderEntityInInventory(this.leftPos + 107, this.topPos + 88, 20, (float)(this.leftPos + 107 - x) , (float)(this.topPos + 88 - 30 - y), (LivingEntity) broomEntity);
//
//        RenderSystem.enableDepthTest();


    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean mouseClicked = super.mouseClicked(x, y, button);


        if (dropdownOffset > 29) {
            if (x > this.leftPos + 188.25f && x < this.leftPos + 188.25f + 18 && y > this.topPos + 88 + this.offset + ((int) dropdownOffset) - OFFSET && y < this.topPos + 88 + this.offset + ((int) dropdownOffset) + 18 - OFFSET) {
                this.menu.setFloatMode(false);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            }
            if (x > this.leftPos + 188.25f && x < this.leftPos + 188.25f + 18 && y > this.topPos + 60 + this.offset + ((int) dropdownOffset) - OFFSET && y < this.topPos + 60 + this.offset + ((int) dropdownOffset) + 18 - OFFSET) {
                this.menu.setFloatMode(true);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            }

        }
        if (x > this.leftPos + 188.25f && x < this.leftPos + 188.25f + 18 && y > this.topPos + 89 + this.offset - OFFSET && y < this.topPos + 89 + 18 + this.offset - OFFSET) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            dropdownClicked = !dropdownClicked;
        }

//        this.menu.playSound();

        return mouseClicked;
    }
}