package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketPlaceFlag extends PacketBase 
{
	public DimBlockPos pos;
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		data.writeInt(pos.dim);
		data.writeInt(pos.getX());
		data.writeInt(pos.getY());
		data.writeInt(pos.getZ());
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		int dim = data.readInt();
		int x = data.readInt();
		int y = data.readInt();
		int z = data.readInt();
		pos = new DimBlockPos(dim, x, y, z);
	}

	@Override
	public void handleServerSide(EntityPlayerMP player) 
	{
		//WarForgeMod.FACTIONS.requestPlaceFlag(player, pos);
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer)
	{
		 //noop
	}

}
