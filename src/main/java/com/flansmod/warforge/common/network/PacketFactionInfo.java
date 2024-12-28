package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.CommonProxy;
import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction.Role;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketFactionInfo extends PacketBase 
{
	// Cheeky hack to make it available to the GUI
	public static FactionDisplayInfo latestInfo = null;
	
	public FactionDisplayInfo info;
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		if(info != null)
		{
			data.writeBoolean(true);
			writeUUID(data, info.factionId);
			writeUTF(data, info.mFactionName);
			
			data.writeInt(info.notoriety);
			data.writeInt(info.wealth);
			data.writeInt(info.legacy);
			
			data.writeInt(info.notorietyRank);
			data.writeInt(info.wealthRank);
			data.writeInt(info.legacyRank);
			data.writeInt(info.totalRank);
			
			data.writeInt(info.mNumClaims);
			
			data.writeInt(info.mCitadelPos.dim);
			data.writeInt(info.mCitadelPos.getX());
			data.writeInt(info.mCitadelPos.getY());
			data.writeInt(info.mCitadelPos.getZ());
			
			// Member list
			data.writeInt(info.members.size());
			for(int i = 0; i < info.members.size(); i++)
			{
				writeUUID(data, info.members.get(i).playerUuid);
				writeUTF(data, info.members.get(i).mPlayerName);
				data.writeInt(info.members.get(i).role.ordinal());
			}
			writeUUID(data, info.mLeaderID);
		}
		else
		{
			data.writeBoolean(false);
		}
		
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		boolean hasInfo = data.readBoolean();
		
		if(hasInfo)
		{
			info = new FactionDisplayInfo();
			
			info.factionId = readUUID(data);
			info.mFactionName = readUTF(data);
			
			info.notoriety = data.readInt();
			info.wealth = data.readInt();
			info.legacy = data.readInt();
			
			info.notorietyRank = data.readInt();
			info.wealthRank = data.readInt();
			info.legacyRank = data.readInt();
			info.totalRank = data.readInt();
			
			info.mNumClaims = data.readInt();
			
			int dim =	data.readInt();
			int x =	data.readInt();
			int y =	data.readInt();
			int z =	data.readInt();
			info.mCitadelPos = new DimBlockPos(dim, x, y, z);
			
			// Member list
			int count = data.readInt();
			for(int i = 0; i < count; i++)
			{
				PlayerDisplayInfo playerInfo = new PlayerDisplayInfo();
				playerInfo.playerUuid = readUUID(data);
				playerInfo.mPlayerName = readUTF(data);
				playerInfo.role = Role.values()[data.readInt()];
				info.members.add(playerInfo);
			}
			info.mLeaderID = readUUID(data);
		}
		else
			info = null;
	}

	@Override
	public void handleServerSide(EntityPlayerMP playerEntity) 
	{
		WarForgeMod.LOGGER.error("Received FactionInfo on server");
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer) 
	{
		latestInfo = info;
		clientPlayer.openGui(
				WarForgeMod.INSTANCE, 
				CommonProxy.GUI_TYPE_FACTION_INFO, 
				clientPlayer.world, 
				clientPlayer.getPosition().getX(),
				clientPlayer.getPosition().getY(),
				clientPlayer.getPosition().getZ());
	}

}
