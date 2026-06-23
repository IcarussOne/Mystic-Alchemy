package com.mysticalchemy.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemPotionJellyChunk extends ItemFood {
    public ItemPotionJellyChunk() {
        super(0, 0.0f, false);
        setMaxStackSize(64);
        setAlwaysEdible();
    }

    public void setStoredEffects(ItemStack stack, List<PotionEffect> effects) {
        if (stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        if (effects != null) {
            for (PotionEffect effect : effects) {
                if (effect == null) {
                    continue;
                }
                NBTTagCompound effectTag = new NBTTagCompound();
                effect.writeCustomPotionEffectToNBT(effectTag);
                list.appendTag(effectTag);
            }
        }
        tag.setTag("StoredEffects", list);
        stack.setTagCompound(tag);
    }

    public List<PotionEffect> getStoredEffects(ItemStack stack) {
        List<PotionEffect> effects = new ArrayList<PotionEffect>();
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return effects;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("StoredEffects", 9)) {
            return effects;
        }

        NBTTagList list = tag.getTagList("StoredEffects", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
            if (effect != null) {
                effects.add(effect);
            }
        }
        return effects;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        ItemStack result = super.onItemUseFinish(stack, worldIn, entityLiving);
        if (!worldIn.isRemote) {
            for (PotionEffect effect : getStoredEffects(stack)) {
                if (effect.getPotion().isInstant()) {
                    effect.getPotion().affectEntity(null, null, entityLiving, effect.getAmplifier(), 1.0d);
                } else {
                    entityLiving.addPotionEffect(new PotionEffect(effect));
                }
            }
        }
        return result;
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
        // Effects are applied in onItemUseFinish so the same path works for all living entities.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        List<PotionEffect> effects = getStoredEffects(stack);
        if (effects.isEmpty()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.mysticalchemy.potion_jelly_chunk.no_effects"));
            return;
        }

        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.mysticalchemy.potion_jelly_chunk.effects"));
        for (PotionEffect effect : effects) {
            String effectText = I18n.format(effect.getEffectName());
            if (effect.getAmplifier() > 0) {
                effectText = effectText + " " + I18n.format("potion.potency." + effect.getAmplifier());
            }
            if (effect.getDuration() > 20) {
                effectText = effectText + " (" + Potion.getPotionDurationString(effect, 1.0f) + ")";
            }

            if (effect.getPotion().isBadEffect()) {
                tooltip.add(TextFormatting.RED + effectText);
            } else {
                tooltip.add(TextFormatting.BLUE + effectText);
            }
        }
    }
}
