package igentuman.spatialcrafter;

import igentuman.spatialcrafter.client.SpatialCrafterScreen;
import igentuman.spatialcrafter.network.NetworkHandler;
import igentuman.spatialcrafter.util.MultiblocksProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static igentuman.spatialcrafter.Setup.*;

@Mod(
        igentuman.spatialcrafter.Main.MODID
)
@EventBusSubscriber
public class Main
{
    public static final String MODID = "spatialcrafter";
    public static final Logger logger = LogManager.getLogger();


    public Main(IEventBus modEventBus, ModContainer modContainer) {
        Setup.init(modEventBus);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::onModConfigEvent);
        modEventBus.addListener(this::registerCapabilities);
        CommonConfig.register(modContainer);
//        NeoForge.EVENT_BUS.register(this);
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                SPATIAL_CRAFTER_BE.get(),
                (be, side) -> be.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                SPATIAL_CRAFTER_BE.get(),
                (be, side) -> be.getItemHandler()
        );
    }

    public void onModConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            //todo implement
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    public void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SPATIAL_CRAFTER_CONTAINER.get(), SpatialCrafterScreen::new);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SPATIAL_CRAFTER_ITEM.get());
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if(CommonConfig.GENERAL.replace_vanilla_recipes.get() && ModList.get().isLoaded("compactmachines")) {
           // CCUtil.disableVanillaRecipes();
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