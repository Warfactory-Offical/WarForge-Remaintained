package com.flansmod.warforge.common.network;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.util.math.Vec3i;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public class SiegeCampAttackInfo
{
	public boolean canAttack;
	public Vec3i mOffset; //more flexible than DirectionFacing, Y value is ignored
	public UUID mFactionUUID;
	public String mFactionName;
	public int mFactionColour;
}
