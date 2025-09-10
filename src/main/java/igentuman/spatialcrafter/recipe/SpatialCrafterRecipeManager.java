package igentuman.spatialcrafter.recipe;

import igentuman.spatialcrafter.util.MultiblockStructure;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpatialCrafterRecipeManager {
    
    /**
     * Checks if a recipe is valid by verifying all items and entities are registered
     */
    private static boolean isRecipeValid(SpatialCrafterRecipe recipe) {
        // Check all output items are registered
        for (ItemStack output : recipe.getOutputs()) {
            if (ForgeRegistries.ITEMS.getValue(ForgeRegistries.ITEMS.getKey(output.getItem())) == null) {
                return false;
            }
        }
        
        // Check all entity outputs are registered
        for (SpatialCrafterRecipe.EntityOutput entityOutput : recipe.getEntityOutputs()) {
            if (ForgeRegistries.ENTITY_TYPES.getValue(ForgeRegistries.ENTITY_TYPES.getKey(entityOutput.getEntityType())) == null) {
                return false;
            }
        }
        
        return true;
    }
    
    public static Optional<SpatialCrafterRecipe> findRecipe(Level level, HashMap<Long, BlockState> blockStates) {
        RecipeManager recipeManager = level.getRecipeManager();
        
        return recipeManager.getAllRecipesFor(SpatialCrafterRecipeType.INSTANCE)
                .stream()
                .filter(SpatialCrafterRecipeManager::isRecipeValid)
                .filter(recipe -> recipe.matchesBlocks(blockStates))
                .findFirst();
    }
    
    public static List<SpatialCrafterRecipe> getAllRecipes(Level level) {
        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getAllRecipesFor(SpatialCrafterRecipeType.INSTANCE)
                .stream()
                .filter(SpatialCrafterRecipeManager::isRecipeValid)
                .collect(Collectors.toList());
    }
    
    public static List<SpatialCrafterRecipe> getRecipesForMultiblock(Level level, MultiblockStructure structure) {
        RecipeManager recipeManager = level.getRecipeManager();
        
        return recipeManager.getAllRecipesFor(SpatialCrafterRecipeType.INSTANCE)
                .stream()
                .filter(SpatialCrafterRecipeManager::isRecipeValid)
                .filter(recipe -> recipe.matchesMultiblock(structure))
                .collect(Collectors.toList());
    }
}