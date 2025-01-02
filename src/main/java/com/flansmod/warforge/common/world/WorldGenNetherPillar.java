package com.flansmod.warforge.common.world;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public class WorldGenNetherPillar extends WorldGenerator 
{
	private final IBlockState denseState, outerState;
	
	public WorldGenNetherPillar(IBlockState dense, IBlockState outer)
	{
		denseState = dense;
		outerState = outer;
	}
	
	@Override
	public boolean generate(World worldIn, Random rand, BlockPos position) 
	{
		for(int x = 0; x < 16; x++)
		{
			for(int z = 0; z < 16; z++)
			{
				double xzDistSq = (x - 8) * (x - 8) + (z - 8) * (z - 8);
				for(int y = 1; y < 127; y++)
				{
					double targetRadius = 1 + 5 * (Math.abs(y - 64) / (double)64);
					BlockPos pos = position.add(x + 8, y - position.getY(), z + 8);
					if(xzDistSq <= targetRadius * targetRadius)
					{
						if(xzDistSq <= (targetRadius - 1) * (targetRadius - 1))
						{
							worldIn.setBlockState(pos, denseState);
						}
						else
						{
							final IBlockState state = rand.nextInt(3) == 0 ? outerState : Blocks.NETHERRACK.getDefaultState();
							worldIn.setBlockState(pos, state);
						}
					}
				}
			}
		}
		
		return false;
	}
	
	
}
