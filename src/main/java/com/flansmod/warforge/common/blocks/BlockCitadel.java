package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.CommonProxy;
import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.DimChunkPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionStorage;
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
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.flansmod.warforge.common.Content.dummyTranslusent;
import static com.flansmod.warforge.common.Content.statue;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.KING;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.TRANSLUCENT;

public class BlockCitadel extends MultiBlockColumn implements ITileEntityProvider, IMultiBlock {
    public BlockCitadel(Material materialIn) {
        super(materialIn);
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.setBlockUnbreakable();
        this.setResistance(30000000f);
    }

    public void initMap() {
         mbmap = Collections.unmodifiableMap(new HashMap<IBlockState, Vec3i>() {{
            put(statue.getDefaultState().withProperty(MODEL, KING), new Vec3i(0, 1, 0));
            put(dummyTranslusent.getDefaultState().withProperty(MODEL, TRANSLUCENT), new Vec3i(0, 2, 0));
        }});

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
//	@Override
//	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
//		return layer == BlockRenderLayer.TRANSLUCENT;
//	}

    /* Unused code that errors #3
    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }
    */
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCitadel();
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        // Can't claim a chunk claimed by another faction
        UUID existingClaim = WarForgeMod.FACTIONS.getClaim(new DimChunkPos(world.provider.getDimension(), pos));
        if (!existingClaim.equals(Faction.nullUuid))
            return false;
        // Can only place on a solid surface
        if (!world.getBlockState(pos.add(0, -1, 0)).isSideSolid(world, pos.add(0, -1, 0), EnumFacing.UP))
            return false;
        return super.canPlaceBlockAt(world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te != null) {
            TileEntityCitadel citadel = (TileEntityCitadel) te;
            citadel.onPlacedBy(placer);
            if (world.isRemote) return;
            super.onBlockPlacedBy(world, pos, state, placer, stack);
        }
    }

    @Override
    public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id, int param) {
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float par7, float par8, float par9) {
        if (player.isSneaking())
            return false;
        if (!world.isRemote) {
            Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUniqueID());
            TileEntityCitadel citadel = (TileEntityCitadel) world.getTileEntity(pos);

            // If the player has no faction and is the placer, they can open the UI
            if (playerFaction == null && player.getUniqueID().equals(citadel.placer)) {
                player.openGui(WarForgeMod.INSTANCE, CommonProxy.GUI_TYPE_CITADEL, world, pos.getX(), pos.getY(), pos.getZ());
            }
            // Any other factionless players, and players who aren't in this faction get an info panel
            else if (playerFaction == null || !playerFaction.uuid.equals(citadel.factionUUID)) {
                Faction citadelFaction = WarForgeMod.FACTIONS.getFaction(citadel.factionUUID);
                if (citadelFaction != null) {
                    PacketFactionInfo packet = new PacketFactionInfo();
                    packet.info = citadelFaction.createInfo();
                    WarForgeMod.NETWORK.sendTo(packet, (EntityPlayerMP) player);
                } else {
                    DimBlockPos citadelPos = new DimBlockPos(world.provider.getDimension(), pos);
                    Faction chunkFaction = WarForgeMod.FACTIONS.getFaction(WarForgeMod.FACTIONS.getClaim(citadelPos.toChunkPos()));
                    // if ghost citadel exists in chunk claimed by faction, delete it
                    if (FactionStorage.isValidFaction(chunkFaction)) {
                        world.destroyBlock(pos, false);
                        player.sendMessage(new TextComponentString("Overlapping citadel placement found; deleting current."));
                    } else {
                        player.sendMessage(new TextComponentString("This citadel is not home to a faction, and was not placed by you."));
                    }
                }
            }
            // So anyone else will be from the target faction
            else {
                player.openGui(WarForgeMod.INSTANCE, CommonProxy.GUI_TYPE_CITADEL, world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityYieldCollector) {
            InventoryHelper.dropInventoryItems(worldIn, pos, (TileEntityYieldCollector) tileentity);
            worldIn.updateComparatorOutputLevel(pos, this);
        }

        super.breakBlock(worldIn, pos, state);
    }
}
