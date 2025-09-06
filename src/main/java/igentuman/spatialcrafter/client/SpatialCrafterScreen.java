package igentuman.spatialcrafter.client;

import igentuman.spatialcrafter.container.SpatialCrafterContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static igentuman.spatialcrafter.Main.rl;

public class SpatialCrafterScreen extends AbstractContainerScreen<SpatialCrafterContainer> {

    private final ResourceLocation GUI = rl("textures/gui/spatial_crafter.png");

    public SpatialCrafterScreen(SpatialCrafterContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        imageWidth = 180;
        imageHeight = 152;
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
    }

    public void containerTick() {
        super.containerTick();

    }

    public void drawEnergyBar(GuiGraphics graphics)
    {
        graphics.blit(GUI, getGuiLeft()+4, getGuiTop()+67, 0, 153, menu.getEnergyScaled(171), 7);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(Minecraft.getInstance().font, I18n.get("gui.block_booster"), imageWidth/2, 4, 0xffffff );

        if(menu.isDisabled()) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.block_booster.disabled"), 10, 65, 0xffffff);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int x, int y) {
        if(x > getGuiLeft()+4 && x < getGuiLeft()+175 && y > getGuiTop()+65 && y < getGuiTop()+78) {
            Component textComponent = Component.translatable("gui.energy.info", menu.getEnergy(), menu.getMaxEnergy());
            graphics.renderTooltip(Minecraft.getInstance().font, textComponent, x, y);
        }
    }
}