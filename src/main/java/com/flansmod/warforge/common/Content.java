package com.flansmod.warforge.common;

import com.flansmod.warforge.common.blocks.BlockAdminClaim;
import com.flansmod.warforge.common.blocks.BlockBasicClaim;
import com.flansmod.warforge.common.blocks.BlockCitadel;
import com.flansmod.warforge.common.blocks.BlockLeaderboard;
import com.flansmod.warforge.common.blocks.BlockSiegeCamp;
import com.flansmod.warforge.common.blocks.BlockYieldProvider;
import com.flansmod.warforge.common.blocks.TileEntityAdminClaim;
import com.flansmod.warforge.common.blocks.TileEntityBasicClaim;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.common.blocks.TileEntityLeaderboard;
import com.flansmod.warforge.common.blocks.TileEntityReinforcedClaim;
import com.flansmod.warforge.common.blocks.TileEntitySiegeCamp;
import com.flansmod.warforge.server.Leaderboard.FactionStat;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

public class Content 
{
	public Block citadelBlock, basicClaimBlock, reinforcedClaimBlock, siegeCampBlock;
	public Item citadelBlockItem, basicClaimBlockItem, reinforcedClaimBlockItem, siegeCampBlockItem;
	
	public Block adminClaimBlock;
	public Item adminClaimBlockItem;
	
	public Block topLeaderboardBlock, notorietyLeaderboardBlock, wealthLeaderboardBlock, legacyLeaderboardBlock;
	public Item topLeaderboardItem, notorietyLeaderboardItem, wealthLeaderboardItem, legacyLeaderboardItem;
	
	
	public void preInit()
	{
        citadelBlock = new BlockCitadel(Material.ROCK).setRegistryName("citadelblock").setTranslationKey("citadelblock");
        citadelBlockItem = new ItemBlock(citadelBlock).setRegistryName("citadelblock").setTranslationKey("citadelblock");
        GameRegistry.registerTileEntity(TileEntityCitadel.class, new ResourceLocation(WarForgeMod.MODID, "citadel"));
        
        // Basic and reinforced claims, they share a tile entity
        basicClaimBlock = new BlockBasicClaim(Material.ROCK).setRegistryName("basicclaimblock").setTranslationKey("basicclaimblock");
        basicClaimBlockItem = new ItemBlock(basicClaimBlock).setRegistryName("basicclaimblock").setTranslationKey("basicclaimblock");
        GameRegistry.registerTileEntity(TileEntityBasicClaim.class, new ResourceLocation(WarForgeMod.MODID, "basicclaim"));
        reinforcedClaimBlock = new BlockBasicClaim(Material.ROCK).setRegistryName("reinforcedclaimblock").setTranslationKey("reinforcedclaimblock");
        reinforcedClaimBlockItem = new ItemBlock(reinforcedClaimBlock).setRegistryName("reinforcedclaimblock").setTranslationKey("reinforcedclaimblock");
        GameRegistry.registerTileEntity(TileEntityReinforcedClaim.class, new ResourceLocation(WarForgeMod.MODID, "reinforcedclaim"));
        
        // Siege camp
        siegeCampBlock = new BlockSiegeCamp(Material.ROCK).setRegistryName("siegecampblock").setTranslationKey("siegecampblock");
        siegeCampBlockItem = new ItemBlock(siegeCampBlock).setRegistryName("siegecampblock").setTranslationKey("siegecampblock");
        GameRegistry.registerTileEntity(TileEntitySiegeCamp.class, new ResourceLocation(WarForgeMod.MODID, "siegecamp"));
 
        // Admin claim block
        adminClaimBlock = new BlockAdminClaim().setRegistryName("adminclaimblock").setTranslationKey("adminclaimblock");
        adminClaimBlockItem = new ItemBlock(adminClaimBlock).setRegistryName("adminclaimblock").setTranslationKey("adminclaimblock");
        GameRegistry.registerTileEntity(TileEntityAdminClaim.class, new ResourceLocation(WarForgeMod.MODID, "adminclaim"));
 

        topLeaderboardBlock = new BlockLeaderboard(Material.ROCK, FactionStat.TOTAL).setRegistryName("topleaderboard").setTranslationKey("topleaderboard");
        wealthLeaderboardBlock = new BlockLeaderboard(Material.ROCK, FactionStat.WEALTH).setRegistryName("wealthleaderboard").setTranslationKey("wealthleaderboard");
        notorietyLeaderboardBlock = new BlockLeaderboard(Material.ROCK, FactionStat.NOTORIETY).setRegistryName("notorietyleaderboard").setTranslationKey("notorietyleaderboard");
        legacyLeaderboardBlock = new BlockLeaderboard(Material.ROCK, FactionStat.LEGACY).setRegistryName("legacyleaderboard").setTranslationKey("legacyleaderboard");
        
        topLeaderboardItem = new ItemBlock(topLeaderboardBlock).setRegistryName("topleaderboard").setTranslationKey("topleaderboard");
        wealthLeaderboardItem = new ItemBlock(wealthLeaderboardBlock).setRegistryName("wealthleaderboard").setTranslationKey("wealthleaderboard");
        notorietyLeaderboardItem = new ItemBlock(notorietyLeaderboardBlock).setRegistryName("notorietyleaderboard").setTranslationKey("notorietyleaderboard");
        legacyLeaderboardItem = new ItemBlock(legacyLeaderboardBlock).setRegistryName("legacyleaderboard").setTranslationKey("legacyleaderboard");
        GameRegistry.registerTileEntity(TileEntityLeaderboard.class, new ResourceLocation(WarForgeMod.MODID, "leaderboard"));
        
        MinecraftForge.EVENT_BUS.register(this);
	}


	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> event)
	{
		final Item[] items = {
				citadelBlockItem, basicClaimBlockItem, reinforcedClaimBlockItem, siegeCampBlockItem, adminClaimBlockItem,
				topLeaderboardItem, wealthLeaderboardItem, notorietyLeaderboardItem,
				legacyLeaderboardItem
		};
		IForgeRegistry<Item> registry = event.getRegistry();
		for(Item item : items) {
			registry.register(item);
		}
		WarForgeMod.LOGGER.info("Registered items");
	}
	
	@SubscribeEvent
	public void registerBlocks(RegistryEvent.Register<Block> event)
	{
		final Block[] blocks = {
			citadelBlock, basicClaimBlock, reinforcedClaimBlock, siegeCampBlock, adminClaimBlock,
			topLeaderboardBlock, wealthLeaderboardBlock, notorietyLeaderboardBlock,
			legacyLeaderboardBlock
		};
		IForgeRegistry<Block> registry = event.getRegistry();
		for(Block block : blocks) {
			registry.register(block);
		}

		WarForgeMod.LOGGER.info("Registered blocks");
	}
}
