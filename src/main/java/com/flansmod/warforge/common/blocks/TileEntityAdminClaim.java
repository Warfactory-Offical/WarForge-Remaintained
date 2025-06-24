package com.flansmod.warforge.common.blocks;

import java.util.UUID;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class TileEntityAdminClaim extends TileEntityClaim
{
	// IClaim
	@Override 
	public boolean canBeSieged() { return false; }
	// ------------
	@Override
	public int getAttackStrength() { return 0; }
	@Override
	public int getDefenceStrength() { return 0; }
	@Override
	public int getSupportStrength() { return 0; }
}
