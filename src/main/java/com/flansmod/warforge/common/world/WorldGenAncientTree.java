package com.flansmod.warforge.common.world;

import java.util.Random;

import com.flansmod.warforge.common.ModuloHelper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.BiomeDictionary;

public class WorldGenAncientTree extends WorldGenerator {
	private final IBlockState blockState, outerState, leafState;
	private int cellSize = 64;
	private float chance = 0.1f;
	private float holeRadius = 10;
	private float treeRadius = 5;
	private float coreRadius = 2;
	private float branchRadius = 3;
	private float maxHeight = 128;
	private int minDepth = 16;

	public WorldGenAncientTree(IBlockState block, IBlockState outer, IBlockState leaf, int cellSize,
							   float chance, float holeRadius, float coreRadius, float treeRadius, float height) {
		this.blockState = block;
		this.outerState = outer;
		this.leafState = leaf;
		this.cellSize = cellSize;
		this.chance = chance;
		this.coreRadius = coreRadius;
		this.treeRadius = treeRadius;
		this.holeRadius = holeRadius;
		maxHeight = height;

		if (this.holeRadius * 4 + 2 > this.cellSize) {
			this.cellSize = MathHelper.ceil(this.holeRadius * 4 + 2);
		}
	}

	private static double calculateAngle(float angle, double distanceFromCenter, double branchRadius) {
		final double adjust = (angle % 2 == 0) ? 2 : -2;
		return (angle + distanceFromCenter * adjust) * Math.PI / 180d;
	}

	private static double distanceToBranchSpline(float branchAngle, double deltaX, double deltaZ, int testHeight,
												 double distanceFromCenter, double branchRadius, float branchHeight)
	{
		final double angle = calculateAngle(branchAngle, distanceFromCenter, branchRadius);
		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double rotX = deltaX * cos + deltaZ * sin;
		final double rotZ = -deltaX * sin + deltaZ * cos;
		final double deltaY = testHeight - branchHeight;
		final double minimalRotation = Math.min(Math.abs(rotX), Math.abs(rotZ));
		return Math.sqrt(minimalRotation * minimalRotation + deltaY * deltaY);
	}

	@Override
	public boolean generate(World world, Random rand, BlockPos cornerPos) 
	{
		// We generate the 8-24 area offset from pos. So +16 is our centerpoint
		final int xCenter = cornerPos.getX() + 8;
		final int zCenter = cornerPos.getZ() + 8;
		
		final int xCell = ModuloHelper.divide(xCenter, cellSize);
		final int zCell = ModuloHelper.divide(zCenter, cellSize);
		
		Random cellRNG = new Random();
		cellRNG.setSeed(world.getSeed());
		long seedA = cellRNG.nextLong() / 2L * 2L + 1L;
		long seedB = cellRNG.nextLong() / 2L * 2L + 1L;
		cellRNG.setSeed((long)xCell * seedA + (long)zCell * seedB ^ world.getSeed());
		
		if(cellRNG.nextFloat() < chance) // Only place in some cells, keep it rare
		{
			int depositX = xCell * cellSize + 8 + cellRNG.nextInt(cellSize - 4 * (int) holeRadius) + (int) holeRadius * 2;
			int depositZ = zCell * cellSize + 8 + cellRNG.nextInt(cellSize - 4 * (int) holeRadius) + (int) holeRadius * 2;
			
			if(BiomeDictionary.hasType(world.getBiome(new BlockPos(depositX, 0, depositZ)), BiomeDictionary.Type.FOREST))
			{
				
				final int NUM_BRANCHES = 12;
				float[] branchHeights = new float[NUM_BRANCHES];
				float[] branchAngles = new float[NUM_BRANCHES];
				for(int branch = 0; branch < NUM_BRANCHES; branch++)
				{
					branchHeights[branch] = minDepth + (cellRNG.nextFloat() + branch) * (float)(maxHeight - minDepth) / (float)NUM_BRANCHES;
					if(Math.abs(branchHeights[branch] - 64) < 8)
						branchHeights[branch] += 12;
					if(Math.abs(branchHeights[branch] - 56) < 8)
						branchHeights[branch] -= 12;
					branchAngles[branch] = cellRNG.nextFloat() * 360f;
				}
				
				for(int x = 8; x < 24; x++)
				{
					for(int z = 8; z < 24; z++)
					{
						BlockPos p = new BlockPos(cornerPos.getX() + x, 0, cornerPos.getZ() + z);
						
						int deltaX = p.getX() - depositX;
						int deltaZ = p.getZ() - depositZ;
						
						double distanceFromCenter = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ );
						
						// Dig a hole
						if(distanceFromCenter < holeRadius)
						{
							double edgeToTreeBlend = (distanceFromCenter - treeRadius) / (holeRadius - treeRadius);
							edgeToTreeBlend = MathHelper.clamp(edgeToTreeBlend, 0, 1);
							int minHeight = MathHelper.floor(minDepth + (64 - minDepth) * edgeToTreeBlend);
							minHeight += rand.nextInt(3);
							
							for(int y = minHeight; y < maxHeight; y++)
							{
								BlockPos pos = p.add(0, y, 0);
								
								double heightParam = (double)(y - minHeight) / (double)(maxHeight - minHeight);
								double trunkRadius = coreRadius + (1.0d - heightParam) * (treeRadius - coreRadius);
								double coreRadius = this.coreRadius * (1.0d - heightParam);
								
								if(distanceFromCenter < coreRadius)
								{
									world.setBlockState(pos, blockState);
								}
								else if(distanceFromCenter < trunkRadius)
								{
									world.setBlockState(pos, outerState);
								}
								else 
								{
									double branchExtents = 
											y > 64 ? holeRadius * (0.5d + 0.5d * ((float)(y - minDepth) / (float)(maxHeight - minDepth)))
											: holeRadius;
									boolean isBranch = false;
									
									if(distanceFromCenter < branchExtents)
									{
										// Test to see if this is part of a branch
										for(int branch = 0; branch < NUM_BRANCHES; branch++)
										{
											int testHeight = y < 64 ? MathHelper.ceil(y + distanceFromCenter * 0.5d) : MathHelper.ceil(y - distanceFromCenter * 0.5d);
											// Quick test to see if we are at the right height
											if(branchHeights[branch] - branchRadius <= testHeight && testHeight <= branchHeights[branch] + branchRadius)
											{
												final double branchSplineDistance =
														distanceToBranchSpline(branchAngles[branch], deltaX, deltaZ,
																testHeight, distanceFromCenter, branchRadius,
																branchHeights[branch]);
												if(branchSplineDistance < branchRadius * ((1.0d - heightParam) * (1.0d - distanceFromCenter / holeRadius) + 0.5d))
												{
													world.setBlockState(pos, outerState);
													isBranch = true;
												}
											}
											// Or leaf height
											if(y > 64 && distanceFromCenter > treeRadius)
											{
												if(branchHeights[branch] - branchRadius + 3 <= testHeight && testHeight <= branchHeights[branch] + branchRadius + 3)
												{
													final double branchSplineDistance =
															distanceToBranchSpline(branchAngles[branch], deltaX, deltaZ,
																	testHeight, distanceFromCenter, branchRadius,
																	branchHeights[branch]);

													if(branchSplineDistance < branchRadius * 2d * ((1.0d - heightParam) * (1.0d - Math.min(0.5d * holeRadius, distanceFromCenter) / holeRadius) + 0.5d))
													{
														world.setBlockState(pos, leafState);
														isBranch = true;
													}
												}
											}
										}
									}
									
									if(!isBranch)
									{
										world.setBlockToAir(pos);
									}
								}
							}
							
						
						}
					}
				}
			}
		}
		
		return false;
	}

}
