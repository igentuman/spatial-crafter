package igentuman.spatialcrafter.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class SpatialCrafterRecipeSerializer implements RecipeSerializer<SpatialCrafterRecipe> {
    public static final SpatialCrafterRecipeSerializer INSTANCE = new SpatialCrafterRecipeSerializer();

    public static final MapCodec<SpatialCrafterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("multiblock").forGetter(SpatialCrafterRecipe::getMultiblockId),
            ItemStack.STRICT_CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(SpatialCrafterRecipe::getOutputs),
            SpatialCrafterRecipe.EntityOutput.CODEC.listOf().optionalFieldOf("entity_outputs", List.of()).forGetter(SpatialCrafterRecipe::getEntityOutputs),
            Codec.INT.fieldOf("processing_time").forGetter(SpatialCrafterRecipe::getProcessingTime),
            Codec.INT.fieldOf("energy_consumption").forGetter(SpatialCrafterRecipe::getEnergyConsumption),
            CompoundTag.CODEC.optionalFieldOf("global_nbt", new CompoundTag()).forGetter(SpatialCrafterRecipe::getOutputNbt),
            Codec.BOOL.optionalFieldOf("do_not_destroy", false).forGetter(SpatialCrafterRecipe::getDoNotDestroy)
    ).apply(instance, SpatialCrafterRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpatialCrafterRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                ResourceLocation.STREAM_CODEC.encode(buffer, recipe.getMultiblockId());
                ItemStack.LIST_STREAM_CODEC.encode(buffer, recipe.getOutputs());
                SpatialCrafterRecipe.EntityOutput.LIST_STREAM_CODEC.encode(buffer, recipe.getEntityOutputs());
                ByteBufCodecs.VAR_INT.encode(buffer, recipe.getProcessingTime());
                ByteBufCodecs.VAR_INT.encode(buffer, recipe.getEnergyConsumption());
                ByteBufCodecs.COMPOUND_TAG.encode(buffer, recipe.getOutputNbt());
                ByteBufCodecs.BOOL.encode(buffer, recipe.getDoNotDestroy());
            },
            buffer -> new SpatialCrafterRecipe(
                    ResourceLocation.STREAM_CODEC.decode(buffer),
                    ItemStack.LIST_STREAM_CODEC.decode(buffer),
                    SpatialCrafterRecipe.EntityOutput.LIST_STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.COMPOUND_TAG.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer)
            )
    );

    @Override
    public MapCodec<SpatialCrafterRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SpatialCrafterRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}