package com.flansmod.warforge.common;

import com.flansmod.warforge.common.blocks.TileEntityBasicClaim;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy implements IGuiHandler
{
	public static final int GUI_TYPE_CITADEL = 0;
	public static final int GUI_TYPE_CREATE_FACTION = 1;
	public static final int GUI_TYPE_BASIC_CLAIM = 2;
	public static final int GUI_TYPE_SIEGE_CAMP = 3;
	public static final int GUI_TYPE_FACTION_INFO = 4;
	public static final int GUI_TYPE_LEADERBOARD = 5;
	public static final int GUI_TYPE_RECOLOUR_FACTION = 6;
	public static final int GUI_TYPE_SHOP = 7;

	// determines the bonus multiplier or reduction multiplier (reciprocal) for veins of
	// increasing or decreasing quality, respectively; not configurable for now, but would need sync packets if it were
	public static float YIELD_QUALITY_MULTIPLIER = 2;
	
	public void preInit(FMLPreInitializationEvent event)
	{
		
	}
	
	public void Init(FMLInitializationEvent event)
	{
		
	}
	
	public void PostInit(FMLPostInitializationEvent event)
	{
		
	}
	
	public void TickClient()
	{
		
	}
	
	public void TickServer()
	{
		
	}
	
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		return null;
	}
	
	public TileEntity GetTile(DimBlockPos pos)
	{
		if(FMLCommonHandler.instance().getSide() == Side.SERVER)
			return WarForgeMod.MC_SERVER.getWorld(pos.dim).getTileEntity(pos.toRegularPos());
		
		WarForgeMod.LOGGER.error("GetTile failed");
		return null;
	}

	
	/**
	 * Gets the container for the specified GUI
	 */
	public Container getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		BlockPos pos = new BlockPos(x, y, z);
		switch(ID)
		{
			case GUI_TYPE_CITADEL: return new ContainerCitadel(player.inventory, (TileEntityCitadel)world.getTileEntity(pos));
			case GUI_TYPE_CREATE_FACTION: return null;
			case GUI_TYPE_BASIC_CLAIM: return new ContainerBasicClaim(player.inventory, (TileEntityBasicClaim)world.getTileEntity(pos));
			case GUI_TYPE_SIEGE_CAMP: return null;
			case GUI_TYPE_FACTION_INFO: return null;
			case GUI_TYPE_LEADERBOARD: return null;
			case GUI_TYPE_RECOLOUR_FACTION: return null;
		}
		return null;
	}

	public void UpdateSiegeInfo(SiegeCampProgressInfo mInfo) 
	{
		// Do nothing, update on client
	}
}
