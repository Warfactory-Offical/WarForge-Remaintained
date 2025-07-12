package com.flansmod.warforge.common.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

public class DimChunkPos extends ChunkPos
{
	public int dim;
	
	public DimChunkPos(int dim, int x, int z) 
	{
		super(x, z);
		this.dim = dim;
	}
	
	public DimChunkPos(int dim, BlockPos pos)
	{
		super(pos);
		this.dim = dim;
	}

	public boolean isSameDim(DimChunkPos other)
	{
		return other.dim == dim;
	}
	
	@Override//Magic numbers ffs
	public int hashCode()
    {
		return super.hashCode() ^ (155225 * this.dim + 140501023);
    }

	@Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if (!(other instanceof DimChunkPos))
            return false;

        DimChunkPos dcpos = (DimChunkPos)other;
        return this.dim == dcpos.dim && this.x == dcpos.x && this.z == dcpos.z;
    }
    
	@Override
    public String toString()
    {
        return "[" + this.dim + ": " + this.x + ", " + this.z + "]";
    }
	
	public DimChunkPos Offset(EnumFacing facing, int n)
	{
	    return new DimChunkPos(dim, x + facing.getXOffset() * n, z + facing.getZOffset() * n);
	}

	public DimChunkPos Offset(Vec3i offset)
	{
		return new DimChunkPos(dim, x +  offset.getX(), z + offset.getZ());
	}

	public DimChunkPos north() { return Offset(EnumFacing.NORTH, 1); }
	public DimChunkPos south() { return Offset(EnumFacing.SOUTH, 1); }
	public DimChunkPos east() { return Offset(EnumFacing.EAST, 1); }
	public DimChunkPos west() { return Offset(EnumFacing.WEST, 1); }
}
