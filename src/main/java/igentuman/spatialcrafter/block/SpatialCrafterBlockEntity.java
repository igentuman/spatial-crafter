package igentuman.spatialcrafter.block;

import igentuman.spatialcrafter.CommonConfig;
import igentuman.spatialcrafter.util.CustomEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_BE;

public class SpatialCrafterBlockEntity extends BlockEntity {

    private final CustomEnergyStorage energy = createEnergyStorage();
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energy);

    public SpatialCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(SPATIAL_CRAFTER_BE.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
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
    }

    private void loadClientData(CompoundTag tag) {
        if (tag.contains("Energy")) {
            energy.deserializeNBT(tag.get("Energy"));
        }
        isDisabled = tag.getBoolean("isDisabled");

    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("Energy")) {
            energy.deserializeNBT(tag.get("Energy"));
        }
        isDisabled = tag.getBoolean("isDisabled");
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
       tag.put("Energy", energy.serializeNBT());
        tag.putBoolean("isDisabled", isDisabled);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public int getMaxEnergy() {
        return CommonConfig.GENERAL.fe_per_tick.get()*100;
    }

    public void tickClient() {


    }
}