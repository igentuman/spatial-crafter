package igentuman.spatialcrafter.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipe;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipeManager;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipeType;
import igentuman.spatialcrafter.util.MultiblocksProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@EmiEntrypoint
public class EMIPlugin implements EmiPlugin {
    
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(MultiblockStructureEmiCategory.INSTANCE);
        
        // Load and register multiblock structure recipes
        List<MultiblockStructureEmiRecipe> multiblockRecipes = loadMultiblockStructures();
        for(MultiblockStructureEmiRecipe recipe : multiblockRecipes) {
            registry.addRecipe(recipe);
        }
    }
    
    private List<MultiblockStructureEmiRecipe> loadMultiblockStructures() {
        List<MultiblockStructureEmiRecipe> recipes = new ArrayList<>();
        
        // Load basic multiblock structures
        MultiblocksProvider.getStructures()
                .stream()
                .map(structure -> new MultiblockStructureEmiRecipe(
                        structure.getId(),
                        structure.getStructureNbt(),
                        structure.getName(),
                        null)) // No spatial recipe initially
                .forEach(recipes::add);
        
        // Load spatial crafter recipes and enhance multiblock recipes with outputs
        if (Minecraft.getInstance().level != null) {
            List<SpatialCrafterRecipe> spatialRecipes = SpatialCrafterRecipeManager.getAllRecipes(Minecraft.getInstance().level);
            
            // Create enhanced recipes with spatial crafter recipe data
            for (SpatialCrafterRecipe spatialRecipe : spatialRecipes) {
                // Find matching multiblock structure
                MultiblocksProvider.getStructures().stream()
                        .filter(structure -> structure.getId().equals(spatialRecipe.getMultiblockId()))
                        .findFirst()
                        .ifPresent(structure -> {
                            // Replace the basic recipe with enhanced one
                            recipes.removeIf(recipe -> recipe.getId().equals(structure.getId()));
                            recipes.add(new MultiblockStructureEmiRecipe(
                                    structure.getId(),
                                    structure.getStructureNbt(),
                                    structure.getName(),
                                    spatialRecipe));
                        });
            }
        }
        
        return recipes;
    }
}