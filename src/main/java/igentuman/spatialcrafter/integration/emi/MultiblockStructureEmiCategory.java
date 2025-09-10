package igentuman.spatialcrafter.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static igentuman.spatialcrafter.Main.MODID;
import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_BLOCK;

public class MultiblockStructureEmiCategory extends EmiRecipeCategory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "multiblock_structure");
    public static final MultiblockStructureEmiCategory INSTANCE = new MultiblockStructureEmiCategory();
    
    public MultiblockStructureEmiCategory() {
        super(ID, EmiStack.of(SPATIAL_CRAFTER_BLOCK.get()), EmiTexture.EMPTY_ARROW);
    }

    @Override
    public Component getName() {
        return Component.translatable("emi.category." + MODID + ".multiblock_structure");
    }
}