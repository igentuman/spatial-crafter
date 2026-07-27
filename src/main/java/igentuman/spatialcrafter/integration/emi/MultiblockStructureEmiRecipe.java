package igentuman.spatialcrafter.integration.emi;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import igentuman.spatialcrafter.client.FakeStructureLevel;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipe;
import igentuman.spatialcrafter.util.MultiblockStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static igentuman.spatialcrafter.Setup.SPATIAL_CRAFTER_BLOCK;

public class MultiblockStructureEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final CompoundTag structureNbt;
    private final String name;
    private final MultiblockStructure structure;
    private final List<EmiStack> outputs;
    private final List<EmiIngredient> inputs;
    private final SpatialCrafterRecipe spatialRecipe;
    public int currentLayer = 0;
    // Fake level view for CTM + neighbour face culling; rebuilt when the visible slice changes.
    private final FakeStructureLevel fakeLevel = new FakeStructureLevel();
    private int builtLayer = Integer.MIN_VALUE;
    
    public MultiblockStructureEmiRecipe(ResourceLocation id, CompoundTag structureNbt, String name, SpatialCrafterRecipe spatialRecipe) {
        this.id = id;
        this.structureNbt = structureNbt;
        this.name = name;
        this.spatialRecipe = spatialRecipe;
        this.structure = new MultiblockStructure(structureNbt);
        this.currentLayer = structure.getMaxY();
        
        // Calculate required blocks for structure
        this.outputs = new ArrayList<>();
        this.inputs = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        for (BlockPos pos : structure.getBlocks().keySet()) {
            Block block = structure.getBlocks().get(pos).getBlock();
            if (!blocks.contains(block)) {
                blocks.add(block);
                inputs.add(EmiIngredient.of(Ingredient.of(new ItemStack(block))));
            }
        }
        
        // Count blocks and create structure ingredient outputs
        for (Block block : blocks) {
            int count = 0;
            for (BlockState blockState : structure.getBlocks().values()) {
                if (blockState.getBlock() == block) {
                    count++;
                }
            }
            EmiStack blockStack = EmiStack.of(new ItemStack(block, count));
            outputs.add(blockStack);
        }
        
        // Add recipe outputs if available
        if (spatialRecipe != null && !spatialRecipe.getOutputs().isEmpty()) {
            for (ItemStack output : spatialRecipe.getOutputs()) {
                outputs.add(EmiStack.of(output));
            }
        }
    }
    
    @Override
    public EmiRecipeCategory getCategory() {
        return MultiblockStructureEmiCategory.INSTANCE;
    }
    
    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }
    
    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return List.of(EmiIngredient.of(Ingredient.of(SPATIAL_CRAFTER_BLOCK.get())));
    }
    
    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }
    
    @Override
    public int getDisplayWidth() {
        return 176;
    }
    
    @Override
    public int getDisplayHeight() {
        return 158;
    }
    
    @Override
    public void addWidgets(WidgetHolder widgets) {
        // Add structure name
        widgets.addText(Component.translatable(name), 5, 2, 0xFFFFFFFF, false);
        
        // Add structure ingredients as slots on the left
        int slotIndex = 0;
        for (EmiStack output : outputs) {
            if(output.isEmpty()) continue;
            if (slotIndex < 30) { // Limit slots for display
                SlotWidget slot = widgets.addSlot(output, 5 + (slotIndex % 4) * 18, 12 + (slotIndex / 4) * 18);
                slot.recipeContext(this);
                slotIndex++;
            }
        }
        
        // Add recipe info if available
        if (spatialRecipe != null) {
            int yOffset = 90;
            if (spatialRecipe.getProcessingTime() > 0) {
                widgets.addText(Component.literal("Time: " + spatialRecipe.getProcessingTime() + " ticks"), 5, yOffset, 0xFFFFFFFF, false);
                yOffset += 10;
            }
            if (spatialRecipe.getEnergyConsumption() > 0) {
                widgets.addText(Component.literal("Energy: " + spatialRecipe.getEnergyConsumption() + " FE"), 5, yOffset, 0xFFFFFFFF, false);
                yOffset += 10;
            }
        }
        
        // Add multiblock render widget
        widgets.add(new MultiblockRenderWidget(50, 10, 120, 80, this));
    }
    
    public String getName() {
        return name;
    }
    
    public MultiblockStructure getStructure() {
        return structure;
    }
    
    public SpatialCrafterRecipe getSpatialRecipe() {
        return spatialRecipe;
    }
    
    public void slice() {
        if (structure.getMaxY() < currentLayer) {
            currentLayer = structure.getMaxY();
        }
        currentLayer--;
        if (currentLayer < structure.getMinY()) {
            currentLayer = structure.getMaxY();
        }
    }
    
    private void renderMultiblock(GuiGraphics graphics, MultiblockStructure structure, int maxLayer, int x, int y, int width, int height) {
        // Simplified multiblock rendering for EMI
        graphics.pose().pushPose();
        graphics.pose().translate(x + width / 2.0f, y + height / 2.0f, 100);
        float scale = 50.0f;
        graphics.pose().scale(scale, -scale, scale);
        
        graphics.pose().mulPose(new org.joml.Quaternionf().rotationX((float)Math.toRadians(15))); // Tilt down slightly
        graphics.pose().mulPose(new org.joml.Quaternionf().rotationY((float)Math.toRadians(45))); // Rotate 45 degrees for better view
        
        // Optional: Add slow rotation for visual appeal
        long time = System.currentTimeMillis();
        float angle = (time % 20000) / 20000.0f * (float)(Math.PI * 2);
        graphics.pose().mulPose(new org.joml.Quaternionf().rotationY(angle * 0.5f)); // Slower rotation
        
        renderStructureBlocks(graphics.pose(), structure, maxLayer);
        graphics.pose().popPose();
    }
    
    private void renderStructureBlocks(com.mojang.blaze3d.vertex.PoseStack stack, MultiblockStructure structure, int maxLayer) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        net.minecraft.client.renderer.block.ModelBlockRenderer modelRenderer = blockRenderer.getModelRenderer();

        java.util.Map<BlockPos, BlockState> blocks = structure.getBlocks();
        if (blocks.isEmpty()) return;

        // Rebuild the fake level when the visible slice changes. Only blocks at/below maxLayer
        // are included so top faces exposed by slicing are not culled against hidden upper blocks.
        if (maxLayer != builtLayer) {
            fakeLevel.clear();
            for (java.util.Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
                if (e.getKey().getY() > maxLayer) continue;
                if (e.getValue().isAir()) continue;
                fakeLevel.put(e.getKey(), e.getValue());
            }
            builtLayer = maxLayer;
        }

        // Calculate structure dimensions for scaling
        int structureWidth = structure.getWidth();
        int structureHeight = structure.getHeight();
        int depth = structure.getDepth();
        float scale = 1.0f / Math.max(Math.max(structureWidth, structureHeight), depth);

        stack.scale(scale, scale, scale);

        // Center the structure
        float centerX = structure.getMinX() + structureWidth / 2.0f;
        float centerY = structure.getMinY() + structureHeight / 2.0f;
        float centerZ = structure.getMinZ() + depth / 2.0f;
        stack.translate(-centerX, -centerY, -centerZ);

        net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();

        // Bind lightmap (tesselateBlock reads light through the level) and kill fog.
        minecraft.gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);

        // Render each block via tesselateBlock + fake level: enables CTM and internal face culling.
        for (java.util.Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() > maxLayer) {
                continue;
            }
            BlockState state = entry.getValue();
            if (state.isAir()) continue;

            stack.pushPose();
            stack.translate(pos.getX(), pos.getY(), pos.getZ());

            net.minecraft.client.resources.model.BakedModel model = blockRenderer.getBlockModel(state);
            ModelData modelData;
            try {
                modelData = model.getModelData(fakeLevel, pos, state, ModelData.EMPTY);
            } catch (Exception ignored) {
                modelData = ModelData.EMPTY;
            }
            long seed = state.getSeed(pos);
            for (net.minecraft.client.renderer.RenderType rt : model.getRenderTypes(state, random, modelData)) {
                com.mojang.blaze3d.vertex.VertexConsumer vc = bufferSource.getBuffer(rt);
                modelRenderer.tesselateBlock(
                        fakeLevel, model, state, pos, stack, vc,
                        true, random, seed, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, modelData, rt
                );
            }

            stack.popPose();
        }

        bufferSource.endBatch();
        minecraft.gameRenderer.lightTexture().turnOffLightLayer();
    }
    
    // Custom widget for handling multiblock rendering and mouse input
    private static class MultiblockRenderWidget extends Widget {
        private final int x, y, width, height;
        private final MultiblockStructureEmiRecipe recipe;
        
        public MultiblockRenderWidget(int x, int y, int width, int height, MultiblockStructureEmiRecipe recipe) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.recipe = recipe;
        }
        
        @Override
        public Bounds getBounds() {
            return new Bounds(x, y, width, height);
        }
        
        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            recipe.renderMultiblock(graphics, recipe.structure, recipe.currentLayer, x, y, width, height);
        }
        
        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            // Right-click to slice layers
            if (button == 1) { // Right mouse button
                recipe.slice();
                return true;
            }
            return false;
        }
    }
}