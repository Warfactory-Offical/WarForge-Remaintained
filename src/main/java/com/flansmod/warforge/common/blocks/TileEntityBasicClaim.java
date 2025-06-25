package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.WarForgeConfig;

public class TileEntityBasicClaim extends TileEntityYieldCollector implements IClaim
{
	public static final int NUM_SLOTS = NUM_BASE_SLOTS; // No additional slots here


	public TileEntityBasicClaim()
	{
		
	}
	
	@Override
	public int getDefenceStrength() { return WarForgeConfig.CLAIM_STRENGTH_BASIC; }
	@Override
	public int getSupportStrength() { return WarForgeConfig.SUPPORT_STRENGTH_BASIC; }
	@Override
	public int getAttackStrength() { return 0; }
	@Override
	protected float getYieldMultiplier() { return 1.0f; }

}
