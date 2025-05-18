package com.flansmod.warforge.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PlayerDisplayInfo;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.Faction.Role;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ResourceLocation;

public class GuiFactionInfo extends GuiScreen
{
	private static final ResourceLocation texture = new ResourceLocation(WarForgeMod.MODID, "gui/factioninfo.png");
	public static HashMap<String, ResourceLocation> skinCache = new HashMap<String, ResourceLocation>();

	private enum EnumTab
	{
		STATS,
		ALLIES,
		SIEGES,
		PLAYER,
	}
	
	// Generic buttons
	private static final int BUTTON_STATS = 0;
	private static final int BUTTON_ALLIES = 1;
	private static final int BUTTON_SIEGES = 2;
	
	// Player buttons
	private static final int BUTTON_PROMOTE = 10;
	private static final int BUTTON_DEMOTE = 11;
	private static final int BUTTON_KICK = 12;
	
	// Player array
	private static final int BUTTON_PLAYER_FIRST = 100;
	
	private EnumTab currentTab = EnumTab.STATS;
	private final int xSize;
    private final int ySize;
	private final FactionDisplayInfo info;
	private PlayerDisplayInfo lookingAt = null;
	
	private GuiButton stats, allies, sieges,
						promote, demote, kick;
	
	public GuiFactionInfo()
	{
		info = PacketFactionInfo.latestInfo;
    	xSize = 176;
    	ySize = 256;
	}
	
	@Override
	public void initGui()
	{
		super.initGui();
		
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		
		// Stats button - this is the default tab, always accessible
		stats = new GuiButton(BUTTON_STATS, j + 53, k + 127, 70, 20, "Stats");
		buttonList.add(stats);
		
		// Allies button - publicly available
		allies = new GuiButton(BUTTON_ALLIES, j + 5, k + 127, 46, 20, "Allies");
		buttonList.add(allies);
		allies.enabled = false; // TODO: Not implemented yet
		
		// Sieges button - publicly available
		sieges = new GuiButton(BUTTON_SIEGES, j + 125, k + 127, 46, 20, "Sieges");
		buttonList.add(sieges);
		sieges.enabled = false; // TODO: Not implemented yet
		
		// Player action buttons
		promote = new GuiButton(BUTTON_PROMOTE, j + 108, k + 152, 60, 20, "Promote");
		buttonList.add(promote);
		demote = new GuiButton(BUTTON_DEMOTE, j + 108, k + 174, 60, 20, "Demote");
		buttonList.add(demote);
		kick = new GuiButton(BUTTON_KICK, j + 108, k + 196, 60, 20, "Kick");
		buttonList.add(kick);
		
		// Player selection buttons
		int rowLength = 7;
		int columnHeight = 2;
		int leaderInfoIndex = -1;
		
		for(int y = 0; y < columnHeight; y++)
		{
			for(int x = 0; x < rowLength; x++)
			{
				int index = x + y * rowLength;
				if(leaderInfoIndex != -1)
				{
					index++;
				}
				
				if(index < info.members.size())
				{
					PlayerDisplayInfo playerInfo = info.members.get(index);
					// If this is the leader, skip them and cache to put at top
					if(leaderInfoIndex == -1)
					{
						if(playerInfo.role == Faction.Role.LEADER || playerInfo.playerUuid.equals(info.mLeaderID))
						{
							// Cache them
							leaderInfoIndex = index;
							
							// Then render the next one
							if(index + 1 < info.members.size())
							{
								playerInfo = info.members.get(index + 1);
								index++;
							}
							else continue;
						}
					}

					GuiInvisibleButton button = new GuiInvisibleButton(BUTTON_PLAYER_FIRST + index, j + 5 + 24 * x, k + 79 + 24 * y, 22, 22, "");
					buttonList.add(button);
				}
			}
		}
		
		if(leaderInfoIndex != -1)
		{
			GuiInvisibleButton button = new GuiInvisibleButton(BUTTON_PLAYER_FIRST + leaderInfoIndex, j + 34, k + 32, 22, 22, "");
			buttonList.add(button);
		}
		
		selectTab(EnumTab.STATS, null);
	}
	
	@Override
	protected void actionPerformed(GuiButton button)
	{
		switch(button.id)
		{
			// Tab selections
			case BUTTON_STATS:
			{
				selectTab(EnumTab.STATS, null);
				break;
			}
			case BUTTON_ALLIES, BUTTON_SIEGES:
			{
				selectTab(EnumTab.ALLIES, null);
				break;
			}
            // Player actions
			case BUTTON_PROMOTE:
			{
				if(lookingAt != null)
				{
					// Request promotion
					Minecraft.getMinecraft().player.sendChatMessage("/f promote " + lookingAt.username);
					
					// Re-request updated data
					ClientProxy.requestFactionInfo(info.factionId);
				}
				break;
			}
			case BUTTON_DEMOTE:
			{
				if(lookingAt != null)
				{
					// Request promotion
					Minecraft.getMinecraft().player.sendChatMessage("/f demote " + lookingAt.username);
					
					// Re-request updated data
					ClientProxy.requestFactionInfo(info.factionId);
				}
				break;
			}
			case BUTTON_KICK:
			{
				if(lookingAt != null)
				{
					// Request promotion
					Minecraft.getMinecraft().player.sendChatMessage("/f kick " + lookingAt.username);
					
					// Re-request updated data
					ClientProxy.requestFactionInfo(info.factionId);
				}
				break;
			}
			
			// Player selections
			default:
			{
				int index = button.id - BUTTON_PLAYER_FIRST;
				if(index >= 0 && index < info.members.size())
				{
					selectTab(EnumTab.PLAYER, info.members.get(index).playerUuid);
				}
				else
				{
					WarForgeMod.LOGGER.error("Pressed a button with unknown player index");
				}
				break;
			}
		}	
	}
	
	private void selectTab(EnumTab tab, UUID playerID)
	{
		UUID myID = Minecraft.getMinecraft().player.getUniqueID();
		PlayerDisplayInfo myInfo = info.getPlayerInfo(myID);
		boolean isMyFaction = myInfo != null;
		
		promote.visible = tab == EnumTab.PLAYER && isMyFaction;
		demote.visible = tab == EnumTab.PLAYER && isMyFaction;
		kick.visible = tab == EnumTab.PLAYER && isMyFaction;
		
		if(tab == EnumTab.PLAYER)
		{
			lookingAt = info.getPlayerInfo(playerID);
			if(lookingAt != null)
			{
				promote.enabled = isMyFaction && myInfo.role == Role.LEADER && lookingAt.role == Role.MEMBER;
				demote.enabled = isMyFaction && myInfo.role == Role.LEADER && lookingAt.role == Role.OFFICER;
				kick.enabled = isMyFaction && lookingAt.role.ordinal() < myInfo.role.ordinal();
			}
		}
		
		currentTab = tab;
	}
	
	private void SelectPlayer()
	{
		stats.enabled = true;
	}
		
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		
		// Draw background
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(texture);
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		drawTexturedModalRect(j, k, 0, 0, xSize, ySize);
		
		// Then draw overlay
		super.drawScreen(mouseX, mouseY, partialTicks);
		
		fontRenderer.drawStringWithShadow(info.factionName, j + xSize / 2 - fontRenderer.getStringWidth(info.factionName) * 0.5f, k + 13, 0xffffff);
		// Some space here for strings
		//fontRenderer.drawStringWithShadow("Notoriety: " + info.mNotoriety, j + 6, k + 57, 0xffffff);
		//fontRenderer.drawStringWithShadow("Wealth: " + info.mNotoriety, j + 2 + xSize / 2, k + 57, 0xffffff);
		//fontRenderer.drawStringWithShadow("Notoriety: " + info.mNotoriety, j + 6, k + 67, 0xffffff);
		//fontRenderer.drawStringWithShadow("Wealth: " + info.mNotoriety, j + 2 + xSize / 2, k + 67, 0xffffff);
			
		// Render user info, and look out for leader info while we there
		int rowLength = 7;
		int columnHeight = 2;
		PlayerDisplayInfo leaderInfo = null; 
		
		for(int y = 0; y < columnHeight; y++)
		{
			for(int x = 0; x < rowLength; x++)
			{
				int index = x + y * rowLength;
				
				if(leaderInfo != null)
				{
					index++;
				}
				
				if(index < info.members.size())
				{
					PlayerDisplayInfo playerInfo = info.members.get(index);
					// If this is the leader, skip them and cache to put at top
					if(leaderInfo == null)
					{
						if(playerInfo.role == Faction.Role.LEADER || playerInfo.playerUuid.equals(info.mLeaderID))
						{
							// Cache
							leaderInfo = playerInfo;
							
							// Then render the next one
							if(index + 1 < info.members.size())
								playerInfo = info.members.get(index + 1);
							else continue;
						}
					}
					
					// Bind our texture, render a background
					mc.renderEngine.bindTexture(texture);
					drawTexturedModalRect(j + 5 + 24 * x, k + 79 + 24 * y, playerInfo.role == Faction.Role.OFFICER ? 176 : 198, 0, 22, 22);
					
					// Then bind their face and render that
					RenderPlayerFace(j + 8 + 24 * x, k + 82 + 24 * y, playerInfo.username);
				}
			}
		}
		
		if(leaderInfo != null)
		{
			fontRenderer.drawStringWithShadow("Leader", j + 56, k + 31, 0xffffff);
			fontRenderer.drawStringWithShadow(leaderInfo.username, j + 56, k + 42, 0xffffff);
			RenderPlayerFace(j + 34, k + 32, leaderInfo.username);
		}
		
		
		// Lower box
		switch(currentTab)
		{
			case STATS:
			{
				// First column - names
				fontRenderer.drawStringWithShadow("Notoriety:", j + 8, k + 152, 0xffffff);
				fontRenderer.drawStringWithShadow("Wealth:", j + 8, k + 162, 0xffffff);
				fontRenderer.drawStringWithShadow("Legacy:", j + 8, k + 172, 0xffffff);
				fontRenderer.drawStringWithShadow("Total:", j + 8, k + 182, 0xffffff);
				fontRenderer.drawStringWithShadow("Members:", j + 8, k + 192, 0xffffff);

				int maxLvl;
				if(WarForgeConfig.ENABLE_CITADEL_UPGRADES)
					maxLvl = WarForgeMod.UPGRADE_HANDLER.getLEVELS().length - 1;
					fontRenderer.drawStringWithShadow("Citadel Level:", j + 8, k + 202, info.lvl >= maxLvl ? 0xFFAA00 : 0xffffff);

				// Second column - numbers
				int column1X = 120;
				fontRenderer.drawStringWithShadow("" + info.notoriety, j + column1X, k + 152, 0xffffff);
				fontRenderer.drawStringWithShadow("" + info.wealth, j + column1X, k + 162, 0xffffff);
				fontRenderer.drawStringWithShadow("" + info.legacy, j + column1X, k + 172, 0xffffff);
				fontRenderer.drawStringWithShadow("" + (info.notoriety + info.wealth + info.legacy), j + column1X, k + 182, 0xffffff);
				fontRenderer.drawStringWithShadow("" + info.members.size(), j + column1X, k + 192, 0xffffff);
				if(WarForgeConfig.ENABLE_CITADEL_UPGRADES)
					fontRenderer.drawStringWithShadow("" + info.lvl + " [" + WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(info.lvl) + "]", j + column1X, k + 202, info.lvl >= maxLvl ? 0xFFAA00 : 0xffffff);

				// Third column - server positioning
				int column2X = 150;
				fontRenderer.drawStringWithShadow("#" + info.notorietyRank, j + column2X, k + 152, 0xffffff);
				fontRenderer.drawStringWithShadow("#" + info.wealthRank, j + column2X, k + 162, 0xffffff);
				fontRenderer.drawStringWithShadow("#" + info.legacyRank, j + column2X, k + 172, 0xffffff);
				fontRenderer.drawStringWithShadow("#" + info.totalRank, j + column2X, k + 182, 0xffffff);
				
				break;
			}
			case ALLIES, SIEGES:
			{
				// TODO:
				break;
			}
			case PLAYER:
			{
				if(lookingAt != null)
				{
					fontRenderer.drawStringWithShadow(lookingAt.username, j + 8, k + 152, 0xffffff);
					switch(lookingAt.role)
					{
						case GUEST: fontRenderer.drawStringWithShadow("Guest", j + 8, k + 162, 0xffffff); break;
						case LEADER: fontRenderer.drawStringWithShadow("Leader", j + 8, k + 162, 0xffffff); break;
						case MEMBER: fontRenderer.drawStringWithShadow("Member", j + 8, k + 162, 0xffffff); break;
						case OFFICER: fontRenderer.drawStringWithShadow("Officer", j + 8, k + 162, 0xffffff); break;
						default: fontRenderer.drawStringWithShadow("Unknown", j + 8, k + 162, 0xffffff); break;
					}
				}
				break;
			}
        }
		
	}
	
	private void RenderPlayerFace(int x, int y, String username)
	{		
		ResourceLocation skinLocation = GetSkin(username);
        mc.renderEngine.bindTexture(skinLocation);
        drawModalRectWithCustomSizedTexture(x, y, 16, 16, 16, 16, 128, 128);
	}	
		
	public static ResourceLocation GetSkin(String username)
	{
		if(!skinCache.containsKey(username))
		{
			// Then bind their face and render that
			ResourceLocation skin = DefaultPlayerSkin.getDefaultSkinLegacy();
			GameProfile profile = TileEntitySkull.updateGameProfile(new GameProfile((UUID)null, username));
            Minecraft minecraft = Minecraft.getMinecraft();
            Map<Type, MinecraftProfileTexture> map = minecraft.getSkinManager().loadSkinFromCache(profile);

            if (map.containsKey(Type.SKIN))
            {
                skin = minecraft.getSkinManager().loadSkin(map.get(Type.SKIN), Type.SKIN);
            }
            else
            {
                UUID uuid = EntityPlayer.getUUID(profile);
                skin = DefaultPlayerSkin.getDefaultSkin(uuid);
            }
            skinCache.put(username, skin);
	        return skin;
		}
		
		return skinCache.get(username);
	}
	
	@Override
	public boolean doesGuiPauseGame()
	{
		return false;
	}
}
