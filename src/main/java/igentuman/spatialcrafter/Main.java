package igentuman.spatialcrafter;

import igentuman.spatialcrafter.client.SpatialCrafterScreen;
import igentuman.spatialcrafter.util.MultiblocksProvider;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_CONTAINER;
import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_ITEM;

@Mod(
        igentuman.spatialcrafter.Main.MODID
)
@Mod.EventBusSubscriber
public class Main
{
    public static final String MODID = "spatialcrafter";
    public static final Logger logger = LogManager.getLogger();

    public Main() {
        this(FMLJavaModLoadingContext.get());
    }

    public Main(FMLJavaModLoadingContext context) {
        Setup.init();
        context.getModEventBus().addListener(Main::init);
        context.getModEventBus().addListener(this::addCreative);
        CommonConfig.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            //NetworkHandler.registerPackets();
        });
    }

    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(SPATIAL_CRAFTER_CONTAINER.get(), SpatialCrafterScreen::new);
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SPATIAL_CRAFTER_ITEM);
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MultiblocksProvider.getInstance());
    }

    public static ResourceLocation rlFromString(String name) {
        return ResourceLocation.tryParse(name);
    }
    public static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }
}