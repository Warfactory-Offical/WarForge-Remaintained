package com.flansmod.warforge.common.network;

import java.util.UUID;

import com.flansmod.warforge.server.Faction;

public class PlayerDisplayInfo
{
	public String username = "";
	public UUID playerUuid = Faction.nullUuid;
	public Faction.Role role = Faction.Role.MEMBER;
}