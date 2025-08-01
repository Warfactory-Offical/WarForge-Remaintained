package com.flansmod.warforge.common;

import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionStorage;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent.PotentialSpawns;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

public class ProtectionsModule {
    public static boolean OP_OVERRIDE = false;
    private static boolean inLoop = false;

    public ProtectionsModule() {
    }

    @Nonnull
    public static ProtectionConfig GetProtections(UUID playerID, DimBlockPos pos) {
        return GetProtections(playerID, pos.toChunkPos());
    }

    // It is generally expected that you are asking about a loaded chunk, not that that should matter
    @Nonnull
    public static ProtectionConfig GetProtections(UUID playerID, DimChunkPos pos) {
        UUID factionID = WarForgeMod.FACTIONS.getClaim(pos);
        if (factionID.equals(FactionStorage.SAFE_ZONE_ID))
            return WarForgeConfig.SAFE_ZONE;
        if (factionID.equals(FactionStorage.WAR_ZONE_ID))
            return WarForgeConfig.WAR_ZONE;

        Faction faction = WarForgeMod.FACTIONS.getFaction(factionID);
        if (faction != null) {
            boolean playerIsInFaction = playerID != null && !playerID.equals(Faction.nullUuid) && faction.isPlayerInFaction(playerID);

            if (faction.citadelPos.toChunkPos().equals(pos))
                return playerIsInFaction ? WarForgeConfig.CITADEL_FRIEND : WarForgeConfig.CITADEL_FOE;

            return playerIsInFaction ? WarForgeConfig.CLAIM_FRIEND : WarForgeConfig.CLAIM_FOE;
        }

        return WarForgeConfig.UNCLAIMED;
    }

    public void UpdateServer() {
		/*
		 * oof pistons are a pain
		for(World world: WarForgeMod.MC_SERVER.worlds)
		{
			ArrayList<TileEntityPiston> list = new ArrayList<TileEntityPiston>();

			for(TileEntity te : world.loadedTileEntityList)
			{
				if(te instanceof TileEntityPiston)
				{
					list.add( (TileEntityPiston)te);
				}
			}

			for(TileEntityPiston piston : list)
			{
				NBTTagCompound tags = new NBTTagCompound();
				piston.writeToNBT(tags);
				tags.setBoolean("extending", !tags.getBoolean("extending"));
				piston.readFromNBT(tags);

				piston.clearPistonTileEntity();
			}
		}
		*/

    }

    @SubscribeEvent
    public void OnDismount(EntityMountEvent event) {
        if (event.getEntity().world.isRemote)
            return;

        if (event.getEntityMounting() instanceof EntityPlayer) {
            DimBlockPos vehiclePos = new DimBlockPos(event.getEntityBeingMounted().dimension, event.getEntityBeingMounted().getPosition());
            ProtectionConfig mountConfig = GetProtections(event.getEntityMounting().getUniqueID(), vehiclePos);

            if (event.isMounting() && !mountConfig.ALLOW_MOUNT_ENTITY)
                event.setCanceled(true);

            if (event.isDismounting() && !mountConfig.ALLOW_DISMOUNT_ENTITY)
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void OnExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote)
            return;

        // Check each pos, but keep a cache of configs so we don't do like 300 lookups
        int dim = event.getWorld().provider.getDimension();
        HashMap<DimChunkPos, ProtectionConfig> checkedPositions = new HashMap<DimChunkPos, ProtectionConfig>();

        for (int i = event.getAffectedBlocks().size() - 1; i >= 0; i--) {
            DimChunkPos cPos = new DimChunkPos(dim, event.getAffectedBlocks().get(i));
            if (!checkedPositions.containsKey(cPos)) {
                ProtectionConfig config = GetProtections(Faction.nullUuid, cPos);
                checkedPositions.put(cPos, config);
            }
            if (!checkedPositions.get(cPos).EXPLOSION_DAMAGE || !checkedPositions.get(cPos).BLOCK_REMOVAL) {
                //WarForgeMod.LOGGER.info("Protected block position from explosion");
                event.getAffectedBlocks().remove(i);
            }
        }
    }

    @SubscribeEvent
    public void OnDamage(LivingDamageEvent event) {
        if (event.getEntity().world.isRemote)
            return;

        DimBlockPos damagedPos = new DimBlockPos(event.getEntity().dimension, event.getEntity().getPosition());
        ProtectionConfig damagedConfig = GetProtections(event.getEntity().getUniqueID(), damagedPos);

        DamageSource source = event.getSource();
        if (source instanceof EntityDamageSource) {
            Entity attacker = source.getTrueSource();
            if (attacker instanceof EntityPlayer) {
                if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_PLAYER) {
                    event.setCanceled(true);
                    //WarForgeMod.LOGGER.info("Cancelled damage event from other player because we are in a safe zone");
                    return;
                }

                DimBlockPos attackerPos = new DimBlockPos(attacker.dimension, attacker.getPosition());
                ProtectionConfig attackerConfig = GetProtections(attacker.getUniqueID(), attackerPos);

                if (!attackerConfig.PLAYER_DEAL_DAMAGE) {
                    event.setCanceled(true);
                    //WarForgeMod.LOGGER.info("Cancelled damage event from player because they were in a safe zone");
                    return;
                }
            } else if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_MOB) {
                event.setCanceled(true);
                //WarForgeMod.LOGGER.info("Cancelled damage event from mob");
                return;
            }
        } else {
            if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_OTHER) {
                event.setCanceled(true);
                //WarForgeMod.LOGGER.info("Cancelled damage event from other source");
                return;
            }
        }
    }

    @SubscribeEvent
    public void BlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isRemote)
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getEntity()))
            return;

        Entity eventEntity = event.getEntity();
        if (eventEntity == null) {
            WarForgeMod.LOGGER.atError().log("Detected null entity for event with detals: pos - " + event.getPos() + "; world - " + event.getWorld() + ";");
            event.setCanceled(true);
            return;
        }

        DimBlockPos pos = new DimBlockPos(eventEntity.dimension, event.getPos());
        ProtectionConfig config = GetProtections(eventEntity.getUniqueID(), pos);

        if (!config.PLACE_BLOCKS) {
            if (!config.BLOCK_PLACE_EXCEPTIONS.contains(event.getBlockSnapshot().getCurrentBlock().getBlock())) {
                //WarForgeMod.LOGGER.info("Cancelled block placement event");
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void BlockRemoved(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote)
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getPlayer()))
            return;

        DimBlockPos pos = new DimBlockPos(event.getPlayer().dimension, event.getPos());
        ProtectionConfig config = GetProtections(event.getPlayer().getUniqueID(), pos);

        if (!config.BREAK_BLOCKS || !config.BLOCK_REMOVAL) {
            if (!config.BLOCK_BREAK_EXCEPTIONS.contains(event.getState().getBlock())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void OnPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote)
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getEntityPlayer()))
            return;

        DimBlockPos pos = new DimBlockPos(event.getTarget().dimension, event.getTarget().getPosition());
        ProtectionConfig config = GetProtections(event.getEntityPlayer().getUniqueID(), pos);

        if (!config.INTERACT) {
            //WarForgeMod.LOGGER.info("Cancelled interact event");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void OnPlayerRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote)
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getEntityPlayer()))
            return;

        DimBlockPos pos = new DimBlockPos(event.getEntity().dimension, event.getPos());
        ProtectionConfig config = GetProtections(event.getEntityPlayer().getUniqueID(), pos);


        if (!config.INTERACT) {
            Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
            if (block != WarForgeMod.CONTENT.citadelBlock
                    && block != WarForgeMod.CONTENT.basicClaimBlock
                    && block != WarForgeMod.CONTENT.reinforcedClaimBlock
                    && block != WarForgeMod.CONTENT.dummyTranslusent
                    && block != WarForgeMod.CONTENT.statue
                    && !config.BLOCK_INTERACT_EXCEPTIONS.contains(block)) {
                //WarForgeMod.LOGGER.info("Cancelled item use event while looking at block");
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void OnPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote)
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getEntityPlayer()))
            return;

        // Always allow food
        if (event.getItemStack().getItem() instanceof ItemFood)
            return;

        DimBlockPos pos = new DimBlockPos(event.getEntity().dimension, event.getPos());
        ProtectionConfig config = GetProtections(event.getEntityPlayer().getUniqueID(), pos);

        if (!config.USE_ITEM) {
            //WarForgeMod.LOGGER.info("Cancelled item use event");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void OnMobSpawn(PotentialSpawns event) {
        ProtectionConfig config = GetProtections(Faction.nullUuid, new DimBlockPos(event.getWorld().provider.getDimension(), event.getPos()));
        if (!config.ALLOW_MOB_SPAWNS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void LivingUpdate(EnteringChunk event) {
        if (!inLoop) {
            if (!(event.getEntity() instanceof EntityPlayer)) {
                ProtectionConfig config = GetProtections(Faction.nullUuid, new DimBlockPos(event.getEntity().dimension, event.getEntity().getPosition()));
                if (!config.ALLOW_MOB_ENTRY) {
                    inLoop = true;
                    boolean wasNoClip = event.getEntity().noClip;
                    event.getEntity().noClip = true;
                    event.getEntity().move(MoverType.SELF, (event.getOldChunkX() - event.getNewChunkX()), 0d, (event.getOldChunkZ() - event.getNewChunkZ()));
                    event.getEntity().noClip = wasNoClip;
                    inLoop = false;
                }
            }
        }
    }
}
