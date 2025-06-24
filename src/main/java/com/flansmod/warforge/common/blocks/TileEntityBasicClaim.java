package com.flansmod.warforge.common.blocks;

import java.util.UUID;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityBasicClaim extends TileEntityYieldCollector implements IClaim
{
	public static final int NUM_SLOTS = NUM_BASE_SLOTS; // No additional slots here


	public TileEntityBasicClaim()
	{
		
	}
	
	@Override
	public int getDefenceStrength() { return WarForgeConfig.CLAIM_STRENGTH_BASIC; }
	@Override
	public int getSupportStrength() { return WarForgeConfig.SUPPORT_STRENGTH_BASIC; }
	@Override
	public int getAttackStrength() { return 0; }
	@Override
	protected float getYieldMultiplier() { return 1.0f; }

}
