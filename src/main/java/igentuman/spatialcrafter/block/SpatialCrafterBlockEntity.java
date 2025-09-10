package igentuman.spatialcrafter.block;

import igentuman.spatialcrafter.CommonConfig;
import igentuman.spatialcrafter.Main;
import igentuman.spatialcrafter.client.SpatialCrafterOverlayHandler;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipe;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipeManager;
import igentuman.spatialcrafter.util.CustomEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_BE;
import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_START;
import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_COMPLETE;
import static net.minecraft.world.level.block.Blocks.AIR;

public class SpatialCrafterBlockEntity extends BlockEntity {

    // Static executor for background scanning tasks
    private static final Executor SCAN_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "SpatialCrafter-Scanner");
        t.setDaemon(true);
        return t;
    });

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
    // Track ongoing scan to prevent multiple concurrent scans
    private CompletableFuture<Void> currentScan = null;

    private int size(int inputSize) {
        return Math.max(1, Math.min(27, inputSize));
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
        
        // Cancel any ongoing scan
        if (currentScan != null && !currentScan.isDone()) {
            currentScan.cancel(true);
        }
        
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
    private long tick = 0;

    private void updateRedstoneControl()
    {
        isDisabled = level.getBestNeighborSignal(worldPosition) > 0;
    }

    public void tickServer() {
        if(level == null || level.isClientSide()) return;
        tick++;
        boolean wasDisabled = isDisabled;

        if(level.getGameTime() % 10 == 0) {
            updateRedstoneControl();
        }
        if(wasDisabled != isDisabled) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState().setValue(BlockStateProperties.POWERED, isDisabled && isProcessing), Block.UPDATE_ALL);
            setChanged();
        }
        if(isDisabled) {
            return;
        }
        // Process crafting
        if (isProcessing) {
            processCrafting();
        } else if(level.getGameTime() % 40 == 0) {
            scanForRecipe();
        }
        
        if(level.getGameTime() % 20 == 0) {
            if(currentRecipe != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState().setValue(BlockStateProperties.POWERED, true), Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState().setValue(BlockStateProperties.POWERED, false), Block.UPDATE_ALL);
            }
        }
    }

    private void scanForRecipe() {
        if (level == null || isProcessing) return;
        
        // Check if there's already a scan running
        if (currentScan != null && !currentScan.isDone()) {
            return; // Skip this scan if one is already in progress
        }
        
        // Capture current state for the background thread
        final Level capturedLevel = level;
        final BlockPos capturedPos = worldPosition.immutable();
        final int capturedSize = size;
/*        Optional<SpatialCrafterRecipe> recipe = SpatialCrafterRecipeManager.findRecipe(capturedLevel, indexBlockStatesAsync(capturedLevel, capturedPos, capturedSize));
        recipe.ifPresent(this::startCrafting);*/
        currentScan = CompletableFuture
        .supplyAsync(() -> {
            // This runs on background thread
            return indexBlockStatesAsync(capturedLevel, capturedPos, capturedSize);
        }, SCAN_EXECUTOR)
        .thenCompose(indexedStates -> {
            // This also runs on background thread
            return CompletableFuture.supplyAsync(() -> {
                return SpatialCrafterRecipeManager.findRecipe(capturedLevel, indexedStates);
            }, SCAN_EXECUTOR);
        })
        .thenAcceptAsync(recipe -> {
            // This runs back on the main thread
            if (level != null && !isRemoved() && !isProcessing) {
                recipe.ifPresent(this::startCrafting);
            }
        }, level.getServer()::execute)
        .exceptionally(throwable -> {
            // Handle any exceptions that occur during scanning
            Main.logger.error("Error during recipe scanning for SpatialCrafter at {}: {}",
                capturedPos, throwable.getMessage());
            return null;
        });
    }

    /**
     * Async version of indexBlockStates that runs on background thread
     * Returns a new HashMap instead of modifying the instance field
     */
    private HashMap<Long, BlockState> indexBlockStatesAsync(Level level, BlockPos centerPos, int scanSize) {
        HashMap<Long, BlockState> states = new HashMap<>();
        
        // Calculate scan area using the same logic as getScanArea but with captured parameters
        BlockState state = level.getBlockState(centerPos);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        
        BlockPos scanCenterPos = centerPos.relative(facing.getOpposite(), scanSize);
        
        net.minecraft.world.phys.AABB scanArea = getScanArea();
        
        BlockPos min = new BlockPos((int) scanArea.minX, (int) scanArea.minY, (int) scanArea.minZ);
        BlockPos max = new BlockPos((int) scanArea.maxX, (int) scanArea.maxY, (int) scanArea.maxZ);
        
        // Calculate the center of the scan area to create relative coordinates
        BlockPos center = new BlockPos(
            (min.getX() + max.getX()) / 2,
            (min.getY() + max.getY()) / 2,
            (min.getZ() + max.getZ()) / 2
        );
        
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    // Create relative coordinates from the center
                    BlockPos relativePos = new BlockPos(x - center.getX(), y - center.getY(), z - center.getZ());
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState blockState = level.getBlockState(currentPos);
                    states.put(relativePos.asLong(), blockState);
                }
            }
        }
        
        return states;
    }

    public void startCrafting(SpatialCrafterRecipe craftingRecipe) {
        if (level == null || isProcessing) return;
        currentRecipe = craftingRecipe;
        clearArea();
        processingProgress = 0;
        isProcessing = true;
        
        level.playSound(null, worldPosition, SPATIAL_CRAFTER_START.get(), SoundSource.BLOCKS, 0.8f, 1.0f);

        setChanged();
    }

    private void clearArea() {
        if (level == null || currentRecipe == null) return;

        net.minecraft.world.phys.AABB scanArea = getScanArea();
        BlockPos min = new BlockPos((int) scanArea.minX, (int) scanArea.minY, (int) scanArea.minZ);
        BlockPos max = new BlockPos((int) scanArea.maxX, (int) scanArea.maxY, (int) scanArea.maxZ);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    level.setBlock(currentPos, AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
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
                        BlockPos center = new BlockPos((int) getScanArea().getCenter().x, (int) getScanArea().getCenter().y, (int) getScanArea().getCenter().z);
                        // Position entity
                        BlockPos spawnPos = center.offset(entityOutput.getRelativePos());
                        entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY()-1, spawnPos.getZ() + 0.5);
                        
                        level.addFreshEntity(entity);
                    }
                }
            }
        }
        
        // Play completion sound
        level.playSound(null, worldPosition, SPATIAL_CRAFTER_COMPLETE.get(), SoundSource.BLOCKS, 0.8f, 1.0f);

        // Reset processing state
        currentRecipe = null;
        processingProgress = 0;
        isProcessing = false;
        setChanged();
    }

    private CustomEnergyStorage createEnergyStorage() {
        return new CustomEnergyStorage (
                getMaxEnergy(),
                getMaxEnergy()
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
        size = tag.contains("size") ? size(tag.getInt("size")) : 5;
        
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
        size = tag.contains("size") ? size(tag.getInt("size")) : 5;
        
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
        return CommonConfig.GENERAL.recipe_energy_multiplier.get()*100000;
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
        this.size = size(newSize);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
    
    public AABB getScanArea() {
        if (level == null) {
            return new AABB(
                worldPosition.getX() - size, worldPosition.getY(), worldPosition.getZ() - size,
                worldPosition.getX() + size + 1, worldPosition.getY() + size + 1, worldPosition.getZ() + size + 1
            );
        }
        
        BlockState state = level.getBlockState(worldPosition);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        
        BlockPos centerPos = worldPosition.relative(facing.getOpposite(), 1);
        return switch (facing) {
            case NORTH -> new AABB(
                    centerPos.getX() - size, worldPosition.getY(), centerPos.getZ(),
                    centerPos.getX() + size, worldPosition.getY() + size*2, centerPos.getZ() + size*2
            );
            case SOUTH -> new AABB(
                    centerPos.getX() - size, worldPosition.getY(), centerPos.getZ(),
                    centerPos.getX() + size, worldPosition.getY() + size*2, centerPos.getZ() - size*2
            );
            case EAST -> new AABB(
                    centerPos.getX(), worldPosition.getY(), centerPos.getZ() - size,
                    centerPos.getX() - size*2, worldPosition.getY() + size*2, centerPos.getZ() + size
            );
            case WEST -> new AABB(
                    centerPos.getX(), worldPosition.getY(), centerPos.getZ() - size,
                    centerPos.getX() + size*2, worldPosition.getY() + size*2, centerPos.getZ() + size
            );
            default -> new AABB(
                    centerPos.getX() - size+1, worldPosition.getY(), centerPos.getZ() - size+1,
                    centerPos.getX() + size - 1.0001, worldPosition.getY() + size*2 - 1.0001, centerPos.getZ() + size - 0.9999
            );
        };
    }
}