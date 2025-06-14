package com.flansmod.warforge.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockDummy extends Block implements ITileEntityProvider {
    public static final PropertyEnum<modelEnum> MODEL = PropertyEnum.create("model", modelEnum.class);

    public BlockDummy() {
        super(Material.ROCK);
        this.setBlockUnbreakable();
        this.setResistance(30000000f);
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(MODEL).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(MODEL, modelEnum.values()[meta]);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, MODEL);
    }

    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }


//    @Override
//    public EnumBlockRenderType getRenderType(IBlockState state) {
//        return EnumBlockRenderType.MODEL;
//    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        //TODO: Not ideal, use interface for that down the line
        IBlockState down = worldIn.getBlockState(pos.down());
        if (down.getBlock() instanceof BlockCitadel)
            return state.withProperty(MODEL, modelEnum.KING);
        else if (down.getBlock() instanceof BlockBasicClaim)
            return state.withProperty(MODEL, modelEnum.KNIGHT);
        else if (down.getBlock() instanceof BlockSiegeCamp)
            return state.withProperty(MODEL, modelEnum.BERSERKER);
        else {
            return state.withProperty(MODEL, modelEnum.TRANSLUCENT);
        }

    }

//    @Override
//    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos,
//                                Block blockIn, BlockPos fromPos) {
//        modelEnum stateEnum = state.getValue(MODEL);
//        BlockPos below = pos.down();
//        IBlockState stateBelow = worldIn.getBlockState(below);
//
//        if (stateEnum == modelEnum.TRANSLUCENT) {
//            if (!(stateBelow.getBlock() instanceof BlockDummy))
//                worldIn.destroyBlock(pos, false);
//        } else {
//            if (!(stateBelow.getBlock() instanceof IClaim)) worldIn.destroyBlock(pos, false);
//        }
//    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (!(tileEntity instanceof IBlockDummy dummy)) return false;

        BlockPos masterPos = dummy.getMasterTile();
        if (masterPos == null) return false;

        if (playerIn.isSneaking()) {
            TileEntity masterTile = worldIn.getTileEntity(masterPos);
            if (masterTile instanceof TileEntityClaim claim) {
                claim.increaseRotation(45f);
                return true;
            }
            return false;
        }

        if (!worldIn.isAirBlock(masterPos)) {
            return worldIn.getBlockState(masterPos).getBlock().onBlockActivated(
                    worldIn, masterPos, state, playerIn, hand, facing, hitX, hitY, hitZ
            );
        }

        return false;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityDummy();
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.BLOCK;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    public enum modelEnum implements IStringSerializable {
        TRANSLUCENT,
        KING,
        KNIGHT,
        BERSERKER,
        ;

        @Override
        public String getName() {
            return name().toLowerCase();
        }
    }


}
