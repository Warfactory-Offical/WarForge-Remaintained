package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.DimBlockPos;

public class SiegeCampProgressInfo
{
	public DimBlockPos defendingPos;
	public DimBlockPos attackingPos;
	public int mAttackingColour;
	public int mDefendingColour;
	public String attackingName;
	public String defendingName;
	
	public int mCompletionPoint = 5;
	public int mPreviousProgress = 0;
	public int mProgress = 0;
	
	public int expiredTicks = 0;
	
	
	public void ClientTick()
	{
		if(mProgress <= -5 || mProgress >= mCompletionPoint)
		{
			expiredTicks++;
		}
	}
	
	public boolean Completed()
	{
		return expiredTicks >= 100;
	}
}
