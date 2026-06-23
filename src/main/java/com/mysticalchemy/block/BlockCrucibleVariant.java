package com.mysticalchemy.block;

import com.mysticalchemy.registry.ModBlocks;
import com.mysticalchemy.registry.ModTabs;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class BlockCrucibleVariant extends BlockCauldron {
    public BlockCrucibleVariant() {
        setHardness(2.0f);
        setResistance(10.0f);
        setSoundType(SoundType.METAL);
        setCreativeTab(ModTabs.MYSTIC_ALCHEMY);
        setDefaultState(blockState.getBaseState().withProperty(LEVEL, Integer.valueOf(3)));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(ModBlocks.EMPTY_CRUCIBLE);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(ModBlocks.EMPTY_CRUCIBLE);
    }
}
