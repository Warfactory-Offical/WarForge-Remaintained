package com.flansmod.warforge.common.network;

import java.util.ArrayList;
import java.util.UUID;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.server.Faction;

// What gets sent over network to display faction information on client
public class FactionDisplayInfo 
{	
	public UUID factionId = Faction.nullUuid;
	public String factionName = "";
	public UUID mLeaderID = Faction.nullUuid;
	public ArrayList<PlayerDisplayInfo> members = new ArrayList<PlayerDisplayInfo>();
	
	
	public int notoriety = 0;
	public int wealth = 0;
	public int legacy = 0;
	
	public int notorietyRank = 0;
	public int wealthRank = 0;
	public int legacyRank = 0;
	public int totalRank = 0;
	
	
	public int mNumClaims = 0;
	public int mNumActiveSiegeCamps = 0;
	public int mNumActiveLeeches = 0;
	public DimBlockPos mCitadelPos = DimBlockPos.ZERO;	
	
	public PlayerDisplayInfo getPlayerInfo(UUID playerID)
	{
		for(PlayerDisplayInfo info : members)
		{
			if(info.playerUuid.equals(playerID))
				return info;
		}
		return null;
	}
}
