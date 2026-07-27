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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;

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
    private static boolean particlesEnabled = true;
    private static float particleIntensity = 0.5f;
    private static int particleTimer = 0;

    // --- Baked geometry cache ------------------------------------------------
    private static final StructureMesh mesh = new StructureMesh();
    private static final FakeStructureLevel fakeLevel = new FakeStructureLevel();
    private static MultiblockStructure builtStructure = null;

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
    
    /**
     * Enable or disable particle effects
     */
    public static void setParticlesEnabled(boolean enabled) {
        particlesEnabled = enabled;
    }
    
    /**
     * Check if particles are enabled
     */
    public static boolean areParticlesEnabled() {
        return particlesEnabled;
    }
    
    /**
     * Set particle intensity (0.0 to 2.0)
     */
    public static void setParticleIntensity(float intensity) {
        particleIntensity = Math.max(0.0f, Math.min(2.0f, intensity));
    }
    
    /**
     * Get current particle intensity
     */
    public static float getParticleIntensity() {
        return particleIntensity;
    }

    public static boolean renderPreview(Vec3 center, PoseStack poseStack, float partialTicks, int renderTick) {
        BlockPos pos = new BlockPos((int) center.x(), (int) center.y(), (int) center.z());
        return renderPreview(pos, poseStack, partialTicks, renderTick);
    }
    public static boolean renderPreview(BlockPos center, PoseStack poseStack, float partialTicks, int renderTick) {

        if (structure == null) return false;

        height = structure.getHeight();
        length = structure.getDepth();
        width = structure.getWidth();

        // Rebuild baked geometry only when the selected structure changes.
        if (structure != builtStructure) {
            rebuildMesh();
        }

        // Advance the pulsing-alpha animation.
        interpolatedAlpha += partialTicks * dir;
        if (interpolatedAlpha >= 0.8f) dir = -0.005f;
        if (interpolatedAlpha <= 0.5f) dir = 0.005f;

        // Update rotation animation only if enabled
        rotation += rotationSpeed * partialTicks;
        if (rotation >= 360.0f) {
            rotation -= 360.0f;
        }

        poseStack.pushPose();

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // Translate to the center position relative to camera
        poseStack.translate(center.getX() - cameraPos.x + 0.5f, center.getY() - cameraPos.y + 0.25, center.getZ() - cameraPos.z + 0.5f);

        // Apply rotation around Y axis at the center
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Apply scale
        float finalScale = 0.5f;
        poseStack.scale(finalScale, finalScale, finalScale);

        // Center the structure around its midpoint so rotation spins around the center.
        poseStack.translate(-(width / 2.0f - 0.5f), -(height / 2.0f - 0.5f), -(length / 2.0f - 0.5f));

        // Combine GL model-view (camera rotation) with poseStack (translation/rotation/scale) so the
        // VBO drawWithShader call gets the full transform — BufferUploader.drawWithShader applies
        // GL state automatically, but vbo.drawWithShader needs explicit matrices.
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());

        // Baked blocks (VBOs) drawn as a translucent ghost (depth test off).
        mesh.draw(mv, RenderSystem.getProjectionMatrix(), interpolatedAlpha, false);

        poseStack.popPose();

        // Render particle effects if enabled
        if (particlesEnabled) {
            renderParticleEffects(center, partialTicks, renderTick);
        }

        return true;
    }

    /**
     * Populates the fake level with structure blocks (local coords, origin at 0,0,0)
     * and re-bakes the mesh. Geometry is produced through {@code tesselateBlock} with a
     * level view, so connected textures (CTM), model data and internal face culling all
     * work correctly. Re-baking only happens when the structure changes, not every frame.
     */
    private static void rebuildMesh() {
        if (structure == null) return;
        fakeLevel.clear();

        Map<BlockPos, BlockState> blocks = structure.getBlocks();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockState blockState = entry.getValue();
            if (blockState.isAir()) continue;

            BlockPos structurePos = entry.getKey();
            int xo = structurePos.getX() - structure.getMinX();
            int yo = structurePos.getY() - structure.getMinY();
            int zo = structurePos.getZ() - structure.getMinZ();

            fakeLevel.put(new BlockPos(xo, yo, zo), blockState);
        }

        try {
            mesh.rebuild(fakeLevel);
        } catch (Exception e) {
            // A bad model can throw during tessellation; drop the mesh and skip drawing it.
            mesh.close();
        }
        // Mark as built regardless so a persistent failure doesn't re-bake every frame.
        builtStructure = structure;
    }
    
    /**
     * Renders magical particle effects around the multiblock structure
     */
    private static void renderParticleEffects(BlockPos center, float partialTicks, int renderTick) {
        Level world = mc.level;
        if (world == null || structure == null) return;
        
        particleTimer++;
        
        // Calculate structure bounds for particle positioning
        double structureWidth = width * 0.5f;
        double structureHeight = height * 0.5f;
        double structureLength = length * 0.5f;
        
        // Magical sparkles around the structure
        if (particleTimer % 10 == 0) { // Spawn particles every 3 ticks
            spawnMagicalSparkles(world, center, structureWidth, structureHeight, structureLength);
        }
        
        // Energy orbs circling the structure
        if (particleTimer % 5 == 0) { // Spawn energy orbs every 5 ticks
           // spawnEnergyOrbs(world, center, structureWidth, structureHeight, structureLength, renderTick);
        }

        // Construction particles at block positions
        if (particleTimer % 4 == 0) { // Spawn construction particles every 2 ticks
            spawnConstructionParticles(world, center);
        }
    }
    
    /**
     * Spawns magical sparkle particles around the structure
     */
    private static void spawnMagicalSparkles(Level world, BlockPos center, double width, double height, double length) {
        for (int i = 0; i < (int)(5 * particleIntensity); i++) {
            // Random position around the structure
            double x = center.getX() + 0.5 + (world.random.nextDouble() - 0.5) * (width + 2);
            double y = center.getY() + 0.5 + (world.random.nextDouble() - 0.5) * (height + 2);
            double z = center.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * (length + 2);
            
            // Sparkle particles with slight upward motion
            double velX = (world.random.nextDouble() - 0.5) * 0.02;
            double velY = world.random.nextDouble() * 0.05 + 0.01;
            double velZ = (world.random.nextDouble() - 0.5) * 0.02;
            
            world.addParticle(ParticleTypes.END_ROD, x, y, z, velX, velY, velZ);
        }
    }
    
    /**
     * Spawns energy orbs that circle around the structure
     */
    private static void spawnEnergyOrbs(Level world, BlockPos center, double width, double height, double length, int renderTick) {
        int numOrbs = (int)(3 * particleIntensity);
        for (int i = 0; i < numOrbs; i++) {
            // Calculate circular motion around the structure
            float angle = (renderTick * 0.05f + i * (360f / numOrbs)) % 360f;
            float radius = (float)(Math.max(width, length) + 1.5);
            
            double x = center.getX() + 0.5 + Math.cos(Math.toRadians(angle)) * radius;
            double y = center.getY() + 0.5 + Math.sin(renderTick * 0.02f + i) * height * 0.5;
            double z = center.getZ() + 0.5 + Math.sin(Math.toRadians(angle)) * radius;
            
            // Create glowing orb effect with multiple particles
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0, 0);
            world.addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.01, 0);
        }
    }
    
    /**
     * Spawns floating rune-like particles
     */
    private static void spawnFloatingRunes(Level world, BlockPos center, double width, double height, double length) {
        int numRunes = (int)(2 * particleIntensity);
        for (int i = 0; i < numRunes; i++) {
            // Position runes at the corners and edges of the structure
            double x = center.getX() + 0.5 + (world.random.nextBoolean() ? width + 1 : -width - 1);
            double y = center.getY() + 0.5 + (world.random.nextDouble() - 0.5) * height;
            double z = center.getZ() + 0.5 + (world.random.nextBoolean() ? length + 1 : -length - 1);
            
            // Slow floating motion
            double velY = 0.02;
            
            // Use enchant glint particles for rune effect
            world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, velY, 0);
        }
    }
    
    /**
     * Spawns construction-themed particles at block positions
     */
    private static void spawnConstructionParticles(Level world, BlockPos center) {
        Map<BlockPos, BlockState> blocks = structure.getBlocks();
        
        // Only spawn particles on a subset of blocks to avoid overwhelming
        int particleCount = 0;
        int maxParticles = (int)(10 * particleIntensity);
        
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (particleCount >= maxParticles) break;
            
            BlockPos structurePos = entry.getKey();
            BlockState blockState = entry.getValue();
            
            if (blockState.isAir()) continue;
            
            // Only spawn particles on some blocks randomly
            if (world.random.nextFloat() > 0.1f * particleIntensity) continue;
            
            // Calculate world position
            int xo = structurePos.getX() - structure.getMinX();
            int yo = structurePos.getY() - structure.getMinY();
            int zo = structurePos.getZ() - structure.getMinZ();
            
            double relativeX = xo - (width / 2.0 - 0.5);
            double relativeY = yo - (height / 2.0 - 0.5);
            double relativeZ = zo - (length / 2.0 - 0.5);
            
            double worldX = center.getX() + 0.5 + relativeX;
            double worldY = center.getY() + 0.5 + relativeY;
            double worldZ = center.getZ() + 0.5 + relativeZ;
            
            // Add small offset for particle position
            worldX += (world.random.nextDouble() - 0.5) * 0.8;
            worldY += (world.random.nextDouble() - 0.5) * 0.8;
            worldZ += (world.random.nextDouble() - 0.5) * 0.8;
            
            // Create colored dust particles based on block
            float r = 0.3f + world.random.nextFloat() * 0.4f;
            float g = 0.6f + world.random.nextFloat() * 0.4f;
            float b = 1.0f;
            
            DustParticleOptions dustOptions = new DustParticleOptions(new org.joml.Vector3f(r, g, b), 1.0f);
            world.addParticle(dustOptions, worldX, worldY, worldZ, 0, 0.02, 0);
            
            particleCount++;
        }
    }
}
