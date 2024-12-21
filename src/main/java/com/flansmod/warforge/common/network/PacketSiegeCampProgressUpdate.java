package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketSiegeCampProgressUpdate extends PacketBase
{
	public SiegeCampProgressInfo info;
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		// Attack
		data.writeInt(info.attackingPos.mDim);
		data.writeInt(info.attackingPos.getX());
		data.writeInt(info.attackingPos.getY());
		data.writeInt(info.attackingPos.getZ());
		data.writeInt(info.mAttackingColour);
		writeUTF(data, info.attackingName);
		
		// Defend
		data.writeInt(info.defendingPos.mDim);
		data.writeInt(info.defendingPos.getX());
		data.writeInt(info.defendingPos.getY());
		data.writeInt(info.defendingPos.getZ());
		data.writeInt(info.mDefendingColour);
		writeUTF(data, info.defendingName);
		
		data.writeInt(info.mProgress);
		data.writeInt(info.mPreviousProgress);
		data.writeInt(info.mCompletionPoint);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		info = new SiegeCampProgressInfo();
		
		// Attacking
		int dim = data.readInt();
		int x = data.readInt();
		int y = data.readInt();
		int z = data.readInt();
		info.attackingPos = new DimBlockPos(dim, x, y, z);
		info.mAttackingColour = data.readInt();
		info.attackingName = readUTF(data);
		
		// Defending
		dim = data.readInt();
		x = data.readInt();
		y = data.readInt();
		z = data.readInt();
		info.defendingPos = new DimBlockPos(dim, x, y, z);
		info.mDefendingColour = data.readInt();
		info.defendingName = readUTF(data);
		
		info.mProgress = data.readInt();
		info.mPreviousProgress = data.readInt();
		info.mCompletionPoint = data.readInt();
	}

	@Override
	public void handleServerSide(EntityPlayerMP playerEntity) 
	{
		WarForgeMod.LOGGER.error("Received siege info on server");
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer) 
	{
		WarForgeMod.proxy.UpdateSiegeInfo(info);
	}

}
