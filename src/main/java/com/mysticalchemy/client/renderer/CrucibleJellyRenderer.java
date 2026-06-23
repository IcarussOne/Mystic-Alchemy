package com.mysticalchemy.client.renderer;

import com.mysticalchemy.tileentity.CrucibleJellyTile;
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
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;

import java.util.List;

public class CrucibleJellyRenderer extends TileEntitySpecialRenderer<CrucibleJellyTile> {
    private static final int DEFAULT_JELLY_COLOR = 0x7AC74F;

    @Override
    public void render(CrucibleJellyTile te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
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

        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("mysticalchemy:block/slime");
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        List<PotionEffect> effects = te.getStoredEffects();
        int color = effects.isEmpty() ? DEFAULT_JELLY_COLOR : PotionUtils.getPotionColorFromEffectList(effects);
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        int brightness = te.getWorld().getCombinedLight(te.getPos().up(), 0);
        int skyLight = brightness >> 16 & 65535;
        int blockLight = brightness & 65535;

        double min = 0.125d;
        double max = 0.875d;
        double height = (6.0d + 3.0d * level) / 16.0d;
        double uMin = sprite.getMinU();
        double uMax = sprite.getMaxU();
        double vMin = sprite.getMinV();
        double vMax = sprite.getMaxV();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);
        buffer.pos(min, height, max).tex(uMin, vMax).lightmap(skyLight, blockLight).color(red, green, blue, 255).endVertex();
        buffer.pos(max, height, max).tex(uMax, vMax).lightmap(skyLight, blockLight).color(red, green, blue, 255).endVertex();
        buffer.pos(max, height, min).tex(uMax, vMin).lightmap(skyLight, blockLight).color(red, green, blue, 255).endVertex();
        buffer.pos(min, height, min).tex(uMin, vMin).lightmap(skyLight, blockLight).color(red, green, blue, 255).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
