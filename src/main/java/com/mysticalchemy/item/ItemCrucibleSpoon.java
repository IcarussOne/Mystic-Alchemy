package com.mysticalchemy.item;

import com.mysticalchemy.block.BlockCrucible;
import com.mysticalchemy.tileentity.CrucibleTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemCrucibleSpoon extends Item {
    public ItemCrucibleSpoon() {
        setMaxStackSize(1);
        setMaxDamage(200);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        if (!(world.getBlockState(pos).getBlock() instanceof BlockCrucible)) {
            return EnumActionResult.PASS;
        }

        CrucibleTile tile = BlockCrucible.getCrucibleTile(world, pos);
        if (tile == null) {
            return EnumActionResult.PASS;
        }

        if (!world.isRemote) {
            ItemStack stack = player.getHeldItem(hand);
            tile.stir();
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.7f, 1.0f);

            if (!player.capabilities.isCreativeMode) {
                stack.damageItem(1, player);
            }
        }

        return EnumActionResult.SUCCESS;
    }
}
