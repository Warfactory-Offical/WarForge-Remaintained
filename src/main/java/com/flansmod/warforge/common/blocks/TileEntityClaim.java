package com.flansmod.warforge.common.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.Faction.PlayerData;
import com.mojang.authlib.GameProfile;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class TileEntityClaim extends TileEntity implements IClaim
{
	protected UUID factionUUID = Faction.nullUuid;
	public int colour = 0xFF_FF_FF;
	public String factionName = "";
	
	//public ArrayList<String> playerFlags = new ArrayList<String>();
	
	// IClaim
	@Override
	public UUID getFaction() { return factionUUID; }
	@Override 
	public void updateColour(int colour) { this.colour = colour; }
	@Override
	public int getColour() { return colour; }
	@Override
	public TileEntity getAsTileEntity() { return this; }
	@Override
	public DimBlockPos getClaimPos()
	{ 
		if(world == null)
		{
			if(worldCreate == null)
				return DimBlockPos.ZERO;
			else
				return new DimBlockPos(worldCreate.provider.getDimension(), getPos());
		}
		return new DimBlockPos(world.provider.getDimension(), getPos());
	}
	@Override 
	public boolean canBeSieged() { return true; }
	@Override
	public String getClaimDisplayName() { return factionName; }
//	@Override
//	public List<String> getPlayerFlags() { return playerFlags; }
	//-----------
	
	
//	@Override
//	public void onServerSetPlayerFlag(String playerName)
//	{
//		playerFlags.add(playerName);
//
//		world.markBlockRangeForRenderUpdate(pos, pos);
//		world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
//		world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
//		markDirty();
//	}
//
//	@Override
//	public void onServerRemovePlayerFlag(String playerName)
//	{
//		playerFlags.remove(playerName);
//
//		world.markBlockRangeForRenderUpdate(pos, pos);
//		world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
//		world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
//		markDirty();
//	}
	
	@Override
	public void onServerSetFaction(Faction faction)
	{
		if(faction == null)
		{
			factionUUID = Faction.nullUuid;
		}
		else
		{
			factionUUID = faction.uuid;
			colour = faction.colour;
			factionName = faction.name;
		}
		
		world.markBlockRangeForRenderUpdate(pos, pos);
		world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
		world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
		markDirty();
	}
	
	
	// This is so weird
	private World worldCreate;
	@Override
	public void setWorldCreate(World world)
	{
		worldCreate = world;
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		
		nbt.setUniqueId("faction", factionUUID);
				
		/*
		NBTTagList list = new NBTTagList();
		for(int i = 0; i < mPlayerFlags.size(); i++)
		{
			list.appendTag(new NBTTagString(mPlayerFlags.get(i)));
		}
		nbt.setTag("flags", list);
		*/
		return nbt;
	}

	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
	
		factionUUID = nbt.getUniqueId("faction");

		// Read player flags
		//playerFlags.clear();
		/*
		NBTTagList list = nbt.getTagList("flags", 8); // String
		if(list != null)
		{
			for(NBTBase base : list)
			{
				mPlayerFlags.add(((NBTTagString)base).getString());
			}
		}			*/
		
		// Verifications
		if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
		{
			Faction faction = WarForgeMod.FACTIONS.getFaction(factionUUID);
			if(!factionUUID.equals(Faction.nullUuid) && faction == null)
			{
				WarForgeMod.LOGGER.error("Faction " + factionUUID + " could not be found for citadel at " + pos);
				//world.setBlockState(getPos(), Blocks.AIR.getDefaultState());
			}
			if(faction != null)
			{
				colour = faction.colour;
				factionName = faction.name;
				//for(HashMap.Entry<UUID, PlayerData> kvp : faction.members.entrySet())
				//{
					//if(kvp.getValue().flagPosition.equals(this.getClaimPos()))
					//{
						//GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getProfileByUUID(kvp.getKey());
						//if(profile != null)
							//playerFlags.add(profile.getName());
					//}
				//}
			}
		}
		else
		{
			WarForgeMod.LOGGER.error("Loaded TileEntity from NBT on client?");
		}
		
	}
	
	@Override
	public SPacketUpdateTileEntity getUpdatePacket()
	{
		return new SPacketUpdateTileEntity(getClaimPos(), getBlockMetadata(), getUpdateTag());
	}
	
	@Override
	public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity packet)
	{
		NBTTagCompound tags = packet.getNbtCompound();
		
		handleUpdateTag(tags);
	}
	
	@Override
	public NBTTagCompound getUpdateTag()
	{
		// You have to get parent tags so that x, y, z are added.
		NBTTagCompound tags = super.getUpdateTag();

		// Custom partial nbt write method
		tags.setUniqueId("faction", factionUUID);
		tags.setInteger("colour", colour);
		tags.setString("name", factionName);
		
//		NBTTagList list = new NBTTagList();
//        for (String playerFlag : playerFlags) {
//            list.appendTag(new NBTTagString(playerFlag));
//        }
//		tags.setTag("flags", list);
		
		return tags;
	}
	
	@Override
	public void handleUpdateTag(NBTTagCompound tags)
	{
		factionUUID = tags.getUniqueId("faction");
		colour = tags.getInteger("colour");
		factionName = tags.getString("name");
		
		
		// Read player flags
//		playerFlags.clear();
//		NBTTagList list = tags.getTagList("flags", 8); // String
//		for(NBTBase base : list)
//		{
//			playerFlags.add(((NBTTagString)base).getString());
//		}
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
		BlockPos pos = getClaimPos();
		return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 16, 2));
    	
    }
}
