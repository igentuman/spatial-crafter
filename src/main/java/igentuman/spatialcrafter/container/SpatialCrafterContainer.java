package igentuman.spatialcrafter.container;

import igentuman.spatialcrafter.block.SpatialCrafterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.HashMap;

import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_BLOCK;
import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_CONTAINER;

public class SpatialCrafterContainer extends AbstractContainerMenu {

    private SpatialCrafterBlockEntity blockEntity;
    private Player playerEntity;

    public SpatialCrafterContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
        super(SPATIAL_CRAFTER_CONTAINER.get(), windowId);
        blockEntity = (SpatialCrafterBlockEntity)player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
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
}