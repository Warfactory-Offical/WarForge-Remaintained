package com.flansmod.warforge.common.network;

import net.minecraft.util.math.Vec3i;

import java.util.UUID;

public class SiegeCampAttackInfo 
{
	public boolean canAttack;
	public Vec3i mOffset; //more flexible than DirectionFacing, Y value is ignored
	public UUID mFactionUUID;
	public String mFactionName;
	public int mFactionColour;
}
