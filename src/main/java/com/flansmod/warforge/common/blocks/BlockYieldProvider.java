package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.api.IItemYieldProvider;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

// An unharvestable resource block that contributes to the yield of any faction that claims it
public class BlockYieldProvider extends Block implements IItemYieldProvider
{
	public ItemStack yieldToProvide = ItemStack.EMPTY;
	public float multiplier = 1.0f;
	 
	public BlockYieldProvider(Material material, ItemStack yieldStack, float multiplier) 
	{
		super(material);
		
		this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
		
		this.setBlockUnbreakable();
		this.setHardness(300000000F);
		
		yieldToProvide = yieldStack;
		this.multiplier = multiplier;
	}

	@Override
	public ItemStack getYieldToProvide()
	{
		return yieldToProvide;
	}

	@Override
	public float getMultiplier()
	{
		return multiplier;
	}
	
	@Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity)
    {
        return false;
    }

	
}
