package igentuman.spatialcrafter.client;

import igentuman.spatialcrafter.container.SpatialCrafterContainer;
import igentuman.spatialcrafter.network.NetworkHandler;
import igentuman.spatialcrafter.network.SizeChangePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static igentuman.spatialcrafter.Main.rl;

public class SpatialCrafterScreen extends AbstractContainerScreen<SpatialCrafterContainer> {

    private final ResourceLocation GUI = rl("textures/gui/spatial_crafter.png");
    private Button decreaseSizeButton;
    private Button increaseSizeButton;

    public SpatialCrafterScreen(SpatialCrafterContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        imageWidth = 180;
        imageHeight = 168;
    }


    @Override
    public void renderBg(GuiGraphics graphics, float pPartialTick, int pMouseX, int pMouseY) {
        this.renderBackground(graphics);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
        this.renderTooltip(graphics, pMouseX, pMouseY);
        drawEnergyBar(graphics);
    }

    protected void init() {
        super.init();
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        // Decrease size button (-)
        decreaseSizeButton = Button.builder(Component.translatable("button.spatialcrafter.size_decrease"), button -> {
            int currentSize = menu.getSize();
            if (currentSize > 1) {
                NetworkHandler.INSTANCE.sendToServer(new SizeChangePacket(menu.getBlockEntity().getBlockPos(), currentSize - 1));
            }
        })
        .bounds(relX + 10, relY + 50, 20, 20)
        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.spatialcrafter.decrease_size")))
        .build();
        
        // Increase size button (+)
        increaseSizeButton = Button.builder(Component.translatable("button.spatialcrafter.size_increase"), button -> {
            int currentSize = menu.getSize();
            if (currentSize < 31) {
                NetworkHandler.INSTANCE.sendToServer(new SizeChangePacket(menu.getBlockEntity().getBlockPos(), currentSize + 1));
            }
        })
        .bounds(relX + 150, relY + 50, 20, 20)
        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.spatialcrafter.increase_size")))
        .build();
        
        addRenderableWidget(decreaseSizeButton);
        addRenderableWidget(increaseSizeButton);
    }

    public void containerTick() {
        super.containerTick();
        
        // Update button states based on current size
        if (decreaseSizeButton != null && increaseSizeButton != null) {
            int currentSize = menu.getSize();
            decreaseSizeButton.active = currentSize > 1;
            increaseSizeButton.active = currentSize < 31;
        }
    }

    public void drawEnergyBar(GuiGraphics graphics)
    {
        graphics.blit(GUI, getGuiLeft()+4, getGuiTop()+151, 1, 171, menu.getEnergyScaled(172), 7);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(Minecraft.getInstance().font, I18n.get("gui.spatialcrafter.spatial_crafter"), imageWidth/2, 4, 0xffffff );

        // Display current size
        Component sizeText = Component.translatable("gui.spatialcrafter.size", menu.getSize());
        graphics.drawCenteredString(Minecraft.getInstance().font, sizeText, imageWidth/2, 55, 0xffffff);

        if(menu.isDisabled()) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.spatialcrafter.disabled"), 10, 65, 0xffffff);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int x, int y) {
        if(x > getGuiLeft()+4 && x < getGuiLeft()+175 && y > getGuiTop()+150 && y < getGuiTop()+160) {
            Component textComponent = Component.translatable("gui.spatialcrafter.energy.info", menu.getEnergy(), menu.getMaxEnergy());
            graphics.renderTooltip(Minecraft.getInstance().font, textComponent, x, y);
        }
    }
}