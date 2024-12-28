package com.flansmod.warforge.common;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class DimBlockPos extends BlockPos
{
	public static final DimBlockPos ZERO = new DimBlockPos(0, 0, 0, 0);
	
	public int dim;
	
	public DimBlockPos(int dim, int x, int y, int z)
    {
        super(x, y, z);
        this.dim = dim;
    }

    public DimBlockPos(int dim, double x, double y, double z)
    {
        super(x, y, z);
        this.dim = dim;
    }

    public DimBlockPos(Entity source)
    {
        super(source);
        dim = source.dimension;
    }
    
    public DimBlockPos(TileEntity source)
    {
        super(source.getPos().getX(), source.getPos().getY(), source.getPos().getZ());
        dim = source.getWorld().provider.getDimension();
    }

    public DimBlockPos(int dim, Vec3d vec)
    {
        super(vec);
        this.dim = dim;
    }

    public DimBlockPos(int dim, Vec3i source)
    {
    	super(source);
    	this.dim = dim;
    }
    
    public DimChunkPos toChunkPos()
    {
    	return new DimChunkPos(dim, getX() >> 4, getZ() >> 4);
    }
    
    public BlockPos toRegularPos()
    {
    	return new BlockPos(getX(), getY(), getZ());
    }
    
    @Override
    public BlockPos offset(EnumFacing facing, int n)
    {
        return n == 0 ? this : new DimBlockPos(this.dim, this.getX() + facing.getXOffset() * n, this.getY() + facing.getYOffset() * n, this.getZ() + facing.getZOffset() * n);
    }

	// HASHING INTO A MAP DEPENDENT ON BLOCKPOS (VANILLA METHODS) WILL RETURN NULL DUE TO THIS CUSTOM IMPL HAVING A DIFFERENT VALUE
	// (dimBlockPos -> func(BlockPos) -> hashMap<BlockPos>.get(blockPos.hashCode() [dimBlockPos.hashCode != blockPos.hashCode] -> diff value -> null
	@Override
	public int hashCode()
    {
		return super.hashCode() ^ (155225 * this.dim + 140501023);
    }

	@Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if (!(other instanceof DimBlockPos dcpos))
            return false;

        return this.dim == dcpos.dim
        		&& this.getX() == dcpos.getX()
        		&& this.getY() == dcpos.getY()
        		&& this.getZ() == dcpos.getZ();
    }
    
	@Override
    public String toString()
    {
        return "[" + this.dim + ": " + this.getX() + ", " + this.getY() + ", " + this.getZ() + "]";
    }
	
	public String toFancyString()
	{
		return "[" + getX() + ", " + getY() + ", " + getZ() + "] in " + getDimensionName();
	}
	
	public String getDimensionName()
	{
        return switch (dim) {
            case -1 -> "The Nether";
            case 0 -> "The Overworld";
            case 1 -> "The End";
            default -> "Dimension #" + dim;
        };
	}
	
	public NBTTagIntArray writeToNBT()
	{
		return new NBTTagIntArray(new int[] {dim, getX(), getY(), getZ()});
	}
	
	public void writeToNBT(NBTTagCompound tags, String prefix)
	{
		tags.setIntArray(prefix, new int[] {dim, getX(), getY(), getZ() });
	}
	
	public static DimBlockPos readFromNBT(NBTTagCompound tags, String prefix)
	{
		int[] data = tags.getIntArray(prefix);
		if(data.length == 4)
			return new DimBlockPos(data[0], data[1], data[2], data[3]);
		else
			return DimBlockPos.ZERO;
	}
	
	public static DimBlockPos readFromNBT(NBTTagIntArray tag)
	{
		if(tag != null)
		{
			int[] data = tag.getIntArray();
			if(data.length == 4)
				return new DimBlockPos(data[0], data[1], data[2], data[3]);
		}
		return DimBlockPos.ZERO;
	}
}
