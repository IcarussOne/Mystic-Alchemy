package com.mysticalchemy.item;

import com.mysticalchemy.block.BlockCrucible;
import com.mysticalchemy.tileentity.CrucibleTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ItemSamplingKit extends Item {
    private static final String CALIBRATION_SEED_TAG = "CalibrationSeed";
    private final float marginOfError;

    public ItemSamplingKit(float marginOfError) {
        this.marginOfError = marginOfError;
        setMaxStackSize(1);
    }

    private static String formatMeasuredValue(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        if (!(world.getBlockState(pos).getBlock() instanceof BlockCrucible) || world.isRemote) {
            return EnumActionResult.PASS;
        }

        CrucibleTile tile = BlockCrucible.getCrucibleTile(world, pos);
        if (tile == null) {
            return EnumActionResult.PASS;
        }

        ItemStack held = player.getHeldItem(hand);

        if (tile.getRemainingTicks() <= 0) {
            player.sendStatusMessage(new TextComponentTranslation("message.mysticalchemy.sampling_kit.failure"), false);
            return EnumActionResult.SUCCESS;
        }

        NBTTagCompound tag = held.hasTagCompound() ? held.getTagCompound() : new NBTTagCompound();
        long calibrationSeed = getOrCreateCalibrationSeed(tag, world);

        tag.setFloat("Temperature", applyMargin(tile.getTemperature(), calibrationSeed, "temperature"));
        tag.setInteger("Duration", tile.getRemainingTicks());

        NBTTagCompound effectsTag = new NBTTagCompound();
        for (Map.Entry<Potion, Float> entry : tile.getEffects().entrySet()) {
            Potion potion = entry.getKey();
            if (potion == null || potion.getRegistryName() == null) {
                continue;
            }

            float exact = entry.getValue();
            float measured = applyMargin(exact, calibrationSeed, potion.getRegistryName().toString());
            String key = potion.getRegistryName().toString();
            effectsTag.setFloat(key, measured);
            String potionName = I18n.translateToLocal(potion.getName()).trim();
            TextComponentString measurementText = new TextComponentString(potionName + ": " + formatMeasuredValue(measured));
            if (measured > 1.0f) {
                measurementText.getStyle().setColor(potion.isBadEffect() ? TextFormatting.RED : TextFormatting.GREEN);
            }
            player.sendStatusMessage(measurementText, false);
        }

        tag.setTag("Effects", effectsTag);
        held.setTagCompound(tag);

        return EnumActionResult.SUCCESS;
    }

    private long getOrCreateCalibrationSeed(NBTTagCompound tag, World world) {
        if (!tag.hasKey(CALIBRATION_SEED_TAG)) {
            tag.setLong(CALIBRATION_SEED_TAG, world.rand.nextLong());
        }
        return tag.getLong(CALIBRATION_SEED_TAG);
    }

    private float applyMargin(float value, long calibrationSeed, String channel) {
        if (value == 0.0f || marginOfError <= 0.0f) {
            return value;
        }

        float min = value * (1.0f - marginOfError);
        float max = value * (1.0f + marginOfError);
        return min + new Random(calibrationSeed ^ channel.hashCode()).nextFloat() * (max - min);
    }
}
