package com.flansmod.warforge.common.blocks;

import java.util.List;
import java.util.UUID;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.server.Faction;

import net.minecraft.tileentity.TileEntity;

public interface IClaim 
{
	public DimBlockPos getClaimPos();
	public TileEntity getAsTileEntity();
	
	public boolean canBeSieged();
	public int getAttackStrength();
	public int getDefenceStrength();
	public int getSupportStrength();
	public List<String> getPlayerFlags();
	
	public void onServerSetFaction(Faction faction);
	public void onServerSetPlayerFlag(String playerName);
	public void onServerRemovePlayerFlag(String playerName);
	
	// Server side uuid - means nothing to a client
	public UUID getFaction();
	public void updateColour(int colour);
	
	// Client side data - can't use UUID to identify anything on client
	public int getColour();
	public String getClaimDisplayName();
	
	
}
