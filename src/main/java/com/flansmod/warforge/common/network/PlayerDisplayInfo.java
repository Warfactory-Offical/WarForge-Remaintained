package com.flansmod.warforge.common.network;

import com.flansmod.warforge.server.Faction;

import java.util.UUID;

public class PlayerDisplayInfo
{
	public String username = "";
	public UUID playerUuid = Faction.nullUuid;
	public Faction.Role role = Faction.Role.MEMBER;
}