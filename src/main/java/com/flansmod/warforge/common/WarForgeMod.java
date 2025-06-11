package com.flansmod.warforge.common;

import com.flansmod.warforge.api.ObjectIntPair;
import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.VeinKey;
import com.flansmod.warforge.client.PlayerNametagCache;
import com.flansmod.warforge.common.blocks.IMultiBlockInit;
import com.flansmod.warforge.common.blocks.TileEntitySiegeCamp;
import com.flansmod.warforge.common.effect.EffectRegistry;
import com.flansmod.warforge.common.network.*;
import com.flansmod.warforge.common.potions.PotionsModule;
import com.flansmod.warforge.server.*;
import com.flansmod.warforge.server.Faction.Role;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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
    // Discord integration
    private static final String DISCORD_MODID = "discordintegration";
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
    private static HashMap<String, UUID> discordUserIdMap = new HashMap<String, UUID>();

    public static boolean containsInt(final int[] base, int compare) {
        return Arrays.stream(base).anyMatch(i -> i == compare);
    }

    public static boolean isClaim(Item item) {
        return item != null && (item.equals(CONTENT.citadelBlockItem)
                || item.equals(CONTENT.basicClaimBlockItem)
                || item.equals(CONTENT.reinforcedClaimBlockItem)
                || item.equals(CONTENT.siegeCampBlockItem));
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

        return !matchesAnyInvalidBlocks && (block.equals(CONTENT.citadelBlock)
                || block.equals(CONTENT.basicClaimBlock)
                || block.equals(CONTENT.reinforcedClaimBlock)
                || block.equals(CONTENT.siegeCampBlock)
                || block.equals(CONTENT.statue)
                || block.equals(CONTENT.dummyTranslusent));
    }

    public static String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        int hours = ((int) minutes) / 60;
        int days = hours / 24;

        // ensure entire time left is not represented in each format, but rather only leftover amounts for that unit

        seconds -= minutes * 60;
        minutes -= hours * 60;
        hours -= days * 24;

        StringBuilder timeBuilder = new StringBuilder();
        if (days > 0) timeBuilder.append(days).append("d ");
        if (hours > 0) timeBuilder.append(hours).append("h ");
        if (minutes > 0) timeBuilder.append(minutes).append("min ");
        if (seconds > 0) timeBuilder.append(seconds).append("s ");
        timeBuilder.append(ms % 1000).append("ms");

        return timeBuilder.toString();
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
        if(!veinList.isEmpty() && veinList != null)
            VeinKey.populateVeinMap(WarForgeMod.VEIN_MAP, veinList);

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            NAMETAG_CACHE = new PlayerNametagCache(60_000, 200);
        }


    }

    public long getSiegeDayLengthMS() {
        return (long) (
                WarForgeConfig.SIEGE_DAY_LENGTH // In hours
                        * 60f // In minutes
                        * 60f // In seconds
                        * 1000f); // In milliseconds
    }

    public long getYieldDayLengthMs() {
        return (long) (
                WarForgeConfig.YIELD_DAY_LENGTH // In hours
                        * 60f // In minutes
                        * 60f // In seconds
                        * 1000f); // In milliseconds
    }

    public long getCooldownIntoTicks(float cooldown) {
        // From Minutes
        return (long) (
                cooldown
                        * 60L // Minutes -> Seconds
                        * 20L // Seconds -> Ticks
        );
    }

    public long getCooldownRemainingSeconds(float cooldown, long startOfCooldown) {
        long ticks = (long) cooldown; // why did you convert it into ticks if its already in ticks
        long elapsed = startOfCooldown - ticks;

        return (long) (
                (serverTick - elapsed) * 20
        );
    }

    public int getCooldownRemainingMinutes(float cooldown, long startOfCooldown) {
        long ticks = (long) cooldown;
        long elapsed = startOfCooldown - ticks;

        return (int) (
                (serverTick - elapsed) * 20 / 60
        );
    }

    public int getCooldownRemainingHours(float cooldown, long startOfCooldown) {
        long ticks = (long) cooldown;
        long elapsed = startOfCooldown - ticks;

        return (int) (
                (serverTick - elapsed) * 20 / 60 / 60
        );
    }

    public long getTimeToNextSiegeAdvanceMs() {
        long elapsedMS = System.currentTimeMillis() - timestampOfFirstDay;
        long todayElapsedMS = elapsedMS % getSiegeDayLengthMS();

        return getSiegeDayLengthMS() - todayElapsedMS;
    }

    public long getTimeToNextYieldMs() {
        long elapsedMS = System.currentTimeMillis() - timestampOfFirstDay;
        long todayElapsedMS = elapsedMS % getYieldDayLengthMs();

        return getYieldDayLengthMs() - todayElapsedMS;
    }

    public void updateServer() {
        boolean shouldUpdate = false;
        previousUpdateTimestamp = currTickTimestamp;
        currTickTimestamp = System.currentTimeMillis();
        long dayLength = getSiegeDayLengthMS();

        FACTIONS.updateConqueredChunks(currTickTimestamp);

        long dayNumber = (currTickTimestamp - timestampOfFirstDay) / dayLength;

        ++serverTick;

        if (dayNumber > numberOfSiegeDaysTicked) {
            // Time to tick a new day
            numberOfSiegeDaysTicked = dayNumber;

            messageAll(new TextComponentString("Battle takes its toll, all sieges have advanced."), true);

            FACTIONS.advanceSiegeDay();
            shouldUpdate = true;
        }

        dayLength = getYieldDayLengthMs();
        dayNumber = (currTickTimestamp - timestampOfFirstDay) / dayLength;

        if (dayNumber > numberOfYieldDaysTicked) {
            // Time to tick a new day
            numberOfYieldDaysTicked = dayNumber;

            messageAll(new TextComponentString("All passive yields have been awarded."), true);

            FACTIONS.advanceYieldDay();
            shouldUpdate = true;
        }

        //COMBAT_LOG.doEnforcements(System.currentTimeMillis());

        if (shouldUpdate) {
            PacketTimeUpdates packet = new PacketTimeUpdates();

            packet.msTimeOfNextSiegeDay = System.currentTimeMillis() + getTimeToNextSiegeAdvanceMs();
            packet.msTimeOfNextYieldDay = System.currentTimeMillis() + getTimeToNextYieldMs();

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
        if (isClaim(state.getBlock(), CONTENT.siegeCampBlock)) {
            event.setCanceled(true);
            return;
        }

        if (!event.getWorld().isRemote) {
            if (state.getBlock() == CONTENT.siegeCampBlock) {
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
                        formatTime(FACTIONS.conqueredChunks.get(pos).getRight())));
                event.setCanceled(true);
                return;
            }
        }

        if (!containsInt(WarForgeConfig.CLAIM_DIM_WHITELIST, pos.mDim)) {
            player.sendMessage(new TextComponentString("You cannot claim chunks in this dimension"));
            event.setCanceled(true);
            return;
        }

        // Cancel block placement for a couple of reasons
        if (block == CONTENT.citadelBlock) {
            if (playerFaction != null) // Can't place a second citadel
            {
                player.sendMessage(new TextComponentString("You are already in a faction"));
                event.setCanceled(true);
                return;
            }
        } else if (block == CONTENT.basicClaimBlock
                || block == CONTENT.reinforcedClaimBlock) {
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
                return;
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

/*
    		if(!playerFaction.CanPlayerMoveFlag(player.getUniqueID()))
    		{
    			player.sendMessage(new TextComponentString("You have already moved your flag today. Check /f time"));
    			event.setCanceled(true);
    			return;
    		}
 */

            if (playerFaction.calcNumSieges() > 2) {
                player.sendMessage(new TextComponentTranslation("warforge.info.too_many_siege_blocks"));
                event.setCanceled(true);
                return;
            }

            ArrayList<DimChunkPos> validTargets = new ArrayList<>(Arrays.asList(new DimChunkPos[4]));
            int numTargets = FACTIONS.GetAdjacentClaims(playerFaction.uuid, new DimBlockPos(event.getWorld().provider.getDimension(), event.getPos()), validTargets);
            if (numTargets == 0) {
                player.sendMessage(new TextComponentString("There are no adjacent claims to siege; Siege camp Y level must be w/in " + WarForgeConfig.VERTICAL_SIEGE_DIST + " of target."));
                event.setCanceled(true);
                return;
            }

            // TODO: Check for alliances with those claims
        }

    }
    
/*
    @EventHandler
    public void IMCEvent(FMLInterModComms.IMCEvent event)
    {
        for (final FMLInterModComms.IMCMessage imc : event.getMessages())
        {
        	//JSON.parseRaw(imc.getStringValue()).;
        	//Gson gson = new Gson();
        	//gson.
        }
    }
*/

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

            packet.msTimeOfNextSiegeDay = System.currentTimeMillis() + getTimeToNextSiegeAdvanceMs();
            packet.msTimeOfNextYieldDay = System.currentTimeMillis() + getTimeToNextYieldMs();

            NETWORK.sendTo(packet, (EntityPlayerMP) event.player);

            // sends packet to client which clears all previously remembered sieges; identical attacking and def names = clear packet
            PacketSiegeCampProgressUpdate clearSiegesPacket = new PacketSiegeCampProgressUpdate();
            clearSiegesPacket.info = new SiegeCampProgressInfo();
            clearSiegesPacket.info.expiredTicks = 0;
            clearSiegesPacket.info.attackingName = "c"; // normally attacking and def names cannot be identical
            clearSiegesPacket.info.defendingName = "c";
            clearSiegesPacket.info.attackingPos = DimBlockPos.ZERO;
            clearSiegesPacket.info.defendingPos = DimBlockPos.ZERO;
            NETWORK.sendTo(clearSiegesPacket, (EntityPlayerMP) event.player);
            FACTIONS.sendAllSiegeInfoToNearby();

            // begin queued sync info
            // don't sync if the upgrade info doesn't exist
            if (UPGRADE_HANDLER != null && UPGRADE_HANDLER.getLEVELS() != null) {

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
                SyncQueueHandler.enqueue(() -> {
                            NETWORK.sendTo(currEntries, (EntityPlayerMP) event.player);
                        }
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
    }

    private void WriteToNBT(NBTTagCompound tags) {
        FACTIONS.WriteToNBT(tags);

        tags.setLong("zero-timestamp", timestampOfFirstDay);
        tags.setLong("num-days-elapsed", numberOfSiegeDaysTicked);
        tags.setLong("num-yields-awarded", numberOfYieldDaysTicked);
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
