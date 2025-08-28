package com.flansmod.warforge.server;

import com.flansmod.warforge.api.interfaces.IClaimStrengthModifier;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.IClaim;
import com.flansmod.warforge.common.blocks.TileEntitySiegeCamp;
import com.flansmod.warforge.common.network.PacketSiegeCampProgressUpdate;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.UUID;

public class Siege {
    public static final FactionStorage FACTION_STORAGE = WarForgeMod.FACTIONS;
    public UUID attackingFaction;
    public UUID defendingFaction;
    public ArrayList<DimBlockPos> attackingCamps;
    public DimBlockPos defendingClaim;
    public long timeRemainingMillis;
    public long siegeEndTimeStamp = 999L;
    public boolean finished = false; //Used for controlling whenever siege has been concluded, Does not require saving

    public int mBaseDifficulty = 5;

    /**
     * The base progress comes from passive sources and must be recalculated whenever checking progress.
     * Sources for the attackers are:
     * - Additional siege camps
     * Sources of the defenders are:
     * - Adjacent claims with differing support strengths
     * - Defender's flags on the defended claim
     */
    private int mExtraDifficulty = 0;
    /**
     * The attack progress is accumulated over time based on active actions in the area of the siege
     * Sources for the attackers are:
     * - Defender deaths in or around the siege
     * - Elapsed days with no defender logins
     * - Elapsed days (there is a constant pressure from the attacker that will eventually wear down the defenders unless they push back)
     * Sources for the defenders are:
     * - Attacker deaths in or around the siege
     * - Elapsed days with no attacker logins
     */
    private int mAttackProgress = 0;

    public Siege() {
        attackingCamps = new ArrayList<>(4);
    }

    public Siege(UUID attacker, UUID defender, DimBlockPos defending, long time) {
        attackingCamps = new ArrayList<>(4);
        attackingFaction = attacker;
        defendingFaction = defender;
        defendingClaim = defending;
        this.timeRemainingMillis = time;

        TileEntity te = WarForgeMod.MC_SERVER.getWorld(defending.dim).getTileEntity(defending.toRegularPos());
        if (te instanceof IClaim) {
            mBaseDifficulty = ((IClaim) te).getDefenceStrength();
        }
    }

    public static boolean isPlayerInRadius(DimChunkPos centerChunkPos, DimChunkPos playerChunkPos) {
        return isPlayerInRadius(centerChunkPos, playerChunkPos, 1);
    }

    public static boolean isPlayerInRadius(DimChunkPos centerChunkPos, DimChunkPos playerChunkPos, int radius) {
        if (playerChunkPos.dim != centerChunkPos.dim) return false;

        // Check if the player's chunk coordinates are within a 3x3 chunk area
        int minChunkX = centerChunkPos.x - radius;
        int maxChunkX = centerChunkPos.x + radius;
        int minChunkZ = centerChunkPos.z - radius;
        int maxChunkZ = centerChunkPos.z + radius;

        // Check if the player's chunk coordinates are within the 3x3 area
        return (playerChunkPos.x >= minChunkX && playerChunkPos.x <= maxChunkX)
                && (playerChunkPos.z >= minChunkZ && playerChunkPos.z <= maxChunkZ);
    }

    // sends packet to client which clears all previously remembered sieges; identical attacking and def names = clear packet
    public static PacketSiegeCampProgressUpdate clearSiegeData() {
        PacketSiegeCampProgressUpdate clearSiegesPacket = new PacketSiegeCampProgressUpdate();
        clearSiegesPacket.info = new SiegeCampProgressInfo();
        clearSiegesPacket.info.expiredTicks = 0;
        clearSiegesPacket.info.attackingName = "c"; // normally attacking and def names cannot be identical
        clearSiegesPacket.info.defendingName = "c";
        clearSiegesPacket.info.attackingPos = DimBlockPos.ZERO;
        clearSiegesPacket.info.defendingPos = DimBlockPos.ZERO;

        return clearSiegesPacket;
    }

    // Attack progress starts at 0 and can be moved to -5 or mAttackSuccessThreshold
    public int GetAttackProgress() {
        return mAttackProgress;
    }

    public void setAttackProgress(int progress) {
        mAttackProgress = progress;
    }

    public int GetDefenceProgress() {
        return -mAttackProgress;
    }

    public int GetAttackSuccessThreshold() {
        return mBaseDifficulty + mExtraDifficulty;
    }

    public boolean isCompleted() {
        boolean endByAttack = GetAttackProgress() >= GetAttackSuccessThreshold();
        boolean endByDef = GetDefenceProgress() >= 5;

        TileEntitySiegeCamp abandonedCamp = hasAbandonedSieges();

        // if a siege could complete, but an abandoned camp is stopping it from happening, notify the attackers
        if (!endByDef && endByAttack && abandonedCamp != null && (WarForgeMod.currTickTimestamp % 60000 > 30000)) {
            Faction attacking = WarForgeMod.FACTIONS.getFaction(attackingFaction);
            attacking.messageAll(new TextComponentString(
                    "Passing of siege delayed due to abandon timer greater than 0 [" +
                            abandonedCamp.getAttackerAbandonTickTimer() + " ticks @ " + abandonedCamp.getPos() +
                            "]; ensure abandon timer is 0 to complete siege."));
        }

        return endByDef || (abandonedCamp == null && endByAttack);
    }

    // ensures attackers are within warzone before siege completes
    public TileEntitySiegeCamp hasAbandonedSieges() {
        Faction attacking = WarForgeMod.FACTIONS.getFaction(attackingFaction);
        ArrayList<TileEntitySiegeCamp> abandonedCamps = new ArrayList<>();

        for (DimBlockPos siegeCampPos : attackingCamps) {
            if (siegeCampPos == null) continue;
            // YOU WILL GET INCOMPREHENSIBLE ERRORS IF YOU DO NOT FOLLOW THE BELOW CONVERSION TO REGULAR POS
            TileEntity siegeCamp = WarForgeMod.MC_SERVER.getWorld(siegeCampPos.dim).getTileEntity(siegeCampPos.toRegularPos());
            if (siegeCamp instanceof TileEntitySiegeCamp) {
                int attackerAbandonTimer = ((TileEntitySiegeCamp) siegeCamp).getAttackerAbandonTickTimer();
                if (attackerAbandonTimer > 0) {
                    abandonedCamps.add((TileEntitySiegeCamp) siegeCamp);
                }
            }
        }

        if (abandonedCamps.size() == 0) {
            return null;
        }

        int largestAbandonTimer = 0;
        var largestAbandonTE = abandonedCamps.get(0);
        for (var TE : abandonedCamps) {
            int currTimer = TE.getAttackerAbandonTickTimer();
            if (currTimer > largestAbandonTimer) {
                largestAbandonTimer = currTimer;
                largestAbandonTE = TE;
            }
        }

        return largestAbandonTE;
    }

    public boolean WasSuccessful() {
        return GetAttackProgress() >= GetAttackSuccessThreshold();
    }

    public SiegeCampProgressInfo GetSiegeInfo() {
        Faction attackers = WarForgeMod.FACTIONS.getFaction(attackingFaction);
        Faction defenders = WarForgeMod.FACTIONS.getFaction(defendingFaction);

        if (attackers == null || defenders == null) {
            WarForgeMod.LOGGER.error("Invalid factions in siege. Can't display info");
            return null;
        }

        SiegeCampProgressInfo info = new SiegeCampProgressInfo();
        info.attackingPos = attackingCamps.get(0);
        info.attackingName = attackers.name;
        info.attackingColour = attackers.colour;
        info.defendingPos = defendingClaim;
        info.defendingName = defenders.name;
        info.defendingColour = defenders.colour;
        info.progress = GetAttackProgress();
        info.completionPoint = GetAttackSuccessThreshold();
        info.timeProgress = timeRemainingMillis;
        info.endTimestamp = siegeEndTimeStamp;
        info.finished = finished;

        return info;
    }

    public boolean start() {
        Faction attackers = WarForgeMod.FACTIONS.getFaction(attackingFaction);
        Faction defenders = WarForgeMod.FACTIONS.getFaction(defendingFaction);
        recalculateEndTimestamp();

        if (attackers == null || defenders == null) {
            WarForgeMod.LOGGER.error("Invalid factions in siege. Cannot start");
            return false;
        }

        calculateBasePower();
        defenders.isCurrentlyDefending = true;
        WarForgeMod.INSTANCE.messageAll(new TextComponentString(attackers.name + " started a siege against " + defenders.name), true);
        WarForgeMod.FACTIONS.sendSiegeInfoToNearby(defendingClaim.toChunkPos());
        return true;
    }

    private void recalculateEndTimestamp() {
        siegeEndTimeStamp = System.currentTimeMillis() + timeRemainingMillis;
    }

    public void updateSiegeTimer() {
        if (timeRemainingMillis <= 0) {
            mAttackProgress += WarForgeConfig.SIEGE_SWING_PER_DAY_ELAPSED_BASE;

            long momentumTime = WarForgeConfig.SIEGE_MOMENTUM_TIME
                    .get(WarForgeMod.FACTIONS.getFaction(attackingFaction).getSiegeMomentum()) * 1000L;
            timeRemainingMillis = momentumTime;

            siegeEndTimeStamp = System.currentTimeMillis() + timeRemainingMillis;

            WarForgeMod.FACTIONS.sendSiegeInfoToNearby(defendingClaim.toChunkPos());
        } else {
            timeRemainingMillis -= 50L;

            long actualRemaining = siegeEndTimeStamp - System.currentTimeMillis();
            if (Math.abs(actualRemaining - timeRemainingMillis) > 1000L) {
                timeRemainingMillis = actualRemaining;
            }
        }
    }

    public void AdvanceDay() {
        Faction attackers = WarForgeMod.FACTIONS.getFaction(attackingFaction);
        Faction defenders = WarForgeMod.FACTIONS.getFaction(defendingFaction);

        if (attackers == null || defenders == null) {
            WarForgeMod.LOGGER.error("Invalid factions in siege.");
            return;
        }

        calculateBasePower();
        float totalSwing = 0.0f;
        totalSwing += WarForgeConfig.SIEGE_SWING_PER_DAY_ELAPSED_BASE;
        if (!defenders.loggedInToday)
            totalSwing += WarForgeConfig.SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS;
        if (!attackers.loggedInToday)
            totalSwing -= WarForgeConfig.SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS;

        mAttackProgress += totalSwing;

        if (totalSwing > 0) {
            attackers.messageAll(new TextComponentString("Your siege on " + defenders.name + " at " + defendingClaim.toFancyString() + " shifted " + totalSwing + " points in your favour. The progress is now at " + GetAttackProgress() + "/" + mBaseDifficulty));
            defenders.messageAll(new TextComponentString("The siege on " + defendingClaim.toFancyString() + " by " + attackers.name + " shifted " + totalSwing + " points in their favour. The progress is now at " + GetAttackProgress() + "/" + mBaseDifficulty));
        } else if (totalSwing < 0) {
            defenders.messageAll(new TextComponentString("The siege on " + defendingClaim.toFancyString() + " by " + attackers.name + " shifted " + -totalSwing + " points in your favour. The progress is now at " + GetAttackProgress() + "/" + mBaseDifficulty));
            attackers.messageAll(new TextComponentString("Your siege on " + defenders.name + " at " + defendingClaim.toFancyString() + " shifted " + -totalSwing + " points in their favour. The progress is now at " + GetAttackProgress() + "/" + mBaseDifficulty));
        } else {
            defenders.messageAll(new TextComponentString("The siege on " + defendingClaim.toFancyString() + " by " + attackers.name + " did not shift today. The progress is at " + GetAttackProgress() + "/" + mBaseDifficulty));
            attackers.messageAll(new TextComponentString("Your siege on " + defenders.name + " at " + defendingClaim.toFancyString() + " did not shift today. The progress is at " + GetAttackProgress() + "/" + mBaseDifficulty));
        }

        FACTION_STORAGE.sendSiegeInfoToNearby(defendingClaim.toChunkPos());
    }

    public void calculateBasePower() {
        Faction attackers = WarForgeMod.FACTIONS.getFaction(attackingFaction);
        Faction defenders = WarForgeMod.FACTIONS.getFaction(defendingFaction);

        if (attackers == null || defenders == null || WarForgeMod.MC_SERVER == null) {
            WarForgeMod.LOGGER.error("Invalid factions in siege.");
            return;
        }

        // Add a point for each defender flag in place
        mExtraDifficulty = defenders.members.entrySet().size() * WarForgeConfig.SIEGE_DIFF_PER_MEMBER;
        if (mExtraDifficulty > 5) {
            mExtraDifficulty = 5;
        }  // cap at 5

        DimChunkPos defendingChunk = defendingClaim.toChunkPos();
        for (EnumFacing direction : EnumFacing.HORIZONTALS) {
            DimChunkPos checkChunk = defendingChunk.Offset(direction, 1);
            UUID factionInChunk = WarForgeMod.FACTIONS.getClaim(checkChunk);
            // Sum up all additional attack claims
            if (factionInChunk.equals(attackingFaction)) {
                DimBlockPos claimBlockPos = attackers.getSpecificPosForClaim(checkChunk);
                if (claimBlockPos != null) {
                    TileEntity te = WarForgeMod.MC_SERVER.getWorld(claimBlockPos.dim).getTileEntity(claimBlockPos.toRegularPos());
                    if (te instanceof IClaim) {
                        mExtraDifficulty += ((IClaim) te).getAttackStrength();
                    }
                }
            }
            // Sum up all defending support claims
            if (factionInChunk.equals(defendingFaction)) {
                DimBlockPos claimBlockPos = defenders.getSpecificPosForClaim(checkChunk);
                if (claimBlockPos != null) {
                    TileEntity te = WarForgeMod.MC_SERVER.getWorld(claimBlockPos.dim).getTileEntity(claimBlockPos.toRegularPos());
                    if (te instanceof IClaim) {
                        mExtraDifficulty -= ((IClaim) te).getSupportStrength();
                    }
                }
            }
        }

        chunkCheck:
        for (DimBlockPos blockPos : defenders.claims.keySet()) {
            if (defendingClaim.dim != blockPos.dim) continue;
            var teMap = WarForgeMod.MC_SERVER.getWorld(defendingClaim.dim).getChunk(blockPos).getTileEntityMap();
            for (TileEntity te : teMap.values()) {
                if (te instanceof IClaimStrengthModifier claimModifier && claimModifier.isActive()) {
                    ChunkPos pos = new ChunkPos(te.getPos());
                    for (Vec3i vec : claimModifier.getEffectArea()) {
                        ChunkPos affectedChunk = new ChunkPos(pos.x + vec.getX(), pos.z + vec.getZ());
                        if (affectedChunk.equals((ChunkPos) blockPos.toChunkPos())) {
                            mExtraDifficulty += claimModifier.getClaimContribution();
                            if (!claimModifier.canStack())
                                break chunkCheck;
                        }
                    }

                }
            }
        }
    }

    // called when siege is ended for any reason and not detected as completed normally
    public void onCancelled() {
        FACTION_STORAGE.getFaction(defendingFaction).isCurrentlyDefending =false;
        // canceling is only run inside EndSiege, which is only run in TE, so no need for this to do anything
    }

    // called when natural conclusion of siege occurs, not called from TE itself
    public void onCompleted(boolean successful) {
        // for every attacking siege camp attempt to locate it, and if an actual siege camp handle appropriately
        finished = true;
        for (DimBlockPos siegeCampPos : attackingCamps) {
            TileEntity siegeCamp = WarForgeMod.MC_SERVER.getWorld(siegeCampPos.dim).getTileEntity(siegeCampPos.toRegularPos());
            if (siegeCamp != null) {
                if (siegeCamp instanceof TileEntitySiegeCamp) {
                    if (successful) ((TileEntitySiegeCamp) siegeCamp).cleanupPassedSiege();
                    else ((TileEntitySiegeCamp) siegeCamp).cleanupFailedSiege();
                }
            }
        }
    }

    private boolean isPlayerInWarzone(DimBlockPos siegeCampPos, EntityPlayerMP player) {
        // convert siege camp pos to chunk pos and player to chunk pos for clarity
        DimChunkPos siegeCampChunkPos = siegeCampPos.toChunkPos();
        DimChunkPos playerChunkPos = new DimChunkPos(player.dimension, player.getPosition());

        return isPlayerInRadius(siegeCampChunkPos, playerChunkPos);
    }

    public void onPVPKill(EntityPlayerMP killer, EntityPlayerMP killed) {
        Faction attackers = WarForgeMod.FACTIONS.getFaction(attackingFaction);
        Faction defenders = WarForgeMod.FACTIONS.getFaction(defendingFaction);
        Faction killerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(killer.getUniqueID());
        Faction killedFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(killed.getUniqueID());

        if (attackers == null || defenders == null || WarForgeMod.MC_SERVER == null) {
            WarForgeMod.LOGGER.error("Invalid factions in siege.");
            return;
        }

        boolean attackValid = false;
        boolean defendValid = false;

        // there may be multiple siege camps per siege, so ensure kill occurred in radius of any
        for (DimBlockPos siegeCamp : attackingCamps) {
            if (isPlayerInWarzone(siegeCamp, killer)) {
                // First case, an attacker killed a defender
                if (killerFaction == attackers && killedFaction == defenders) {
                    attackValid = true;
                    // Other case, a defender killed an attacker
                } else if (killerFaction == defenders && killedFaction == attackers) {
                    defendValid = true;
                }

            }
        }

        if (!attackValid && !defendValid) return; // no more logic needs to be done for invalid kill

        // update progress appropriately; either valid attack, or def by this point, so state of one bool implies the state of the other
        mAttackProgress += attackValid ? WarForgeConfig.SIEGE_SWING_PER_DEFENDER_DEATH : -WarForgeConfig.SIEGE_SWING_PER_ATTACKER_DEATH;
        WarForgeMod.FACTIONS.sendSiegeInfoToNearby(defendingClaim.toChunkPos());

        // build notification
        ITextComponent notification = new TextComponentTranslation("warforge.notification.siege_death",
                killed.getName(), WarForgeConfig.SIEGE_SWING_PER_ATTACKER_DEATH,
                GetAttackProgress(), GetAttackSuccessThreshold(), GetDefenceProgress());

        // send notification
        attackers.messageAll(notification);
        defenders.messageAll(notification);
    }

    public void ReadFromNBT(NBTTagCompound tags) {
        attackingCamps.clear();

        // Get the attacker and defender
        attackingFaction = tags.getUniqueId("attacker");
        defendingFaction = tags.getUniqueId("defender");

        // Get the important locations
        NBTTagList claimList = tags.getTagList("attackLocations", 11); // IntArray (see NBTBase.class)
        if (claimList != null) {
            for (NBTBase base : claimList) {
                NBTTagIntArray claimInfo = (NBTTagIntArray) base;
                DimBlockPos pos = DimBlockPos.readFromNBT(claimInfo);
                attackingCamps.add(pos);
            }
        }

        defendingClaim = DimBlockPos.readFromNBT(tags, "defendLocation");
        mAttackProgress = tags.getInteger("progress");
        mBaseDifficulty = tags.getInteger("baseDifficulty");
        mExtraDifficulty = tags.getInteger("extraDifficulty");
        if (WarForgeConfig.SIEGE_ENABLE_NEW_TIMER) {

            timeRemainingMillis = tags.getInteger("timeElapsed");
            recalculateEndTimestamp();
        }
    }

    public void WriteToNBT(NBTTagCompound tags) {
        // Set attacker / defender
        tags.setUniqueId("attacker", attackingFaction);
        tags.setUniqueId("defender", defendingFaction);

        // Set important locations
        NBTTagList claimsList = new NBTTagList();
        for (DimBlockPos pos : attackingCamps) {
            claimsList.appendTag(pos.writeToNBT());
        }

        tags.setTag("attackLocations", claimsList);
        tags.setTag("defendLocation", defendingClaim.writeToNBT());
        tags.setInteger("progress", mAttackProgress);
        tags.setInteger("baseDifficulty", mBaseDifficulty);
        tags.setInteger("extraDifficulty", mExtraDifficulty);
        tags.setLong("timeElapsed", timeRemainingMillis);
    }
}
