package igentuman.spatialcrafter.integration.jei;

import igentuman.spatialcrafter.recipe.SpatialCrafterRecipe;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipeType;
import igentuman.spatialcrafter.util.MultiblocksProvider;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static igentuman.spatialcrafter.Main.rl;


@JeiPlugin
public class JEIPlugin implements IModPlugin {

    private IIngredientManager ingredientManager;
    @Override
    public ResourceLocation getPluginUid() {
        return rl("jei_plugin");
    }
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MultiblockStructureCategory(registration.getJeiHelpers().getGuiHelper())
        );
        ingredientManager = registration.getJeiHelpers().getIngredientManager();
    }

    public void registerRecipes(IRecipeRegistration registration) {
        List<MultiblockStructureRecipe> multiblockRecipes = loadMultiblockStructures();
        registration.addRecipes(MultiblockStructureCategory.TYPE, multiblockRecipes);
    }

    private List<MultiblockStructureRecipe> loadMultiblockStructures() {
        List<MultiblockStructureRecipe> recipes = new ArrayList<>();
        
        // Load basic multiblock structures
        MultiblocksProvider.getStructures()
                .stream()
                .map(structure -> new MultiblockStructureRecipe(
                        structure.getId(),
                        structure.getStructureNbt(),
                        structure.getName(),
                        ingredientManager))
                .forEach(recipes::add);
        
        // Load spatial crafter recipes and enhance multiblock recipes with outputs
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<SpatialCrafterRecipe> spatialRecipes = recipeManager.getAllRecipesFor(SpatialCrafterRecipeType.INSTANCE);
            
            // Enhance existing multiblock recipes with spatial crafter recipe data
            for (SpatialCrafterRecipe spatialRecipe : spatialRecipes) {
                for (MultiblockStructureRecipe multiblockRecipe : recipes) {
                    if (multiblockRecipe.getId().equals(spatialRecipe.getMultiblockId())) {
                        multiblockRecipe.setSpatialRecipe(spatialRecipe);
                        break;
                    }
                }
            }
        }
        
        return recipes;
    }
}
