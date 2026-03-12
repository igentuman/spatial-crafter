package igentuman.spatialcrafter.container;

import igentuman.spatialcrafter.block.SpatialCrafterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_BLOCK;
import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_CONTAINER;

public class SpatialCrafterContainer extends AbstractContainerMenu {

    private SpatialCrafterBlockEntity blockEntity;
    private Player playerEntity;
    private final IItemHandler itemHandler;

    public SpatialCrafterContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
        super(SPATIAL_CRAFTER_CONTAINER.get(), windowId);
        blockEntity = (SpatialCrafterBlockEntity)player.level().getBlockEntity(pos);
        this.playerEntity = player;
        this.itemHandler = blockEntity.getItemHandler();
        if (itemHandler != null) {
            addCrafterInventory();
        }
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addCrafterInventory() {
        int index = -1;
        for (int row = 0; row < 5; row++) {
            index++;
            this.addSlot(new SlotItemHandler(itemHandler, index, 46 + 18*row, 21));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int yOffset = 72;
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 10 + l * 18, yOffset + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        int yOffset = 130;
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 10 + i * 18, yOffset));
        }
    }

    public int getEnergyScaled(int scale)
    {
        return (int) (scale*((float)getEnergy()/(float)getMaxEnergy()));
    }

    public boolean isDisabled()
    {
        return blockEntity.isDisabled;
    }


    public int getEnergy() {
        return blockEntity.getEnergyStorage().getEnergyStored();
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerIn, SPATIAL_CRAFTER_BLOCK.get());
    }

    public void checkboxClicked(int id, int val) {
        //Messages.sendToServer(new BoosterPacket(blockEntity.getBlockPos(), id, (byte) val));
    }


    public int getMaxEnergy() {
        return blockEntity.getMaxEnergy();
    }
    
    public int getSize() {
        return blockEntity.getSize();
    }
    
    public SpatialCrafterBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public int getProcessingProgress() {
        return blockEntity.getProcessingProgress();
    }
    
    public int getMaxProgress() {
        return blockEntity.getMaxProgress();
    }
    
    public boolean isProcessing() {
        return blockEntity.isProcessing();
    }
}