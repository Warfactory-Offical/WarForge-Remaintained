package com.flansmod.warforge.common.world;

import java.util.Random;

import com.flansmod.warforge.common.ModuloHelper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public class WorldGenDenseOre extends WorldGenerator
{
	private final IBlockState blockState, outerState;
	private int cellSize = 64;
	private int depositRadius = 4;
	private int outerShellRadius = 8;
	private float outerShellProbability = 0.1f;
	private int minInstancesPerCell = 1;
	private int maxInstancesPerCell = 3;
	private int minHeight = 110;
	private int maxHeight = 120;
	
	public WorldGenDenseOre(IBlockState block, IBlockState outer, int cellSize, int depositRadius, int outerShellRadius, float outerShellProbability,
			int minInstancesPerCell, int maxInstancesPerCell, int minHeight, int maxHeight)
	{
		blockState = block;
		outerState = outer;
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
		final int xOffset = WorldHelpers.calculateOffset(cornerPos.getX());
		final int zOffset = WorldHelpers.calculateOffset(cornerPos.getZ());
		
		int xCell = ModuloHelper.divide(xOffset, cellSize);
		int zCell = ModuloHelper.divide(zOffset, cellSize);
		
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
			
			// Offset by a random amount for the endpoint
			BlockPos depositPosB = new BlockPos(
					depositPosA.getX() + cellRNG.nextInt(9) - 4, 
					depositPosA.getY() + cellRNG.nextInt(7) - 3, 
					depositPosA.getZ() + cellRNG.nextInt(9) - 4);
			Vec3i depositPosDelta = new Vec3i(cellRNG.nextInt(9) - 4, cellRNG.nextInt(7) - 3, cellRNG.nextInt(9) - 4);
			
			final double distBetweenDepos = depositPosB.distanceSq(depositPosA);
			int minY = Math.min(depositPosA.getY(), depositPosB.getY()) - depositRadius;
			int maxY = Math.max(depositPosA.getY(), depositPosB.getY()) + depositRadius;
			
			for(int x = 8; x < 24; x++)
			{
				for(int z = 8; z < 24; z++)
				{
					// Create the vein
					for(int y = minY; y < maxY; y++)
					{
						final BlockPos p = new BlockPos(cornerPos.getX() + x, y, cornerPos.getZ() + z);
						final BlockPos delta = depositPosA.subtract(p);

						if(WorldHelpers.isAnyBlock(world, p, Blocks.WATER, Blocks.FLOWING_WATER, Blocks.BEDROCK))
						{
							continue;
						}
						
						final double distToLineSq = delta.crossProduct(depositPosDelta).distanceSq(Vec3i.NULL_VECTOR) /
								depositPosDelta.distanceSq(Vec3i.NULL_VECTOR);
						final double radius = depositRadius + rand.nextGaussian();

						if(WorldHelpers.withinDistance(distToLineSq, p.distanceSq(depositPosA), p.distanceSq(depositPosB),
								radius, distBetweenDepos))
							world.setBlockState(p, blockState);

						if(radius > 0 && rand.nextFloat() < outerShellProbability)
						{
							// outerShellRadius
							if(WorldHelpers.withinDistance(distToLineSq, p.distanceSq(depositPosA), p.distanceSq(depositPosB),
									outerShellRadius, distBetweenDepos))
								world.setBlockState(p, outerState);
						}
					}
					
					// And create surface indicators, but with high rarity
					if(rand.nextFloat() < outerShellProbability * 0.25f)
					{
						BlockPos y0Pos = new BlockPos(cornerPos.getX() + x, 0, cornerPos.getZ() + z);
						final double distToDepoA = y0Pos.distanceSq(depositPosA.getX(), 0, depositPosA.getZ());
						final double distToDepoB = y0Pos.distanceSq(depositPosB.getX(), 0, depositPosB.getZ());

						if(WorldHelpers.withinRadius(distToDepoA, depositRadius)
						|| WorldHelpers.withinRadius(distToDepoB, depositRadius))
						{
							final BlockPos topPos = world.getTopSolidOrLiquidBlock(y0Pos);
							world.setBlockState(topPos.down(), Blocks.GRAVEL.getDefaultState());
						}
					}
				}
			}
		}
		
		return false;
	}

}
