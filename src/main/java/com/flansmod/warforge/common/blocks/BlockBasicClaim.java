package com.flansmod.warforge.common.blocks;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import com.flansmod.warforge.common.CommonProxy;
import com.flansmod.warforge.common.DimChunkPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.server.Faction;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import static com.flansmod.warforge.common.Content.dummyTranslusent;
import static com.flansmod.warforge.common.Content.statue;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.*;

public class BlockBasicClaim extends MultiBlockColumn implements ITileEntityProvider
{
	public BlockBasicClaim(Material materialIn) 
	{
		super(materialIn);
		this.setCreativeTab(CreativeTabs.COMBAT);
		this.setBlockUnbreakable();
		this.setResistance(30000000f);
	}
	
	@Override
    public boolean isOpaqueCube(IBlockState state) { return true; }
	@Override
    public boolean isFullCube(IBlockState state) { return true; }
	@Override
    public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }
	@Override
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
		return layer == BlockRenderLayer.SOLID;
	}


	/* No usages but it errors sooooooooo
	@SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }
    */

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta)
	{
		if(this == WarForgeMod.CONTENT.basicClaimBlock)
			return new TileEntityBasicClaim();
		else
			return new TileEntityReinforcedClaim();
	}
	
	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos)
	{
		if(!world.isRemote)
		{
			// Can't claim a chunk claimed by another faction
			UUID existingClaim = WarForgeMod.FACTIONS.getClaim(new DimChunkPos(world.provider.getDimension(), pos));
			if(!existingClaim.equals(Faction.nullUuid))
				return false;
					
			// Can only place on a solid surface
            if( !world.getBlockState(pos.add(0, -1, 0)).isSideSolid(world, pos.add(0, -1, 0), EnumFacing.UP))
				return false;
		}
		
		return super.canPlaceBlockAt(world, pos);
	}

	@Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
		if(!world.isRemote) 
		{
			TileEntity te = world.getTileEntity(pos);
			if(te != null)
			{
				TileEntityBasicClaim claim = (TileEntityBasicClaim)te;

				WarForgeMod.FACTIONS.onNonCitadelClaimPlaced(claim, placer);
				super.onBlockPlacedBy(world,pos,state,placer,stack);
			}

		}
    }
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float par7, float par8, float par9)
	{
		if(player.isSneaking()) {
			TileEntityBasicClaim claim = (TileEntityBasicClaim) world.getTileEntity(pos);
			assert claim != null;
			claim.increaseRotation(45f);
			return true;
		}
		if(!world.isRemote)
		{
			Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUniqueID());
			TileEntityBasicClaim claimTE = (TileEntityBasicClaim)world.getTileEntity(pos);
			
			// Any factionless players, and players who aren't in this faction get an info panel			
			if(playerFaction == null || !playerFaction.uuid.equals(claimTE.factionUUID))
			{
				Faction citadelFaction = WarForgeMod.FACTIONS.getFaction(claimTE.factionUUID);
				if(citadelFaction != null)
				{
					PacketFactionInfo packet = new PacketFactionInfo();
					packet.info = citadelFaction.createInfo();
					WarForgeMod.INSTANCE.NETWORK.sendTo(packet, (EntityPlayerMP) player);
				}
				else
				{
					player.sendMessage(new TextComponentString("This claim has no faction."));
				}
			}
			// So anyone else will be from the target faction
			else
			{
				player.openGui(WarForgeMod.INSTANCE, CommonProxy.GUI_TYPE_BASIC_CLAIM, world, pos.getX(), pos.getY(), pos.getZ());
			}
		}
		return true;
	}
	
	@Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity)
    {
        return false;
    }
	
	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
	{
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityYieldCollector)
        {
            InventoryHelper.dropInventoryItems(worldIn, pos, (TileEntityYieldCollector)tileentity);
            worldIn.updateComparatorOutputLevel(pos, this);
        }

        super.breakBlock(worldIn, pos, state);
    }

	@Override
	public void initMap() {
		multiBlockMap = Collections.unmodifiableMap(new HashMap<IBlockState, Vec3i>() {{
			put(statue.getDefaultState().withProperty(MODEL, KNIGHT), new Vec3i(0, 1, 0));
			put(dummyTranslusent.getDefaultState().withProperty(MODEL, TRANSLUCENT), new Vec3i(0, 2, 0));
		}});
	}
}
