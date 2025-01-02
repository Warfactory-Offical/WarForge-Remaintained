package com.flansmod.warforge.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.DimChunkPos;
import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.LeaderboardInfo;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PacketLeaderboardInfo;
import com.flansmod.warforge.server.Faction.PlayerData;
import com.flansmod.warforge.server.Leaderboard.FactionStat;
import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class CommandFactions extends CommandBase
{
	private static final List<String> ALIASES;
	static
	{
		ALIASES = new ArrayList<String>(4);
		ALIASES.add("f");
		ALIASES.add("factions");
		ALIASES.add("war");
		ALIASES.add("warforge");
	}
	
	@Override
	public String getName() { return "faction"; }
	@Override
    public List<String> getAliases() {  return ALIASES;  }

	@Override
	public String getUsage(ICommandSender sender) 
	{
		return null;
	}
	
	@Override 
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }
	
	private static final String[] tabCompletions = new String[] { 
			"invite", "accept", "disband", "expel", "leave", "time", "info", "top", "notoriety", "wealth", "legacy",
			"promote", "demote", "msg", "flag", "setleader",
	};
	
	private static final String[] tabCompletionsOp = new String[] { 
			"invite", "accept", "disband", "expel", "leave", "time", "info", "top", "notoriety", "wealth", "legacy",
			"promote", "demote", "msg", "flag", "setleader",
			"safe", "war", "protection", "resetflagcooldowns",
	};
	
	@Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if (args.length == 1)
        {
        	return getListOfStringsMatchingLastWord(args, tabCompletions);
        }
        
        if(args.length == 2)
        {
            return switch (args[0]) {
                case "info" -> getListOfStringsMatchingLastWord(args, WarForgeMod.FACTIONS.GetFactionNames());
                case "invite", "expel", "demote", "promote", "setleader" ->
                        getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
                default -> getListOfStringsMatchingLastWord(args, new String[0]);
            };
        }
        
        return getListOfStringsMatchingLastWord(args, new String[0]);
    }

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException 
	{
		if(args.length == 0)
		{
			sender.sendMessage(new TextComponentString("Try /f help"));
			return;
		}
		
		Faction faction = null;
		if(sender instanceof EntityPlayer)
		{
			faction = WarForgeMod.FACTIONS.getFactionOfPlayer(((EntityPlayer)sender).getUniqueID());
		}
		
		// Argument 0 is subcommand
		switch(args[0].toLowerCase())
		{
			case "help":
			{
				sender.sendMessage(new TextComponentString("/f invite <playerName>"));
				sender.sendMessage(new TextComponentString("/f accept"));
				sender.sendMessage(new TextComponentString("/f disband"));
				sender.sendMessage(new TextComponentString("/f expel <playerName>"));
				sender.sendMessage(new TextComponentString("/f leave"));
				sender.sendMessage(new TextComponentString("/f time"));
				sender.sendMessage(new TextComponentString("/f info <factionName>"));
				sender.sendMessage(new TextComponentString("/f top"));
				sender.sendMessage(new TextComponentString("/f wealth"));
				sender.sendMessage(new TextComponentString("/f legacy"));
				sender.sendMessage(new TextComponentString("/f notoriety"));
				
				if(WarForgeMod.isOp(sender))
				{
					sender.sendMessage(new TextComponentString("/f safezone"));
					sender.sendMessage(new TextComponentString("/f warzone"));
				}
				
				break;
			}
		
			case "create":
			{
				sender.sendMessage(new TextComponentString("Craft a Citadel to create a faction"));
				break;
			}
			case "invite":
			{
				// Argument 1 is the player to invite
				if(args.length < 2)
				{
					sender.sendMessage(new TextComponentString("Invalid arguments, please specify player name"));
					break;
				}
				
				EntityPlayer invitee = server.getPlayerList().getPlayerByUsername(args[1]);
				if(invitee == null)
				{
					sender.sendMessage(new TextComponentString("Could not find player " + args[1]));
					break;
				}
				
				// First, resolve the op version where we can specify the faction
				if(args.length >= 3 && WarForgeMod.isOp(sender))
				{
					faction = WarForgeMod.FACTIONS.getFaction(args[2]);
					if(faction != null)
						WarForgeMod.FACTIONS.RequestInvitePlayerToFaction(sender, faction.uuid, invitee.getUniqueID());
					else 
						sender.sendMessage(new TextComponentString("Could not find faction " + args[2]));
					
					break;
				}
				
				// Any other case, we assume players can only invite to their own faction
				if(sender instanceof EntityPlayer)
				{
					WarForgeMod.FACTIONS.RequestInvitePlayerToMyFaction((EntityPlayer)sender, invitee.getUniqueID());
				}	

				break;
			}
			case "accept":
			{
				if(sender instanceof EntityPlayer)
				{
					WarForgeMod.FACTIONS.RequestAcceptInvite((EntityPlayer)sender);
				}
				else
				{
					sender.sendMessage(new TextComponentString("The server can't accept a faction invite"));
				}
				break;
			}
			case "disband":
			{
				if(WarForgeMod.isOp(sender) && args.length == 2)
				{
					Faction toDisband = WarForgeMod.FACTIONS.getFaction(args[1]);
					if(toDisband != null)
					{
						WarForgeMod.FACTIONS.FactionDefeated(toDisband);
					}
					else
					{
						sender.sendMessage(new TextComponentString("Could not find that faction"));
					}
				}
				else
				{
					if(sender instanceof EntityPlayer && faction != null)
					{
						WarForgeMod.FACTIONS.RequestDisbandFaction((EntityPlayer)sender, faction.uuid);
					}
					else
					{
						sender.sendMessage(new TextComponentString("You aren't in a faction"));
					}
				}
				break;
			}
			case "expel":
			case "remove":
			{
				if(args.length >= 2)
				{
					EntityPlayer toRemove = server.getPlayerList().getPlayerByUsername(args[1]);
					if(toRemove != null)
					{
						UUID toRemoveID =  toRemove.getUniqueID();
						
						if(faction == null)
						{
							faction = WarForgeMod.FACTIONS.getFactionOfPlayer(toRemoveID);
						}
						
						WarForgeMod.FACTIONS.RequestRemovePlayerFromFaction(sender, faction.uuid, toRemoveID);
					}
					else
					{
						sender.sendMessage(new TextComponentString("Could not find player " + args[1]));
					}
				}
				else
				{
					sender.sendMessage(new TextComponentString("Correct usage is /f " + args[0] + " <username>"));
				}
				break;
			}
			case "leave":
			case "exit":
			{
				if(sender instanceof EntityPlayer)
				{
					WarForgeMod.FACTIONS.RequestRemovePlayerFromFaction(sender, faction.uuid, ((EntityPlayer) sender).getUniqueID());
				}
				else
				{
					sender.sendMessage(new TextComponentString("This command is only for players"));
				}
				break;
			}
			case "setleader":
			{
				if(sender instanceof EntityPlayer)
				{
					if(args.length < 2)
					{
						sender.sendMessage(new TextComponentString("Please specify the new leader /f setleader <name>"));
					}
					else
					{
						GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getGameProfileForUsername(args[1]);
						if(profile == null)
						{
							sender.sendMessage(new TextComponentString("Could not find player " + args[1]));
						}
						else
						{
							Faction targetFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(profile.getId());
							if(targetFaction == null)
							{
								sender.sendMessage(new TextComponentString("That player is not in a faction"));
							}
							else
							{
								WarForgeMod.FACTIONS.RequestTransferLeadership((EntityPlayer)sender, targetFaction.uuid, profile.getId());
							}
						}
					}
				}
				else
				{
					sender.sendMessage(new TextComponentString("This command is only for players"));
				}
				break;
			}
			case "time":
			{

				long day = WarForgeMod.numberOfSiegeDaysTicked;
				long flagCoolown = WarForgeMod.FACTIONS.getPlayerCooldown(sender.getCommandSenderEntity().getUniqueID());
				long ms = WarForgeMod.INSTANCE.getTimeToNextYieldMs();
				long s = ms / 1000;
				long m = s / 60;
				long h = m / 60;
				long d = h / 24;
				
				sender.sendMessage(new TextComponentString("Yields will next be awarded in "
				+ (d) + " days, "
				+ String.format("%02d", (h % 24))  + ":"
				+ String.format("%02d", (m % 60)) + ":"
				+ String.format("%02d", (s % 60))));
				
				ms = WarForgeMod.INSTANCE.getTimeToNextSiegeAdvanceMs();
				s = ms / 1000;
				m = s / 60;
				h = m / 60;
				d = h / 24;
				
				sender.sendMessage(new TextComponentString("Sieges will progress in "
				+ (d) + " days, "
				+ String.format("%02d", (h % 24)) + ":"
				+ String.format("%02d", (m % 60)) + ":"
				+ String.format("%02d", (s % 60))));

				ms = flagCoolown;
				s = ms / 1000;
				m = s / 60;
				h = m / 60;
				d = h / 24;

				sender.sendMessage(new TextComponentString("You can move flag in "
						+ String.format("%02d", (h % 24)) + ":"
						+ String.format("%02d", (m % 60)) + ":"
						+ String.format("%02d", (s % 60))));
				break;
			}
			
			// Faction info
			case "info":
			{
				if(sender instanceof EntityPlayerMP)
				{
					Faction factionToSend = null;
					if(args.length >= 2)
					{
						factionToSend = WarForgeMod.FACTIONS.getFaction(args[1]);
					}
					if(factionToSend == null)
					{
						factionToSend = WarForgeMod.FACTIONS.getFactionOfPlayer(((EntityPlayerMP)sender).getUniqueID());
					}

					if(factionToSend == null)
					{
						sender.sendMessage(new TextComponentString("Could not find that faction"));
					}
					else {
						Faction senderFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(((EntityPlayerMP) sender).getUniqueID());
						if (senderFaction == null || senderFaction.uuid.equals(Faction.nullUuid) || !senderFaction.equals(factionToSend)) {
							sender.sendMessage(new TextComponentString("Information cannot be provided to non-faction members"));
						} else {
							PacketFactionInfo packet = new PacketFactionInfo();
							packet.info = factionToSend.createInfo();
							WarForgeMod.NETWORK.sendTo(packet, (EntityPlayerMP)sender);
						}
					}
				}
				else
				{
					Faction factionToSend = WarForgeMod.FACTIONS.getFaction(args[1]);
					if(factionToSend != null)
					{
						StringBuilder memberList = new StringBuilder("Members: ");
						for(HashMap.Entry<UUID, PlayerData> kvp : factionToSend.members.entrySet())
						{
							GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getProfileByUUID(kvp.getKey());
							if(profile != null)
							{
								memberList.append(profile.getName()).append(", ");
							}
						}
						
						sender.sendMessage(new TextComponentString("**" + factionToSend.name + "**\n"
																	+ memberList + "\n"
																	+ "Notoriety: " + factionToSend.notoriety + "\n"
																	+ "Wealth: " + factionToSend.wealth + "\n"
																	+ "Legacy: " + factionToSend.legacy + "\n"));
					}
				}
				break;
			}
			
			// Leaderboards
			case "top":
			{
				if(sender instanceof EntityPlayerMP)
				{
					UUID uuid = ((EntityPlayerMP)sender).getUniqueID();
					PacketLeaderboardInfo packet = new PacketLeaderboardInfo();
					packet.info = WarForgeMod.LEADERBOARD.CreateInfo(0, FactionStat.TOTAL, uuid);
					WarForgeMod.NETWORK.sendTo(packet, (EntityPlayerMP)sender);
				}
				else
				{
					LeaderboardInfo info = WarForgeMod.LEADERBOARD.CreateInfo(0, FactionStat.TOTAL, Faction.nullUuid);
					StringBuilder result = new StringBuilder("**Top Leaderboard**");
					for(int i = 0; i < LeaderboardInfo.NUM_LEADERBOARD_ENTRIES_PER_PAGE; i++)
					{
						FactionDisplayInfo facInfo = info.factionInfos[i];
						result.append("\n#").append(facInfo.totalRank).append(" | ").append(facInfo.legacy + facInfo.notoriety + facInfo.wealth);
					}
					sender.sendMessage(new TextComponentString(result.toString()));
				}
				break;
			}
			case "wealth":
			case "wealthtop":
			case "bal":
			case "baltop":
			{
				if(sender instanceof EntityPlayerMP)
				{
					UUID uuid = ((EntityPlayerMP)sender).getUniqueID();
					PacketLeaderboardInfo packet = new PacketLeaderboardInfo();
					packet.info = WarForgeMod.LEADERBOARD.CreateInfo(0, FactionStat.WEALTH, uuid);
					WarForgeMod.NETWORK.sendTo(packet, (EntityPlayerMP)sender);
				}
				else
				{
					LeaderboardInfo info = WarForgeMod.LEADERBOARD.CreateInfo(0, FactionStat.WEALTH, Faction.nullUuid);
					String result = "**Top Leaderboard**";
					for(int i = 0; i < LeaderboardInfo.NUM_LEADERBOARD_ENTRIES_PER_PAGE; i++)
					{
						FactionDisplayInfo facInfo = info.factionInfos[i];
						result = result + "\n#" + facInfo.wealthRank + " | " + facInfo.wealth;
					}
					sender.sendMessage(new TextComponentString(result));
				}
				break;
			}
			case "notoriety":
			case "notorietytop":
			case "pvp":
			case "pvptop":
			{
				if(sender instanceof EntityPlayerMP)
				{
					UUID uuid = ((EntityPlayerMP)sender).getUniqueID();
					PacketLeaderboardInfo packet = new PacketLeaderboardInfo();
					packet.info = WarForgeMod.LEADERBOARD.CreateInfo(0, FactionStat.NOTORIETY, uuid);
					WarForgeMod.NETWORK.sendTo(packet, (EntityPlayerMP)sender);
				}
				else
				{
					LeaderboardInfo info = WarForgeMod.LEADERBOARD.CreateInfo(0, FactionStat.NOTORIETY, Faction.nullUuid);
					String result = "**Notoriety Leaderboard**";
					for(int i = 0; i < LeaderboardInfo.NUM_LEADERBOARD_ENTRIES_PER_PAGE; i++)
					{
						FactionDisplayInfo facInfo = info.factionInfos[i];
						result = result + "\n#" + facInfo.notorietyRank + " | " + facInfo.notoriety;
					}
					sender.sendMessage(new TextComponentString(result));
				}
				break;
			}
			case "legacy":
			case "legacytop":
			case "playtime":
			case "playtimetop":
			{
				if(sender instanceof EntityPlayerMP)
				{
					UUID uuid = ((EntityPlayerMP)sender).getUniqueID();
					PacketLeaderboardInfo packet = new PacketLeaderboardInfo();
					packet.info = WarForgeMod.LEADERBOARD.CreateInfo(0, FactionStat.LEGACY, uuid);
					WarForgeMod.NETWORK.sendTo(packet, (EntityPlayerMP)sender);
				}
				else
				{
					LeaderboardInfo info = WarForgeMod.LEADERBOARD.CreateInfo(0, FactionStat.LEGACY, Faction.nullUuid);
					String result = "**Legacy Leaderboard**";
					for(int i = 0; i < LeaderboardInfo.NUM_LEADERBOARD_ENTRIES_PER_PAGE; i++)
					{
						FactionDisplayInfo facInfo = info.factionInfos[i];
						result = result + "\n#" + facInfo.legacyRank + " | " + facInfo.legacy;
					}
					sender.sendMessage(new TextComponentString(result));
				}
				break;
			}
			case "safe":
			case "safezone":
			case "claimsafe":
			{
				if(WarForgeMod.isOp(sender))
				{
					if(sender instanceof EntityPlayer)
					{
						EntityPlayer player = (EntityPlayer)sender;
						DimChunkPos pos = new DimBlockPos(player.dimension, player.getPosition()).toChunkPos();
						WarForgeMod.FACTIONS.RequestOpClaim(player, pos, FactionStorage.SAFE_ZONE_ID);
					}
					else
					{
						sender.sendMessage(new TextComponentString("Use an in-game operator account."));
					}
				}
				else
				{
					sender.sendMessage(new TextComponentString("You are not op."));
				}
				break;
			}
			case "warzone":
			case "war":
			case "claimwarzone":
			{
				if(WarForgeMod.isOp(sender))
				{
					if(sender instanceof EntityPlayer)
					{
						EntityPlayer player = (EntityPlayer)sender;
						DimChunkPos pos = new DimBlockPos(player.dimension, player.getPosition()).toChunkPos();
						WarForgeMod.FACTIONS.RequestOpClaim(player, pos, FactionStorage.WAR_ZONE_ID);
					}
					else
					{
						sender.sendMessage(new TextComponentString("Use an in-game operator account."));
					}
				}
				else
				{
					sender.sendMessage(new TextComponentString("You are not op."));
				}
				break;
			}
			case "opProtection":
			case "protection":
			case "protectionOverride":
			{
				if(WarForgeMod.isOp(sender))
				{
					ProtectionsModule.OP_OVERRIDE = !ProtectionsModule.OP_OVERRIDE;
					if(ProtectionsModule.OP_OVERRIDE)
						sender.sendMessage(new TextComponentString("Admins can now build in protected areas."));
					else
						sender.sendMessage(new TextComponentString("Admins can no longer build in protected areas."));
				}
				else
				{
					sender.sendMessage(new TextComponentString("You are not op."));
				}
				break;
			}
			case "home":
			{
				if(sender instanceof EntityPlayer)
					WarForgeMod.TELEPORTS.RequestFHome((EntityPlayer)sender);
				else
					sender.sendMessage(new TextComponentString("Only valid for players"));
				break;
			}
			case "spawn":
			{
				if(sender instanceof EntityPlayer)
					WarForgeMod.TELEPORTS.requestSpawn((EntityPlayer)sender);
				else
					sender.sendMessage(new TextComponentString("Only valid for players"));
				break;
			}
			case "promote":
			{
				if(sender instanceof EntityPlayer)
				{
					if(args.length == 2)
					{
						EntityPlayerMP target = WarForgeMod.MC_SERVER.getPlayerList().getPlayerByUsername(args[1]);
						if(target == null)
						{
							sender.sendMessage(new TextComponentString("Could not find " + args[1]));
						}
						else
						{
							WarForgeMod.FACTIONS.requestPromote((EntityPlayer)sender, target);
						}
					}
					else
					{
						sender.sendMessage(new TextComponentString("Please specify the player you want to promote"));
					}
				}
				else
					sender.sendMessage(new TextComponentString("Only valid for players"));
				
				break;
			}
			case "demote":
			{
				if(sender instanceof EntityPlayer)
				{
					if(args.length == 2)
					{
						EntityPlayerMP target = WarForgeMod.MC_SERVER.getPlayerList().getPlayerByUsername(args[1]);
						if(target == null)
						{
							sender.sendMessage(new TextComponentString("Could not find " + args[1]));
						}
						else
						{
							WarForgeMod.FACTIONS.requestDemote((EntityPlayer)sender, target);
						}
					}
					else
					{
						sender.sendMessage(new TextComponentString("Please specify the player you want to promote"));
					}
				}
				else
					sender.sendMessage(new TextComponentString("Only valid for players"));
				
				break;
			}
			case "clearnotoriety":
			{
				if(WarForgeMod.isOp(sender))
				{
					WarForgeMod.FACTIONS.clearNotoriety();
				}
				break;
			}
			case "clearlegacy":
			{
				if(WarForgeMod.isOp(sender))
				{
					WarForgeMod.FACTIONS.clearLegacy();
				}
				break;
			}
			case "chat":
			case "msg":
			{
				if(sender instanceof EntityPlayer)
				{
					if(faction != null)
					{
						StringBuilder msg = new StringBuilder("§a[" + sender.getName() + " > Faction]§f");
						for(int i = 1; i < args.length; i++)
							msg.append(" ").append(args[i]);
						faction.messageAll(new TextComponentString(msg.toString()));
					}
				}
				break;
			}
			case "placeflag":
			case "flag":
			{
				if(sender instanceof EntityPlayer)
				{
					if(faction != null)
					{
						DimBlockPos pos = faction.getSpecificPosForClaim(new DimChunkPos(((EntityPlayer) sender).dimension, sender.getPosition()));
						if(pos != null)
						{
							faction.placeFlag((EntityPlayer)sender, pos);
						}
						else
							sender.sendMessage(new TextComponentString("Could not find a claim of your faction in this chunk"));
					}
					else 
						sender.sendMessage(new TextComponentString("You aren't in a faction"));
				}
				else 
					sender.sendMessage(new TextComponentString("This command is only for players"));
				break;
			}
			case "resetflagcooldowns":
			{
				if(WarForgeMod.isOp(sender))
				{
					WarForgeMod.FACTIONS.opResetFlagCooldowns();
				}
				break;
			}
			case "tpa":
			case "tpaccept":
			case "tp":
			case "tprequest":
			{
				sender.sendMessage(new TextComponentString("Try brewing a potion of Teleportation / Telereception"));
				break;
			}
			
			
			
			default:
			{
				break;
			}
		}
	
	}

}
