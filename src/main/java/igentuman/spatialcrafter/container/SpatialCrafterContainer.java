package igentuman.spatialcrafter.container;

import igentuman.spatialcrafter.block.SpatialCrafterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_BLOCK;
import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_CONTAINER;

public class SpatialCrafterContainer extends AbstractContainerMenu {

    private SpatialCrafterBlockEntity blockEntity;
    private Player playerEntity;
    private final IItemHandler itemHandler;

    public SpatialCrafterContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
        super(SPATIAL_CRAFTER_CONTAINER.get(), windowId);
        blockEntity = (SpatialCrafterBlockEntity)player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (itemHandler != null) {
            addCrafterInventory();
        }
        addPlayerInventory(playerEntity.getInventory());
        addPlayerHotbar(playerEntity.getInventory());
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
        return (int) (scale*((float)blockEntity.getEnergy()/(float)blockEntity.getMaxEnergy()));
    }

    public boolean isDisabled()
    {
        return blockEntity.isDisabled;
    }


    public int getEnergy() {
        return blockEntity.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0);
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return null;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, SPATIAL_CRAFTER_BLOCK.get());
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
}