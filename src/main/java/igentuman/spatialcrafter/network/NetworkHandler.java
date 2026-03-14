package igentuman.spatialcrafter.network;

import igentuman.spatialcrafter.Main;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Main.MODID)
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                SizeChangePacket.TYPE,
                SizeChangePacket.STREAM_CODEC,
                SizeChangePacket::handle
        );
        registrar.playToClient(
                SyncStructuresPacket.TYPE,
                SyncStructuresPacket.STREAM_CODEC,
                SyncStructuresPacket::handle
        );
    }
}