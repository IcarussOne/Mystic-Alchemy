package com.mysticalchemy.client.renderer;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.entity.EntityPotionSlime;
import net.minecraft.client.model.ModelSlime;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSlime;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerSlimeGel;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.util.ResourceLocation;

public class RenderPotionSlime extends RenderSlime {
    private static final ResourceLocation POTION_SLIME_TEXTURE = new ResourceLocation(MysticAlchemy.MODID, "textures/entity/potion_slime.png");

    public RenderPotionSlime(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.layerRenderers.removeIf(layer -> layer instanceof LayerSlimeGel);
        this.addLayer(new LayerPotionSlimeGel(this));
    }

    @Override
    public void doRender(EntitySlime entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected void renderModel(EntitySlime entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
        if (entity instanceof EntityPotionSlime) {
            int color = ((EntityPotionSlime) entity).getSourceColor();
            float red = ((color >> 16) & 255) / 255.0f;
            float green = ((color >> 8) & 255) / 255.0f;
            float blue = (color & 255) / 255.0f;
            GlStateManager.color(red, green, blue, 1.0f);
        }
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected int getColorMultiplier(EntitySlime entitylivingbaseIn, float lightBrightness, float partialTickTime) {
        if (entitylivingbaseIn instanceof EntityPotionSlime) {
            return 0xFF000000 | ((EntityPotionSlime) entitylivingbaseIn).getSourceColor();
        }
        return super.getColorMultiplier(entitylivingbaseIn, lightBrightness, partialTickTime);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySlime entity) {
        return POTION_SLIME_TEXTURE;
    }

    private static final class LayerPotionSlimeGel implements LayerRenderer<EntitySlime> {
        private final RenderPotionSlime renderer;
        private final ModelSlime gelModel = new ModelSlime(0);

        private LayerPotionSlimeGel(RenderPotionSlime renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntitySlime entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (entity.isInvisible()) {
                return;
            }

            int color = entity instanceof EntityPotionSlime ? ((EntityPotionSlime) entity).getSourceColor() : 0x7AC74F;
            float red = ((color >> 16) & 255) / 255.0f;
            float green = ((color >> 8) & 255) / 255.0f;
            float blue = (color & 255) / 255.0f;

            renderer.bindTexture(POTION_SLIME_TEXTURE);
            GlStateManager.color(red, green, blue, 0.85f);
            GlStateManager.enableNormalize();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            this.gelModel.setModelAttributes(this.renderer.getMainModel());
            this.gelModel.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            this.gelModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.disableBlend();
            GlStateManager.disableNormalize();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
