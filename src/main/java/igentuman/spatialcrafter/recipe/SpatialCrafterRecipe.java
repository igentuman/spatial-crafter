package igentuman.spatialcrafter.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import igentuman.spatialcrafter.util.MultiblockStructure;
import igentuman.spatialcrafter.util.MultiblocksProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpatialCrafterRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final ResourceLocation multiblockId;
    private final List<ItemStack> outputs;
    private final List<EntityOutput> entityOutputs;
    private final int processingTime;
    private final int energyConsumption;
    private final CompoundTag outputNbt;
    
    public SpatialCrafterRecipe(ResourceLocation id, ResourceLocation multiblockId, 
                               List<ItemStack> outputs, List<EntityOutput> entityOutputs,
                               int processingTime, int energyConsumption, CompoundTag outputNbt) {
        this.id = id;
        this.multiblockId = multiblockId;
        this.outputs = outputs;
        this.entityOutputs = entityOutputs;
        this.processingTime = processingTime;
        this.energyConsumption = energyConsumption;
        this.outputNbt = outputNbt;
    }

    @Override
    public boolean matches(Container container, Level level) {
        // This will be handled by the block entity logic
        return true;
    }

    public boolean matchesMultiblock(MultiblockStructure structure) {
        return structure.getId().equals(this.multiblockId);
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SpatialCrafterRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return SpatialCrafterRecipeType.INSTANCE;
    }

    // Getters
    public ResourceLocation getMultiblockId() { return multiblockId; }
    public List<ItemStack> getOutputs() { return outputs; }
    public List<EntityOutput> getEntityOutputs() { return entityOutputs; }
    public int getProcessingTime() { return processingTime; }
    public int getEnergyConsumption() { return energyConsumption; }
    public CompoundTag getOutputNbt() { return outputNbt; }

    public static class EntityOutput {
        private final EntityType<?> entityType;
        private final CompoundTag nbt;
        private final BlockPos relativePos;
        private final int count;

        public EntityOutput(EntityType<?> entityType, CompoundTag nbt, BlockPos relativePos, int count) {
            this.entityType = entityType;
            this.nbt = nbt;
            this.relativePos = relativePos;
            this.count = count;
        }

        public EntityType<?> getEntityType() { return entityType; }
        public CompoundTag getNbt() { return nbt; }
        public BlockPos getRelativePos() { return relativePos; }
        public int getCount() { return count; }
    }
}