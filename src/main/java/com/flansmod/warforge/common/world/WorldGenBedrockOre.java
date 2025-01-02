package com.flansmod.warforge.common.world;

import java.util.Random;

import com.flansmod.warforge.common.ModuloHelper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public class WorldGenBedrockOre extends WorldGenerator 
{
	private final IBlockState blockState, outerState;
	private int cellSize = 64;
	private int depositRadius = 4;
	private int outerShellRadius = 8;
	private float outerShellProbability = 0.1f;
	private int minInstancesPerCell = 1;
	private int maxInstancesPerCell = 3;
	private int minHeight = 0;
	private int maxHeight = 5;
	
	public WorldGenBedrockOre(IBlockState block, IBlockState outer, int cellSize, int depositRadius, int outerShellRadius, float outerShellProbability,
			int minInstancesPerCell, int maxInstancesPerCell, int minHeight, int maxHeight)
	{
		this.blockState = block;
		this.outerState = outer;
		this.cellSize = cellSize;
		this.depositRadius = depositRadius;
		this.outerShellRadius = outerShellRadius;
		this.outerShellProbability = outerShellProbability;
		this.minInstancesPerCell = minInstancesPerCell;
		this.maxInstancesPerCell = maxInstancesPerCell;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		
		if(this.depositRadius * 2 >= this.cellSize)
		{
			this.cellSize = this.depositRadius * 4;
		}
	}
	
	
	@Override
	public boolean generate(World world, Random rand, BlockPos cornerPos) 
	{
		// We generate the 8-24 area offset from pos. So +16 is our centerpoint
		int x = cornerPos.getX() + 8;
		int z = cornerPos.getZ() + 8;
		
		int xCell = ModuloHelper.divide(x, cellSize);
		int zCell = ModuloHelper.divide(z, cellSize);
		
		Random cellRNG = new Random();
		cellRNG.setSeed(world.getSeed());
		long seedA = cellRNG.nextLong() / 2L * 2L + 1L;
		long seedB = cellRNG.nextLong() / 2L * 2L + 1L;
		cellRNG.setSeed((long)xCell * seedA + (long)zCell * seedB ^ world.getSeed());
		
		int numDepositsInCell = cellRNG.nextInt(maxInstancesPerCell - minInstancesPerCell + 1) + minInstancesPerCell;
		for(int cellIndex = 0; cellIndex < numDepositsInCell; cellIndex++)
		{
			// Choose a random starting point, at least one radius from the edge of the cell
			int depositX = xCell * cellSize + 8 + cellRNG.nextInt(cellSize - 4 * depositRadius) + depositRadius * 2;
			int depositZ = zCell * cellSize + 8 + cellRNG.nextInt(cellSize - 4 * depositRadius) + depositRadius * 2;
			BlockPos depositPosA = new BlockPos(depositX, minHeight + cellRNG.nextInt(maxHeight - minHeight), depositZ);
						
			int minY = depositPosA.getY() - depositRadius;
			int maxY = depositPosA.getY() + depositRadius;
			
			for(int i = 8; i < 24; i++)
			{
				for(int k = 8; k < 24; k++)
				{
					// Create the vein, cap to y=4 because we are replacing bedrock blocks
					// We don't mind making holes, because we are indestructible too
					for(int j = minY; j < Math.min(maxY, 5); j++)
					{
						BlockPos p = new BlockPos(cornerPos.getX() + i, j, cornerPos.getZ() + k);
						if(world.getBlockState(p).getBlock() == Blocks.BEDROCK)
						{							
							double radius = depositRadius + rand.nextGaussian();
							
							if(p.distanceSq(depositPosA) <= (radius * radius))
								world.setBlockState(p, blockState);
							
							radius = outerShellRadius;
							// These could be breakable, so don't swap bottom layer bedrock
							if(radius > 0 && j > 0)
							{
								if(rand.nextFloat() < outerShellProbability)
								{
									if(p.distanceSq(depositPosA) <= (radius * radius))
										world.setBlockState(p, outerState);
								}
							}
						}
					}
					
					// And create surface indicators, but with high rarity
					if(rand.nextFloat() < outerShellProbability * 0.25f)
					{
						BlockPos y0Pos = new BlockPos(cornerPos.getX() + i, 0, cornerPos.getZ() + k);
						if(y0Pos.distanceSq(depositPosA.getX(), 0, depositPosA.getZ()) <= depositRadius * depositRadius)
						{
							BlockPos topPos = world.getTopSolidOrLiquidBlock(y0Pos);
							world.setBlockState(topPos.down(), Blocks.GRAVEL.getDefaultState());
						}
					}
				}
			}
		}
		
		return false;
	}

}
