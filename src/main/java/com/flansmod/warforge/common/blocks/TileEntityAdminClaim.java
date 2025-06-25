package com.flansmod.warforge.common.blocks;

public class TileEntityAdminClaim extends TileEntityClaim
{
	// IClaim
	@Override 
	public boolean canBeSieged() { return false; }
	// ------------
	@Override
	public int getAttackStrength() { return 0; }
	@Override
	public int getDefenceStrength() { return 0; }
	@Override
	public int getSupportStrength() { return 0; }
}
