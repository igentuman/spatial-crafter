package igentuman.spatialcrafter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import igentuman.spatialcrafter.CommonConfig;
import igentuman.spatialcrafter.Main;
import igentuman.spatialcrafter.block.SpatialCrafterBlockEntity;
import igentuman.spatialcrafter.recipe.SpatialCrafterRecipe;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT)
public class SpatialCrafterOverlayHandler {
    
    public static final CopyOnWriteArrayList<BlockPos> spatialCrafterBlocks = new CopyOnWriteArrayList<>();
    
    @SubscribeEvent
    public static void onRenderWorldEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        
        Player player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) {
            return;
        }
        
        Level level = player.level();
        if (!level.isClientSide) {
            return;
        }
        
        // Render overlay for all registered spatial crafter blocks
        for (BlockPos pos : spatialCrafterBlocks) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpatialCrafterBlockEntity spatialCrafter) {
                if(spatialCrafter.isDisabled) continue;
                AABB scanArea = spatialCrafter.getScanArea();
                float alpha = 0.125f;
                // Check if there's a current recipe and render structure preview
                SpatialCrafterRecipe currentRecipe = spatialCrafter.getCurrentRecipe();
                if (currentRecipe instanceof SpatialCrafterRecipe && spatialCrafter.isProcessing()) {
                    PreviewRenderer.setStructure(currentRecipe.getStructure());
                    PreviewRenderer.renderPreview(scanArea.getCenter(), event.getPoseStack(), event.getPartialTick().getRealtimeDeltaTicks(), event.getRenderTick());
                    alpha = 0.065f;
                }
                scanArea = scanArea
                        .setMaxX((int)scanArea.maxX+1.0001)
                        .setMinX((int)scanArea.minX-0.0001)
                        .setMaxY((int)scanArea.maxY+1.0001)
                        .setMinY((int)scanArea.minY-0.0001)
                        .setMaxZ((int)scanArea.maxZ+1.0001)
                        .setMinZ((int)scanArea.minZ-0.0001);

                // Render the filled box with transparency
                renderFilledBox(event.getPoseStack(), scanArea, 0.2f, 0.8f, 1.0f, alpha);
            }
        }
    }
    
    public static void addSpatialCrafterBlock(BlockPos pos) {
        if (!spatialCrafterBlocks.contains(pos)) {
            spatialCrafterBlocks.add(pos);
        }
    }
    
    public static void removeSpatialCrafterBlock(BlockPos pos) {
        spatialCrafterBlocks.remove(pos);
    }
    
    public static void renderFilledBox(PoseStack poseStack, AABB box, float r, float g, float b, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull(); // Disable face culling so we can see from inside
        
        // Get camera position for proper translation
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double camX = cam.x, camY = cam.y, camZ = cam.z;
        
        // Push matrix to apply transformations
        poseStack.pushPose();
        
        // Just translate relative to camera
        poseStack.translate(-camX, -camY, -camZ);
        
        Tesselator tesselator = Tesselator.getInstance();
        
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = poseStack.last().pose();
        
        // Use the box coordinates directly - they're already in world space
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;
        
        // Small offset to prevent z-fighting between inner and outer faces
        float offset = 0.0005f;
        
        // Outer faces (normal box)
        // Bottom face
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, alpha);
        
        // Top face
        buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, alpha);
        
        // North face
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, alpha);
        
        // South face
        buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, alpha);
        
        // West face
        buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, alpha);
        
        // East face
        buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, alpha);
        
        // Inner faces (slightly offset inward to prevent z-fighting)
        // Bottom face (inner)
        buffer.addVertex(matrix, x1 + offset, y1 + offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1 + offset, y1 + offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y1 + offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y1 + offset, z1 + offset).setColor(r, g, b, alpha);
        
        // Top face (inner)
        buffer.addVertex(matrix, x1 + offset, y2 - offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y2 - offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y2 - offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1 + offset, y2 - offset, z2 - offset).setColor(r, g, b, alpha);
        
        // North face (inner)
        buffer.addVertex(matrix, x1 + offset, y1 + offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y1 + offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y2 - offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1 + offset, y2 - offset, z1 + offset).setColor(r, g, b, alpha);
        
        // South face (inner)
        buffer.addVertex(matrix, x2 - offset, y1 + offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1 + offset, y1 + offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1 + offset, y2 - offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y2 - offset, z2 - offset).setColor(r, g, b, alpha);
        
        // West face (inner)
        buffer.addVertex(matrix, x1 + offset, y1 + offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1 + offset, y1 + offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1 + offset, y2 - offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x1 + offset, y2 - offset, z2 - offset).setColor(r, g, b, alpha);
        
        // East face (inner)
        buffer.addVertex(matrix, x2 - offset, y1 + offset, z1 + offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y1 + offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y2 - offset, z2 - offset).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, x2 - offset, y2 - offset, z1 + offset).setColor(r, g, b, alpha);
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        poseStack.popPose();
        RenderSystem.enableCull(); // Re-enable face culling
        RenderSystem.disableBlend();
    }
}