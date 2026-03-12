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
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static igentuman.spatialcrafter.Main.MODID;

public class Setup {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, MODID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    
    // Recipe Types
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = 
        DeferredRegister.create(Registries.RECIPE_TYPE, MODID);

    // Recipe Serializers
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = 
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);


    public static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops();
    public static final Item.Properties ITEM_PROPERTIES = new Item.Properties();

    public static final DeferredHolder<Block, SpatialCrafterBlock> SPATIAL_CRAFTER_BLOCK = BLOCKS.register("spatial_crafter", () -> new SpatialCrafterBlock(BLOCK_PROPERTIES));

    public static final DeferredHolder<Item, Item> SPATIAL_CRAFTER_ITEM = fromBlock(SPATIAL_CRAFTER_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpatialCrafterBlockEntity>> SPATIAL_CRAFTER_BE = BLOCK_ENTITIES.register("spatial_crafter", () -> BlockEntityType.Builder.of(SpatialCrafterBlockEntity::new, SPATIAL_CRAFTER_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<SpatialCrafterContainer>> SPATIAL_CRAFTER_CONTAINER = CONTAINERS.register("spatial_crafter",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new SpatialCrafterContainer(windowId, data.readBlockPos(), inv, inv.player)));

    // Recipe registrations
    public static final DeferredHolder<RecipeType<?>, RecipeType<SpatialCrafterRecipe>> SPATIAL_CRAFTING_TYPE = 
        RECIPE_TYPES.register("spatial_crafting", () -> SpatialCrafterRecipeType.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpatialCrafterRecipe>> SPATIAL_CRAFTING_SERIALIZER = 
        RECIPE_SERIALIZERS.register("spatial_crafting", () -> SpatialCrafterRecipeSerializer.INSTANCE);

    // Sound Events
    public static final DeferredHolder<SoundEvent, SoundEvent> SPATIAL_CRAFTER_START = SOUND_EVENTS.register("spatial_crafter_start",
            () -> SoundEvent.createVariableRangeEvent(Main.rl("spatial_crafter_start")));
    
    public static final DeferredHolder<SoundEvent, SoundEvent> SPATIAL_CRAFTER_COMPLETE = SOUND_EVENTS.register("spatial_crafter_complete",
            () -> SoundEvent.createVariableRangeEvent(Main.rl("spatial_crafter_complete")));

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        CONTAINERS.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        SOUND_EVENTS.register(bus);
    }


    public static <B extends Block> DeferredHolder<Item, Item> fromBlock(DeferredHolder<Block, B> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), ITEM_PROPERTIES));
    }

}
