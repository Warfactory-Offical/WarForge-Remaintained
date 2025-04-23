package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.Content;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.Map;

import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;

public abstract class MultiBlockColumn extends Block implements IMultiBlockInit {

    protected Map<IBlockState, Vec3i> multiBlockMap;

    public MultiBlockColumn(Material materialIn) {
        super(materialIn);
        IMultiBlockInit.INSTANCES.add(this);
    }

    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        for (Vec3i relativePos : multiBlockMap.values())
            if (!world.isAirBlock(pos.add(relativePos))) return false;
        return true;

    }

    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.BLOCK;
    }


    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        world.setBlockState(pos.up(), Content.statue.getDefaultState().withProperty(MODEL, BlockDummy.modelEnum.KNIGHT), 3);
        world.notifyBlockUpdate(pos.up(), state, state, 3);

        TileEntity teMiddle = world.getTileEntity(pos.up());
        if (teMiddle instanceof TileEntityDummy) {
            ((TileEntityDummy) teMiddle).setMaster(pos);
        }

        world.setBlockState(pos.up(2), Content.dummyTranslusent.getDefaultState().withProperty(MODEL, BlockDummy.modelEnum.TRANSLUCENT), 3);
        world.notifyBlockUpdate(pos.up(2), state, state, 3);

        TileEntity teTop = world.getTileEntity(pos.up(2));
        if (teTop instanceof TileEntityDummy) {
            ((TileEntityDummy) teMiddle).setMaster(pos);
        }
    }


    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        for (Vec3i offset : multiBlockMap.values()) {
            BlockPos offsetPos = pos.add(offset);
            worldIn.setBlockToAir(offsetPos);
        }
        super.breakBlock(worldIn, pos, state);
    }
}
