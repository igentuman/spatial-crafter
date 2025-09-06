package igentuman.spatialcrafter.network;

import igentuman.spatialcrafter.block.SpatialCrafterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SizeChangePacket {
    private final BlockPos pos;
    private final int newSize;
    
    public SizeChangePacket(BlockPos pos, int newSize) {
        this.pos = pos;
        this.newSize = newSize;
    }
    
    public static void encode(SizeChangePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.newSize);
    }
    
    public static SizeChangePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int newSize = buffer.readInt();
        return new SizeChangePacket(pos, newSize);
    }
    
    public static void handle(SizeChangePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                BlockEntity be = context.getSender().level().getBlockEntity(packet.pos);
                if (be instanceof SpatialCrafterBlockEntity spatialCrafter) {
                    spatialCrafter.setSize(packet.newSize);
                }
            }
        });
        context.setPacketHandled(true);
    }
}