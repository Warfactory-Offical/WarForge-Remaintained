package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

public class PacketCreateFaction extends PacketBase
{
	public DimBlockPos mCitadelPos;
	public String mFactionName = "";
	public int mColour = 0xffffff;


	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		data.writeInt(mCitadelPos.dim);
		data.writeInt(mCitadelPos.getX());
		data.writeInt(mCitadelPos.getY());
		data.writeInt(mCitadelPos.getZ());
		data.writeInt(mColour);
		writeUTF(data, mFactionName);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		int dim = data.readInt();
		int x = data.readInt();
		int y = data.readInt();
		int z = data.readInt();
		mCitadelPos = new DimBlockPos(dim, x, y, z);
		mColour = data.readInt();
		mFactionName = readUTF(data);
	}

	@Override
	public void handleServerSide(EntityPlayerMP playerEntity) 
	{
		if(playerEntity.dimension != mCitadelPos.dim)
		{
			WarForgeMod.LOGGER.error("Player requested creating a faction in the wrong dim");
		}
		else
		{
			TileEntity te = playerEntity.world.getTileEntity(mCitadelPos.toRegularPos());
			if(te != null && te instanceof TileEntityCitadel)
			{
				WarForgeMod.FACTIONS.requestCreateFaction((TileEntityCitadel)te, playerEntity, mFactionName, mColour);
			}
		}
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer) 
	{
		WarForgeMod.LOGGER.error("Recieved create faction message on client");
	}
	
}
