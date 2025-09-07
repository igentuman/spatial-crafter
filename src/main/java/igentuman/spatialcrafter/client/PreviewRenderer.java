package igentuman.spatialcrafter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import igentuman.spatialcrafter.util.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;


public class PreviewRenderer {
    
    // Interpolate alpha based on partialTicks
    private static float interpolatedAlpha = 0.5F;
    private static MultiblockStructure structure = null;
    private static final Minecraft mc = Minecraft.getInstance();
    private static int height;
    private static int length;
    private static int width;
    private static BlockPos hit;
    private static float dir = 0.005f;
    private static float scale = 1.0f;
    private static float rotation = 0f;
    private static float rotationSpeed = 2.0f; // degrees per tick
    private static float glowIntensity = 1.0f;

    public static void setStructure(MultiblockStructure structure) {
        PreviewRenderer.structure = structure;
    }
    
    /**
     * Get the current rotation angle in degrees
     */
    public static float getRotation() {
        return rotation;
    }
    
    /**
     * Set the rotation angle manually (useful for debugging or specific positioning)
     */
    public static void setRotation(float angle) {
        rotation = angle % 360.0f;
    }
    
    /**
     * Reset the rotation to 0
     */
    public static void resetRotation() {
        rotation = 0f;
    }
    
    /**
     * Set the rotation speed in degrees per tick
     */
    public static void setRotationSpeed(float speed) {
        rotationSpeed = speed;
    }
    
    /**
     * Get the current rotation speed
     */
    public static float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Set the base scale for the preview
     */
    public static void setScale(float newScale) {
        scale = Math.max(0.1f, Math.min(2.0f, newScale)); // Clamp between 0.1 and 2.0
    }
    
    /**
     * Get the current base scale
     */
    public static float getScale() {
        return scale;
    }

    
    /**
     * Set the glow intensity (0.0 to 2.0)
     */
    public static void setGlowIntensity(float intensity) {
        glowIntensity = Math.max(0.0f, Math.min(2.0f, intensity));
    }
    
    /**
     * Get the current glow intensity
     */
    public static float getGlowIntensity() {
        return glowIntensity;
    }

    public static boolean renderPreview(BlockPos center, PoseStack poseStack, float partialTicks, int renderTick) {

        if (structure == null) return false;
        poseStack.pushPose();

        height = structure.getHeight();
        length = structure.getDepth();
        width = structure.getWidth();

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // Update rotation animation only if enabled
        rotation += rotationSpeed * partialTicks;
        if (rotation >= 360.0f) {
            rotation -= 360.0f;
        }

        // Translate to the center position relative to camera
        poseStack.translate(center.getX() - cameraPos.x+0.5f, center.getY() - cameraPos.y +0.25, center.getZ() - cameraPos.z+0.5f);
        
        // Apply rotation around Y axis at the center
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // Apply scale
        float finalScale = 0.1f;
        poseStack.scale(finalScale, finalScale, finalScale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        renderPreviewBlocks(poseStack, partialTicks);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
        return true;
    }

    private static void renderPreviewBlocks(PoseStack poseStack, float partialTicks) {
        Level world = mc.level;
        if (world == null || structure == null) return;
        
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        
        // Update alpha animation
        interpolatedAlpha = interpolatedAlpha + partialTicks * dir;
        if (interpolatedAlpha >= 0.8f) {
            dir = -0.005f;
        }
        if (interpolatedAlpha <= 0.5f) {
            dir = 0.005f;
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        
        // Get the buffer source for rendering
        var bufferSource = mc.renderBuffers().bufferSource();
        
        Map<BlockPos, BlockState> blocks = structure.getBlocks();
        
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos structurePos = entry.getKey();
            BlockState blockState = entry.getValue();
            
            if (blockState.isAir()) continue;
            
            // Calculate relative position from structure origin
            int xo = structurePos.getX() - structure.getMinX();
            int yo = structurePos.getY() - structure.getMinY();
            int zo = structurePos.getZ() - structure.getMinZ();
            
            // Position relative to structure center
            double relativeX = xo - (width / 2.0 - 0.5);
            double relativeY = yo - (height / 2.0 - 0.5);
            double relativeZ = zo - (length / 2.0 - 0.5);
            
            poseStack.pushPose();
            poseStack.translate(relativeX, relativeY, relativeZ);
            
            try {
                // Render the block with transparency using translucent render type
                renderTranslucentBlock(blockState, poseStack, bufferSource, interpolatedAlpha);
            } catch (Exception e) {
                renderSimpleCube(poseStack, bufferSource);
            }
            
            poseStack.popPose();
        }
        
        // Flush the buffer to ensure all blocks are rendered
        bufferSource.endBatch();
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    private static void renderTranslucentBlock(BlockState blockState, PoseStack poseStack, 
                                             net.minecraft.client.renderer.MultiBufferSource bufferSource, 
                                             float alpha) {
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        BakedModel model = blockRenderer.getBlockModel(blockState);
        RandomSource random = RandomSource.create(42L);
        
        // Get translucent buffer for transparency
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        
        // Render all faces of the block model with custom alpha
        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(blockState, direction, random);
            renderQuadsWithAlpha(poseStack, buffer, quads, alpha, 15728880, OverlayTexture.NO_OVERLAY);
        }
        
        // Render quads without specific direction (general quads)
        List<BakedQuad> generalQuads = model.getQuads(blockState, null, random);
        renderQuadsWithAlpha(poseStack, buffer, generalQuads, alpha, 15728880, OverlayTexture.NO_OVERLAY);
    }
    
    private static void renderQuadsWithAlpha(PoseStack poseStack, VertexConsumer buffer, 
                                           List<BakedQuad> quads, float alpha, 
                                           int light, int overlay) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            Vec3 quadNormal = new Vec3(quad.getDirection().getNormal().getX(), 
                                     quad.getDirection().getNormal().getY(), 
                                     quad.getDirection().getNormal().getZ());
            
            // Process each vertex (4 vertices per quad)
            for (int i = 0; i < 4; i++) {
                int vertexIndex = i * 8; // Each vertex has 8 integers of data
                
                // Extract position
                float x = Float.intBitsToFloat(vertices[vertexIndex]);
                float y = Float.intBitsToFloat(vertices[vertexIndex + 1]);
                float z = Float.intBitsToFloat(vertices[vertexIndex + 2]);
                
                // Extract color (if available)
                int color = vertices[vertexIndex + 3];
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                
                // Extract UV coordinates
                float u = Float.intBitsToFloat(vertices[vertexIndex + 4]);
                float v = Float.intBitsToFloat(vertices[vertexIndex + 5]);
                
                // Add vertex with custom alpha
                buffer.vertex(pose, x, y, z)
                      .color(r, g, b, alpha)
                      .uv(u, v)
                      .overlayCoords(overlay)
                      .uv2(light)
                      .normal(normal, (float)quadNormal.x, (float)quadNormal.y, (float)quadNormal.z)
                      .endVertex();
            }
        }
    }

    private static void renderSimpleCube(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource) {
        // Fallback method to render a simple translucent cube
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        
        float alpha = interpolatedAlpha;
        float r = 0.8f, g = 0.8f, b = 0.8f; // Light gray color
        
        // Render all 6 faces of the cube
        // Bottom face
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, alpha).endVertex();
        
        // Top face
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, alpha).endVertex();
        
        // North face
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, alpha).endVertex();
        
        // South face
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, alpha).endVertex();
        
        // West face
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, alpha).endVertex();
        
        // East face
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, alpha).endVertex();
        
        tessellator.end();
    }
}
