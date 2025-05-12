package com.flansmod.warforge.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.DimChunkPos;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.IClaim;
import com.flansmod.warforge.common.blocks.TileEntityYieldCollector;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.PlayerDisplayInfo;
import com.flansmod.warforge.server.Leaderboard.FactionStat;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;


/**
 * Data class for faction, responsible for storing faction info
 */
public class Faction {
    public static final UUID nullUuid = new UUID(0, 0);
    private static final float INVITE_DECAY_TIME = 20 * 60 * 5; // 5 minutes, TODO: Config
    public UUID uuid;
    public int onlinePlayerCount = 0; // the number of current online players
    public long lastSiegeTimestamp = 0;
    public String name;
    public DimBlockPos citadelPos;
    public HashMap<DimBlockPos, Integer> claims;
    public HashMap<UUID, PlayerData> members;
    public HashMap<UUID, Float> pendingInvites;
    public HashMap<UUID, Integer> killCounter;
    public boolean loggedInToday;
    public int colour = 0xFF_FF_FF;
    public int notoriety = 0;
    public int wealth = 0;
    public int legacy = 0;
    public short citadelLevel = 0;
    public int citadelMoveCooldown = 1;

    public Faction() {
        members = new HashMap<UUID, PlayerData>();
        pendingInvites = new HashMap<UUID, Float>();
        claims = new HashMap<DimBlockPos, Integer>();
        killCounter = new HashMap<UUID, Integer>();
    }

    public static UUID createUUID(String factionName) {
        return new UUID(0xfedcba0987654321L, ((long) factionName.hashCode()) * 0xa0a0b1b1c2c2d3d3L);
    }

    private static String getPlayerName(UUID playerID) {
        EntityPlayer player = getPlayer(playerID);
        return player.getName();
    }

    // the map used in getPlayerByUUID removes players on logout
    private static EntityPlayer getPlayer(UUID playerID) {
        return WarForgeMod.MC_SERVER.getPlayerList().getPlayerByUUID(playerID);
    }

    public void setLastSiegeTimestamp(long timestamp) {
        lastSiegeTimestamp = timestamp;
    }

    public int getMemberCount() {
        return members.size();
    }


    public void update() {
        UUID uuidToRemove = nullUuid;
        for (HashMap.Entry<UUID, Float> entry : pendingInvites.entrySet()) {
            entry.setValue(entry.getValue() - 1);
            if (entry.getValue() <= 0)
                uuidToRemove = entry.getKey();
        }

        // So this could break if players were sending > 1 unique invite per tick, but why would they do that?
        if (!uuidToRemove.equals(nullUuid))
            pendingInvites.remove(uuidToRemove);

        if (!loggedInToday) {
            for (HashMap.Entry<UUID, PlayerData> kvp : members.entrySet()) {
                loggedInToday = true;
            }
        }
    }

    // array list needed to be able to pre-allocate size, but not know if all players will pass check
    public ArrayList<EntityPlayer> getOnlinePlayers(Predicate<EntityPlayer> playerCondition) {
        ArrayList<EntityPlayer> players = new ArrayList<>(members.size());
        //mMembers.keySet() seems to have a null default
        for (UUID playerID : members.keySet()) {
            EntityPlayer player = getPlayer(playerID);
            if (playerCondition.test(player)) players.add(player);
        }

        return players;
    }


    public void increaseLegacy() {
        if (loggedInToday) {
            legacy += WarForgeConfig.LEGACY_PER_DAY;
        }

//		for(HashMap.Entry<UUID, PlayerData> kvp : mMembers.entrySet())
//		{
//			kvp.getValue().mHasMovedFlagToday = false;
//		}

        loggedInToday = false;
        citadelMoveCooldown--;
    }

    public FactionDisplayInfo createInfo() {
        FactionDisplayInfo info = new FactionDisplayInfo();
        info.factionId = uuid;
        info.factionName = name;
        info.notoriety = notoriety;
        info.wealth = wealth;
        info.legacy = legacy;

        info.legacyRank = WarForgeMod.LEADERBOARD.GetOneIndexedRankOf(this, FactionStat.LEGACY);
        info.notorietyRank = WarForgeMod.LEADERBOARD.GetOneIndexedRankOf(this, FactionStat.NOTORIETY);
        info.wealthRank = WarForgeMod.LEADERBOARD.GetOneIndexedRankOf(this, FactionStat.WEALTH);
        info.totalRank = WarForgeMod.LEADERBOARD.GetOneIndexedRankOf(this, FactionStat.TOTAL);

        info.mNumClaims = claims.size();
        info.mCitadelPos = citadelPos;

        for (HashMap.Entry<UUID, PlayerData> entry : members.entrySet()) {
            if (entry.getValue().role == Role.LEADER)
                info.mLeaderID = entry.getKey();

            PlayerDisplayInfo playerInfo = new PlayerDisplayInfo();
            GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getProfileByUUID(entry.getKey());
            playerInfo.username = profile == null ? "Unknown Player" : profile.getName();
            playerInfo.playerUuid = entry.getKey();
            playerInfo.role = entry.getValue().role;
            info.members.add(playerInfo);
        }
        return info;
    }

    public void invitePlayer(UUID playerID) {
        // Don't invite offline players
        getPlayer(playerID);

        if (pendingInvites.containsKey(playerID))
            pendingInvites.replace(playerID, INVITE_DECAY_TIME);
        else
            pendingInvites.put(playerID, INVITE_DECAY_TIME);
    }

    public boolean isInvitingPlayer(UUID playerID) {
        return pendingInvites.containsKey(playerID);
    }

    public void addPlayer(UUID playerID) {
        members.put(playerID, new PlayerData());
        pendingInvites.remove(playerID);

        // Let everyone know
        messageAll(new TextComponentString(getPlayerName(playerID) + " joined " + name));


        // re-check number of online players
        onlinePlayerCount = getOnlinePlayers(entityPlayer -> true).size();

    }

    // TODO:
    // Rank up
    // Rank down

    public boolean setLeader(UUID playerID) {
        if (!members.containsKey(playerID))
            return false;

        for (HashMap.Entry<UUID, PlayerData> entry : members.entrySet()) {
            // Set the target player as leader
            if (entry.getKey().equals(playerID))
                entry.getValue().role = Role.LEADER;
                // And set any existing leaders to officers
            else if (entry.getValue().role == Role.LEADER)
                entry.getValue().role = Role.OFFICER;
        }


        messageAll(new TextComponentString(getPlayerName(playerID) + " was made leader of " + name));
        return true;
    }

    public void removePlayer(UUID playerID) {
        members.remove(playerID);
    }

    public void disband() {
        // Clean up remaining claims
        for (Map.Entry<DimBlockPos, Integer> kvp : claims.entrySet()) {
            World world = WarForgeMod.MC_SERVER.getWorld(kvp.getKey().dim);
            world.setBlockToAir(kvp.getKey().toRegularPos());
        }

        World world = WarForgeMod.MC_SERVER.getWorld(citadelPos.dim);
        this.citadelLevel = 0;
        world.setBlockToAir(citadelPos.toRegularPos());


        String message = getMemberCount() > 0 ? name + " was disbanded." : name + "was abandoned and disbanded";
        messageAll(new TextComponentString(message));
        members.clear();
        claims.clear();
        pendingInvites.clear();
    }

    public boolean isPlayerInFaction(UUID playerID) {
        return members.containsKey(playerID);
    }

    public boolean canPlaceClaim() {
        int claimLimitForLevel = WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(citadelLevel);
        if (claimLimitForLevel == -1)
            return true;
        else
            return claimLimitForLevel > claims.size();
    }

    public boolean isPlayerRoleInFaction(UUID playerID, Role role) {
        if (members.containsKey(playerID)) {
            Role thierRole = members.get(playerID).role;
            return thierRole.ordinal() >= role.ordinal();
        }
        return false;
    }

    public boolean isPlayerOutrankingOfficer(UUID playerID, UUID targetID) {
        if (members.containsKey(playerID) && members.containsKey(targetID)) {
            Role playerRole = members.get(playerID).role;
            Role targetRole = members.get(targetID).role;
            return playerRole.ordinal() >= Role.OFFICER.ordinal()
                    && playerRole.ordinal() > targetRole.ordinal();
        }
        return false;
    }

    public void onClaimPlaced(IClaim claim) {
        claims.put(claim.getClaimPos(), 0);
    }

    // for methods where claim block is actually being removed
    public void onClaimLost(DimBlockPos claimBlockPos) {
        onClaimLost(claimBlockPos, false);
    }

    // avoids duplication of claim block on siege capture, as the way capture is done is
    // by losing the claim (this method) and then creating another in its place
    public void onClaimLost(DimBlockPos claimBlockPos, boolean captureAttempted) {
        // Destroy our claim block
        World world = WarForgeMod.MC_SERVER.getWorld(claimBlockPos.dim);
        IBlockState claimBlock = world.getBlockState(claimBlockPos.toRegularPos());
        ItemStack drop = new ItemStack(Item.getItemFromBlock(claimBlock.getBlock()));
        world.setBlockToAir(claimBlockPos);
        if (!captureAttempted || !WarForgeConfig.SIEGE_CAPTURE) world.spawnEntity(new EntityItem(world,
                claimBlockPos.getX() + 0.5d, claimBlockPos.getY() + 0.5d,
                claimBlockPos.getZ() + 0.5d, drop));

        // Uh oh
        if (claimBlockPos.equals(citadelPos)) {
            WarForgeMod.FACTIONS.FactionDefeated(this);
            WarForgeMod.INSTANCE.messageAll(new TextComponentString(name + "'s citadel was destroyed. " + name + " is no more."), true);
        } else {
            messageAll(new TextComponentString("Our faction lost a claim at " + claimBlockPos.toFancyString()));

            claims.remove(claimBlockPos);
        }

    }

    public void claimNoTileEntity(DimChunkPos pos) {
        claims.put(new DimBlockPos(pos.mDim, pos.getXStart(), 0, pos.getZStart()), 0);
    }

//	public boolean placeFlag(EntityPlayer player, DimBlockPos claimPos)
//	{
//		if(!claims.containsKey(claimPos))
//		{
//			player.sendMessage(new TextComponentString("Your faction does not own this claim"));
//			return false;
//		}
//
//		TileEntity te = WarForgeMod.MC_SERVER.getWorld(claimPos.dim).getTileEntity(claimPos.toRegularPos());
//		if(!(te instanceof IClaim))
//		{
//			player.sendMessage(new TextComponentString("Internal error"));
//			WarForgeMod.LOGGER.error("Faction claim could not get tile entity");
//			return false;
//		}
//
//		PlayerData data = members.get(player.getUniqueID());
//		if(data == null)
//		{
//			player.sendMessage(new TextComponentString("Your faction member data is corrupt"));
//			return false;
//		}
//
//		//if(data.mHasMovedFlagToday)
//		if(!canPlayerMoveFlag(player.getUniqueID()))
//		{
//			player.sendMessage(new TextComponentString("You have already moved your flag today. Check /f time"));
//			return false;
//		}
//
//		if(data.flagPosition.equals(claimPos))
//		{
//			player.sendMessage(new TextComponentString("Your flag is already here"));
//			return false;
//		}
//
//		// Clean up old pos
//		if(!data.flagPosition.equals(DimBlockPos.ZERO))
//		{
//			TileEntity oldTE = WarForgeMod.MC_SERVER.getWorld(data.flagPosition.dim)
//					.getTileEntity(data.flagPosition.toRegularPos());
//			if(oldTE instanceof IClaim)
//			{
//				((IClaim)oldTE).onServerRemovePlayerFlag(player.getName());
//			}
//		}
//
//		data.flagPosition = claimPos;
//		//data.mHasMovedFlagToday = true;
//		data.addCooldown();
//		((IClaim)te).onServerSetPlayerFlag(player.getName());
//		messageAll(new TextComponentString(player.getName() + " placed their flag at " + claimPos.toFancyString()));
//		player.sendMessage(new TextComponentString("Your flag can move again on the next siege day"));
//
//		return true;
//	}

    // Messaging
    public void messageAll(ITextComponent chat) {
        for (UUID playerID : members.keySet()) {
            final EntityPlayer player = getPlayer(playerID);
            player.sendMessage(chat);
        }
    }

    public DimBlockPos getSpecificPosForClaim(DimChunkPos pos) {
        for (DimBlockPos claimPos : claims.keySet()) {
            if (claimPos.toChunkPos().equals(pos))
                return claimPos;
        }
        return null;
    }

    public void evaluateVault() {
        World world = WarForgeMod.MC_SERVER.getWorld(citadelPos.dim);
        DimChunkPos chunkPos = citadelPos.toChunkPos();

        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    BlockPos blockPos = chunkPos.getBlock(x, y, z);
                    IBlockState state = world.getBlockState(blockPos);
                    if (WarForgeConfig.VAULT_BLOCKS.contains(state.getBlock()))
                        count++;
                }
            }
        }

        wealth = count;
    }

    public void awardYields() {
        //
        for (HashMap.Entry<DimBlockPos, Integer> kvp : claims.entrySet()) {
            DimBlockPos pos = kvp.getKey();
            World world = WarForgeMod.MC_SERVER.getWorld(pos.dim);

            // If It's loaded, process immediately
            if (world.isBlockLoaded(pos)) {
                TileEntity te = world.getTileEntity(pos.toRegularPos());
                if (te instanceof TileEntityYieldCollector) {
                    ((TileEntityYieldCollector) te).processYield(1);
					kvp.setValue(0);
                }
            }
            // Otherwise, cache the number of times it needs to process when it next loads
            else {
                kvp.setValue(kvp.getValue() + 1);
            }
        }
    }

    public void promote(UUID playerID) {
        PlayerData data = members.get(playerID);
        if (data != null) {
            if (data.role == Role.MEMBER) {
                data.role = Role.OFFICER;
                GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getProfileByUUID(playerID);
                if (profile != null)
                    messageAll(new TextComponentString(profile.getName() + " was promoted to officer"));
            }
        }
    }

    public void demote(UUID playerID) {
        PlayerData data = members.get(playerID);
        if (data != null) {
            if (data.role == Role.OFFICER) {
                data.role = Role.MEMBER;
                GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getProfileByUUID(playerID);
                if (profile != null)
                    messageAll(new TextComponentString(profile.getName() + " was demoted to member"));
            }
        }
    }


    public void setColour(int colour) {
        this.colour = colour;
    }
	public void AwardYields()
	{
		//
		for(HashMap.Entry<DimBlockPos, Integer> kvp : mClaims.entrySet())
		{
			DimBlockPos pos = kvp.getKey();
			World world = WarForgeMod.MC_SERVER.getWorld(pos.mDim);

			// If It's loaded, process immediately
			if(world.isBlockLoaded(pos))
			{
				TileEntity te = world.getTileEntity(pos.ToRegularPos());
				if(te instanceof TileEntityYieldCollector)
				{
					((TileEntityYieldCollector)te).ProcessYield(1);
					kvp.setValue(0);  // no claims saved
				}
			}
			// Otherwise, cache the number of times it needs to process when it next loads
			else
			{
				kvp.setValue(kvp.getValue() + 1);
			}
		}
	}

	public void Promote(UUID playerID)
	{
		PlayerData data = mMembers.get(playerID);
		if(data != null)
		{
			if(data.mRole == Role.MEMBER)
			{
				data.mRole = Role.OFFICER;
				GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getProfileByUUID(playerID);
				if(profile != null)
					MessageAll(new TextComponentString(profile.getName() + " was promoted to officer"));
			}
		}
	}

	public void Demote(UUID playerID)
	{
		PlayerData data = mMembers.get(playerID);
		if(data != null)
		{
			if(data.mRole == Role.OFFICER)
			{
				data.mRole = Role.MEMBER;
				GameProfile profile = WarForgeMod.MC_SERVER.getPlayerProfileCache().getProfileByUUID(playerID);
				if(profile != null)
					MessageAll(new TextComponentString(profile.getName() + " was demoted to member"));
			}
		}
	}

	public boolean CanPlayerMoveFlag(UUID uniqueID)
	{
		PlayerData data = mMembers.get(uniqueID);
		if(data != null)
		{
			//return !data.mHasMovedFlagToday;
		if(data.mMoveFlagCooldown - System.currentTimeMillis() <= 0)
				return true;
		}
		return false;
	}

	public void SetColour(int colour)
	{
		mColour = colour;
	}


    public void readFromNBT(NBTTagCompound tags) {
        claims.clear();
        members.clear();

        // Get citadel pos and defining params
        uuid = tags.getUniqueId("uuid");
        name = tags.getString("name");
        colour = tags.getInteger("colour");
        citadelLevel = tags.getShort("citadel_lvl");

        // Get our claims and citadel
        citadelPos = DimBlockPos.readFromNBT(tags, "citadelPos");


        NBTTagList claimList = tags.getTagList("claims", 10); // CompoundTag (see NBTBase.class)
        for (NBTBase base : claimList) {
            NBTTagCompound claimInfo = (NBTTagCompound) base;
            DimBlockPos pos = DimBlockPos.readFromNBT((NBTTagIntArray) claimInfo.getTag("pos"));
            int pendingYields = claimInfo.getInteger("pendingYields");
            claims.put(pos, pendingYields);
        }
        if (!claims.containsKey(citadelPos)) {
            WarForgeMod.LOGGER.error("Citadel was not claimed by the faction. Forcing claim");
            claims.put(citadelPos, 0);
        }

        NBTTagList killList = tags.getTagList("kills", 10); // CompoundTag (see NBTBase.class)
        for (NBTBase base : killList) {
            NBTTagCompound killInfo = (NBTTagCompound) base;
            UUID uuid = killInfo.getUniqueId("id");
            int kills = killInfo.getInteger("count");

            killCounter.put(uuid, kills);
        }


        // Get gameplay params
        notoriety = tags.getInteger("notoriety");
        wealth = tags.getInteger("wealth");
        legacy = tags.getInteger("legacy");

        citadelMoveCooldown = tags.getInteger("citadelMoveCooldown");

        // Get member data
        NBTTagList memberList = tags.getTagList("members", 10); // NBTTagCompound (see NBTBase.class)
        for (NBTBase base : memberList) {
            NBTTagCompound memberTags = (NBTTagCompound) base;
            UUID uuid = memberTags.getUniqueId("uuid");
            PlayerData data = new PlayerData();
            data.readFromNBT(memberTags);
            members.put(uuid, data);

            // Fixup for old data
            if (data.flagPosition.equals(DimBlockPos.ZERO)) {
                data.flagPosition = citadelPos;
            }
        }
    }

    public void writeToNBT(NBTTagCompound tags) {
        // Set citadel pos and core params
        tags.setUniqueId("uuid", uuid);
        tags.setString("name", name);
        tags.setInteger("colour", colour);
        tags.setShort("citadel_lvl", citadelLevel);

        // Set claims
        NBTTagList claimsList = new NBTTagList();
        for (HashMap.Entry<DimBlockPos, Integer> kvp : claims.entrySet()) {
            NBTTagCompound claimTags = new NBTTagCompound();
            claimTags.setTag("pos", kvp.getKey().writeToNBT());
            claimTags.setInteger("pendingYields", kvp.getValue());

            claimsList.appendTag(claimTags);
        }
        tags.setTag("claims", claimsList);
        citadelPos.writeToNBT(tags, "citadelPos");

        NBTTagList killsList = new NBTTagList();
        for (HashMap.Entry<UUID, Integer> kvp : killCounter.entrySet()) {
            NBTTagCompound killTags = new NBTTagCompound();
            killTags.setUniqueId("id", kvp.getKey());
            killTags.setInteger("count", kvp.getValue());

            killsList.appendTag(killTags);
        }
        tags.setTag("kills", killsList);

        // Set gameplay params
        tags.setInteger("notoriety", notoriety);
        tags.setInteger("wealth", wealth);
        tags.setInteger("legacy", legacy);

        tags.setInteger("citadelMoveCooldown", citadelMoveCooldown);

        // Add member data
        NBTTagList memberList = new NBTTagList();
        for (HashMap.Entry<UUID, PlayerData> kvp : members.entrySet()) {
            NBTTagCompound memberTags = new NBTTagCompound();
            memberTags.setUniqueId("uuid", kvp.getKey());
            kvp.getValue().writeToNBT(memberTags);
            memberList.appendTag(memberTags);
        }
        tags.setTag("members", memberList);
    }

    // checks all stored claim locations to check if they are siege blocks
    public int calcNumSieges() {
        int result = 0;
        for (DimBlockPos claimPos : claims.keySet())
            if (WarForgeMod.FACTIONS.getSieges().get(claimPos) != null) ++result;
        return result;
    }

    public enum Role {
        GUEST, // ?
        MEMBER,
        OFFICER,
        LEADER,
    }

    public static class PlayerData {
        public Faction.Role role = Faction.Role.MEMBER;
        @Deprecated
        public DimBlockPos flagPosition = DimBlockPos.ZERO;
        //public boolean mHasMovedFlagToday = false;
        @Deprecated
        public long moveFlagCooldown = 0; // in ms

        public void readFromNBT(NBTTagCompound tags) {
            // Read and write role by string so enum order can change
            role = Faction.Role.valueOf(tags.getString("role"));
            //mHasMovedFlagToday = tags.getBoolean("movedFlag");
            moveFlagCooldown = tags.getLong("flagCooldown");
            flagPosition = new DimBlockPos(
                    tags.getInteger("dim"),
                    tags.getInteger("x"),
                    tags.getInteger("y"),
                    tags.getInteger("z"));
        }

        public void writeToNBT(NBTTagCompound tags) {
            tags.setString("role", role.name());
            //tags.setBoolean("movedFlag", mHasMovedFlagToday);
            tags.setLong("flagCooldown", moveFlagCooldown);
            tags.setInteger("dim", flagPosition.dim);
            tags.setInteger("x", flagPosition.getX());
            tags.setInteger("y", flagPosition.getY());
            tags.setInteger("z", flagPosition.getZ());
        }

        public void addCooldown() {
            moveFlagCooldown = System.currentTimeMillis() + (long) (WarForgeConfig.FLAG_COOLDOWN * 60 * 1000);
        }
    }
}
