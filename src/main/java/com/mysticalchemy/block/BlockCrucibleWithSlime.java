package com.mysticalchemy.block;

import com.mysticalchemy.registry.ModBlocks;
import com.mysticalchemy.registry.ModItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCrucibleWithSlime extends BlockCrucibleVariant {
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() != ModItems.SPOON) {
            return false;
        }

        if (!world.isRemote) {
            ItemStack slime = new ItemStack(Items.SLIME_BALL);
            if (!player.inventory.addItemStackToInventory(slime)) {
                player.dropItem(slime, false);
            }

            if (!player.capabilities.isCreativeMode) {
                held.damageItem(1, player);
            }

            world.setBlockState(pos, ModBlocks.CRUCIBLE.getDefaultState(), 3);
            world.playEvent(2005, pos, 0);
        }

        return true;
    }
}
