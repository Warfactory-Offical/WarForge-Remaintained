package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Leaderboard.FactionStat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketRequestLeaderboardInfo extends PacketBase
{
	public FactionStat stat = FactionStat.TOTAL;
	public int firstIndex = 0;
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		data.writeInt(firstIndex);
		data.writeInt(stat.ordinal());
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		firstIndex = data.readInt();
		stat = FactionStat.values()[data.readInt()];
	}

	@Override
	public void handleServerSide(EntityPlayerMP playerEntity) 
	{
		PacketLeaderboardInfo packet = new PacketLeaderboardInfo();
		packet.info = WarForgeMod.LEADERBOARD.CreateInfo(firstIndex, stat, playerEntity.getUniqueID());
		WarForgeMod.NETWORK.sendTo(packet, playerEntity);
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer) 
	{
		WarForgeMod.LOGGER.error("Received LeaderboardInfo request on client");
	}
	
}
