package com.mysticalchemy.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockCrucibleMelted extends Block {
    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 3);
    private static final AxisAlignedBB STAGE_0_AABB = new AxisAlignedBB(0.05d, 0.0d, 0.05d, 0.95d, 0.625d, 0.95d);
    private static final AxisAlignedBB STAGE_1_AABB = new AxisAlignedBB(0.05d, 0.0d, 0.05d, 0.95d, 0.375d, 0.95d);
    private static final AxisAlignedBB STAGE_2_AABB = new AxisAlignedBB(0.05d, 0.0d, 0.05d, 0.95d, 0.1875d, 0.95d);
    private static final AxisAlignedBB STAGE_3_AABB = new AxisAlignedBB(0.05d, 0.0d, 0.05d, 0.95d, 0.0625d, 0.95d);

    public BlockCrucibleMelted() {
        super(Material.ROCK);
        setHardness(1.2f);
        setResistance(4.0f);
        setSoundType(SoundType.STONE);
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState().withProperty(STAGE, Integer.valueOf(0)));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        int stage = state.getValue(STAGE).intValue();
        if (stage <= 0) {
            return STAGE_0_AABB;
        }
        if (stage == 1) {
            return STAGE_1_AABB;
        }
        if (stage == 2) {
            return STAGE_2_AABB;
        }
        return STAGE_3_AABB;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) {
            return;
        }
        int stage = state.getValue(STAGE).intValue();
        if (stage < 3 && rand.nextInt(3) == 0) {
            world.setBlockState(pos, state.withProperty(STAGE, Integer.valueOf(stage + 1)), 3);
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
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
            int stage = state.getValue(STAGE).intValue();
            int iron = Math.max(1, 3 - stage);
            int gold = Math.max(1, 2 - stage);
            spawnAsEntity(world, pos, new ItemStack(Items.IRON_NUGGET, iron));
            spawnAsEntity(world, pos, new ItemStack(Items.GOLD_NUGGET, gold));
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{STAGE});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        int stage = meta;
        if (stage < 0) {
            stage = 0;
        } else if (stage > 3) {
            stage = 3;
        }
        return getDefaultState().withProperty(STAGE, Integer.valueOf(stage));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STAGE).intValue();
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, net.minecraft.util.EnumHand hand, net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        return false;
    }
}
