package com.mysticalchemy.client.renderer;

import com.mysticalchemy.tileentity.CrucibleTile;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class CrucibleRenderer extends TileEntitySpecialRenderer<CrucibleTile> {
    private static final int SURFACE_SUBDIVISIONS = 8;
    private static final double UV_INSET = 0.02d;

    private static void addSurfaceVertex(BufferBuilder buffer, TextureAtlasSprite sprite, double x, double baseHeight, double z, double center, double halfSpan, float spinAngle, float whirlStrength, int skyLight, int blockLight, int red, int green, int blue) {
        double normalizedX = (x - center) / halfSpan;
        double normalizedZ = (z - center) / halfSpan;
        double radius = Math.min(1.0d, Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ));
        double centerPull = 1.0d - radius;
        double centerPullSq = centerPull * centerPull;
        double phase = Math.toRadians(spinAngle);
        double wave = Math.sin(phase + radius * 9.0d) * whirlStrength * 0.012d * centerPull;
        double height = baseHeight - whirlStrength * 0.14d * centerPullSq + wave;

        double tangentX = -normalizedZ;
        double tangentZ = normalizedX;
        double inwardX = -normalizedX;
        double inwardZ = -normalizedZ;
        double swirlFlow = whirlStrength * (0.28d * centerPull + 0.85d * centerPullSq);
        double sinkFlow = whirlStrength * 0.18d * centerPullSq;
        double pulse = Math.sin(phase * 0.8d + radius * 7.5d) * whirlStrength * 0.06d * centerPull;
        double flowX = tangentX * (swirlFlow + pulse) + inwardX * sinkFlow;
        double flowZ = tangentZ * (swirlFlow + pulse) + inwardZ * sinkFlow;

        double sampleX = normalizedX + flowX;
        double sampleZ = normalizedZ + flowZ;
        double globalSpin = phase * 0.35d;
        double globalSin = Math.sin(globalSpin);
        double globalCos = Math.cos(globalSpin);
        double rotatedX = sampleX * globalCos - sampleZ * globalSin;
        double rotatedZ = sampleX * globalSin + sampleZ * globalCos;
        double normalizer = Math.max(1.0d, Math.max(Math.abs(rotatedX), Math.abs(rotatedZ)));
        double stableX = rotatedX / normalizer;
        double stableZ = rotatedZ / normalizer;
        double uMin = lerp(sprite.getMinU(), sprite.getMaxU(), UV_INSET);
        double uMax = lerp(sprite.getMinU(), sprite.getMaxU(), 1.0d - UV_INSET);
        double vMin = lerp(sprite.getMinV(), sprite.getMaxV(), UV_INSET);
        double vMax = lerp(sprite.getMinV(), sprite.getMaxV(), 1.0d - UV_INSET);
        double u = lerp(uMin, uMax, (stableX + 1.0d) * 0.5d);
        double v = lerp(vMin, vMax, (stableZ + 1.0d) * 0.5d);

        buffer.pos(x, height, z).tex(u, v).lightmap(skyLight, blockLight).color(red, green, blue, 230).endVertex();
    }

    private static void addSlimeSurfaceVertex(BufferBuilder buffer, TextureAtlasSprite sprite, double x, double baseHeight, double z, double center, double halfSpan, float blobTime, float progress, int skyLight, int blockLight, int red, int green, int blue) {
        double normalizedX = (x - center) / halfSpan;
        double normalizedZ = (z - center) / halfSpan;
        double radius = Math.min(1.0d, Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ));
        double core = 1.0d - radius;
        double coreSq = core * core;
        double pulse = Math.sin(blobTime) * 0.020d;
        double wobbleA = Math.sin(blobTime * 1.45d + normalizedX * 5.0d + normalizedZ * 3.0d) * 0.010d;
        double wobbleB = Math.cos(blobTime * 2.1d + radius * 8.0d) * 0.008d;
        double progressLift = progress * 0.018d;
        double mound = 0.018d * coreSq;
        double height = baseHeight + progressLift + mound + pulse * (0.55d + core * 0.45d) + (wobbleA + wobbleB) * (0.3d + core * 0.7d);

        double u = lerp(sprite.getMinU(), sprite.getMaxU(), (normalizedX + 1.0d) * 0.5d);
        double v = lerp(sprite.getMinV(), sprite.getMaxV(), (normalizedZ + 1.0d) * 0.5d);
        buffer.pos(x, height, z).tex(u, v).lightmap(skyLight, blockLight).color(red, green, blue, 255).endVertex();
    }

    private static double lerp(double min, double max, double pct) {
        return min + (max - min) * pct;
    }

    @Override
    public void render(CrucibleTile te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te == null || te.getWorld() == null) {
            return;
        }

        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (!state.getProperties().containsKey(BlockCauldron.LEVEL)) {
            return;
        }

        int level = state.getValue(BlockCauldron.LEVEL).intValue();
        if (level <= 0) {
            return;
        }

        boolean slimeAnimating = te.isSlimeSpawnAnimating();
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
                slimeAnimating ? "mysticalchemy:block/slime" : "minecraft:blocks/water_still"
        );
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        double min = 0.125d;
        double max = 0.875d;
        double height = (6.0d + 3.0d * level) / 16.0d;
        double center = 0.5d;
        double halfSpan = (max - min) * 0.5d;
        int red = Math.round(te.getColorRed() * 255.0f);
        int green = Math.round(te.getColorGreen() * 255.0f);
        int blue = Math.round(te.getColorBlue() * 255.0f);
        int brightness = te.getWorld().getCombinedLight(te.getPos().up(), 0);
        int skyLight = brightness >> 16 & 65535;
        int blockLight = brightness & 65535;
        float spinAngle = te.getLiquidSpinAngle(partialTicks);
        float whirlStrength = te.getLiquidSpinStrength();
        float slimeProgress = te.getSlimeSpawnAnimationProgress(partialTicks);
        float blobTime = (te.getWorld().getTotalWorldTime() + partialTicks) * 0.35f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);
        for (int gridX = 0; gridX < SURFACE_SUBDIVISIONS; gridX++) {
            double x0 = lerp(min, max, gridX / (double) SURFACE_SUBDIVISIONS);
            double x1 = lerp(min, max, (gridX + 1) / (double) SURFACE_SUBDIVISIONS);

            for (int gridZ = 0; gridZ < SURFACE_SUBDIVISIONS; gridZ++) {
                double z0 = lerp(min, max, gridZ / (double) SURFACE_SUBDIVISIONS);
                double z1 = lerp(min, max, (gridZ + 1) / (double) SURFACE_SUBDIVISIONS);

                if (slimeAnimating) {
                    addSlimeSurfaceVertex(buffer, sprite, x0, height, z1, center, halfSpan, blobTime, slimeProgress, skyLight, blockLight, red, green, blue);
                    addSlimeSurfaceVertex(buffer, sprite, x1, height, z1, center, halfSpan, blobTime, slimeProgress, skyLight, blockLight, red, green, blue);
                    addSlimeSurfaceVertex(buffer, sprite, x1, height, z0, center, halfSpan, blobTime, slimeProgress, skyLight, blockLight, red, green, blue);
                    addSlimeSurfaceVertex(buffer, sprite, x0, height, z0, center, halfSpan, blobTime, slimeProgress, skyLight, blockLight, red, green, blue);
                } else {
                    addSurfaceVertex(buffer, sprite, x0, height, z1, center, halfSpan, spinAngle, whirlStrength, skyLight, blockLight, red, green, blue);
                    addSurfaceVertex(buffer, sprite, x1, height, z1, center, halfSpan, spinAngle, whirlStrength, skyLight, blockLight, red, green, blue);
                    addSurfaceVertex(buffer, sprite, x1, height, z0, center, halfSpan, spinAngle, whirlStrength, skyLight, blockLight, red, green, blue);
                    addSurfaceVertex(buffer, sprite, x0, height, z0, center, halfSpan, spinAngle, whirlStrength, skyLight, blockLight, red, green, blue);
                }
            }
        }
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
