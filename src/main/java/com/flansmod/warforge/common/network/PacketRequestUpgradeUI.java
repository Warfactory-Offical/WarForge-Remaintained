package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketRequestUpgradeUI extends PacketRequestFactionInfo {


    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        Faction faction = null;
        if (!mFactionIDRequest.equals(Faction.nullUuid)) {
            faction = WarForgeMod.FACTIONS.getFaction(mFactionIDRequest);
        } else if (!mFactionNameRequest.isEmpty()) {
            faction = WarForgeMod.FACTIONS.getFaction(mFactionNameRequest);
        } else {
            WarForgeMod.LOGGER.error("Player " + playerEntity.getName() + " made a request for faction info with no valid key");
        }

        if (faction != null) {
            UUID playerUUID = playerEntity.getUniqueID();
            PacketUpgradeUI packet = new PacketUpgradeUI();
            packet.mFactionID = faction.uuid;
            packet.mFactionName = faction.name;
            packet.color = faction.colour;
            packet.outrankingOfficer = faction.isPlayerRoleInFaction(playerUUID, Faction.Role.OFFICER);
            WarForgeMod.NETWORK.sendTo(packet, playerEntity);
        } else {
            WarForgeMod.LOGGER.error("Could not find faction for info");
        }
    }
}
