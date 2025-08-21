package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.client.models.DefaultTransform;
import com.flansmod.warforge.client.models.ThemableBakedModel;
import com.flansmod.warforge.common.CommonProxy;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.models.ClaimModels;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.IDynamicModels;
import com.flansmod.warforge.server.Faction;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.PerspectiveMapWrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static com.flansmod.warforge.common.Content.dummyTranslusent;
import static com.flansmod.warforge.common.Content.statue;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.KING;
import static com.flansmod.warforge.common.blocks.BlockDummy.modelEnum.TRANSLUCENT;

public class BlockBasicClaim extends MultiBlockColumn implements ITileEntityProvider, IMultiBlock, IDynamicModels {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public BlockBasicClaim(Material materialIn) {
        super(materialIn);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.setBlockUnbreakable();
        this.setResistance(30000000f);
        IDynamicModels.INSTANCES.add(this);
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
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        if (this == Content.basicClaimBlock)
            return new TileEntityBasicClaim();
        else
            return new TileEntityReinforcedClaim();
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (!world.isRemote) {
            // Can't claim a chunk claimed by another faction
            UUID existingClaim = WarForgeMod.FACTIONS.getClaim(new DimChunkPos(world.provider.getDimension(), pos));
            if (!existingClaim.equals(Faction.nullUuid))
                return false;

            // Can only place on a solid surface
            if (!world.getBlockState(pos.add(0, -1, 0)).isSideSolid(world, pos.add(0, -1, 0), EnumFacing.UP))
                return false;
        }

        return super.canPlaceBlockAt(world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        EnumFacing facing = placer.getHorizontalFacing().getOpposite();
        world.setBlockState(pos, state.withProperty(FACING, facing), 2);
        if (!world.isRemote) {

            TileEntity te = world.getTileEntity(pos);
            if (te != null) {
                TileEntityBasicClaim claim = (TileEntityBasicClaim) te;

                WarForgeMod.FACTIONS.onNonCitadelClaimPlaced(claim, placer);
                super.onBlockPlacedBy(world, pos, state, placer, stack);
            }

        }
    }

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

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float par7, float par8, float par9) {
        if (player.isSneaking()) {
            TileEntityBasicClaim claim = (TileEntityBasicClaim) world.getTileEntity(pos);
            assert claim != null;
            claim.increaseRotation(45f);
            return true;
        }
        if (!world.isRemote) {
            Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUniqueID());
            TileEntityBasicClaim claimTE = (TileEntityBasicClaim) world.getTileEntity(pos);

            // Any factionless players, and players who aren't in this faction get an info panel
            if (playerFaction == null || !playerFaction.uuid.equals(claimTE.factionUUID)) {
                Faction citadelFaction = WarForgeMod.FACTIONS.getFaction(claimTE.factionUUID);
                if (citadelFaction != null) {
                    PacketFactionInfo packet = new PacketFactionInfo();
                    packet.info = citadelFaction.createInfo();
                    WarForgeMod.NETWORK.sendTo(packet, (EntityPlayerMP) player);
                } else {
                    player.sendMessage(new TextComponentString("This claim has no faction."));
                }
            }
            // So anyone else will be from the target faction
            else {
                player.openGui(WarForgeMod.INSTANCE, CommonProxy.GUI_TYPE_BASIC_CLAIM, world, pos.getX(), pos.getY(), pos.getZ());
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

    @Override
    public void initMap() {
        multiBlockMap = Collections.unmodifiableMap(new HashMap<>() {{
            put(statue.getDefaultState().withProperty(MODEL, KING), new Vec3i(0, 1, 0));
            put(dummyTranslusent.getDefaultState().withProperty(MODEL, TRANSLUCENT), new Vec3i(0, 2, 0));
        }});
    }

    @Override
    public StateMapperBase getStateMapper(ResourceLocation loc) {
        return IDynamicModels.super.getStateMapper(loc);
    }

    @Override
    @SneakyThrows
    //TODO:Streamline this
    public void bakeModel(ModelBakeEvent event) {
        IModel medivalModel = ModelLoaderRegistry.getModelOrMissing(
                new ResourceLocation(WarForgeMod.MODID, "block/citadelblock"));
        IModel modernModel = ModelLoaderRegistry.getModelOrMissing(
                new ResourceLocation(WarForgeMod.MODID, "block/statues/modern/onlytable"));

        // Bake each cardinal facing
        for (EnumFacing facing : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST}) {
            ModelRotation rotation = switch (facing) {
                case SOUTH -> ModelRotation.X0_Y180;
                case WEST -> ModelRotation.X0_Y270;
                case EAST -> ModelRotation.X0_Y90;
                default -> ModelRotation.X0_Y0;
            };

            IBakedModel themedModel = new ThemableBakedModel(ImmutableMap.of(
                    ClaimModels.THEME.MEDIVAL, medivalModel.bake(rotation, DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()),
                    ClaimModels.THEME.MODERN, modernModel.bake(rotation, DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter())
            ));

            ModelResourceLocation modelLoc = new ModelResourceLocation(
                    getRegistryName(), "facing=" + facing.getName().toLowerCase()
            );
            event.getModelRegistry().putObject(modelLoc, themedModel);
        }

        IBakedModel bakedMedival = new PerspectiveMapWrapper(
                medivalModel.bake(ModelRotation.X0_Y0, DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()),
                DefaultTransform.BLOCK_TRANSFORMS
        );

        IBakedModel bakedModern = new PerspectiveMapWrapper(
                modernModel.bake(ModelRotation.X0_Y0, DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()),
                DefaultTransform.BLOCK_TRANSFORMS
        );


        IBakedModel themedModel = new ThemableBakedModel(ImmutableMap.of(
                ClaimModels.THEME.MEDIVAL, bakedMedival,
                ClaimModels.THEME.MODERN, bakedModern
        ));
        ModelResourceLocation modelLoc = new ModelResourceLocation(
                getRegistryName(), "inventory"
        );

        event.getModelRegistry().putObject(modelLoc, themedModel);

    }

    @Override
    public void registerModel() {
        ModelLoader.setCustomModelResourceLocation(
                Item.getItemFromBlock(this),
                0,
                new ModelResourceLocation(this.getRegistryName(), "inventory")
        );
    }

    @Override
    public void registerSprite(TextureMap map) {
        //Already registered via ClaimModels's recursive register
    }
}
