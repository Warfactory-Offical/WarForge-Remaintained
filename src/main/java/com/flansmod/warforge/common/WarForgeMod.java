package com.flansmod.warforge.common;

import com.flansmod.warforge.api.ObjectIntPair;
import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.VeinKey;
import com.flansmod.warforge.client.PlayerNametagCache;
import com.flansmod.warforge.common.blocks.IMultiBlockInit;
import com.flansmod.warforge.common.blocks.TileEntityClaim;
import com.flansmod.warforge.common.blocks.TileEntitySiegeCamp;
import com.flansmod.warforge.common.effect.EffectRegistry;
import com.flansmod.warforge.common.network.*;
import com.flansmod.warforge.common.potions.PotionsModule;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.IDynamicModels;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.server.*;
import com.flansmod.warforge.server.Faction.Role;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.tokens.Token;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Mod(modid = WarForgeMod.MODID, name = WarForgeMod.NAME, version = WarForgeMod.VERSION)
@Mod.EventBusSubscriber(modid = WarForgeMod.MODID)
public class WarForgeMod implements ILateMixinLoader {
    public static final String MODID = "warforge";
    public static final String NAME = "WarForge Factions";
    public static final String VERSION = "1.2.0";
    public static final PacketHandler NETWORK = new PacketHandler();
    public static final Leaderboard LEADERBOARD = new Leaderboard();
    public static final FactionStorage FACTIONS = new FactionStorage();
    public static final Content CONTENT = new Content();
    public static final ProtectionsModule PROTECTIONS = new ProtectionsModule();
    public static final TeleportsModule TELEPORTS = new TeleportsModule();
    public static final PotionsModule POTIONS = new PotionsModule();
    public static final UpgradeHandler UPGRADE_HANDLER = new UpgradeHandler();
    public static final ModelEventHandler MODEL_EVENT_HANDLER = new ModelEventHandler();
    // Discord integration
    private static final String DISCORD_MODID = "discordintegration";
    private static final HashMap<String, UUID> discordUserIdMap = new HashMap<String, UUID>();
    public static Int2ObjectOpenHashMap<TreeMap<VeinKey, Vein>> VEIN_MAP = new Int2ObjectOpenHashMap<>();
    @SideOnly(Side.CLIENT)
    public static PlayerNametagCache NAMETAG_CACHE;
    @Instance(MODID)
    public static WarForgeMod INSTANCE;
    @SidedProxy(clientSide = "com.flansmod.warforge.client.ClientProxy", serverSide = "com.flansmod.warforge.common.CommonProxy")
    public static CommonProxy proxy;
    // Instances of component parts of the mod
    public static Logger LOGGER;
    public static MinecraftServer MC_SERVER = null;
    //public static CombatLogHandler COMBAT_LOG = new CombatLogHandler();
    public static Random rand = new Random();
    public static long numberOfSiegeDaysTicked = 0L;
    public static long numberOfYieldDaysTicked = 0L;
    public static long timestampOfFirstDay = 0L;
    public static long previousUpdateTimestamp = 0L;
    // Timers
    public static long serverTick = 0L;
    public static long currTickTimestamp = 0L;
    public static long serverStopTimestamp = 0L;
    // Border toggle
    public static boolean showBorders = true;
    public static TimeHelper timeHelper = new TimeHelper();

    public static boolean containsInt(final int[] base, int compare) {
        return Arrays.stream(base).anyMatch(i -> i == compare);
    }

    public static boolean isClaim(Item item) {
        return item != null && (item.equals(Content.citadelBlockItem)
                || item.equals(Content.basicClaimBlockItem)
                || item.equals(Content.reinforcedClaimBlockItem)
                || item.equals(Content.siegeCampBlockItem));
    }

    public static boolean isClaim(Block block, Block... notEquals) {
        if (block == null) return false;

        boolean matchesAnyInvalidBlocks = false;
        for (Block invalidBlock : notEquals) {
            if (block.equals(invalidBlock)) {
                matchesAnyInvalidBlocks = true;
                break;
            }
        }

        return !matchesAnyInvalidBlocks && (block.equals(Content.citadelBlock)
                || block.equals(Content.basicClaimBlock)
                || block.equals(Content.reinforcedClaimBlock)
                || block.equals(Content.siegeCampBlock)
                || block.equals(Content.statue)
                || block.equals(Content.dummyTranslusent));
    }

    private static File getFactionsFile() {
        if (MC_SERVER.isDedicatedServer()) {
            return new File(MC_SERVER.getFolderName() + "/warforgefactions.dat");
        }

        return new File("saves/" + MC_SERVER.getFolderName() + "/warforgefactions.dat");
    }

    private static File getFactionsFileBackup() {
        if (MC_SERVER.isDedicatedServer()) {
            return new File(MC_SERVER.getFolderName() + "/warforgefactions.dat.bak");
        }
        return new File("saves/" + MC_SERVER.getFolderName() + "/warforgefactions.dat.bak");

        //return new File(MC_SERVER.getWorld(0).getSaveHandler().getWorldDirectory() + "/warforgefactions.dat.bak");
    }

    public static UUID getUUID(ICommandSender sender) {
        if (sender instanceof EntityPlayer)
            return ((EntityPlayer) sender).getUniqueID();
        return UUID.fromString("Unknown");
    }

    public static boolean isOp(ICommandSender sender) {
        if (sender instanceof EntityPlayer)
            return MC_SERVER.getPlayerList().canSendCommands(((EntityPlayer) sender).getGameProfile());
        return sender instanceof MinecraftServer;
    }

    @SubscribeEvent
    public static void onRegisterSounds(RegistryEvent.Register<SoundEvent> event) {
        Sounds.register(event.getRegistry());
    }


    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        //Load config
        WarForgeConfig.syncConfig(event.getSuggestedConfigurationFile());

        timestampOfFirstDay = System.currentTimeMillis();
        numberOfSiegeDaysTicked = 0L;
        numberOfYieldDaysTicked = 0L;

        CONTENT.preInit();
        POTIONS.preInit();

        MinecraftForge.EVENT_BUS.register(new ServerTickHandler());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PROTECTIONS);
        MinecraftForge.EVENT_BUS.register(MODEL_EVENT_HANDLER);
        proxy.preInit(event);
        EffectRegistry.init();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        NETWORK.initialise();
        proxy.Init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        NETWORK.postInitialise();
        proxy.PostInit(event);

        WarForgeConfig.VAULT_BLOCKS.clear();
        for (String blockID : WarForgeConfig.VAULT_BLOCK_IDS) {
            Block block = Block.getBlockFromName(blockID);
            if (block != null) {
                WarForgeConfig.VAULT_BLOCKS.add(block);
                LOGGER.info("Found block with ID " + blockID + " as a valuable block for the vault");
            } else
                LOGGER.error("Could not find block with ID " + blockID + " as a valuable block for the vault");

        }

        FMLInterModComms.sendRuntimeMessage(this, DISCORD_MODID, "registerListener", "");

        WarForgeConfig.UNCLAIMED.findBlocks();
        WarForgeConfig.SAFE_ZONE.findBlocks();
        WarForgeConfig.WAR_ZONE.findBlocks();
        WarForgeConfig.CITADEL_FRIEND.findBlocks();
        WarForgeConfig.CITADEL_FOE.findBlocks();
        WarForgeConfig.CLAIM_FRIEND.findBlocks();
        WarForgeConfig.CLAIM_FOE.findBlocks();
        WarForgeConfig.SIEGECAMP_SIEGER.findBlocks();
        WarForgeConfig.SIEGECAMP_OTHER.findBlocks();
        IMultiBlockInit.registerMaps();
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            Path configFile = Paths.get("config/" + WarForgeMod.MODID + "/upgrade_levels.cfg");
            try {
                UpgradeHandler.writeStubIfEmpty(configFile);
                UpgradeHandler.parseConfig(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<VeinConfigHandler.VeinEntry> veinList = new ArrayList<>();
        try {
            VeinConfigHandler.writeStubIfEmpty();
            veinList = VeinConfigHandler.loadVeins();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (!veinList.isEmpty() && veinList != null)
            VeinKey.populateVeinMap(WarForgeMod.VEIN_MAP, veinList);

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            NAMETAG_CACHE = new PlayerNametagCache(60_000, 200);
        }


    }

    public long getCooldownRemainingSeconds(float cooldown, long startOfCooldown) {

        return timeHelper.getCooldownRemainingSeconds(cooldown, startOfCooldown);
    }

    public int getCooldownRemainingMinutes(float cooldown, long startOfCooldown) {

        return timeHelper.getCooldownRemainingMinutes(cooldown, startOfCooldown);
    }

    public int getCooldownRemainingHours(float cooldown, long startOfCooldown) {

        return timeHelper.getCooldownRemainingHours(cooldown, startOfCooldown);
    }

    public long getTimeToNextSiegeAdvanceMs() {

        return timeHelper.getTimeToNextSiegeAdvanceMs();
    }

    public long getTimeToNextYieldMs() {

        return timeHelper.getTimeToNextYieldMs();
    }

    public void updateServer() {
        currTickTimestamp = System.currentTimeMillis();
        previousUpdateTimestamp = currTickTimestamp;

        FACTIONS.updateConqueredChunks(currTickTimestamp);

        ++serverTick;

        boolean shouldUpdate = false;

        if (!WarForgeConfig.SIEGE_ENABLE_NEW_TIMER) {
            long siegeDayLength = TimeHelper.getSiegeDayLengthMS();
            long siegeDayNumber = (currTickTimestamp - timestampOfFirstDay) / siegeDayLength;

            if (siegeDayNumber > numberOfSiegeDaysTicked) {
                numberOfSiegeDaysTicked = siegeDayNumber;
                messageAll(new TextComponentString("Battle takes its toll, all sieges have advanced."), true);
                FACTIONS.advanceSiegeDay();
                shouldUpdate = true;
            }
        } else {
            FACTIONS.updateSiegeTimers();
        }

        //Yield timer
        long yieldDayLength = TimeHelper.getYieldDayLengthMs();
        long yieldDayNumber = (currTickTimestamp - timestampOfFirstDay) / yieldDayLength;

        if (yieldDayNumber > numberOfYieldDaysTicked) {
            numberOfYieldDaysTicked = yieldDayNumber;
            messageAll(new TextComponentString("All passive yields have been awarded."), true);
            FACTIONS.advanceYieldDay();
            shouldUpdate = true;
        }

        if (shouldUpdate) {
            PacketTimeUpdates packet = new PacketTimeUpdates();

            if (!WarForgeConfig.SIEGE_ENABLE_NEW_TIMER)
                packet.msTimeOfNextSiegeDay = System.currentTimeMillis() + timeHelper.getTimeToNextSiegeAdvanceMs();

            packet.msTimeOfNextYieldDay = System.currentTimeMillis() + timeHelper.getTimeToNextYieldMs();

            NETWORK.sendToAll(packet);
        }
    }


    @SubscribeEvent
    public void playerInteractBlock(RightClickBlock event) {
        if (WarForgeConfig.BLOCK_ENDER_CHEST) {
            if (!event.getWorld().isRemote) {
                if (event.getWorld().getBlockState(event.getPos()).getBlock() == Blocks.ENDER_CHEST) {
                    event.getEntityPlayer().sendMessage(new TextComponentString("WarForge has disabled Ender Chests"));
                    event.setCanceled(true);

                    event.getWorld().spawnEntity(new EntityItem(event.getWorld(), event.getPos().getX() + 0.5d, event.getPos().getY() + 1.0d, event.getPos().getZ() + 0.5d, new ItemStack(Items.ENDER_EYE)));
                    event.getWorld().spawnEntity(new EntityItem(event.getWorld(), event.getPos().getX() + 0.5d, event.getPos().getY() + 1.0d, event.getPos().getZ() + 0.5d, new ItemStack(Blocks.OBSIDIAN)));
                    event.getWorld().setBlockToAir(event.getPos());
                }
            }
        }
    }

    @SubscribeEvent
    public void playerDied(LivingDeathEvent event) {
        if (event.getEntity().world.isRemote)
            return;

        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            FACTIONS.playerDied((EntityPlayerMP) event.getEntityLiving(), event.getSource());
        }
    }

    private void blockPlacedOrRemoved(BlockEvent event, IBlockState state) {
        // Check for vault value
        if (WarForgeConfig.VAULT_BLOCKS.contains(state.getBlock())) {
            DimChunkPos chunkPos = new DimBlockPos(event.getWorld().provider.getDimension(), event.getPos()).toChunkPos();
            UUID factionID = FACTIONS.getClaim(chunkPos);
            if (!factionID.equals(Faction.nullUuid)) {
                Faction faction = FACTIONS.getFaction(factionID);
                if (faction != null) {
                    if (faction.citadelPos.toChunkPos().equals(chunkPos)) {
                        faction.evaluateVault();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!event.getWorld().isRemote) {
            blockPlacedOrRemoved(event, event.getPlacedBlock());
        }
    }

    @SubscribeEvent
    public void blockRemoved(BlockEvent.BreakEvent event) {
        IBlockState state = event.getState();
        if (isClaim(state.getBlock(), Content.siegeCampBlock)) {
            if (event.getWorld().getTileEntity(event.getPos()) instanceof TileEntityClaim te && te.getFaction().equals(Faction.nullUuid))
                return;
            event.setCanceled(true);
            return;
        }

        if (!event.getWorld().isRemote) {
            if (state.getBlock() == Content.siegeCampBlock) {
                TileEntitySiegeCamp siegeBlock = (TileEntitySiegeCamp) event.getWorld().getTileEntity(event.getPos());
                if (siegeBlock != null) siegeBlock.onDestroyed();
            }

            blockPlacedOrRemoved(event, event.getState());
        }
    }

    @SubscribeEvent
    public void PreBlockPlaced(RightClickBlock event) {
        if (event.getWorld().isRemote) {
            // This is a server op
            return;
        }

        Item item = event.getItemStack().getItem();
        if (!isClaim(item)) {
            // We don't care if its not one of ours
            return;
        }

        Block block = ((ItemBlock) item).getBlock();
        BlockPos placementPos = event.getPos().offset(event.getFace() != null ? event.getFace() : EnumFacing.UP);

        // Only players can place these blocks
        if (!(event.getEntity() instanceof EntityPlayer player)) {
            event.setCanceled(true);
            return;
        }

        Faction playerFaction = FACTIONS.getFactionOfPlayer(player.getUniqueID());
        // TODO : Op override

        // All block placements are cancelled if there is already a block from this mod in that chunk
        DimChunkPos pos = new DimBlockPos(event.getWorld().provider.getDimension(), placementPos).toChunkPos();
        if (!FACTIONS.getClaim(pos).equals(Faction.nullUuid)) {
            // check if claim chunk has actual claim pos, and if not then remove it
            Faction claimingFaction = FACTIONS.getFaction(FACTIONS.getClaim(pos));
            DimBlockPos claimPos = claimingFaction.getSpecificPosForClaim(pos);
            if (claimPos == null) {
                FACTIONS.getClaims().remove(pos);
            } else {
                // check if block is not claim, and if it is marked as claim, but no claim block can be found, then remove phantom claim
                if (!isClaim(event.getWorld().getBlockState(claimPos.toRegularPos()).getBlock())) {
                    FACTIONS.getClaims().remove(claimingFaction.uuid);
                    claimingFaction.onClaimLost(claimPos);
                } else {
                    player.sendMessage(new TextComponentString("This chunk already has a claim"));
                    event.setCanceled(true);
                    return;
                }
            }
        }

        ObjectIntPair<UUID> conqueredChunkInfo = FACTIONS.conqueredChunks.get(pos);
        if (conqueredChunkInfo != null) {
            // remove invalid entries if necessary, and if not then do actual comparison
            if (conqueredChunkInfo.getLeft() == null || conqueredChunkInfo.getLeft().equals(Faction.nullUuid) || FACTIONS.getFaction(conqueredChunkInfo.getLeft()) == null) {
                WarForgeMod.LOGGER.atError().log("Found invalid conquered chunk at " + pos + "; removing and permitting placement.");
                FACTIONS.conqueredChunks.remove(pos);
            } else if (!conqueredChunkInfo.getLeft().equals(playerFaction.uuid)) {
                player.sendMessage(new TextComponentTranslation("warforge.info.chunk_is_conquered",
                        WarForgeMod.FACTIONS.getFaction(FACTIONS.conqueredChunks.get(pos).getLeft()).name,
                        TimeHelper.formatTime(FACTIONS.conqueredChunks.get(pos).getRight())));
                event.setCanceled(true);
                return;
            }
        }

        if (!containsInt(WarForgeConfig.CLAIM_DIM_WHITELIST, pos.dim)) {
            player.sendMessage(new TextComponentString("You cannot claim chunks in this dimension"));
            event.setCanceled(true);
            return;
        }

        // Cancel block placement for a couple of reasons
        if (block == Content.citadelBlock) {
            if (playerFaction != null) // Can't place a second citadel
            {
                player.sendMessage(new TextComponentString("You are already in a faction"));
                event.setCanceled(true);
            }
        } else if (block == Content.basicClaimBlock
                || block == Content.reinforcedClaimBlock) {
            if (playerFaction == null) // Can't expand your claims if you aren't in a faction
            {
                player.sendMessage(new TextComponentString("You aren't in a faction. Craft a citadel or join a faction"));
                event.setCanceled(true);
                return;
            }

            if (!playerFaction.isPlayerRoleInFaction(player.getUniqueID(), Role.OFFICER)) {
                player.sendMessage(new TextComponentString("You are not an officer of your faction"));
                event.setCanceled(true);
                return;
            }

            if (WarForgeConfig.ENABLE_CITADEL_UPGRADES && !playerFaction.canPlaceClaim()) {
                player.sendMessage(new TextComponentString("Your faction reached it's level's claim limit, upgrade the level to incrase the limit"));
                event.setCanceled(true);
            }
        } else // Must be siege block
        {
            if (playerFaction == null) // Can't start sieges if you aren't in a faction
            {
                player.sendMessage(new TextComponentString("You aren't in a faction. Craft a citadel or join a faction"));
                event.setCanceled(true);
                return;
            }

            if (!playerFaction.isPlayerRoleInFaction(player.getUniqueID(), Role.OFFICER)) {
                player.sendMessage(new TextComponentString("You are not an officer of your faction"));
                event.setCanceled(true);
                return;
            }

            if (playerFaction.calcNumSieges() > 2) {
                player.sendMessage(new TextComponentTranslation("warforge.info.too_many_siege_blocks"));
                event.setCanceled(true);
                return;
            }

            ArrayList<DimChunkPos> validTargets = new ArrayList<>(Arrays.asList(new DimChunkPos[4]));
            int numTargets = FACTIONS.getAdjacentClaims(playerFaction.uuid, new DimBlockPos(event.getWorld().provider.getDimension(), event.getPos()), validTargets);
            if (numTargets == 0) {
                player.sendMessage(new TextComponentString("There are no adjacent claims to siege; Siege camp Y level must be w/in " + WarForgeConfig.VERTICAL_SIEGE_DIST + " of target."));
                event.setCanceled(true);
            }

            // TODO: Check for alliances with those claims
        }

    }

    @SubscribeEvent
    public void playerLeftGame(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.player.world.isRemote) {
            Faction playerFaction = FACTIONS.getFactionOfPlayer(event.player.getUniqueID());
            if (FactionStorage.isValidFaction(playerFaction)) {
                playerFaction.onlinePlayerCount -= 1;
            }
        }
    }

    @SubscribeEvent
    public void playerJoinedGame(PlayerLoggedInEvent event) {

        NETWORK.sendTo(WarForgeConfig.createConfigSyncPacket(), (EntityPlayerMP) event.player);
        if (!event.player.world.isRemote) {
            if (Double.isNaN(event.player.posX) || Double.isInfinite(event.player.posX)
                    || Double.isNaN(event.player.posY) || Double.isInfinite(event.player.posY)
                    || Double.isNaN(event.player.posZ) || Double.isInfinite(event.player.posZ)) {
                event.player.posX = 0d;
                event.player.posY = 256d;
                event.player.posZ = 0d;
                event.player.attemptTeleport(0d, 256d, 0d);
                event.player.setDead();
                event.player.world.getSaveHandler().getPlayerNBTManager().writePlayerData(event.player);
                LOGGER.info("Player moved from the void to 0,256,0");
            }

            Faction playerFaction = FACTIONS.getFactionOfPlayer(event.player.getUniqueID());
            if (FactionStorage.isValidFaction(playerFaction)) {
                playerFaction.onlinePlayerCount += 1;
            }

            PacketTimeUpdates packet = new PacketTimeUpdates();

            packet.msTimeOfNextSiegeDay = System.currentTimeMillis() + timeHelper.getTimeToNextSiegeAdvanceMs();
            packet.msTimeOfNextYieldDay = System.currentTimeMillis() + timeHelper.getTimeToNextYieldMs();

            NETWORK.sendTo(packet, (EntityPlayerMP) event.player);

            // sends packet to client which clears all previously remembered sieges; identical attacking and def names = clear packet

            NETWORK.sendTo(Siege.clearSiegeData(), (EntityPlayerMP) event.player);
            FACTIONS.sendAllSiegeInfoToNearby();

            // begin queued sync info
            // don't sync if the upgrade info doesn't exist
            if (UPGRADE_HANDLER.getLEVELS() != null) {

                for (int i = 0; i < UPGRADE_HANDLER.getLEVELS().length; i++) {
                    final int level = i;
                    final HashMap<StackComparable, Integer> requirements = UPGRADE_HANDLER.getLEVELS()[i];
                    final int limit = UPGRADE_HANDLER.getLIMITS()[i];

                    SyncQueueHandler.enqueue(() ->
                            NETWORK.sendTo(new PacketCitadelUpgradeRequirement(level, requirements, limit), (EntityPlayerMP) event.player)
                    );
                }
            }

            // go over all ordered veins that the server has and send them to the client
            for (int veinID = 0; veinID < Vein.orderedVeins.size(); ) {
                PacketVeinEntries currEntries = new PacketVeinEntries().fillFrom(Vein.orderedVeins, veinID);
                veinID += currEntries.entryCount();

                // queue up each compressed packet to be sent
                SyncQueueHandler.enqueue(() -> NETWORK.sendTo(currEntries, (EntityPlayerMP) event.player)
                );
            }
        }
    }

    public UUID getPlayerIDOfDiscordUser(String discordUserID) {
        if (discordUserIdMap.containsKey(discordUserID))
            return discordUserIdMap.get(discordUserID);
        return Faction.nullUuid;
    }

    public void messageAll(ITextComponent msg, boolean sendToDiscord) // TODO: optional list of pings
    {
        if (MC_SERVER != null) {
            for (EntityPlayerMP player : MC_SERVER.getPlayerList().getPlayers()) {
                player.sendMessage(msg);
            }
        }

        NBTTagCompound sendDiscordMessageTagCompound = new NBTTagCompound();
        sendDiscordMessageTagCompound.setString("message", msg.getFormattedText());
        sendDiscordMessageTagCompound.setLong("channel", WarForgeConfig.FACTIONS_BOT_CHANNEL_ID);
        FMLInterModComms.sendRuntimeMessage(this, DISCORD_MODID, "sendMessage", sendDiscordMessageTagCompound);
    }

    private void readFromNBT(NBTTagCompound tags) {
        FACTIONS.readFromNBT(tags);

        timestampOfFirstDay = tags.getLong("zero-timestamp");
        numberOfSiegeDaysTicked = tags.getLong("num-days-elapsed");
        numberOfYieldDaysTicked = tags.getLong("num-yields-awarded");
        serverStopTimestamp = tags.getLong("shutdown-timestamp");
    }

    private void WriteToNBT(NBTTagCompound tags) {
        FACTIONS.WriteToNBT(tags);

        tags.setLong("zero-timestamp", timestampOfFirstDay);
        tags.setLong("num-days-elapsed", numberOfSiegeDaysTicked);
        tags.setLong("num-yields-awarded", numberOfYieldDaysTicked);
        tags.setLong("shutdown-timestamp", serverStopTimestamp);
    }

    @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        MC_SERVER = event.getServer();
        CommandHandler handler = ((CommandHandler) MC_SERVER.getCommandManager());
        handler.registerCommand(new CommandFactions());

        try {
            // try to read from data or backup, then generates a new file if both fail
            File dataFile = getFactionsFile();
            if (!dataFile.isFile()) {
                // try to read from faction backup
                dataFile = getFactionsFileBackup();
                if (!dataFile.isFile()) {
                    dataFile = getFactionsFile(); // ensure path is correct

                    if (!dataFile.getParentFile().exists())
                        return; // only make new file to read from if world folder has been made
                    dataFile.createNewFile(); // create new data file

                    // puts file in correct format with empty tags
                    CompressedStreamTools.writeCompressed(new NBTTagCompound(), new FileOutputStream(dataFile));
                }
            }

            NBTTagCompound tags = CompressedStreamTools.readCompressed(new FileInputStream(dataFile));
            readFromNBT(tags);
            LOGGER.info("Successfully loaded " + dataFile.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to load data from warforgefactions.dat and backup; restart strongly recommended");
            e.printStackTrace();
        }

        currTickTimestamp = System.currentTimeMillis(); // will cause some update time to be registered immediately
    }

    private void save(String event) {
        try {
            if (MC_SERVER != null) {
                NBTTagCompound tags = new NBTTagCompound();
                WriteToNBT(tags);

                File factionsFile = getFactionsFile();
                if (factionsFile.exists()) {
                    Files.copy(factionsFile.toPath(), getFactionsFileBackup().toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    factionsFile.createNewFile();
                }

                CompressedStreamTools.writeCompressed(tags, new FileOutputStream(factionsFile));
                LOGGER.info("Successfully saved warforgefactions.dat on event - " + event);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save warforgefactions.dat");
            e.printStackTrace();
        }
    }

//	@EventHandler
//	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
//		/*
//		EntityPlayer player = event.player;
//		DimBlockPos playerPos = new DimBlockPos(player);
//		if(FACTIONS.isPlayerDefending(player.getUniqueID())){
//			COMBAT_LOG.add(playerPos, player.getUniqueID(), System.currentTimeMillis());
//		}
//		*/
//	}

    // Helpers

    @SubscribeEvent
    public void saveEvent(WorldEvent.Save event) {
        if (!event.getWorld().isRemote) {
            int dimensionID = event.getWorld().provider.getDimension();
            save("World Save - DIM " + dimensionID);
        }
    }

    @EventHandler
    public void serverStopped(FMLServerStoppingEvent event) {
        save("Server Stop");
        MC_SERVER = null;
    }

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.warforge.json");
    }
}
