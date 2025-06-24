package com.flansmod.warforge.common.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public interface IMultiBlockInit {
    public List<MultiBlockColumn> INSTANCES = new ArrayList<>();
    static public void registerMaps(){
        INSTANCES.forEach(IMultiBlockInit::initMap);
    }

    default void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        setUpMultiblock(world, pos, state);
    }

    public void setUpMultiblock(World world, BlockPos pos, IBlockState state);

    public void initMap();

}
