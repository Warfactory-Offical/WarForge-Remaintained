package com.flansmod.warforge.common.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.flansmod.warforge.common.WarForgeMod;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Flan's Mod packet handler class. Directs packet data to packet classes.
 *
 * @author Flan
 * With much inspiration from http://www.minecraftforge.net/wiki/Netty_Packet_Handling
 */
@ChannelHandler.Sharable
public class PacketHandler extends MessageToMessageCodec<FMLProxyPacket, PacketBase>
{
	//Map of channels for each side
	private EnumMap<Side, FMLEmbeddedChannel> channels;
	//The list of registered packets. Should contain no more than 256 packets.
	private LinkedList<Class<? extends PacketBase>> packets = new LinkedList<>();
	//Whether or not Flan's Mod has initialised yet. Once true, no more packets may be registered.
	private boolean modInitialised = false;

	/**
	 * Store received packets in these queues and have the main Minecraft threads use these
	 */
	private ConcurrentLinkedQueue<PacketBase> receivedPacketsClient = new ConcurrentLinkedQueue<>();
	private HashMap<String, ConcurrentLinkedQueue<PacketBase>> receivedPacketsServer = new HashMap<>();

	/**
	 * Registers a packet with the handler
	 */
	public boolean registerPacket(Class<? extends PacketBase> cl)
	{
		if(packets.size() > 255)
		{
			WarForgeMod.LOGGER.warn("Packet limit exceeded in Flan's Mod packet handler by packet " + cl.getCanonicalName() + ".");
			return false;
		}
		if(packets.contains(cl))
		{
			WarForgeMod.LOGGER.warn("Tried to register " + cl.getCanonicalName() + " packet class twice.");
			return false;
		}
		if(modInitialised)
		{
			WarForgeMod.LOGGER.warn("Tried to register packet " + cl.getCanonicalName() + " after mod initialisation.");
			return false;
		}

		packets.add(cl);
		return true;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, PacketBase msg, List<Object> out) throws Exception
	{
		try
		{
			//Define a new buffer to store our data upon encoding
			ByteBuf encodedData = Unpooled.buffer();
            ByteBuf tempBuffer = Unpooled.buffer();
			//Get the packet class

			Class<? extends PacketBase> cl = msg.getClass();

			//If this packet has not been registered by our handler, reject it
			if(!packets.contains(cl))
				throw new NullPointerException("Packet not registered : " + cl.getCanonicalName());

			//Like a packet ID. Stored as the first entry in the packet code for recognition
			byte discriminator = (byte)packets.indexOf(cl);
			encodedData.writeByte(discriminator);
			//Get the packet class to encode our packet
            msg.encodeInto(ctx, tempBuffer);
			boolean shouldCompress = msg.canUseCompression() && tempBuffer.readableBytes() > 256;
            boolean compressionFail = false;

            if(shouldCompress){
                // Now we encode the data normally
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteArrayOutputStream))
                {
                    byte[] data = new byte[tempBuffer.readableBytes()];
                    tempBuffer.readBytes(data);
                    gzipOut.write(data);

                } catch (IOException e){
                    compressionFail = true;
                    WarForgeMod.LOGGER.error("Compression failed, defaulting to uncompressed data.", e);
                }
                if (!compressionFail) {
                    byte[] compressedData = byteArrayOutputStream.toByteArray();
					encodedData.writeByte(1);
                    encodedData.writeBytes(compressedData);
                }

            }
            // If compression failed or we don't need compression, send the uncompressed data
            if(!shouldCompress || compressionFail) {
				encodedData.writeByte(0); //Termination for uncompressed packets
                msg.encodeInto(ctx, encodedData);
            }


			//Convert our packet into a Forge packet to get it through the Netty system
			FMLProxyPacket proxyPacket = new FMLProxyPacket(new PacketBuffer(encodedData.copy()), ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get());
			//Add our packet to the outgoing packet queue
			out.add(proxyPacket);
		}
		catch(Exception e)
		{
			WarForgeMod.LOGGER.error("ERROR encoding packet");
			WarForgeMod.LOGGER.throwing(e);
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception
	{
		try
		{
			//Get the encoded data from the incoming packet
			ByteBuf encodedData = msg.payload();
			//Get the class for interpreting this packet
			byte discriminator = encodedData.readByte();
			Class<? extends PacketBase> cl = packets.get(discriminator);

			//If this discriminator returns no class, reject it
			if(cl == null)
				throw new NullPointerException("Packet not registered for discriminator : " + discriminator);

			//Create an empty packet and decode our packet data into it
			PacketBase packet = cl.getConstructor().newInstance();
			byte compressionFlag = encodedData.readByte();
			if(packet.canUseCompression() && compressionFlag == 1){
				//Compressed
				ByteBuf decompressedData = decompress(encodedData.slice());
				packet.decodeInto(ctx, decompressedData);

			} else {
				//Uncompressed
				packet.decodeInto(ctx, encodedData.slice());
			}
			//Check the side and handle our packet accordingly
			switch(FMLCommonHandler.instance().getEffectiveSide())
			{
				case CLIENT:
				{
					receivedPacketsClient.offer(packet);
					//packet.handleClientSide(getClientPlayer());
					break;
				}
				case SERVER:
				{
					INetHandler netHandler = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();
					EntityPlayer player = ((NetHandlerPlayServer)netHandler).player;
					if(!receivedPacketsServer.containsKey(player.getName()))
						receivedPacketsServer.put(player.getName(), new ConcurrentLinkedQueue<>());
					receivedPacketsServer.get(player.getName()).offer(packet);
					//packet.handleServerSide();
					break;
				}
			}
		}
		catch(Exception e)
		{
			WarForgeMod.LOGGER.error("ERROR decoding packet");
			WarForgeMod.LOGGER.throwing(e);
		}
	}

	private ByteBuf decompress(ByteBuf compressedData) throws IOException {
		// Create a GZIPInputStream to decompress the data
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData.array());
		GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);

		// Use a ByteArrayOutputStream to hold the decompressed data
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		// Create a buffer to read and write data
		byte[] buffer = new byte[1024];
		int len;
		while ((len = gzipInputStream.read(buffer)) > 0) {
			byteArrayOutputStream.write(buffer, 0, len);
		}

		// Convert the decompressed data into a ByteBuf and return it
		return Unpooled.wrappedBuffer(byteArrayOutputStream.toByteArray());
	}

	@SideOnly(Side.CLIENT)
	public void handleClientPackets()
	{
		while(!receivedPacketsClient.isEmpty())
		{
			PacketBase packet = receivedPacketsClient.poll();
			packet.handleClientSide(getClientPlayer());
		}
	}

	public void handleServerPackets()
	{
		for(String playerName : receivedPacketsServer.keySet())
		{
			ConcurrentLinkedQueue<PacketBase> receivedPacketsFromPlayer = receivedPacketsServer.get(playerName);
			EntityPlayerMP player = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(playerName);
			while(!receivedPacketsFromPlayer.isEmpty())
			{
				PacketBase packet = receivedPacketsFromPlayer.poll();
				packet.handleServerSide(player);
			}
		}
	}

	/**
	 * Initialisation method called from FMLInitializationEvent in WarForgeMod
	 */
	public void initialise()
	{
		channels = NetworkRegistry.INSTANCE.newChannel("WarForgeMod", this);

		registerPacket(PacketCreateFaction.class);
		registerPacket(PacketRequestFactionInfo.class);
		registerPacket(PacketFactionInfo.class);
		registerPacket(PacketSiegeCampInfo.class);
		registerPacket(PacketSiegeCampProgressUpdate.class);
		registerPacket(PacketStartSiege.class);
		registerPacket(PacketLeaderboardInfo.class);
		registerPacket(PacketRequestLeaderboardInfo.class);
		registerPacket(PacketDisbandFaction.class);
		registerPacket(PacketRemoveClaim.class);
		registerPacket(PacketPlaceFlag.class);
		registerPacket(PacketTimeUpdates.class);
		registerPacket(PacketSetFactionColour.class);
		registerPacket(PacketMoveCitadel.class);
		registerPacket(PacketCitadelUpgradeRequirement.class);
		registerPacket(PacketRequestUpgradeUI.class);
		registerPacket(PacketUpgradeUI.class);
		registerPacket(PacketRequestUpgrade.class);
		registerPacket(PacketEffect.class);
		registerPacket(PacketNamePlateChange.class);
		registerPacket(PacketRequestNamePlate.class);
		registerPacket(PacketChunkPosVeinID.class);
		registerPacket(PacketVeinEntries.class);
		registerPacket(PacketSyncConfig.class);
	}

	/**
	 * Post-Initialisation method called from FMLPostInitializationEvent in WarForgeMod
	 * Logically sorts the packets client and server side to ensure a matching ordering
	 */
	public void postInitialise()
	{
		if(modInitialised)
			return;

		modInitialised = true;
		//Define our comparator on the fly and apply it to our list
		packets.sort((c1, c2) ->
		{
			int com = String.CASE_INSENSITIVE_ORDER.compare(c1.getCanonicalName(), c2.getCanonicalName());
			if(com == 0)
				com = c1.getCanonicalName().compareTo(c2.getCanonicalName());
			return com;
		});
	}

	@SideOnly(Side.CLIENT)
	private EntityPlayer getClientPlayer()
	{
		return Minecraft.getMinecraft().player;
	}

	/**
	 * Send a packet to all players
	 */
	public void sendToAll(PacketBase packet)
	{
		channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
		channels.get(Side.SERVER).writeAndFlush(packet);
	}

	/**
	 * Send a packet to a player
	 */
	public void sendTo(PacketBase packet, EntityPlayerMP player)
	{
		channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
		channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
		channels.get(Side.SERVER).writeAndFlush(packet);
	}

	/**
	 * Send a packet to all around a point
	 */
	public void sendToAllAround(PacketBase packet, NetworkRegistry.TargetPoint point)
	{
		channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
		channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(point);
		channels.get(Side.SERVER).writeAndFlush(packet);
	}

	/**
	 * Send a packet to all in a dimension
	 */
	public void sendToDimension(PacketBase packet, int dimensionID)
	{
		channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.DIMENSION);
		channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(dimensionID);
		channels.get(Side.SERVER).writeAndFlush(packet);
	}

	/**
	 * Send a packet to the server
	 */
	public void sendToServer(PacketBase packet)
	{
		channels.get(Side.CLIENT).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
		channels.get(Side.CLIENT).writeAndFlush(packet);
	}

	//Vanilla packets follow

	/**
	 * Send a packet to all players
	 */
	public void sendToAll(Packet<?> packet)
	{
		FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendPacketToAllPlayers(packet);
	}

	/**
	 * Send a packet to a player
	 */
	public void sendTo(Packet<?> packet, EntityPlayerMP player)
	{
		player.connection.sendPacket(packet);
	}

	/**
	 * Send a packet to all around a point
	 */
	public void sendToAllAround(Packet<?> packet, NetworkRegistry.TargetPoint point)
	{
		FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendToAllNearExcept(null, point.x, point.y, point.z, point.range, point.dimension, packet);
	}

	/**
	 * Send a packet to all in a dimension
	 */
	public void sendToDimension(Packet<?> packet, int dimensionID)
	{
		FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendPacketToAllPlayersInDimension(packet, dimensionID);
	}

	/**
	 * Send a packet to the server
	 */
	public void sendToServer(Packet<?> packet)
	{
		Minecraft.getMinecraft().player.connection.sendPacket(packet);
	}

	/**
	 * Send a packet to all around a point without having to create one's own TargetPoint
	 */
	public void sendToAllAround(PacketBase packet, double x, double y, double z, float range, int dimension)
	{
		sendToAllAround(packet, new NetworkRegistry.TargetPoint(dimension, x, y, z, range));
	}
}
