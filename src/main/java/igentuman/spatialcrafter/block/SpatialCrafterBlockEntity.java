package igentuman.spatialcrafter.block;

import igentuman.spatialcrafter.CommonConfig;
import igentuman.spatialcrafter.client.SpatialCrafterOverlayHandler;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipe;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipeManager;
import igentuman.spatialcrafter.util.CustomEnergyStorage;
import igentuman.spatialcrafter.util.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;

import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_BE;

public class SpatialCrafterBlockEntity extends BlockEntity {

    private final CustomEnergyStorage energy = createEnergyStorage();
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energy);
    
    // Recipe processing fields
    private final ItemStackHandler itemHandler = createItemHandler();
    private final LazyOptional<IItemHandler> itemHandlerLazyOptional = LazyOptional.of(() -> itemHandler);
    private SpatialCrafterRecipe currentRecipe;
    private int processingProgress = 0;
    private boolean isProcessing = false;
    private HashMap<Long, BlockState> indexedBlockStates = new HashMap<>();
    // Scan area size field
    private int size = 5;

    private int ensureOddSize(int inputSize) {
        // Clamp to valid range (1-31, keeping it odd)
        int clampedSize = Math.max(1, Math.min(31, inputSize));
        
        // Make sure it's odd
        if (clampedSize % 2 == 0) {
            // If even, subtract 1 to make it odd (but ensure it doesn't go below 1)
            clampedSize = Math.max(1, clampedSize - 1);
        }
        
        return clampedSize;
    }

    public SpatialCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(SPATIAL_CRAFTER_BE.get(), pos, state);
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> 
                SpatialCrafterOverlayHandler.addSpatialCrafterBlock(worldPosition));
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        energyHandler.invalidate();
        itemHandlerLazyOptional.invalidate();
        if (level != null && level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> 
                SpatialCrafterOverlayHandler.removeSpatialCrafterBlock(worldPosition));
        }
    }
    
    private ItemStackHandler createItemHandler() {
        return new ItemStackHandler(5) { // 5 slot output inventory
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public int getEnergy()
    {
        return energy.getEnergyStored();
    }

    public void consumeEnergy(int amount)
    {
        energy.consumeEnergy(amount);
    }

    public boolean isDisabled = false;
    public int fePerTick = CommonConfig.GENERAL.fe_per_tick.get();
    private long tick = 0;

    private void updateRedstoneControl()
    {

        /*if(level.getBlockState(worldPosition).getValue(BlockStateProperties.POWERED) != false) {
            setChanged();
            level.setBlock(worldPosition, level.getBlockState(worldPosition).setValue(BlockStateProperties.POWERED, false),
                    Block.UPDATE_ALL);
        }*/
    }

    public void tickServer() {
        if(level == null) return;
        tick++;
        
        // Process crafting
        if (isProcessing) {
            processCrafting();
        } else if(level.getGameTime() % 20 == 0) {
            scanForRecipe();
        }
        
        if(tick % 10 == 0) {
            if(!level.isClientSide()) {
                boolean lastState = isDisabled;
                updateRedstoneControl();
                if(lastState != isDisabled) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
                }
                if(isDisabled) {
                    return;
                }
            }
        }
    }

    private void scanForRecipe() {
        if (level == null || isProcessing) return;
        indexBlockStates();
        Optional<SpatialCrafterRecipe> recipe = SpatialCrafterRecipeManager.findRecipe(level, indexedBlockStates);
        recipe.ifPresent(this::startCrafting);
    }

    private void indexBlockStates() {
        indexedBlockStates.clear();
        net.minecraft.world.phys.AABB scanArea = getScanArea();
        BlockPos min = new BlockPos((int) scanArea.minX, (int) scanArea.minY, (int) scanArea.minZ);
        BlockPos max = new BlockPos((int) scanArea.maxX, (int) scanArea.maxY, (int) scanArea.maxZ);
        int localX = 0;
        int localY = 0;
        int localZ = 0;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos localPos = new BlockPos(localX, localY, localZ);
                    BlockPos currentPos = new BlockPos(x, y, z);
                    assert level != null;
                    BlockState state = level.getBlockState(currentPos);
                    indexedBlockStates.put(localPos.asLong(), state);
                    localZ++;
                }
                localZ = 0;
                localY++;
            }
            localY = 0;
            localX++;
        }
    }

    public void startCrafting(SpatialCrafterRecipe craftingRecipe) {
        if (level == null || isProcessing) return;
        currentRecipe = craftingRecipe;
        processingProgress = 0;
        isProcessing = true;
        setChanged();
    }

    private void processCrafting() {
        if (!isProcessing || currentRecipe == null || level == null) return;
        
        processingProgress++;
        
        // Apply config multipliers
        int adjustedEnergyConsumption = (int) (currentRecipe.getEnergyConsumption() * CommonConfig.GENERAL.recipe_energy_multiplier.get());
        int adjustedProcessingTime = (int) (currentRecipe.getProcessingTime() * CommonConfig.GENERAL.recipe_time_multiplier.get());
        
        // Consume energy per tick
        int energyPerTick = adjustedEnergyConsumption / adjustedProcessingTime;
        if (energy.getEnergyStored() >= energyPerTick) {
            energy.consumeEnergy(energyPerTick);
        } else {
            // Not enough energy, pause processing
            return;
        }
        
        if (processingProgress >= adjustedProcessingTime) {
            completeCrafting();
        }
        
        setChanged();
    }

    private void completeCrafting() {
        if (currentRecipe == null || level == null) return;
        
        // Produce item outputs
        for (ItemStack output : currentRecipe.getOutputs()) {
            ItemStack result = output.copy();
            
            // Apply global NBT if present
            if (!currentRecipe.getOutputNbt().isEmpty()) {
                CompoundTag existingNbt = result.getOrCreateTag();
                existingNbt.merge(currentRecipe.getOutputNbt());
            }
            
            // Try to insert into inventory
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                result = itemHandler.insertItem(i, result, false);
                if (result.isEmpty()) break;
            }
            
            // If inventory is full, drop items
            if (!result.isEmpty()) {
                Block.popResource(level, worldPosition.above(), result);
            }
        }
        
        // Spawn entities (if enabled in config)
        if (CommonConfig.GENERAL.enable_entity_spawning.get()) {
            for (SpatialCrafterRecipe.EntityOutput entityOutput : currentRecipe.getEntityOutputs()) {
                for (int i = 0; i < entityOutput.getCount(); i++) {
                    Entity entity = entityOutput.getEntityType().create(level);
                    if (entity != null) {
                        // Apply NBT data
                        if (!entityOutput.getNbt().isEmpty()) {
                            entity.load(entityOutput.getNbt());
                        }
                        
                        // Position entity
                        BlockPos spawnPos = worldPosition.offset(entityOutput.getRelativePos());
                        entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                        
                        level.addFreshEntity(entity);
                    }
                }
            }
        }
        
        // Reset processing state
        currentRecipe = null;
        processingProgress = 0;
        isProcessing = false;
        setChanged();
    }

    private CustomEnergyStorage createEnergyStorage() {
        return new CustomEnergyStorage (
                getMaxEnergy(),
                        CommonConfig.GENERAL.fe_per_tick.get()*100
        ) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int rc = super.receiveEnergy(maxReceive, simulate);
                if (rc > 0 && !simulate) {
                    setChanged();
                }
                return rc;
            }
        };
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveClientData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag != null) {
            loadClientData(tag);
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        handleUpdateTag(tag);
    }

    private void saveClientData(CompoundTag tag) {
        tag.put("Energy", energy.serializeNBT());
        tag.putBoolean("isDisabled", isDisabled);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putBoolean("isProcessing", isProcessing);
        tag.putInt("processingProgress", processingProgress);
        tag.putInt("size", size);
        if (currentRecipe != null) {
            tag.putString("currentRecipe", currentRecipe.getId().toString());
        }
    }

    private void loadClientData(CompoundTag tag) {
        if (tag.contains("Energy")) {
            energy.deserializeNBT(tag.get("Energy"));
        }
        isDisabled = tag.getBoolean("isDisabled");
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
        isProcessing = tag.getBoolean("isProcessing");
        processingProgress = tag.getInt("processingProgress");
        size = tag.contains("size") ? ensureOddSize(tag.getInt("size")) : 5;
        
        if (tag.contains("currentRecipe") && level != null) {
            ResourceLocation recipeId = ResourceLocation.tryParse(tag.getString("currentRecipe"));
            if (recipeId != null) {
                currentRecipe = level.getRecipeManager().byKey(recipeId)
                        .filter(recipe -> recipe instanceof SpatialCrafterRecipe)
                        .map(recipe -> (SpatialCrafterRecipe) recipe)
                        .orElse(null);
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("Energy")) {
            energy.deserializeNBT(tag.get("Energy"));
        }
        isDisabled = tag.getBoolean("isDisabled");
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
        isProcessing = tag.getBoolean("isProcessing");
        processingProgress = tag.getInt("processingProgress");
        size = tag.contains("size") ? ensureOddSize(tag.getInt("size")) : 5;
        
        if (tag.contains("currentRecipe")) {
            ResourceLocation recipeId = ResourceLocation.tryParse(tag.getString("currentRecipe"));
            // Recipe will be resolved when level is available
        }
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
       tag.put("Energy", energy.serializeNBT());
        tag.putBoolean("isDisabled", isDisabled);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putBoolean("isProcessing", isProcessing);
        tag.putInt("processingProgress", processingProgress);
        tag.putInt("size", size);
        if (currentRecipe != null) {
            tag.putString("currentRecipe", currentRecipe.getId().toString());
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerLazyOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    public int getMaxEnergy() {
        return CommonConfig.GENERAL.fe_per_tick.get()*100;
    }

    public void tickClient() {

    }
    
    public int getProcessingProgress() {
        return processingProgress; 
    }
    
    public int getMaxProgress() { 
        return currentRecipe != null ? (int) (currentRecipe.getProcessingTime() * CommonConfig.GENERAL.recipe_time_multiplier.get()) : 0; 
    }
    
    public boolean isProcessing() { 
        return isProcessing; 
    }
    
    public SpatialCrafterRecipe getCurrentRecipe() { 
        return currentRecipe; 
    }
    
    public ItemStackHandler getItemHandler() { 
        return itemHandler; 
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int newSize) {
        this.size = ensureOddSize(newSize);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
    
    public net.minecraft.world.phys.AABB getScanArea() {
        if (level == null) {
            return new net.minecraft.world.phys.AABB(
                worldPosition.getX() - size, worldPosition.getY(), worldPosition.getZ() - size,
                worldPosition.getX() + size + 1, worldPosition.getY() + size + 1, worldPosition.getZ() + size + 1
            );
        }
        
        BlockState state = level.getBlockState(worldPosition);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        
        BlockPos centerPos = worldPosition.relative(facing.getOpposite(), size);
        
        return new net.minecraft.world.phys.AABB(
            centerPos.getX() - size+1, worldPosition.getY(), centerPos.getZ() - size+1,
            centerPos.getX() + size - 1.0001, worldPosition.getY() + size + 1.9999, centerPos.getZ() + size - 0.9999
        );
    }
}