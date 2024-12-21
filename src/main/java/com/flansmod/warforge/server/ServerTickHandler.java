package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public class ServerTickHandler 
{	
	@SubscribeEvent
	public void OnTick(ServerTickEvent tick) {
		// for some reason, ticks may occur with 0ms between them. A tick timer is more useful for distinguishing whether an update has occurred
		WarForgeMod.INSTANCE.updateServer();
		WarForgeMod.INSTANCE.NETWORK.handleServerPackets();
		WarForgeMod.PROTECTIONS.UpdateServer();
		WarForgeMod.TELEPORTS.Update();
		WarForgeMod.proxy.TickServer();
		WarForgeMod.FACTIONS.Update();

		// WarForgeMod.LOGGER.info("Current tick: " + WarForgeMod.ServerTick);
	}
}
