package com.flansmod.warforge.common.network;

import java.util.UUID;

import com.flansmod.warforge.server.Faction;

public class PlayerDisplayInfo
{
	public String mPlayerName = "";
	public UUID playerUuid = Faction.NULL;
	public Faction.Role role = Faction.Role.MEMBER;
}