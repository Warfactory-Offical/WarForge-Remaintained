package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;

public class SiegeCampProgressInfo
{
	public DimBlockPos defendingPos;
	public DimBlockPos attackingPos;
	public int attackingColour;
	public int defendingColour;
	public String attackingName;
	public String defendingName;
	
	public int completionPoint = 5;
	public int mPreviousProgress = 0;
	public int progress = 0;
	
	public int expiredTicks = 0;
	
	
	public void ClientTick()
	{
		if(progress <= -5 || progress >= completionPoint)
		{
			expiredTicks++;
		}
	}
	
	public boolean Completed()
	{
		return expiredTicks >= 100;
	}
}
