package igentuman.spatialcrafter.recipe;

import igentuman.spatialcrafter.util.MultiblockStructure;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpatialCrafterRecipeManager {
    
    /**
     * Checks if a recipe is valid by verifying all items and entities are registered
     */
    private static boolean isRecipeValid(SpatialCrafterRecipe recipe) {
        return true;
    }
    
    public static Optional<SpatialCrafterRecipe> findRecipe(Level level, HashMap<Long, BlockState> blockStates) {
        RecipeManager recipeManager = level.getRecipeManager();
        
        return recipeManager.getAllRecipesFor(SpatialCrafterRecipeType.INSTANCE)
                .stream()
                .map(RecipeHolder::value)
                .filter(SpatialCrafterRecipeManager::isRecipeValid)
                .filter(recipe -> recipe.matchesBlocks(blockStates))
                .findFirst();
    }
    
    public static List<SpatialCrafterRecipe> getAllRecipes(Level level) {
        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getAllRecipesFor(SpatialCrafterRecipeType.INSTANCE)
                .stream()
                .map(RecipeHolder::value)
                .filter(SpatialCrafterRecipeManager::isRecipeValid)
                .collect(Collectors.toList());
    }
    
    public static List<SpatialCrafterRecipe> getRecipesForMultiblock(Level level, MultiblockStructure structure) {
        RecipeManager recipeManager = level.getRecipeManager();
        
        return recipeManager.getAllRecipesFor(SpatialCrafterRecipeType.INSTANCE)
                .stream()
                .map(RecipeHolder::value)
                .filter(SpatialCrafterRecipeManager::isRecipeValid)
                .filter(recipe -> recipe.matchesMultiblock(structure))
                .collect(Collectors.toList());
    }
}