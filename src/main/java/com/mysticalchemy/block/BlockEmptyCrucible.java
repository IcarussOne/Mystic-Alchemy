package com.mysticalchemy.block;

import com.mysticalchemy.registry.ModBlocks;
import com.mysticalchemy.registry.ModTabs;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockEmptyCrucible extends BlockCauldron {
    public BlockEmptyCrucible() {
        setHardness(2.0f);
        setResistance(10.0f);
        setSoundType(SoundType.METAL);
        setCreativeTab(ModTabs.MYSTIC_ALCHEMY);
        setDefaultState(blockState.getBaseState().withProperty(LEVEL, Integer.valueOf(0)));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == Items.WATER_BUCKET) {
            if (!world.isRemote) {
                world.setBlockState(pos, ModBlocks.CRUCIBLE.getDefaultState().withProperty(LEVEL, Integer.valueOf(3)), 3);
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                player.addStat(StatList.CAULDRON_FILLED);
                if (!player.capabilities.isCreativeMode) {
                    player.setHeldItem(hand, new ItemStack(Items.BUCKET));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void fillWithRain(World world, BlockPos pos) {
        if (world.rand.nextInt(20) == 0) {
            world.setBlockState(pos, ModBlocks.CRUCIBLE.getDefaultState().withProperty(LEVEL, Integer.valueOf(1)), 3);
        }
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(this);
    }
}
