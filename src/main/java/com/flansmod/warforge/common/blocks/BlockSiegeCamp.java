package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.DimChunkPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRemoveClaim;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.server.Faction;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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

import java.util.*;

import static com.flansmod.warforge.common.Content.dummyTranslusent;
import static com.flansmod.warforge.common.Content.statue;
import static com.flansmod.warforge.common.WarForgeMod.FACTIONS;
import static com.flansmod.warforge.common.WarForgeMod.isOp;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.BERSERKER;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.TRANSLUCENT;

public class BlockSiegeCamp extends MultiBlockColumn implements ITileEntityProvider {
    //25s break time, no effective tool.
    public BlockSiegeCamp(Material materialIn) {
        super(materialIn);
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.setResistance(30000000f);
        this.setHardness(5f); // (*5) to get harvest time
    }

    // these are likely redundant, as the default is no tool, but I guess it doesnt hurt
    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return false;
    }

    @Override
    public String getHarvestTool(IBlockState state) {
        return null;
    }

    // we want to give the siege block back
    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.TRANSLUCENT;
    }


    // vanilla hasTileEntity check
    @Override
    public boolean hasTileEntity() {
        return true;
    }

    // forge version which is state dependent (apparently for extending vanilla blocks)
    @Override
    public boolean hasTileEntity(IBlockState blockState) {
        return true;
    }

    // called on block place
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySiegeCamp();
    }

    // called before block place
    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        // Can't claim a chunk claimed by another faction
        if (!world.isRemote) {
            UUID existingClaim = FACTIONS.getClaim(new DimChunkPos(world.provider.getDimension(), pos));
            if (!existingClaim.equals(Faction.nullUuid))
                return false;
        }

        // Can only place on a solid surface
        if (!world.getBlockState(pos.add(0, -1, 0)).isSideSolid(world, pos.add(0, -1, 0), EnumFacing.UP))
            return false;

        return super.canPlaceBlockAt(world, pos);
    }

    // called after block place
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te != null) {
                TileEntitySiegeCamp siegeCamp = (TileEntitySiegeCamp) te;
                FACTIONS.onNonCitadelClaimPlaced(siegeCamp, placer);
                siegeCamp.onPlacedBy(placer);
                super.onBlockPlacedBy(world, pos, state, placer, stack);
//				if(placer instanceof EntityPlayerMP)
//				{
//					FACTIONS.requestPlaceFlag((EntityPlayerMP)placer, new DimBlockPos(world.provider.getDimension(), pos));
//				}
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float par7, float par8, float par9) {
        if (player.isSneaking()) {
            if (!world.isRemote) return true;
            TileEntityClaim te = (TileEntityClaim) world.getTileEntity(pos);
            PacketRemoveClaim packet = new PacketRemoveClaim();

            packet.pos = te.getClaimPos();

            WarForgeMod.NETWORK.sendToServer(packet);

            return true;
        }


        if (!world.isRemote) {
            TileEntityClaim te = (TileEntityClaim) world.getTileEntity(pos);
            Faction faction = FACTIONS.getFaction(te.getFaction());

            if (!isOp(player) && !faction.isPlayerRoleInFaction(player.getUniqueID(), Faction.Role.OFFICER)) {
                player.sendMessage(new TextComponentString("You are not an officer of the faction"));
                return false;
            }

            DimChunkPos chunkPos = new DimChunkPos(world.provider.getDimension(), pos);
            if (FACTIONS.IsSiegeInProgress(chunkPos)) FACTIONS.sendAllSiegeInfoToNearby();
            PacketSiegeCampInfo info = new PacketSiegeCampInfo();
            info.mPossibleAttacks = CalculatePossibleAttackDirections(world, pos, player);
            info.mSiegeCampPos = new DimBlockPos(world.provider.getDimension(), pos);
            WarForgeMod.NETWORK.sendTo(info, (EntityPlayerMP) player);
        }

        return true;
    }

    private List<SiegeCampAttackInfo> CalculatePossibleAttackDirections(World world, BlockPos pos, EntityPlayer player) {
        List<SiegeCampAttackInfo> list = new ArrayList<>();

        TileEntitySiegeCamp siegeCamp = (TileEntitySiegeCamp) world.getTileEntity(pos);
        if (siegeCamp == null) return list;

        int RADIUS = 2;
        int BORDER_SIZE = 2 * RADIUS + 1; // odd-sized grid including center

        UUID factionUUID = FACTIONS.getFactionOfPlayer(player.getUniqueID()).uuid;
        DimBlockPos siegePos = new DimBlockPos(world.provider.getDimension(), pos);
        var validTargets = FACTIONS.getClaimRadiusAround(factionUUID, siegePos, RADIUS);

        int centerIndexX = BORDER_SIZE / 2;
        int centerIndexZ = BORDER_SIZE / 2;

        int index = 0;
        for (DimChunkPos chunk : new ArrayList<>(validTargets.keySet())) {
            int row = index / BORDER_SIZE;
            int col = index % BORDER_SIZE;

            int dx = col - centerIndexX;
            int dz = row - centerIndexZ;

            Vec3i offset = new Vec3i(dx, 0, dz);

            Faction claimedBy = FACTIONS.getFaction(FACTIONS.getClaim(chunk));

            SiegeCampAttackInfo info = new SiegeCampAttackInfo();
            info.mOffset = offset;
            info.canAttack = (Math.abs(offset.getZ()) <= 1 && Math.abs(offset.getX()) <= 1) && validTargets.get(chunk);

            info.mFactionUUID = claimedBy == null ? Faction.nullUuid : claimedBy.uuid;
            info.mFactionName = claimedBy == null ? "" : claimedBy.name;
            info.mFactionColour = claimedBy == null ? 0 : claimedBy.colour;

            list.add(info);
            index++;
        }

        return list;
    }


    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.IGNORE;
    }
    // called when block is removed on both client and server, but block is intact at time of call
	/* UNFINISHED/ UNNECESSARY CURRENTLY
	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		if (world.isRemote) return true; // don't do logic on client
		TileEntity te = world.getTileEntity(pos);
		if(te != null) {
			TileEntitySiegeCamp siegeCamp = (TileEntitySiegeCamp)te;
			siegeCamp.OnServerRemovePlayerFlag(siegeCamp.getPlacer().getName());
		}
		return true;
	}
	 */

    /**
     * Called on both Client and Server when World#addBlockEvent is called. On the Server, this may perform additional
     * changes to the world, like pistons replacing the block with an extended base. On the client, the update may
     * involve replacing tile entities, playing sounds, or performing other visual actions to reflect the server side
     * changes.
     */
	/*
	boolean onBlockEventReceived(World worldIn, BlockPos pos, int id, int param) {
		TileEntity te = worldIn.getTileEntity(pos);
		if (worldIn.isRemote && te instanceof TileEntitySiegeCamp && param == 2) {
			((TileEntitySiegeCamp) te).concludeSiege();
			return true;
		}
		return false;
	}
	 */

    // server side and allows client to have the possibility to accept events, alongside enabling server acceptance
    @Deprecated
    public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id, int param) {
        return true;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    // called before te is updated and does not necessarily mean block is being removed by player
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
		/*
		if (worldIn.isRemote) return;
		TileEntity te = worldIn.getTileEntity(pos);
		if(te != null) {
			TileEntitySiegeCamp siegeCamp = (TileEntitySiegeCamp)te;
			if (siegeCamp != null) siegeCamp.onDestroyed();
		}

		 */
    }

    @Override
    public void initMap() {
        multiBlockMap = Collections.unmodifiableMap(new HashMap<>() {{
            put(statue.getDefaultState().withProperty(MODEL, BERSERKER), new Vec3i(0, 1, 0));
            put(dummyTranslusent.getDefaultState().withProperty(MODEL, TRANSLUCENT), new Vec3i(0, 2, 0));
        }});

    }
}
