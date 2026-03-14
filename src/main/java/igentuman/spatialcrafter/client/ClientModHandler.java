package igentuman.spatialcrafter.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static igentuman.spatialcrafter.Main.MODID;
import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_CONTAINER;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientModHandler {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(SPATIAL_CRAFTER_CONTAINER.get(), SpatialCrafterScreen::new);
    }
}
