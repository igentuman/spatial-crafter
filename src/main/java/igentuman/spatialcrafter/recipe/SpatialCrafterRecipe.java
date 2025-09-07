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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

public class SpatialCrafterRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final ResourceLocation multiblockId;
    private final List<ItemStack> outputs;
    private final List<EntityOutput> entityOutputs;
    private final int processingTime;
    private final int energyConsumption;
    private final CompoundTag outputNbt;
    private final HashMap<Long, BlockState> requiredBlockStates = new HashMap<>();
    private final HashMap<Long, BlockState> requiredBlockStatesCW = new HashMap<>();
    private final HashMap<Long, BlockState> requiredBlockStatesCCW = new HashMap<>();
    private final HashMap<Long, BlockState> requiredBlockStatesOposite = new HashMap<>();

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
        initBlockStates();
    }

    private void initBlockStates() {
        // Get the multiblock structure from the provider
        MultiblockStructure structure = MultiblocksProvider.getStructures().stream()
                .filter(s -> s.getId().equals(this.multiblockId))
                .findFirst()
                .orElse(null);
        
        if (structure == null) {
            return; // Structure not found
        }
        
        // Clear existing lists
        requiredBlockStates.clear();
        requiredBlockStatesCW.clear();
        requiredBlockStatesCCW.clear();
        requiredBlockStatesOposite.clear();
        
        // Get all block states from the original structure
        Map<BlockPos, BlockState> originalBlocks = structure.getBlocks();
        
        // Fill the original orientation
        for (Map.Entry<BlockPos, BlockState> entry : originalBlocks.entrySet()) {
            long posKey = entry.getKey().asLong();
            requiredBlockStates.put(posKey, entry.getValue());
        }
        
        // Generate rotated versions
        generateRotatedBlockStates(originalBlocks);
    }
    
    private void generateRotatedBlockStates(Map<BlockPos, BlockState> originalBlocks) {
        // For each rotation, we need to rotate both positions and block states
        
        // Clockwise 90 degrees (Y-axis rotation)
        Map<BlockPos, BlockState> clockwiseBlocks = rotateBlocksClockwise(originalBlocks);
        for (Map.Entry<BlockPos, BlockState> entry : clockwiseBlocks.entrySet()) {
            long posKey = entry.getKey().asLong();
            requiredBlockStatesCW.put(posKey, entry.getValue());
        }
        
        // Counter-clockwise 90 degrees (Y-axis rotation)
        Map<BlockPos, BlockState> counterClockwiseBlocks = rotateBlocksCounterClockwise(originalBlocks);
        for (Map.Entry<BlockPos, BlockState> entry : counterClockwiseBlocks.entrySet()) {
            long posKey = entry.getKey().asLong();
            requiredBlockStatesCCW.put(posKey, entry.getValue());
        }
        
        // 180 degrees rotation (Y-axis rotation)
        Map<BlockPos, BlockState> oppositeBlocks = rotateBlocksOpposite(originalBlocks);
        for (Map.Entry<BlockPos, BlockState> entry : oppositeBlocks.entrySet()) {
            long posKey = entry.getKey().asLong();
            requiredBlockStatesOposite.put(posKey, entry.getValue());
        }
    }
    
    private Map<BlockPos, BlockState> rotateBlocksClockwise(Map<BlockPos, BlockState> originalBlocks) {
        Map<BlockPos, BlockState> rotatedBlocks = new HashMap<>();
        
        for (Map.Entry<BlockPos, BlockState> entry : originalBlocks.entrySet()) {
            BlockPos originalPos = entry.getKey();
            BlockState originalState = entry.getValue();
            
            // Rotate position: (x, y, z) -> (-z, y, x)
            BlockPos rotatedPos = new BlockPos(-originalPos.getZ(), originalPos.getY(), originalPos.getX());
            
            // Rotate block state properties (like facing direction)
            BlockState rotatedState = rotateBlockStateClockwise(originalState);
            
            rotatedBlocks.put(rotatedPos, rotatedState);
        }
        
        return rotatedBlocks;
    }
    
    private Map<BlockPos, BlockState> rotateBlocksCounterClockwise(Map<BlockPos, BlockState> originalBlocks) {
        Map<BlockPos, BlockState> rotatedBlocks = new HashMap<>();
        
        for (Map.Entry<BlockPos, BlockState> entry : originalBlocks.entrySet()) {
            BlockPos originalPos = entry.getKey();
            BlockState originalState = entry.getValue();
            
            // Rotate position: (x, y, z) -> (z, y, -x)
            BlockPos rotatedPos = new BlockPos(originalPos.getZ(), originalPos.getY(), -originalPos.getX());
            
            // Rotate block state properties (like facing direction)
            BlockState rotatedState = rotateBlockStateCounterClockwise(originalState);
            
            rotatedBlocks.put(rotatedPos, rotatedState);
        }
        
        return rotatedBlocks;
    }
    
    private Map<BlockPos, BlockState> rotateBlocksOpposite(Map<BlockPos, BlockState> originalBlocks) {
        Map<BlockPos, BlockState> rotatedBlocks = new HashMap<>();
        
        for (Map.Entry<BlockPos, BlockState> entry : originalBlocks.entrySet()) {
            BlockPos originalPos = entry.getKey();
            BlockState originalState = entry.getValue();
            
            // Rotate position: (x, y, z) -> (-x, y, -z)
            BlockPos rotatedPos = new BlockPos(-originalPos.getX(), originalPos.getY(), -originalPos.getZ());
            
            // Rotate block state properties (like facing direction)
            BlockState rotatedState = rotateBlockStateOpposite(originalState);
            
            rotatedBlocks.put(rotatedPos, rotatedState);
        }
        
        return rotatedBlocks;
    }
    
    private BlockState rotateBlockStateClockwise(BlockState state) {
        // Handle common directional properties
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            net.minecraft.core.Direction rotatedFacing = facing.getClockWise();
            return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, rotatedFacing);
        }
        
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            if (facing.getAxis() != net.minecraft.core.Direction.Axis.Y) { // Don't rotate up/down
                net.minecraft.core.Direction rotatedFacing = facing.getClockWise();
                return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, rotatedFacing);
            }
        }
        
        return state; // Return unchanged if no directional properties
    }
    
    private BlockState rotateBlockStateCounterClockwise(BlockState state) {
        // Handle common directional properties
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            net.minecraft.core.Direction rotatedFacing = facing.getCounterClockWise();
            return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, rotatedFacing);
        }
        
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            if (facing.getAxis() != net.minecraft.core.Direction.Axis.Y) { // Don't rotate up/down
                net.minecraft.core.Direction rotatedFacing = facing.getCounterClockWise();
                return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, rotatedFacing);
            }
        }
        
        return state; // Return unchanged if no directional properties
    }
    
    private BlockState rotateBlockStateOpposite(BlockState state) {
        // Handle common directional properties
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            net.minecraft.core.Direction rotatedFacing = facing.getOpposite();
            return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, rotatedFacing);
        }
        
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            if (facing.getAxis() != net.minecraft.core.Direction.Axis.Y) { // Don't rotate up/down
                net.minecraft.core.Direction rotatedFacing = facing.getOpposite();
                return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, rotatedFacing);
            }
        }
        
        return state; // Return unchanged if no directional properties
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
    public HashMap<Long, BlockState> getRequiredBlockStates() { return requiredBlockStates; }
    public HashMap<Long, BlockState> getRequiredBlockStatesCW() { return requiredBlockStatesCW; }
    public HashMap<Long, BlockState> getRequiredBlockStatesCCW() { return requiredBlockStatesCCW; }
    public HashMap<Long, BlockState> getRequiredBlockStatesOpposite() { return requiredBlockStatesOposite; }

    public boolean matchesBlocks(HashMap<Long, BlockState> values) {
        if(values == null || values.isEmpty() || values.size() != requiredBlockStates.size()) {
            return false;
        }
        // Check if the provided blocks match any of the four orientations
        return matchesOrientation(values, requiredBlockStates) ||
               matchesOrientation(values, requiredBlockStatesCW) ||
               matchesOrientation(values, requiredBlockStatesCCW) ||
               matchesOrientation(values, requiredBlockStatesOposite);
    }
    
    private boolean matchesOrientation(HashMap<Long, BlockState> providedBlocks, HashMap<Long, BlockState> requiredBlocks) {
        // Check if all required blocks are present in the provided blocks
        for (Map.Entry<Long, BlockState> requiredEntry : requiredBlocks.entrySet()) {
            Long requiredPos = requiredEntry.getKey();
            BlockState requiredState = requiredEntry.getValue();
            
            if (!providedBlocks.containsKey(requiredPos) || 
                !providedBlocks.get(requiredPos).equals(requiredState)) {
                return false;
            }
        }
        return true;
    }

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