package igentuman.spatialcrafter.network;

import igentuman.spatialcrafter.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    public static void registerPackets() {
        int id = 0;
        INSTANCE.registerMessage(id++, SizeChangePacket.class, SizeChangePacket::encode, SizeChangePacket::decode, SizeChangePacket::handle);
    }
}