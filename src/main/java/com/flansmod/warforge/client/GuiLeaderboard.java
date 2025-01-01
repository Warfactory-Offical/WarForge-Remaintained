package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.LeaderboardInfo;
import com.flansmod.warforge.common.network.PacketLeaderboardInfo;
import com.flansmod.warforge.common.network.PacketRequestLeaderboardInfo;
import com.flansmod.warforge.server.Leaderboard.FactionStat;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiLeaderboard extends GuiScreen
{
	private static final ResourceLocation texture = new ResourceLocation(WarForgeMod.MODID, "gui/leaderboard.png");

	private FactionStat currentStat = FactionStat.TOTAL;
	private int lookingAtIndex = 0;
	private final int xSize;
    private final int ySize;
	private LeaderboardInfo info;
	
	public GuiLeaderboard()
	{
		info = PacketLeaderboardInfo.sLatestInfo;
		currentStat = info.stat;
    	xSize = 256;
    	ySize = 191;
	}
	
	@Override
	public void initGui()
	{
		super.initGui();
		
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		
		// Total button - this is the default tab, always accessible
		GuiButton totalButton = new GuiButton(FactionStat.TOTAL.ordinal(), j + 6, k + 18, 58, 20, "Total");
		buttonList.add(totalButton);
		totalButton.enabled = currentStat != FactionStat.TOTAL;
		
		GuiButton notorietyButton = new GuiButton(FactionStat.NOTORIETY.ordinal(), j + 6 + 62, k + 18, 58, 20, "Notoriety");
		buttonList.add(notorietyButton);
		notorietyButton.enabled = currentStat != FactionStat.NOTORIETY;
		
		GuiButton legacyButton = new GuiButton(FactionStat.LEGACY.ordinal(), j + 6 + 124, k + 18, 58, 20, "Legacy");
		buttonList.add(legacyButton);
		legacyButton.enabled = currentStat != FactionStat.LEGACY;
		
		GuiButton wealthButton = new GuiButton(FactionStat.WEALTH.ordinal(), j + 6 + 186, k + 18, 58, 20, "Wealth");
		buttonList.add(wealthButton);
		wealthButton.enabled = currentStat != FactionStat.WEALTH;
		
	}
	
	@Override
	protected void actionPerformed(GuiButton button)
	{
		FactionStat stat = FactionStat.values()[button.id];
		currentStat = stat;
		info = null;
		
		// TODO: Next page buttons
		
		PacketRequestLeaderboardInfo packet = new PacketRequestLeaderboardInfo();
		packet.firstIndex = 0;
		packet.stat = stat;
		WarForgeMod.NETWORK.sendToServer(packet);
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
		
		String line = switch (currentStat) {
            case LEGACY -> "Legacy Leaderboard";
            case NOTORIETY -> "Notoriety Leaderboard";
            case TOTAL -> "Top Leaderboard";
            case WEALTH -> "Wealth Leaderboard";
        };
        fontRenderer.drawStringWithShadow(line, j + xSize / 2 - fontRenderer.getStringWidth(line) / 2, k + 6, 0xffffff);

        line = switch (currentStat) {
            case LEGACY -> "Factions that have been active longest";
            case NOTORIETY -> "Factions that are top in PvP and Siegeing";
            case TOTAL -> "Top Factions, with combined scores";
            case WEALTH -> "Factions with the most wealth in their citadel";
			default -> "";
        };
		fontRenderer.drawStringWithShadow(line, j + 8, k + 40, 0xf0f0f0);
			
		if(info == null)
		{
			String text = "Waiting for server...";
			fontRenderer.drawStringWithShadow(text, j + xSize / 2 - fontRenderer.getStringWidth(text) / 2, k + 80, 0xa0a0a0);
		}
		else
		{
			if(info.myFaction != null)
			{
				int rank = switch (currentStat) {
                    case LEGACY -> info.myFaction.legacyRank;
                    case NOTORIETY -> info.myFaction.notorietyRank;
                    case WEALTH -> info.myFaction.wealthRank;
					case TOTAL -> info.myFaction.totalRank;
				};
                RenderFactionInfo(j + 7, k + 55, info.myFaction, info.myFaction.legacyRank);
			}
			
			for(int i = 0; i < LeaderboardInfo.NUM_LEADERBOARD_ENTRIES_PER_PAGE; i++)
			{
				if(info.factionInfos[i] != null)
				{
					RenderFactionInfo(j + 7, k + 72 + 12 * i, info.factionInfos[i], info.firstIndex + i + 1);
				}
			}
		}
	}
	
	private void RenderFactionInfo(int x, int y, FactionDisplayInfo faction, int oneIndexedRank)
	{
		int stat = switch (currentStat) {
            case LEGACY -> faction.legacy;
            case NOTORIETY -> faction.notoriety;
            case WEALTH -> faction.wealth;
			case TOTAL -> faction.legacy + faction.notoriety + faction.wealth;
		};

        if(oneIndexedRank <= 3)
		{
			int colour = switch (oneIndexedRank) {
                case 1 -> 0xFFD700;
                case 2 -> 0xC0C0C0;
                case 3 -> 0xcd7f32;
                default -> 0xffffff;
            };
            fontRenderer.drawStringWithShadow(faction.factionName, x + 2, y + 1, colour);
			fontRenderer.drawStringWithShadow("" + stat, x + 150, y + 1, colour);
			fontRenderer.drawStringWithShadow("#" + oneIndexedRank, x + 200, y + 1, colour);
		}
		else	
		{
			fontRenderer.drawString(faction.factionName, x + 2, y + 1, 0xffffff);
			fontRenderer.drawString("" + stat, x + 150, y + 1, 0xffffff);
			fontRenderer.drawString("#" + oneIndexedRank, x + 200, y + 1, 0xffffff);
		}
	}
	
	@Override
	public boolean doesGuiPauseGame()
	{
		return false;
	}
}
