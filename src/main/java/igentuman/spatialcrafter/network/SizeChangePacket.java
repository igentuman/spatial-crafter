package igentuman.spatialcrafter.network;

import igentuman.spatialcrafter.Main;
import igentuman.spatialcrafter.block.SpatialCrafterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SizeChangePacket(BlockPos pos, int newSize) implements CustomPacketPayload {
    public static final Type<SizeChangePacket> TYPE = new Type<>(Main.rl("size_change"));

    public static final StreamCodec<FriendlyByteBuf, SizeChangePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SizeChangePacket::pos,
            ByteBufCodecs.INT, SizeChangePacket::newSize,
            SizeChangePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null) {
                BlockEntity be = context.player().level().getBlockEntity(pos);
                if (be instanceof SpatialCrafterBlockEntity spatialCrafter) {
                    spatialCrafter.setSize(newSize);
                }
            }
        });
    }
}