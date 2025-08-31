package com.flansmod.warforge.client;

import akka.japi.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.init.VeinUtils;
import com.flansmod.warforge.common.*;
import akka.japi.Pair;
import com.flansmod.warforge.client.util.RenderUtil;
import com.flansmod.warforge.client.util.ScreenSpaceUtil;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.IClaim;
import com.flansmod.warforge.common.network.PacketChunkPosVeinID;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.StackComparable;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBanner;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static com.flansmod.warforge.client.ClientProxy.CHUNK_VEIN_CACHE;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.flansmod.warforge.client.ClientProxy.CHUNK_VEIN_CACHE;
import static com.flansmod.warforge.client.util.RenderUtil.*;

public class ClientTickHandler {
    final static double alignment = 0.25d;
    final static double smaller_alignment = alignment - 0.125d;
    private static final ResourceLocation texture = new ResourceLocation(WarForgeMod.MODID, "world/borders.png");
    private static final ResourceLocation fastTexture = new ResourceLocation(WarForgeMod.MODID, "world/borders_fast.png");
    private static final ResourceLocation overlayTex = new ResourceLocation(WarForgeMod.MODID, "world/overlay.png");
    private static final ResourceLocation siegeprogress = new ResourceLocation(WarForgeMod.MODID, "gui/siegeprogressslim.png");
    public static long nextSiegeDayMs = 0L;
    public static long nextYieldDayMs = 0L;
    public static long timerSiegeEndStamp = 0L;
   	public static boolean CLAIMS_DIRTY = false;
    public static boolean UI_DEBUG = false;
    public static boolean TIMER_DEBUG = false;
    private final Tessellator tess;
    private final ModelBanner bannerModel = new ModelBanner();
    private final HashMap<ItemStack, ResourceLocation> bannerTextures = new HashMap<ItemStack, ResourceLocation>();
    private final int renderList = GLAllocation.generateDisplayLists(1);
    private DimChunkPos playerChunkPos = new DimChunkPos(0, 0, 0);
    private float newAreaToastTime = 0;
    private String areaMessage = "";
    private int areaMessageColour = 0xFF_FF_FF_FF;
    private HashMap<DimChunkPos, BorderRenderData> renderData = new HashMap<>();

	// -1 indicates the chunk wasn't the targeting of previous probe(s)
	private static ArrayList<String> cachedCompStrings = null;
	private static Object2LongOpenHashMap<DimChunkPos> permitChunkReprobeMs = new Object2LongOpenHashMap<>();
	private static long lastRenderStartTimeMs = -1;  // (curr time - this) / (display time (ms)) to get index
	private static Iterator<StackComparable> compIt = null;
	private static StackComparable currComp = null;

    public ClientTickHandler() {
        tess = Tessellator.getInstance();
    toggleBordersKey = new KeyBinding("key.warforge.showborders", Keyboard.KEY_B, "key.warforge.cathegory");
		ClientRegistry.registerKeyBinding(toggleBordersKey);

	}
	public static KeyBinding toggleBordersKey;

    @SubscribeEvent
    public void onPlayerLogin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        // init and clear stale data
		permitChunkReprobeMs = new Object2LongOpenHashMap<>();
		permitChunkReprobeMs.defaultReturnValue(-1);
		lastRenderStartTimeMs = -1;
		compIt = null;
		currComp = null;
		cachedCompStrings = null;

        // clear stale data
        CHUNK_VEIN_CACHE.purge();
        ClientProxy.VEIN_ENTRIES.clear();
        WarForgeMod.NAMETAG_CACHE.purge(); //Purge to remove possible stale data
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent tick) {

        // Handle client packets and perform the client-side tick
        WarForgeMod.NETWORK.handleClientPackets();
        WarForgeMod.proxy.TickClient();

        // Use a more efficient approach for expired siege info removal
        ArrayList<DimBlockPos> expired = null;

        // Iterate over entries and mark completed ones for removal
        for (HashMap.Entry<DimBlockPos, SiegeCampProgressInfo> kvp : ClientProxy.sSiegeInfo.entrySet()) {
            SiegeCampProgressInfo siegeInfo = kvp.getValue();
            siegeInfo.ClientTick();
            if (siegeInfo.Completed()) {
                if (expired == null) expired = new ArrayList<>();
                expired.add(kvp.getKey());
            }
        }

        // Remove completed siege camps from the map
        if (expired != null) {
            for (DimBlockPos pos : expired) {
                ClientProxy.sSiegeInfo.remove(pos);
            }
        }

        // Handle new area toast time
        if (newAreaToastTime > 0.0f) {
            newAreaToastTime--;
        }

        // Avoid calling Minecraft.getMinecraft() multiple times
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && player.ticksExisted % 200 == 0) {
            CLAIMS_DIRTY = true;
        }

        if (player != null) {
            DimChunkPos standing = new DimChunkPos(player.dimension, player.getPosition());

            // when we leave a chunk, restart iteration on vein members
            if (!standing.equals(playerChunkPos)) {
				lastRenderStartTimeMs = -1;
            }

            // Show new area timer if configured
            if (WarForgeConfig.SHOW_NEW_AREA_TIMER > 0.0f) {

                // Only perform claim checks if the player has moved to a new chunk
                if (!standing.equals(playerChunkPos)) {
                    IClaim preClaim = null;
                    IClaim postClaim = null;

                    // Iterate only through the necessary tile entities (avoid loading all entities unnecessarily)
                    for (TileEntity te : player.world.loadedTileEntityList) {
                        if (te instanceof IClaim && !((IClaim) te).getFaction().equals(Faction.nullUuid)) {
                            DimChunkPos tePos = ((IClaim) te).getClaimPos().toChunkPos();
                            if (tePos.equals(playerChunkPos)) {
                                preClaim = (IClaim) te;
                            }
                            if (tePos.equals(standing)) {
                                postClaim = (IClaim) te;
                            }
                        }
                    }

                    // Generate area message only if needed (reduce redundant logic)
                    if (preClaim == null) {
                        if (postClaim != null) {
                            // Entered a new claim
                            areaMessage = "Entering " + postClaim.getClaimDisplayName();
                            areaMessageColour = postClaim.getColour();
                            newAreaToastTime = WarForgeConfig.SHOW_NEW_AREA_TIMER;
                        }
                    } else // Left a claim
                    {
                        if (postClaim == null) {
                            // Gone nowhere
                            areaMessage = "Leaving " + preClaim.getClaimDisplayName();
                            areaMessageColour = preClaim.getColour();
                            newAreaToastTime = WarForgeConfig.SHOW_NEW_AREA_TIMER;
                        } else {
                            // Entered another claim, possibly different faction
                            if (!preClaim.getFaction().equals(postClaim.getFaction())) {
                                areaMessage = "Leaving " + preClaim.getClaimDisplayName() + ", Entering " + postClaim.getClaimDisplayName();
                                areaMessageColour = postClaim.getColour();
                                newAreaToastTime = WarForgeConfig.SHOW_NEW_AREA_TIMER;
                            }
                        }
                    }

                    playerChunkPos = standing;
                }
            }
        }


    }

    @SubscribeEvent
    public void onRenderHUD(RenderGameOverlayEvent event) {
        ScreenSpaceUtil.resetOffsets(event);

        if (event.getType() == ElementType.BOSSHEALTH) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;

            if (player != null) {

                // Siege camp info
                SiegeCampProgressInfo infoToRender = !UI_DEBUG ? getClosestSiegeCampInfo(player) : SiegeCampProgressInfo.getDebugInfo();

                if (infoToRender != null) {
                    renderSiegeOverlay(mc, infoToRender, event);
                }

                // Timer info
                if (WarForgeConfig.SHOW_YIELD_TIMERS) {
                    renderTimers(mc);
                }

                // New Area Toast
                if (newAreaToastTime > 0.0f) {
                    renderNewAreaToast(mc, event);
                }

				// get the vein info
				if (player.isSneaking()) {
					DimChunkPos currPos = new DimChunkPos(player.dimension, player.getPosition());
					boolean hasPosData = CHUNK_VEIN_CACHE.isReceived(currPos);
					boolean hasValidData = hasPosData && CHUNK_VEIN_CACHE.isRecognized(currPos);
					Pair<Vein, Quality> veinInfo = CHUNK_VEIN_CACHE.get(currPos);

					// probe the server for the data for this chunk
					if (!hasValidData && permitChunkReprobeMs.getLong(currPos) <= System.currentTimeMillis()) {
						WarForgeMod.LOGGER.atInfo().log("Pinging server for chunk vein info");
						permitChunkReprobeMs.put(currPos, System.currentTimeMillis() + 5000);  // only ping every 5s as needed
						PacketChunkPosVeinID packetChunkVeinRequest = new PacketChunkPosVeinID();
						packetChunkVeinRequest.veinLocation = currPos;
						WarForgeMod.NETWORK.sendToServer(packetChunkVeinRequest);
					}

					renderVeinData(mc, veinInfo, hasPosData, event);
				}

            }
        }
    }

    private void renderTimers(Minecraft mc) {
        int screenWidth = ScreenSpaceUtil.RESOLUTIONX;

        int padding = 4;
        int textHeight = ScreenSpaceUtil.TEXTHEIGHT + padding;

        ScreenSpaceUtil.ScreenPos pos = WarForgeConfig.POS_TIMERS;

        // Siege progress
        if (!WarForgeConfig.SIEGE_ENABLE_NEW_TIMER || UI_DEBUG) {
            String siegeText = "Siege Progress: " + formatPaddedTimer(nextSiegeDayMs - System.currentTimeMillis());
            int textWidth = mc.fontRenderer.getStringWidth(siegeText);
            int x = ScreenSpaceUtil.shouldCenterX(pos) ? ScreenSpaceUtil.centerX(screenWidth, textWidth) : ScreenSpaceUtil.getX(pos, textWidth) + ScreenSpaceUtil.getXOffset(pos, padding);
            int ySiege = pos.getY() + ScreenSpaceUtil.getYOffset(pos, textHeight);

            mc.fontRenderer.drawStringWithShadow(siegeText, x, ySiege, 0xffffff);
            ScreenSpaceUtil.incrementY(pos, textHeight);
        }

        // Next yields
        String yieldText = "Next yields: " + formatPaddedTimer(nextYieldDayMs - System.currentTimeMillis());
        int textWidth = mc.fontRenderer.getStringWidth(yieldText);
        int x = ScreenSpaceUtil.shouldCenterX(pos) ? ScreenSpaceUtil.centerX(screenWidth, textWidth) : ScreenSpaceUtil.getX(pos, textWidth) + ScreenSpaceUtil.getXOffset(pos, padding);
        int yYield = pos.getY() + ScreenSpaceUtil.getYOffset(pos, textHeight);

        mc.fontRenderer.drawStringWithShadow(yieldText, x, yYield, 0xffffff);
        ScreenSpaceUtil.incrementY(pos, textHeight);
    }

    public static String formatPaddedTimer(long msRemaining) {
        long s = msRemaining / 1000;
        long m = s / 60;
        long h = m / 60;
        long d = h / 24;

        return (d > 0 ? (d) + " days, " : "") + String.format("%02d", (h % 24)) + ":" + String.format("%02d", (m % 60)) + ":" + String.format("%02d", (s % 60));
    }

    private SiegeCampProgressInfo getClosestSiegeCampInfo(EntityPlayerSP player) {
        SiegeCampProgressInfo closestInfo = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (SiegeCampProgressInfo info : ClientProxy.sSiegeInfo.values()) {
            double distSq = info.defendingPos.distanceSq(player.posX, player.posY, player.posZ);
            if (info.defendingPos.dim == player.dimension && distSq < WarForgeConfig.SIEGE_INFO_RADIUS * WarForgeConfig.SIEGE_INFO_RADIUS) {
                if (distSq < bestDistanceSq) {
                    bestDistanceSq = distSq;
                    closestInfo = info;
                }
            }
        }

        return closestInfo;
    }

	private void renderVeinData(Minecraft mc, Pair<Vein, Quality> veinInfo, boolean hasData, RenderGameOverlayEvent event) {
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();

		// even if we aren't rendering, count up start time to indicate we are within the same chunk as before
		boolean isNewChunk = lastRenderStartTimeMs == -1;
		long currTimeMs = System.currentTimeMillis();

		// even though intelliJ thinks veinInfo is never null, it definitely should be able to be
		// we render either the item, or some waiting icon
		ItemStack currMemberItemStack = null;
		boolean hasItemToRender = veinInfo != null && veinInfo.first() != null && veinInfo.first().compIds.size() > 0;
		if (hasItemToRender)  {
			// initialize render info
			if (lastRenderStartTimeMs == -1) {
				lastRenderStartTimeMs = currTimeMs;
				compIt = veinInfo.first().compIds.iterator();  // LinkedHashSet should give a consistent ordering
				currComp = compIt.next();
			}

			// check if the display time is up
			else if (currTimeMs - lastRenderStartTimeMs > WarForgeConfig.VEIN_MEMBER_DISPLAY_TIME_MS) {
				lastRenderStartTimeMs = currTimeMs;  // we are updating the component that we are rendering
				if (!compIt.hasNext()) { compIt = veinInfo.first().compIds.iterator(); } // restart from beginning
				currComp = compIt.next();
			}

			currMemberItemStack = currComp.toItem();

			if (currMemberItemStack == null) {
				WarForgeMod.LOGGER.atError().log("Got unexpected null stack for vein " + veinInfo.first().toString());
				return;
			}
		}

		// either use the cached string or make a new one if either no cached exists or we are in a new chunk
		ArrayList<String> compInfoStrings = cachedCompStrings;
		if (cachedCompStrings == null || isNewChunk) {
			compInfoStrings = createVeinInfoStrings(veinInfo, hasData);
		}

        ScreenSpaceUtil.ScreenPos veinPos = WarForgeConfig.POS_VEIN_INDICATOR;
		final int imageSize = hasItemToRender ? 24 : 0;

		// draw the vein info
        final int textHeight = ScreenSpaceUtil.TEXTHEIGHT;
        final int veinTitleWidth = mc.fontRenderer.getStringWidth(compInfoStrings.get(0));
        final int titleX = ScreenSpaceUtil.getX(veinPos, veinTitleWidth);
        final int veinInfoHeight = Math.max(textHeight, (textHeight + imageSize) / 2);
        final int titleY = ScreenSpaceUtil.getY(veinPos, veinInfoHeight) + 4 + (veinInfoHeight - textHeight);
        final int imageX = titleX - imageSize / 2;  // offset the image to the left of the title + gap

        if (titleY > WarForgeConfig.HUD_VERT_CUTOFF_PERCENT * ScreenSpaceUtil.RESOLUTIONY) { return; }  // don't overdraw onto main part of screen
		mc.fontRenderer.drawStringWithShadow(compInfoStrings.get(0), titleX, titleY, 0xFFFFFF);
        veinPos.incrementY(textHeight);

		// draw the item
		if (currMemberItemStack != null) {
			// prepare to render
			GlStateManager.pushMatrix();
			RenderHelper.disableStandardItemLighting();
			GlStateManager.enableDepth();

			// render the item
            // y offset is position of dead center of image, so we need to offset down to align with the text
			GlStateManager.translate(imageX - 4, titleY + textHeight / 2f, 0);
			GlStateManager.scale(imageSize, imageSize, 1);
			GlStateManager.rotate(180, 0, 1, 0);
			GlStateManager.rotate(180, 0, 0, 1);

			RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
			renderItem.renderItem(currMemberItemStack, ItemCameraTransforms.TransformType.GUI);

			// disable the things we just used
			GlStateManager.disableDepth();
			RenderHelper.enableStandardItemLighting();
			GlStateManager.disableLighting();
			GlStateManager.popMatrix();
		}

		// draw the component strings
		for (int i = 1; i < compInfoStrings.size(); ++i) {
            // we want the components to look left aligned and indented
			String currFormattedComp = compInfoStrings.get(i);
			mc.fontRenderer.drawStringWithShadow(currFormattedComp, titleX + 4, veinPos.getY(), 0xFFFFFF);
            veinPos.incrementY(textHeight);
		}
	}

	private ArrayList<String> createVeinInfoStrings(Pair<Vein, Quality> veinInfo, boolean hasCached) {
		ArrayList<String> result = new ArrayList<>(1);

		// handle no data specially
		if (!hasCached) {
			result.add(I18n.format("warforge.info.vein.waiting"));
			return result;
		}

		// handle null veins specially
		if (veinInfo == null) {
			result.add(I18n.format("warforge.info.vein.null"));
			return result;
		}

		// translate and format the vein name by supplying the localized quality name as an argument
		Vein currVein = veinInfo.first();
		Quality currQual = veinInfo.second();

		// handle unrecognized veins specially
		if (currVein == null || currQual == null) {
			result.add(I18n.format("warforge.info.vein.unrecognized"));
			return result;
		}

		// vein is now guaranteed valid and received; begin formatting data to be displayed
		result.add(I18n.format(currVein.translationKey, I18n.format(currQual.getTranslationKey())));
		int dim = Minecraft.getMinecraft().player.dimension;

		// turn each component into the item we will be displaying and list them
		for (StackComparable currComp : currVein.compIds) {
			ItemStack currStack = currComp.toItem();
			if (currStack == null) {
				WarForgeMod.LOGGER.atError().log("Couldn't find item with component id " +
						currComp + " in vein " + currVein.translationKey);
				continue;
			}

			// janky work around for weird default minecraft items which sometimes decide to append .name to the key
			// without updating the translation key the item itself returns
			String translationKey = currStack.getItem().getTranslationKey();
			if (!I18n.hasKey(translationKey) && I18n.hasKey(translationKey + ".name")) { translationKey += ".name"; }

			// if we got an item stack, translate it and display information about it
			StringBuilder compInfo = new StringBuilder(I18n.format(translationKey));
			parseCompInfo(compInfo, currComp, veinInfo, dim);
			result.add(compInfo.toString());
		}

		return result;
	}

	private void parseCompInfo(StringBuilder compInfoStr, StackComparable currComp, Pair<Vein, Quality> veinInfo, int dim) {
		compInfoStr.append(":");
		ArrayList<short[]> yieldInfos = VeinUtils.getYieldInfo(currComp, veinInfo, dim);  // weight, guaranteed yield, % extra yield

		// format each subComp in the form <guaranteedYield# - %comp; +%extra> to show yield distribution for item
		for (short[] subCompInfo : yieldInfos) {
			// show the guaranteed yield info and component weight
			compInfoStr.append(" <[");
			compInfoStr.append(subCompInfo[1]);
			compInfoStr.append("]-");
			compInfoStr.append(VeinUtils.shortToPercentStr(subCompInfo[0]));

			// if there is a chance for an extra yield, display as much
			if (subCompInfo[2] > 0) {
				compInfoStr.append(";+");
				compInfoStr.append(VeinUtils.shortToPercentStr(subCompInfo[2]));
			}

			compInfoStr.append(">,");  // setup next sub comp
		}

		// there will be an extra comma at the end
		compInfoStr.deleteCharAt(compInfoStr.length() - 1);
	}

	private void renderSiegeOverlay(Minecraft mc, SiegeCampProgressInfo infoToRender, RenderGameOverlayEvent event) {
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();

		// Colors for attacking and defending
		float attackR = (float)(infoToRender.attackingColour >> 16 & 255) / 255.0F;
		float attackG = (float)(infoToRender.attackingColour >> 8 & 255) / 255.0F;
		float attackB = (float)(infoToRender.attackingColour & 255) / 255.0F;
		float defendR = (float)(infoToRender.defendingColour >> 16 & 255) / 255.0F;
		float defendG = (float)(infoToRender.defendingColour >> 8 & 255) / 255.0F;
		float defendB = (float)(infoToRender.defendingColour & 255) / 255.0F;

		// Render Background and Bars
        var pos = WarForgeConfig.POS_SIEGE;
		int xText = ScreenSpaceUtil.getX(pos, 256);  // 256 = width of bar
		int yText = ScreenSpaceUtil.getY(pos, 40);   // 40 = total height (bar + text)

		float scroll = (mc.getFrameTimer().getIndex() + event.getPartialTicks()) * 0.25f;
		scroll = scroll % 10;

        mc.renderEngine.bindTexture(siegeprogress);
        GlStateManager.color(1f, 1f, 1f, 1f);
        RenderUtil.drawTexturedModalRect(tess, xText, yText, 0, 0, 256, 30);

        renderSiegeProgressBar(mc, infoToRender, xText, yText, attackR, attackG, attackB, defendR, defendG, defendB, scroll);
        renderSiegeNotches(mc, infoToRender, xText, yText);

        renderSiegeText(mc, infoToRender, xText, yText);
        if(WarForgeConfig.SIEGE_ENABLE_NEW_TIMER)
            renderSiegeTimer(mc, infoToRender, xText, yText+5);
    }

    private void renderSiegeTimer(Minecraft mc, SiegeCampProgressInfo infoToRender, int xText, int yText){
        String siegeText = formatPaddedTimer( infoToRender.endTimestamp - System.currentTimeMillis() );
        int textWidth = mc.fontRenderer.getStringWidth(siegeText);
        int color =  infoToRender.endTimestamp - System.currentTimeMillis() < 60000 ? 0xFF0000 : 0xFFFFFF;

        mc.fontRenderer.drawStringWithShadow(siegeText, xText+(128-textWidth/2), yText + 28, color);
        if(TIMER_DEBUG){
            mc.fontRenderer.drawStringWithShadow( "End timestamp :"+ infoToRender.endTimestamp, xText-textWidth, yText + 30, 0xFFFFFF);
            mc.fontRenderer.drawStringWithShadow("Raw timestamp difference: "+ (infoToRender.endTimestamp - System.currentTimeMillis()), xText-textWidth, yText + 40, 0xFFFFFF);
        }
    }

    private void renderSiegeProgressBar(Minecraft mc, SiegeCampProgressInfo infoToRender, int xText, int yText, float attackR, float attackG, float attackB, float defendR, float defendG, float defendB, float scroll) {
        int xSize = 256;
        float siegeLength = infoToRender.completionPoint + 5;
        float notchDistance = 224 / siegeLength;

        int firstPx = (int) (notchDistance * (infoToRender.progress > 0 ? 5 : 5 + infoToRender.progress));
        int lastPx = (int) (notchDistance * (infoToRender.progress > 0 ? (infoToRender.progress + 5) : 5));

        boolean isIncreasing = infoToRender.progress > infoToRender.mPreviousProgress;

        if (isIncreasing) {
            GlStateManager.color(attackR, attackG, attackB, 1.0F);
            RenderUtil.drawTexturedModalRect(tess, xText + 16 + firstPx, yText + 17, 16 + (10 - scroll), 44, lastPx - firstPx, 8);
        } else {
            GlStateManager.color(defendR, defendG, defendB, 1.0F);
            RenderUtil.drawTexturedModalRect(tess, xText + 16 + firstPx, yText + 17, 16 + scroll, 54, lastPx - firstPx, 8);
        }
    }

    private void renderSiegeNotches(Minecraft mc, SiegeCampProgressInfo infoToRender, int xText, int yText) {
        float notchDistance = (float) 224 / (infoToRender.completionPoint + 5);

        for (int i = -4; i < infoToRender.completionPoint; i++) {
            int x = (int) ((i + 5) * notchDistance + 16);
            if (i == 0) RenderUtil.drawTexturedModalRect(tess, xText + x - 2, yText + 17, 6, 43, 5, 8);
            else RenderUtil.drawTexturedModalRect(tess, xText + x - 2, yText + 17, 1, 43, 4, 8);
        }
    }

    private void renderSiegeText(Minecraft mc, SiegeCampProgressInfo infoToRender, int xText, int yText) {
        mc.fontRenderer.drawStringWithShadow(infoToRender.defendingName, xText + 6, yText + 6, infoToRender.defendingColour);
        mc.fontRenderer.drawStringWithShadow("VS", xText + 128 - (float) mc.fontRenderer.getStringWidth("VS") / 2, yText + 6, 0xffffff);
        mc.fontRenderer.drawStringWithShadow(infoToRender.attackingName, xText + 256 - 6 - mc.fontRenderer.getStringWidth(infoToRender.attackingName), yText + 6, infoToRender.attackingColour);

        String toWin = (infoToRender.progress < infoToRender.completionPoint) ? (infoToRender.completionPoint - infoToRender.progress) + " to win" : "Station siege to win";
        String toDefend = (infoToRender.progress + 5) + " to defend";
        mc.fontRenderer.drawStringWithShadow(toWin, xText + 256 - 8 - mc.fontRenderer.getStringWidth(toWin), yText + 32, infoToRender.attackingColour);
        mc.fontRenderer.drawStringWithShadow(toDefend, xText + 8, yText + 32, infoToRender.attackingColour);
    }

    private void renderNewAreaToast(Minecraft mc, RenderGameOverlayEvent event) {
        final int stringWidth = mc.fontRenderer.getStringWidth(areaMessage);
        final int totalHeight = 24;

        final ScreenSpaceUtil.ScreenPos pos = WarForgeConfig.POS_TOAST_INDICATOR;
        final boolean isTop = ScreenSpaceUtil.isTop(pos);
        final int extraPadding = pos == ScreenSpaceUtil.ScreenPos.TOP ? 24 : 0;

        final int yOffsetFromBar = 14;  // offset below/above the hotbar or title bar
        final int yText = isTop ? pos.getY() + yOffsetFromBar + extraPadding : pos.getY() - totalHeight - yOffsetFromBar;

        final int xText = ScreenSpaceUtil.getX(pos, stringWidth) + ScreenSpaceUtil.getXOffset(pos, 10);

        float fadeOut = 2.0f * newAreaToastTime / WarForgeConfig.SHOW_NEW_AREA_TIMER;
        fadeOut = Math.min(fadeOut, 1.0f);
        int colour = areaMessageColour | ((int) (fadeOut * 255f) << 24);

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, fadeOut);
        GlStateManager.disableTexture2D();

        RenderUtil.drawTexturedModalRect(tess, xText - 50, yText, 0, 0, stringWidth + 100, 1);            // top line
        RenderUtil.drawTexturedModalRect(tess, xText - 25, yText + 23, 0, 0, stringWidth + 50, 1);        // bottom line

        GlStateManager.enableTexture2D();
        mc.fontRenderer.drawStringWithShadow(areaMessage, xText, yText + 11, colour);   // vertically centered text

        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        ScreenSpaceUtil.incrementY(pos, totalHeight + 14 + extraPadding);
    }

    private void updateRenderData() {
        World world = Minecraft.getMinecraft().world;
        if (world == null) return;

        // Update our list from the old one
        HashMap<DimChunkPos, BorderRenderData> tempData = new HashMap<DimChunkPos, BorderRenderData>();

        // Find all our data entries first
        for (TileEntity te : world.loadedTileEntityList) {
            if (te instanceof IClaim) {
                if (((IClaim) te).getFaction().equals(Faction.nullUuid)) continue;
                DimBlockPos blockPos = ((IClaim) te).getClaimPos();
                DimChunkPos chunkPos = blockPos.toChunkPos();

                if (renderData.containsKey(chunkPos)) {
                    tempData.put(chunkPos, renderData.get(chunkPos));
                } else {
                    BorderRenderData data = new BorderRenderData();
                    data.claim = (IClaim) te;
                    tempData.put(chunkPos, data);
                }
            }
        }

        renderData = tempData;

    }

    private void updateRandomMesh() {
        World world = Minecraft.getMinecraft().world;
        if (world == null || renderData.isEmpty()) return;
        int index = world.rand.nextInt(renderData.size());

        // Then construct the mesh for one random entry
        for (HashMap.Entry<DimChunkPos, BorderRenderData> kvp : renderData.entrySet()) {
            if (index > 0) {
                index--;
                continue;
            }

            DimChunkPos pos = kvp.getKey();
            BorderRenderData data = kvp.getValue();

            if (data.renderList != 0) {
                GlStateManager.glDeleteLists(data.renderList, 1);
                data.renderList = 0;
            }
            data.renderList = GLAllocation.generateDisplayLists(1);
            GlStateManager.glNewList(data.renderList, 4864);

            boolean renderNorth = true, renderEast = true, renderWest = true, renderSouth = true, renderNorthWest = true, renderNorthEast = true, renderSouthWest = true, renderSouthEast = true;
            if (renderData.containsKey(pos.north()))
                renderNorth = !renderData.get(pos.north()).claim.getFaction().equals(data.claim.getFaction());
            if (renderData.containsKey(pos.east()))
                renderEast = !renderData.get(pos.east()).claim.getFaction().equals(data.claim.getFaction());
            if (renderData.containsKey(pos.south()))
                renderSouth = !renderData.get(pos.south()).claim.getFaction().equals(data.claim.getFaction());
            if (renderData.containsKey(pos.west()))
                renderWest = !renderData.get(pos.west()).claim.getFaction().equals(data.claim.getFaction());

            //for super spesific edge cases
            if (renderData.containsKey(pos.north().west()))
                renderNorthWest = !renderData.get(pos.north().west()).claim.getFaction().equals(data.claim.getFaction());
            if (renderData.containsKey(pos.north().east()))
                renderNorthEast = !renderData.get(pos.north().east()).claim.getFaction().equals(data.claim.getFaction());
            if (renderData.containsKey(pos.south().west()))
                renderSouthWest = !renderData.get(pos.south().west()).claim.getFaction().equals(data.claim.getFaction());
            if (renderData.containsKey(pos.south().east()))
                renderSouthEast = !renderData.get(pos.south().east()).claim.getFaction().equals(data.claim.getFaction());

            // North edge, [0,0] -> [16,0] wall
            if (renderNorth) {
                // A smidge of semi-translucent wall from [0,0,0] to [2,256,0] offset by 0.25
                if (renderWest) {
                    tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                    tess.getBuffer().pos(0 + alignment, 0, alignment).tex(64f, 0.5f).endVertex();
                    tess.getBuffer().pos(2 + alignment, 0, alignment).tex(64f, 0f).endVertex();
                    tess.getBuffer().pos(2 + alignment, 128, alignment).tex(0f, 0f).endVertex();
                    tess.getBuffer().pos(0 + alignment, 128, alignment).tex(0f, 0.5f).endVertex();
                    tess.draw();
                }

                // A smidge of semi-translucent wall from [14,0,0] to [16,256,0] offset by 0.25
                if (renderEast) {
                    tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                    tess.getBuffer().pos(16 - alignment, 0, alignment).tex(64f, 0.5f).endVertex();
                    tess.getBuffer().pos(14 - alignment, 0, alignment).tex(64f, 0f).endVertex();
                    tess.getBuffer().pos(14 - alignment, 128, alignment).tex(0f, 0f).endVertex();
                    tess.getBuffer().pos(16 - alignment, 128, alignment).tex(0f, 0.5f).endVertex();
                    tess.draw();
                }
            }

            // South edge
            if (renderSouth) {
                if (renderWest) {
                    tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                    tess.getBuffer().pos(0 + alignment, 0, 16d - alignment).tex(64f, 0.5f).endVertex();
                    tess.getBuffer().pos(2 + alignment, 0, 16d - alignment).tex(64f, 0f).endVertex();
                    tess.getBuffer().pos(2 + alignment, 128, 16d - alignment).tex(0f, 0f).endVertex();
                    tess.getBuffer().pos(0 + alignment, 128, 16d - alignment).tex(0f, 0.5f).endVertex();
                    tess.draw();
                }

                if (renderEast) {
                    tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                    tess.getBuffer().pos(16 - alignment, 0, 16d - alignment).tex(64f, 0.5f).endVertex();
                    tess.getBuffer().pos(14 - alignment, 0, 16d - alignment).tex(64f, 0f).endVertex();
                    tess.getBuffer().pos(14 - alignment, 128, 16d - alignment).tex(0f, 0f).endVertex();
                    tess.getBuffer().pos(16 - alignment, 128, 16d - alignment).tex(0f, 0.5f).endVertex();
                    tess.draw();
                }
            }

            // East edge, [0,0] -> [0,16] wall
            if (renderWest) {
                if (renderNorth) {
                    tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                    tess.getBuffer().pos(alignment, 0, 0 + alignment).tex(64f, 0.5f).endVertex();
                    tess.getBuffer().pos(alignment, 0, 2 + alignment).tex(64f, 0f).endVertex();
                    tess.getBuffer().pos(alignment, 128, 2 + alignment).tex(0f, 0f).endVertex();
                    tess.getBuffer().pos(alignment, 128, 0 + alignment).tex(0f, 0.5f).endVertex();
                    tess.draw();
                }

                if (renderSouth) {
                    tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                    tess.getBuffer().pos(alignment, 0, 16 - alignment).tex(64f, 0.5f).endVertex();
                    tess.getBuffer().pos(alignment, 0, 14 - alignment).tex(64f, 0f).endVertex();
                    tess.getBuffer().pos(alignment, 128, 14 - alignment).tex(0f, 0f).endVertex();
                    tess.getBuffer().pos(alignment, 128, 16 - alignment).tex(0f, 0.5f).endVertex();
                    tess.draw();
                }
            }

            // West edge
            if (renderEast) {
                if (renderNorth) {
                    tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                    tess.getBuffer().pos(16d - alignment, 0, 0 + alignment).tex(64f, 0.5f).endVertex();
                    tess.getBuffer().pos(16d - alignment, 0, 2 + alignment).tex(64f, 0f).endVertex();
                    tess.getBuffer().pos(16d - alignment, 128, 2 + alignment).tex(0f, 0f).endVertex();
                    tess.getBuffer().pos(16d - alignment, 128, 0 + alignment).tex(0f, 0.5f).endVertex();
                    tess.draw();
                }

                if (renderSouth) {
                    tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                    tess.getBuffer().pos(16d - alignment, 0, 16 - alignment).tex(64f, 0.5f).endVertex();
                    tess.getBuffer().pos(16d - alignment, 0, 14 - alignment).tex(64f, 0f).endVertex();
                    tess.getBuffer().pos(16d - alignment, 128, 14 - alignment).tex(0f, 0f).endVertex();
                    tess.getBuffer().pos(16d - alignment, 128, 16 - alignment).tex(0f, 0.5f).endVertex();
                    tess.draw();
                }
            }
            if (renderNorth || renderSouth) {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 256; y++) {
                        if (x < 15) {
                            if (renderNorth) {
                                boolean air0 = world.isAirBlock(new BlockPos(pos.getXStart() + x, y, pos.getZStart()));
                                boolean air1 = world.isAirBlock(new BlockPos(pos.getXStart() + x + 1, y, pos.getZStart()));
                                renderZEdge(world, tess, x, y, pos.getZStart(), smaller_alignment + 0.001d, air0, air1, 0);
                            }
                            if (renderSouth) {
                                boolean air0 = world.isAirBlock(new BlockPos(pos.getXStart() + x, y, pos.getZEnd()));
                                boolean air1 = world.isAirBlock(new BlockPos(pos.getXStart() + x + 1, y, pos.getZEnd()));
                                renderZEdge(world, tess, x, y, pos.getZEnd(), 16d - smaller_alignment + 0.001d, air0, air1, 0);
                            }
                        }
                        if (y < 255) {
                            if (renderNorth) {
                                boolean air0 = world.isAirBlock(new BlockPos(pos.getXStart() + x, y, pos.getZStart()));
                                boolean air1 = world.isAirBlock(new BlockPos(pos.getXStart() + x, y + 1, pos.getZStart()));
                                //renderZVerticalEdge(world, x, y, pos.getZStart(), smaller_alignment, air0, air1, 0);
                                if (x == 15 && renderEast) {
                                    renderZVerticalCorner(world, tess, x - smaller_alignment, y, smaller_alignment, air0, air1, 0, -smaller_alignment);
                                } else if (x == 0 && renderWest) {
                                    renderZVerticalCorner(world, tess, x, y, smaller_alignment, air0, air1, 0, -smaller_alignment);
                                } else {
                                    renderZVerticalEdge(world, tess, x, y, pos.getZStart(), smaller_alignment, air0, air1, 0);
                                }
                            }
                            if (renderSouth) {
                                boolean air0 = world.isAirBlock(new BlockPos(pos.getXStart() + x, y, pos.getZEnd()));
                                boolean air1 = world.isAirBlock(new BlockPos(pos.getXStart() + x, y + 1, pos.getZEnd()));
                                if (x == 15 && renderEast) {
                                    renderZVerticalCorner(world, tess, x - smaller_alignment, y, 16 - smaller_alignment, air0, air1, 0, -smaller_alignment);
                                } else if (x == 0 && renderWest) {
                                    renderZVerticalCorner(world, tess, x, y, 16 - smaller_alignment, air0, air1, 0, -smaller_alignment);
                                } else {
                                    renderZVerticalEdge(world, tess, x, y, pos.getZEnd(), 16d - smaller_alignment, air0, air1, 0);
                                }
                            }
                        }
                    }
                }
            }

            if (renderEast || renderWest) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        if (z < 15) {
                            if (renderWest) {
                                boolean air0 = world.isAirBlock(new BlockPos(pos.getXStart(), y, pos.getZStart() + z));
                                boolean air1 = world.isAirBlock(new BlockPos(pos.getXStart(), y, pos.getZStart() + z + 1));
                                renderXEdge(world, tess, pos.getXStart(), y, z, smaller_alignment + 0.001d, air0, air1, 0);
                            }
                            if (renderEast) {
                                boolean air0 = world.isAirBlock(new BlockPos(pos.getXEnd(), y, pos.getZStart() + z));
                                boolean air1 = world.isAirBlock(new BlockPos(pos.getXEnd(), y, pos.getZStart() + z + 1));
                                renderXEdge(world, tess, pos.getXEnd(), y, z, 16d - smaller_alignment + 0.001d, air0, air1, 0);
                            }
                        }
                        if (y < 255) {
                            if (renderWest) {
                                boolean air0 = world.isAirBlock(new BlockPos(pos.getXStart(), y, pos.getZStart() + z));
                                boolean air1 = world.isAirBlock(new BlockPos(pos.getXStart(), y + 1, pos.getZStart() + z));
                                if (z == 15 && renderSouth) {
                                    renderXVerticalCorner(world, tess, smaller_alignment, y, z - smaller_alignment, air0, air1, 0, -smaller_alignment);
                                } else if (z == 0 && renderNorth) {
                                    renderXVerticalCorner(world, tess, smaller_alignment, y, z, air0, air1, 0, -smaller_alignment);
                                } else {
                                    renderXVerticalEdge(world, tess, pos.getXStart(), y, z, smaller_alignment, air0, air1, 0);
                                }
                            }
                            if (renderEast) {
                                boolean air0 = world.isAirBlock(new BlockPos(pos.getXEnd(), y, pos.getZStart() + z));
                                boolean air1 = world.isAirBlock(new BlockPos(pos.getXEnd(), y + 1, pos.getZStart() + z));
                                if (z == 15 && renderSouth) {
                                    renderXVerticalCorner(world, tess, 16d - smaller_alignment, y, z - smaller_alignment, air0, air1, 0, -smaller_alignment);
                                } else if (z == 0 && renderNorth) {
                                    renderXVerticalCorner(world, tess, 16d - smaller_alignment, y, z, air0, air1, 0, -smaller_alignment);
                                } else {
                                    renderXVerticalEdge(world, tess, pos.getXEnd(), y, z, 16d - smaller_alignment, air0, air1, 0);
                                }
                            }
                        }
                    }
                }
            }

            //Edge corner cases because of autism

            if (renderNorthEast) {
                if (!renderNorth && !renderEast) {
                    for (int y = 0; y < 256; y++) {
                        boolean air0 = world.isAirBlock(new BlockPos(pos.getXEnd(), y, pos.getZStart()));
                        boolean air1 = world.isAirBlock(new BlockPos(pos.getXEnd(), y + 1, pos.getZStart()));
                        renderZVerticalCorner(world, tess, 15, y, smaller_alignment, air0, air1, 0, smaller_alignment - 1.0);
                        renderXVerticalCorner(world, tess, 16 - smaller_alignment, y, smaller_alignment - 1, air0, air1, 0, smaller_alignment - 1.0);
                    }
                }
            }
            if (renderNorthWest) {
                if (!renderNorth && !renderWest) {
                    for (int y = 0; y < 256; y++) {
                        boolean air0 = world.isAirBlock(new BlockPos(pos.getXStart(), y, pos.getZStart()));
                        boolean air1 = world.isAirBlock(new BlockPos(pos.getXStart(), y + 1, pos.getZStart()));
                        renderZVerticalCorner(world, tess, -1 + smaller_alignment, y, smaller_alignment, air0, air1, 0, smaller_alignment - 1.0);
                        renderXVerticalCorner(world, tess, smaller_alignment, y, smaller_alignment - 1, air0, air1, 0, smaller_alignment - 1.0);
                    }
                }
            }
            if (renderSouthWest) {
                if (!renderSouth && !renderWest) {
                    for (int y = 0; y < 256; y++) {
                        boolean air0 = world.isAirBlock(new BlockPos(pos.getXStart(), y, pos.getZEnd()));
                        boolean air1 = world.isAirBlock(new BlockPos(pos.getXStart(), y + 1, pos.getZEnd()));
                        renderZVerticalCorner(world, tess, -1 + smaller_alignment, y, 16 - smaller_alignment, air0, air1, 0, smaller_alignment - 1.0);
                        renderXVerticalCorner(world, tess, smaller_alignment, y, 15, air0, air1, 0, smaller_alignment - 1.0);
                    }
                }
            }
            if (renderSouthEast) {
                if (!renderSouth && !renderEast) {
                    for (int y = 0; y < 256; y++) {
                        boolean air0 = world.isAirBlock(new BlockPos(pos.getXEnd(), y, pos.getZEnd()));
                        boolean air1 = world.isAirBlock(new BlockPos(pos.getXEnd(), y + 1, pos.getZEnd()));
                        renderZVerticalCorner(world, tess, 15, y, 16 - smaller_alignment, air0, air1, 0, smaller_alignment - 1.0);
                        renderXVerticalCorner(world, tess, 16 - smaller_alignment, y, 15, air0, air1, 0, smaller_alignment - 1.0);
                    }
                }
            }


            GlStateManager.glEndList();
            break;
        }
    }


    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        // Cache Minecraft instance
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        // Get the camera position
        Entity camera = mc.getRenderViewEntity();
        double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.getPartialTicks();
        double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.getPartialTicks();
        double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.getPartialTicks();

        // Push OpenGL matrix and attributes
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        // Setup lighting and textures
        mc.entityRenderer.enableLightmap();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        mc.entityRenderer.disableLightmap();

        // Choose textures based on rendering config
        if (WarForgeConfig.DO_FANCY_RENDERING) {
            mc.renderEngine.bindTexture(texture);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
        } else {
            mc.renderEngine.bindTexture(fastTexture);
        }

        // Update render data if necessary
        if (CLAIMS_DIRTY) {
            updateRenderData();
            CLAIMS_DIRTY = false;
        }

        // Slower update speed on fast graphics
        if (player.world.rand.nextInt(WarForgeConfig.RANDOM_BORDER_REDRAW_DENOMINATOR) == 0) {
            updateRandomMesh();
        }

        // Render chunk borders
        renderChunkBorders(x, y, z);

        // Render player placement overlay (if necessary)
        renderPlayerPlacementOverlay(player, x, y, z, event.getPartialTicks());

        // Render flags (Citadels)
        //renderCitadelFlags(x, y, z);

        // Reset OpenGL state
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private void renderChunkBorders(double x, double y, double z) {
        if (!WarForgeMod.showBorders) {
			return;
		}for (HashMap.Entry<DimChunkPos, BorderRenderData> kvp : renderData.entrySet()) {
            DimChunkPos pos = kvp.getKey();
            BorderRenderData data = kvp.getValue();

            if (data.renderList >= 0) {
                GlStateManager.pushMatrix();

                int colour = data.claim.getColour();
                float r = (float) (colour >> 16 & 255) / 255.0F;
                float g = (float) (colour >> 8 & 255) / 255.0F;
                float b = (float) (colour & 255) / 255.0F;
                GlStateManager.color(r, g, b, 1.0F);

                GlStateManager.translate(pos.x * 16 - x, 0 - y, pos.z * 16 - z);
                GlStateManager.callList(data.renderList);

                GlStateManager.popMatrix();
            }
        }
    }

    private void renderPlayerPlacementOverlay(EntityPlayer player, double x, double y, double z, float partialTicks) {
        if (player.getHeldItemMainhand().getItem() instanceof ItemBlock) {
            boolean shouldRender = false;
            Block holding = ((ItemBlock) player.getHeldItemMainhand().getItem()).getBlock();

            // Check if the block being held is one that should render the placement overlay
            if (holding == Content.basicClaimBlock || holding == Content.citadelBlock || holding == Content.reinforcedClaimBlock) {
                shouldRender = true;
            }

            // If we need to render, check for ray tracing and render accordingly
            if (shouldRender) {
                renderPlacementOverlay(player, x, y, z, partialTicks);
            }
        }
    }

    private void renderPlacementOverlay(EntityPlayer player, double x, double y, double z, float partialTicks) {
        DimChunkPos playerPos = new DimChunkPos(player.dimension, player.getPosition());
        RayTraceResult result = player.rayTrace(10.0f, partialTicks);
        if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
            playerPos = new DimChunkPos(player.dimension, result.getBlockPos());
        }

        boolean canPlace = checkPlacementValidity(playerPos, player.getHeldItem(EnumHand.MAIN_HAND).getItem(), player.getHorizontalFacing());
        GlStateManager.color(canPlace ? 0f : 1f, canPlace ? 1f : 0f, 0f, 1.0F);
        Minecraft.getMinecraft().renderEngine.bindTexture(overlayTex);
        GlStateManager.translate(playerPos.x * 16 - x, 0 - y, playerPos.z * 16 - z);

        for (int i = 0; i < 16; i++) {
            for (int k = 0; k < 16; k++) {
                BlockPos pos = new BlockPos(playerPos.x * 16 + i, player.posY, playerPos.z * 16 + k);
                tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
                tess.getBuffer().pos(i, pos.getY() + 1.5d, k).tex(0f, 0f).endVertex();
                tess.getBuffer().pos(i + 1, pos.getY() + 1.5d, k).tex(1f, 0f).endVertex();
                tess.getBuffer().pos(i + 1, pos.getY() + 1.5d, k + 1).tex(1f, 1f).endVertex();
                tess.getBuffer().pos(i, pos.getY() + 1.5d, k + 1).tex(0f, 1f).endVertex();
                tess.draw();
            }
        }
    }

    private boolean checkPlacementValidity(DimChunkPos playerPos, Item holding, EnumFacing facing) {
        boolean canPlace = true;
        List<DimChunkPos> siegeablePositions = new ArrayList<>();

        for (TileEntity te : Minecraft.getMinecraft().world.loadedTileEntityList) {
            if (te instanceof IClaim) {
                DimBlockPos blockPos = ((IClaim) te).getClaimPos();
                DimChunkPos chunkPos = blockPos.toChunkPos();

                if (playerPos.x == chunkPos.x && playerPos.z == chunkPos.z) {
                    canPlace = false;
                }
                if (((IClaim) te).canBeSieged()) {
                    siegeablePositions.add(chunkPos);
                }
            }
        }

        // If holding siege camp block, allow placement if adjacent to siegable positions
        if (holding == Content.siegeCampBlockItem) {
            canPlace = canPlace && siegeablePositions.contains(playerPos.Offset(facing, 1));
        }

        return canPlace;
    }


    private static class BorderRenderData {
        public IClaim claim;
        public int renderList = -1;
    }
}