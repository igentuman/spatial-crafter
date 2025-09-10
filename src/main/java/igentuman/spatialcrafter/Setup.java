package igentuman.spatialcrafter;

import igentuman.spatialcrafter.block.SpatialCrafterBlock;
import igentuman.spatialcrafter.block.SpatialCrafterBlockEntity;
import igentuman.spatialcrafter.container.SpatialCrafterContainer;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipe;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipeSerializer;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipeType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static igentuman.spatialcrafter.Main.MODID;

public class Setup {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    
    // Recipe Types
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = 
        DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);

    // Recipe Serializers
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = 
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);


    public static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops();
    public static final Item.Properties ITEM_PROPERTIES = new Item.Properties();

    public static final RegistryObject<SpatialCrafterBlock> SPATIAL_CRAFTER_BLOCK = BLOCKS.register("spatial_crafter", SpatialCrafterBlock::new);

    public static final RegistryObject<Item> SPATIAL_CRAFTER_ITEM = fromBlock(SPATIAL_CRAFTER_BLOCK);

    public static final RegistryObject<BlockEntityType<SpatialCrafterBlockEntity>> SPATIAL_CRAFTER_BE = BLOCK_ENTITIES.register("spatial_crafter", () -> BlockEntityType.Builder.of(SpatialCrafterBlockEntity::new, SPATIAL_CRAFTER_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<SpatialCrafterContainer>> SPATIAL_CRAFTER_CONTAINER = CONTAINERS.register("spatial_crafter",
            () -> IForgeMenuType.create((windowId, inv, data) -> new SpatialCrafterContainer(windowId, data.readBlockPos(), inv, inv.player)));

    // Recipe registrations
    public static final RegistryObject<RecipeType<SpatialCrafterRecipe>> SPATIAL_CRAFTING_TYPE = 
        RECIPE_TYPES.register("spatial_crafting", () -> SpatialCrafterRecipeType.INSTANCE);

    public static final RegistryObject<RecipeSerializer<SpatialCrafterRecipe>> SPATIAL_CRAFTING_SERIALIZER = 
        RECIPE_SERIALIZERS.register("spatial_crafting", () -> SpatialCrafterRecipeSerializer.INSTANCE);

    // Sound Events
    public static final RegistryObject<SoundEvent> SPATIAL_CRAFTER_START = SOUND_EVENTS.register("spatial_crafter_start",
            () -> SoundEvent.createVariableRangeEvent(Main.rl("spatial_crafter_start")));
    
    public static final RegistryObject<SoundEvent> SPATIAL_CRAFTER_COMPLETE = SOUND_EVENTS.register("spatial_crafter_complete",
            () -> SoundEvent.createVariableRangeEvent(Main.rl("spatial_crafter_complete")));

    public static void init() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        CONTAINERS.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        SOUND_EVENTS.register(bus);
    }


    public static <B extends Block> RegistryObject<Item> fromBlock(RegistryObject<B> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), ITEM_PROPERTIES));
    }

}
