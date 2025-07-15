package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
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
		data.writeInt(info.attackingPos.dim);
		data.writeInt(info.attackingPos.getX());
		data.writeInt(info.attackingPos.getY());
		data.writeInt(info.attackingPos.getZ());
		data.writeInt(info.attackingColour);
		writeUTF(data, info.attackingName);
		
		// Defend
		data.writeInt(info.defendingPos.dim);
		data.writeInt(info.defendingPos.getX());
		data.writeInt(info.defendingPos.getY());
		data.writeInt(info.defendingPos.getZ());
		data.writeInt(info.defendingColour);
		writeUTF(data, info.defendingName);
		
		data.writeInt(info.progress);
		data.writeInt(info.mPreviousProgress);
		data.writeInt(info.completionPoint);

		//Common
		data.writeLong(info.timeProgress);
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
		info.attackingColour = data.readInt();
		info.attackingName = readUTF(data);
		
		// Defending
		dim = data.readInt();
		x = data.readInt();
		y = data.readInt();
		z = data.readInt();
		info.defendingPos = new DimBlockPos(dim, x, y, z);
		info.defendingColour = data.readInt();
		info.defendingName = readUTF(data);
		
		info.progress = data.readInt();
		info.mPreviousProgress = data.readInt();
		info.completionPoint = data.readInt();

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

	@Override
	public boolean canUseCompression() {
		return true;
	}
}
