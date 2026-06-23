package com.mysticalchemy.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class BlockCrucibleWithStone extends BlockCrucibleVariant {
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // Stone state is intentionally non-interactive: it should be break-only.
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            spawnAsEntity(world, pos, new ItemStack(Blocks.COBBLESTONE, 1 + world.rand.nextInt(2)));
            spawnAsEntity(world, pos, new ItemStack(Items.IRON_NUGGET, 2 + world.rand.nextInt(3)));
            spawnAsEntity(world, pos, new ItemStack(Items.GOLD_NUGGET, 1 + world.rand.nextInt(2)));
        }
        super.breakBlock(world, pos, state);
    }
}
