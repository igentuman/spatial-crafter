package igentuman.spatialcrafter.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import igentuman.spatialcrafter.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SpatialCrafterRecipeSerializer implements RecipeSerializer<SpatialCrafterRecipe> {
    public static final SpatialCrafterRecipeSerializer INSTANCE = new SpatialCrafterRecipeSerializer();

    @Override
    public SpatialCrafterRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        ResourceLocation multiblockId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "multiblock"));
        if (multiblockId == null) {
            throw new JsonParseException("Invalid multiblock ID in recipe " + recipeId);
        }
        
        // Parse outputs
        List<ItemStack> outputs = new ArrayList<>();
        if (json.has("outputs")) {
            JsonArray outputsArray = GsonHelper.getAsJsonArray(json, "outputs");
            for (JsonElement element : outputsArray) {
                JsonObject outputObj = element.getAsJsonObject();
                
                ItemStack stack = ShapedRecipe.itemStackFromJson(outputObj);
                
                // Handle NBT data
                if (outputObj.has("nbt")) {
                    try {
                        CompoundTag nbt = TagParser.parseTag(outputObj.get("nbt").getAsString());
                        stack.setTag(nbt);
                    } catch (Exception e) {
                        throw new JsonParseException("Invalid NBT data in recipe " + recipeId, e);
                    }
                }
                outputs.add(stack);
            }
        }

        // Parse entity outputs
        List<SpatialCrafterRecipe.EntityOutput> entityOutputs = new ArrayList<>();
        if (json.has("entity_outputs")) {
            JsonArray entityArray = GsonHelper.getAsJsonArray(json, "entity_outputs");
            for (JsonElement element : entityArray) {
                JsonObject entityObj = element.getAsJsonObject();
                
                ResourceLocation entityId = ResourceLocation.tryParse(GsonHelper.getAsString(entityObj, "entity"));
                if (entityId == null) {
                    throw new JsonParseException("Invalid entity ID in recipe " + recipeId);
                }
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                if (entityType == null) {
                    throw new JsonParseException("Unknown entity type '" + entityId + "' in recipe " + recipeId);
                }
                
                CompoundTag nbt = new CompoundTag();
                if (entityObj.has("nbt")) {
                    try {
                        nbt = TagParser.parseTag(entityObj.get("nbt").getAsString());
                    } catch (Exception e) {
                        throw new JsonParseException("Invalid entity NBT data in recipe " + recipeId, e);
                    }
                }
                
                BlockPos relativePos = BlockPos.ZERO;
                if (entityObj.has("position")) {
                    JsonArray posArray = GsonHelper.getAsJsonArray(entityObj, "position");
                    relativePos = new BlockPos(
                        posArray.get(0).getAsInt(),
                        posArray.get(1).getAsInt(),
                        posArray.get(2).getAsInt()
                    );
                }
                
                int count = GsonHelper.getAsInt(entityObj, "count", 1);
                
                entityOutputs.add(new SpatialCrafterRecipe.EntityOutput(entityType, nbt, relativePos, count));
            }
        }

        int processingTime = GsonHelper.getAsInt(json, "processing_time", 200);
        int energyConsumption = GsonHelper.getAsInt(json, "energy_consumption", 1000);
        
        CompoundTag outputNbt = new CompoundTag();
        if (json.has("global_nbt")) {
            try {
                outputNbt = TagParser.parseTag(json.get("global_nbt").getAsString());
            } catch (Exception e) {
                throw new JsonParseException("Invalid global NBT data in recipe " + recipeId, e);
            }
        }

        return new SpatialCrafterRecipe(recipeId, multiblockId, outputs, entityOutputs, 
                                      processingTime, energyConsumption, outputNbt);
    }

    @Override
    public @Nullable SpatialCrafterRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        ResourceLocation multiblockId = buffer.readResourceLocation();
        
        // Read outputs
        int outputCount = buffer.readVarInt();
        List<ItemStack> outputs = new ArrayList<>();
        for (int i = 0; i < outputCount; i++) {
            outputs.add(buffer.readItem());
        }
        
        // Read entity outputs
        int entityOutputCount = buffer.readVarInt();
        List<SpatialCrafterRecipe.EntityOutput> entityOutputs = new ArrayList<>();
        for (int i = 0; i < entityOutputCount; i++) {
            ResourceLocation entityId = buffer.readResourceLocation();
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
            CompoundTag nbt = buffer.readNbt();
            BlockPos pos = buffer.readBlockPos();
            int count = buffer.readVarInt();
            
            // If entity type is null, skip this recipe (shouldn't happen in network but safety check)
            if (entityType == null) {
                throw new RuntimeException("Unknown entity type '" + entityId + "' received from network");
            }
            
            entityOutputs.add(new SpatialCrafterRecipe.EntityOutput(entityType, nbt, pos, count));
        }
        
        int processingTime = buffer.readVarInt();
        int energyConsumption = buffer.readVarInt();
        CompoundTag outputNbt = buffer.readNbt();

        return new SpatialCrafterRecipe(recipeId, multiblockId, outputs, entityOutputs, 
                                      processingTime, energyConsumption, outputNbt);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, SpatialCrafterRecipe recipe) {
        buffer.writeResourceLocation(recipe.getMultiblockId());
        
        // Write outputs
        buffer.writeVarInt(recipe.getOutputs().size());
        for (ItemStack output : recipe.getOutputs()) {
            buffer.writeItem(output);
        }
        
        // Write entity outputs
        buffer.writeVarInt(recipe.getEntityOutputs().size());
        for (SpatialCrafterRecipe.EntityOutput entityOutput : recipe.getEntityOutputs()) {
            buffer.writeResourceLocation(ForgeRegistries.ENTITY_TYPES.getKey(entityOutput.getEntityType()));
            buffer.writeNbt(entityOutput.getNbt());
            buffer.writeBlockPos(entityOutput.getRelativePos());
            buffer.writeVarInt(entityOutput.getCount());
        }
        
        buffer.writeVarInt(recipe.getProcessingTime());
        buffer.writeVarInt(recipe.getEnergyConsumption());
        buffer.writeNbt(recipe.getOutputNbt());
    }
}