package com.flansmod.warforge.common.blocks;

import akka.japi.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.models.RotatableStateMapper;
import com.flansmod.warforge.common.network.PacketRemoveClaim;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.IDynamicModels;
import com.flansmod.warforge.server.Faction;
import lombok.SneakyThrows;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

import static com.flansmod.warforge.client.models.BakingUtil.registerFacingModels;
import static com.flansmod.warforge.common.Content.dummyTranslusent;
import static com.flansmod.warforge.common.Content.statue;
import static com.flansmod.warforge.common.WarForgeMod.*;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.BERSERKER;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.TRANSLUCENT;

public class BlockSiegeCamp extends MultiBlockColumn implements ITileEntityProvider, IDynamicModels {
    //25s break time, no effective tool.
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public BlockSiegeCamp(Material materialIn) {
        super(materialIn);
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.setBlockUnbreakable();
        this.setResistance(30000000f); //Makes sense. We probably don't wanna let people bomb it
        IDynamicModels.INSTANCES.add(this);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.HORIZONTALS[meta]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
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
        return true;
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
        return layer == BlockRenderLayer.CUTOUT;
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
            if ( te instanceof TileEntitySiegeCamp siegeCamp) {
                FACTIONS.onNonCitadelClaimPlaced(siegeCamp, placer);
                siegeCamp.onPlacedBy(placer);
                super.onBlockPlacedBy(world, pos, state, placer, stack);
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
            info.momentum = faction.getSiegeMomentum();
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
            Pair<Vein, Quality> veinInfo = VEIN_HANDLER.getVein(chunk.dim, chunk.x, chunk.z,
                    FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0].getSeed());
            if (veinInfo != null) {
                info.mWarforgeVein = veinInfo.first();
                info.mOreQuality =  veinInfo.second();
            }

            list.add(info);
            index++;
        }

        return list;
    }


    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.IGNORE;
    }

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
    }

    @Override
    public void initMap() {
        multiBlockMap = Collections.unmodifiableMap(new HashMap<>() {{
            put(statue.getDefaultState().withProperty(MODEL, BERSERKER), new Vec3i(0, 1, 0));
            put(dummyTranslusent.getDefaultState().withProperty(MODEL, TRANSLUCENT), new Vec3i(0, 2, 0));
        }});

    }

    @Override
    @SideOnly(Side.CLIENT)
    public StateMapperBase getStateMapper(ResourceLocation loc) {
        return new RotatableStateMapper(getRegistryName());
    }

    @Override
    @SneakyThrows
    public void bakeModel(ModelBakeEvent event) {
        IModel medieval = ModelLoaderRegistry.getModelOrMissing(
                new ResourceLocation(WarForgeMod.MODID, "block/warstump"));
        IModel modern = ModelLoaderRegistry.getModelOrMissing(
                new ResourceLocation(WarForgeMod.MODID, "block/statues/modern/flag_pole"));
        registerFacingModels(medieval, modern, event.getModelRegistry(), getRegistryName());
    }

    @Override
    public void registerModel() {
        ModelLoader.setCustomModelResourceLocation(
                Item.getItemFromBlock(this),
                0,
                new ModelResourceLocation(Objects.requireNonNull(getRegistryName()), "inventory")
        );
    }

    @Override
    public void registerSprite(TextureMap map) {
        //Already registered via ClaimModels's recursive register
    }
}
