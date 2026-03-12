package igentuman.spatialcrafter.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.common.gui.textures.Textures;
import mezz.jei.gui.input.UserInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IngredientsButton {

	private Rect2i bounds;

	public static IngredientsButton create(MultiblockStructureRecipe recipe) {
		Textures textures = Internal.getTextures();
		return new IngredientsButton(recipe);
	}

	public MultiblockStructureRecipe recipe;
	private IngredientsButton(MultiblockStructureRecipe recipe) {
		this.recipe = recipe;
	}

	public void updateBounds(Rect2i bounds) {
		this.bounds = bounds;
	}

	public void drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		List<Component> tooltip = new ArrayList<>();
		for(ItemStack i : recipe.getStructureIngredients().getItems())
			tooltip.add(Component.literal(i.getCount() + "x ").append(i.getHoverName()));
	}

	public void draw(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		IDrawable icon = Internal.getTextures().getBookmarkButtonEnabledIcon();
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(8, 8, 0);
		guiGraphics.pose().scale(0.5f, 0.5f, 1f);
		icon.draw(guiGraphics, mouseX, mouseY);
		guiGraphics.pose().popPose();
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		if (bounds == null) return false;
		return mouseX >= bounds.getX() && mouseX <= bounds.getX() + bounds.getWidth() &&
				mouseY >= bounds.getY() && mouseY <= bounds.getY() + bounds.getHeight();
	}
}
