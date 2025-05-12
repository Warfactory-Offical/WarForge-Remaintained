package com.flansmod.warforge.client;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.flansmod.warforge.common.CommonProxy;
import com.flansmod.warforge.common.ContainerCitadel;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketDisbandFaction;
import com.flansmod.warforge.common.network.PacketPlaceFlag;
import com.flansmod.warforge.common.network.PacketRequestUpgradeUI;
import com.flansmod.warforge.server.Faction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

public class GuiCitadel extends GuiContainer
{	
	private static final ResourceLocation texture = new ResourceLocation(WarForgeMod.MODID, "gui/citadelmenu.png");

	private static final int BUTTON_INFO = 0;
	private static final int BUTTON_DISBAND = 1;
	private static final int BUTTON_CREATE = 2;
	private static final int BUTTON_UPGRADE = 3;
	private static final int BUTTON_CHANGE_COLOUR = 4;
	
	public ContainerCitadel citadelContainer;
	
	public GuiCitadel(Container container) 
	{
		super(container);
		citadelContainer = (ContainerCitadel)container;
		
		ySize = 182;
	}

	@Override
	public void initGui()
	{
		super.initGui();
		
		boolean hasFactionSet = !citadelContainer.citadel.getFaction().equals(Faction.nullUuid);

		//Create button
		GuiButton createButton = new GuiButton(BUTTON_CREATE, width / 2 - 20, height / 2 - 70, 100, 20, "Create");
		createButton.enabled = !hasFactionSet;
		createButton.visible = !hasFactionSet;
		buttonList.add(createButton);
		
		//Place Flag Button
		GuiButton placeFlagButton = new GuiButton(BUTTON_UPGRADE, width / 2 - 20, height / 2 - 48, 100, 20, "Upgrade Citadel");
		placeFlagButton.enabled = hasFactionSet;
		buttonList.add(placeFlagButton);
		
		//Info Button
		GuiButton infoButton = new GuiButton(BUTTON_INFO, width / 2 - 20, height / 2 - 26, 36, 20, "Info");
		infoButton.enabled = hasFactionSet;
		buttonList.add(infoButton);
		
		//Disband button
		GuiButton disbandButton = new GuiButton(BUTTON_DISBAND, width / 2 + 10, height / 2 - 70, 70, 20, "Disband");
		disbandButton.enabled = hasFactionSet;
		disbandButton.visible = hasFactionSet;
		buttonList.add(disbandButton);
		
		// Change colour button
		GuiButton colourButton = new GuiButton(BUTTON_CHANGE_COLOUR, width / 2 - 20, height / 2 - 70, 20, 20, "");
		colourButton.enabled = hasFactionSet;
		colourButton.visible = hasFactionSet;
		buttonList.add(colourButton);
		
	}
	

	@Override
	protected void actionPerformed(GuiButton button)
	{
		switch(button.id)
		{
			case BUTTON_CREATE:
			{
				// Open creation GUI
				mc.player.openGui(
						WarForgeMod.INSTANCE, 
						CommonProxy.GUI_TYPE_CREATE_FACTION, 
						mc.world, 
						citadelContainer.citadel.getClaimPos().getX(),
						citadelContainer.citadel.getClaimPos().getY(),
						citadelContainer.citadel.getClaimPos().getZ());
				
				break;
			}
			case BUTTON_INFO:
			{
				ClientProxy.requestFactionInfo(citadelContainer.citadel.getFaction());
				break;
			}
			case BUTTON_DISBAND:
			{
				WarForgeMod.NETWORK.sendToServer(new PacketDisbandFaction());
				mc.player.closeScreen();
				break;
			}
			case BUTTON_UPGRADE:
			{
				PacketRequestUpgradeUI packet = new PacketRequestUpgradeUI();
				packet.mFactionIDRequest = citadelContainer.citadel.getFaction();
				packet.mFactionNameRequest = citadelContainer.citadel.factionName;

				//ClientGUI.open(new GuiUpgradePanel());

//				PacketPlaceFlag packet = new PacketPlaceFlag();
//				packet.pos = citadelContainer.citadel.getClaimPos();
				WarForgeMod.NETWORK.sendToServer(packet);
//
//				Minecraft.getMinecraft().displayGuiScreen(null);
				break;
			}
			case BUTTON_CHANGE_COLOUR:
			{
				mc.player.openGui(
						WarForgeMod.INSTANCE, 
						CommonProxy.GUI_TYPE_RECOLOUR_FACTION, 
						mc.world, 
						citadelContainer.citadel.getClaimPos().getX(),
						citadelContainer.citadel.getClaimPos().getY(),
						citadelContainer.citadel.getClaimPos().getZ());
				break;
			}
		}	
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
	{
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(texture);
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		drawTexturedModalRect(j, k, 0, 0, xSize, ySize);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int x, int y)
	{
		super.drawGuiContainerForegroundLayer(x, y);
		
		fontRenderer.drawString(citadelContainer.citadel.getClaimDisplayName(), 6, 6, 0x404040);
		
		fontRenderer.drawString("Yields", 6, 20, 0x404040);
		fontRenderer.drawString("Banner:", 148 - fontRenderer.getStringWidth("Banner:"), 72, 0x404040);
		
		fontRenderer.drawString("Inventory", 8, (ySize - 96) + 2, 0x404040);

		boolean hasFactionSet = !citadelContainer.citadel.getFaction().equals(Faction.nullUuid);
		if(hasFactionSet)
		{
			GlStateManager.disableTexture2D();
			
			float scale = 1f/256f;
			float red = scale * ((citadelContainer.citadel.colour >> 16) & 0xff);
			float green = scale * ((citadelContainer.citadel.colour >> 8) & 0xff);
			float blue = scale * ((citadelContainer.citadel.colour) & 0xff);
			GlStateManager.color(red, green, blue);
			drawTexturedModalRect(72, 25, 0, 0, 12, 12);
			
			GlStateManager.enableTexture2D();
		}
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		super.drawScreen(mouseX, mouseY, partialTicks);
		renderHoveredToolTip(mouseX, mouseY);
	}
	
	@Override
	public boolean doesGuiPauseGame()
	{
		return false;
	}
}
