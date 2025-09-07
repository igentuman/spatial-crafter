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
    private static boolean rotationEnabled = true;
    private static boolean pulseEnabled = true;
    private static float pulseScale = 0.0f;
    private static boolean glowEnabled = true;
    private static float glowIntensity = 1.0f;

    public static void setStructure(MultiblockStructure structure) {
        PreviewRenderer.structure = structure;
        // Reset rotation when setting a new structure
        rotation = 0f;
        pulseScale = 0f;
        glowIntensity = 0f;
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
     * Enable or disable rotation animation
     */
    public static void setRotationEnabled(boolean enabled) {
        rotationEnabled = enabled;
    }
    
    /**
     * Check if rotation animation is enabled
     */
    public static boolean isRotationEnabled() {
        return rotationEnabled;
    }
    
    /**
     * Enable or disable pulse scaling effect
     */
    public static void setPulseEnabled(boolean enabled) {
        pulseEnabled = enabled;
    }
    
    /**
     * Check if pulse effect is enabled
     */
    public static boolean isPulseEnabled() {
        return pulseEnabled;
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
     * Enable or disable glow effect
     */
    public static void setGlowEnabled(boolean enabled) {
        glowEnabled = enabled;
    }
    
    /**
     * Check if glow effect is enabled
     */
    public static boolean isGlowEnabled() {
        return glowEnabled;
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
        hit = center.offset(- width/2, 0, - length/2);

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // Update rotation animation
        if (rotationEnabled) {
            rotation += rotationSpeed * partialTicks;
            if (rotation >= 360.0f) {
                rotation -= 360.0f;
            }
        }
        
        // Update pulse animation
        if (pulseEnabled) {
            pulseScale += partialTicks * 0.05f; // Pulse speed
            if (pulseScale >= Math.PI * 2) {
                pulseScale -= Math.PI * 2;
            }
        }

        // Calculate structure center for better rotation
        double centerX = (double) width / 2.0;
        double centerZ = (double) length / 2.0;
        double centerY = (double) height / 2.0;

        // Translate to center position
        poseStack.translate(hit.getX() - cameraPos.x + centerX, hit.getY() - cameraPos.y + centerY, hit.getZ() - cameraPos.z + centerZ);
        
        // Apply rotation around Y-axis (vertical rotation)
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // Calculate final scale with pulse effect
        float finalScale = scale * 0.1f; // Base scale factor
        if (pulseEnabled) {
            float pulseMultiplier = 1.0f + (float) Math.sin(pulseScale) * 0.1f; // 10% pulse variation
            finalScale *= pulseMultiplier;
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        poseStack.scale(finalScale, finalScale, finalScale);

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
            
            // Calculate relative position within structure
            int xo = structurePos.getX() - structure.getMinX();
            int yo = structurePos.getY() - structure.getMinY();
            int zo = structurePos.getZ() - structure.getMinZ();
            
            // Center the coordinates for rotation
            double centerX = (double) width / 2.0;
            double centerZ = (double) length / 2.0;
            
            // Translate to center, apply rotation, then translate back
            double relativeX = xo - centerX;
            double relativeZ = zo - centerZ;
            
            // Since rotation is handled by the pose stack, we don't need to manually rotate coordinates
            // The pose stack rotation will handle the visual rotation
            int finalX = xo;
            int finalZ = zo;
            
            BlockPos worldPos = hit.offset(finalX, yo, finalZ);
            
            // Only render if the position is replaceable
            if (world.getBlockState(worldPos).canBeReplaced()) {
                poseStack.pushPose();
                // Translate relative to structure center (which is now at origin due to earlier translation)
                poseStack.translate(relativeX, yo - (double) height / 2.0, relativeZ);
                
                try {
                    // Calculate enhanced alpha with glow effect
                    float enhancedAlpha = interpolatedAlpha;
                    if (glowEnabled) {
                        enhancedAlpha = Math.min(1.0f, interpolatedAlpha * glowIntensity);
                    }
                    
                    // Render the block with transparency using translucent render type
                    renderTranslucentBlock(blockState, poseStack, bufferSource, enhancedAlpha);
                } catch (Exception e) {
                    // Fallback: render a simple colored cube if block rendering fails
                    renderSimpleCube(poseStack, bufferSource);
                }
                
                poseStack.popPose();
            }
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
        
        // Calculate lighting - use full brightness if glow is enabled
        int lightLevel = glowEnabled ? 15728880 : 15728880; // Always use full brightness for preview
        
        // Render all faces of the block model with custom alpha
        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(blockState, direction, random);
            renderQuadsWithAlpha(poseStack, buffer, quads, alpha, lightLevel, OverlayTexture.NO_OVERLAY);
        }
        
        // Render quads without specific direction (general quads)
        List<BakedQuad> generalQuads = model.getQuads(blockState, null, random);
        renderQuadsWithAlpha(poseStack, buffer, generalQuads, alpha, lightLevel, OverlayTexture.NO_OVERLAY);
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
