package igentuman.spatialcrafter.network;

import igentuman.spatialcrafter.Main;
import igentuman.spatialcrafter.util.MultiblockStructure;
import igentuman.spatialcrafter.util.MultiblocksProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record SyncStructuresPacket(List<SyncStructuresPacket.MultiblockData> structures) implements CustomPacketPayload {
    public record MultiblockData(ResourceLocation id, CompoundTag nbt, String name) {
        public static final StreamCodec<FriendlyByteBuf, MultiblockData> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, MultiblockData::id,
                ByteBufCodecs.COMPOUND_TAG, MultiblockData::nbt,
                ByteBufCodecs.STRING_UTF8, MultiblockData::name,
                MultiblockData::new
        );
    }

    public static final Type<SyncStructuresPacket> TYPE = new Type<>(Main.rl("sync_structures"));

    public static final StreamCodec<FriendlyByteBuf, SyncStructuresPacket> STREAM_CODEC = StreamCodec.composite(
            MultiblockData.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncStructuresPacket::structures,
            SyncStructuresPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            List<MultiblockStructure> newStructures = structures.stream()
                    .map(data -> new MultiblockStructure(data.id(), data.nbt(), data.name()))
                    .collect(Collectors.toList());
            MultiblocksProvider.setStructures(newStructures);
        });
    }

    public static SyncStructuresPacket create() {
        List<MultiblockData> dataList = MultiblocksProvider.getStructures().stream()
                .map(s -> new MultiblockData(s.getId(), s.getStructureNbt(), s.getName()))
                .collect(Collectors.toList());
        return new SyncStructuresPacket(dataList);
    }
}
