package com.flansmod.warforge.api.modularui;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.flansmod.warforge.api.Color4i;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.SiegeCampAttackInfoRender;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapDrawable implements IDrawable, Interactable {

    public final static int CB_THICKNESS = 2; //Controls border thickness
    public final static int HL_THICKNESS = 1; //Controls border thickness on highlighted chunks
    public static final int HL_COLOR = 0xFFC6C6C6;
    public static final int GRID_COLOR = new Color4i(0.15f, 26, 26, 26).toARGB();
    public static final int GRID_THICKNESS = HL_THICKNESS;
    public  float[] rgb;
    public final static int OFFSET = 2;
    public final static int SIZE = 12;
    public static final boolean DEBUG = false;

    private final String mapData;
    private final SiegeCampAttackInfoRender chunkState;
    private final ResourceLocation attackIcon = new ResourceLocation(WarForgeMod.MODID, "gui/icon_siege_attack.png");
    private final ResourceLocation selfIcon = new ResourceLocation(WarForgeMod.MODID, "gui/icon_siege_self.png");
    private final ResourceLocation selfIconBase = new ResourceLocation(WarForgeMod.MODID, "gui/icon_siege_self_base.png");
    private final boolean[] adjesency;
    private final boolean campChunk;

    public MapDrawable(String mapData, SiegeCampAttackInfoRender chunkState, boolean[] adjesency) {
        this.mapData = mapData;
        this.chunkState = chunkState;
        this.adjesency = adjesency;
        this.campChunk = chunkState.mOffset.getZ() == 0 && chunkState.mOffset.getX() == 0;
        rgb = Color4i.fromRGB(chunkState.mFactionColour).asFloatRGB();
    }

    public static String extractNumbers(String input) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = Pattern.compile("\\d+").matcher(input);
        while (matcher.find()) {
            builder.append(matcher.group());
        }
        return builder.toString();
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
        GlStateManager.pushAttrib();
        GlStateManager.pushMatrix();
        boolean hovered = context.getMouseX() >= x && context.getMouseX() < x + width &&
                context.getMouseY() >= y && context.getMouseY() < y + height;

        if (!hovered || chunkState.mFactionUUID.equals(Faction.nullUuid))
            GlStateManager.color(0.9f, 0.9f, 0.9f, 1f);
        else
            setGLColor();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(WarForgeMod.MODID, mapData));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, 16 * 4, 16 * 4);
        if (DEBUG) {
            FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
            String numberText = extractNumbers(mapData);
            fontRenderer.drawString(numberText, x + 10, y + 10, 0xFFFFFF); // index
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        if (!chunkState.mFactionUUID.equals(Faction.nullUuid)) {
            int baseNoAlpha = chunkState.mFactionColour & 0x00FFFFFF;
            int base = baseNoAlpha | 0xFF000000;
            int lightColor = brighten(base);
            int darkColor = darken(base);

            Gui.drawRect(x, y, x + width + 1, y + height + 1, baseNoAlpha | 0x20_000000);

            // Top (light)
            if (adjesency[3])
                Gui.drawRect(x, y, x + width + 1, y + CB_THICKNESS, lightColor);
            // Left (light)
            if (adjesency[0])
                Gui.drawRect(x, y + CB_THICKNESS - 2, x + CB_THICKNESS, y + height, lightColor);

            // Bottom (dark)
            if (adjesency[1])
                Gui.drawRect(x, y + height - CB_THICKNESS, x + width, y + height, darkColor);
            // Right (dark)
            if (adjesency[2])
                Gui.drawRect(x + width - CB_THICKNESS, y, x + width, y + height, darkColor);
        } else {
            Gui.drawRect(x, y, x + width + 1, y + GRID_THICKNESS, GRID_COLOR);
            Gui.drawRect(x, y + GRID_THICKNESS - 2, x + GRID_THICKNESS, y + height, GRID_COLOR);
            Gui.drawRect(x, y + height - GRID_THICKNESS, x + width, y + height, GRID_COLOR);
            Gui.drawRect(x + width - GRID_THICKNESS, y, x + width, y + height, GRID_COLOR);
        }
        if (hovered) {
            if (chunkState.canAttack && !campChunk) {
                setGLColor(rgb);
                Minecraft.getMinecraft().getTextureManager().bindTexture(attackIcon);
                int xOffset = x + (width - 46) / 2;
                int yOffset = y + (height - 46) / 2;
                Gui.drawModalRectWithCustomSizedTexture(

                        xOffset, yOffset, // top-left of texture, centered
                        0, 0,             // UV coords
                        46, 46,     // draw size
                        46, 46            // full texture size
                );
            } else {
                Gui.drawRect(x, y, x + width + 1, y + HL_THICKNESS, HL_COLOR);
                Gui.drawRect(x, y + HL_THICKNESS - 2, x + HL_THICKNESS, y + height, HL_COLOR);
                Gui.drawRect(x, y + height - HL_THICKNESS, x + width, y + height, HL_COLOR);
                Gui.drawRect(x + width - HL_THICKNESS, y, x + width, y + height, HL_COLOR);
            }
        }
        if (campChunk) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(selfIcon);
            setGLColor(rgb);
            int xOffset = x + (width - 48) / 2;
            int yOffset = y + (height - 48) / 2;
            Gui.drawModalRectWithCustomSizedTexture(

                    xOffset, yOffset,
                    0, 0,             // UV coords
                    48, 48, // draw size
                    48, 48            // full texture size
            );

            setGLColor();
            Minecraft.getMinecraft().getTextureManager().bindTexture(selfIconBase);
            Gui.drawModalRectWithCustomSizedTexture(

                    xOffset, yOffset,
                    0, 0,             // UV coords
                    48, 48, // draw size
                    48, 48            // full texture size
            );
        }

        if (chunkState.veinSprite != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.color(1f, 1f, 1f, 1f);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();

            float u0 = chunkState.veinSprite.getMinU();
            float v0 = chunkState.veinSprite.getMinV();
            float u1 = chunkState.veinSprite.getMaxU();
            float v1 = chunkState.veinSprite.getMaxV();

            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buf.pos(x + OFFSET, y + OFFSET, 0).tex(u0, v0).endVertex();
            buf.pos(x + OFFSET, y + SIZE + OFFSET, 0).tex(u0, v1).endVertex();
            buf.pos(x + SIZE + OFFSET, y + SIZE + OFFSET, 0).tex(u1, v1).endVertex();
            buf.pos(x + SIZE + OFFSET, y + OFFSET, 0).tex(u1, v0).endVertex();
            tess.draw();
        }


        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
        GlStateManager.popAttrib();
    }

    private int brighten(int color) {
        int r = Math.min(((color >> 16) & 0xFF) + 16, 255);
        int g = Math.min(((color >> 8) & 0xFF) + 16, 255);
        int b = Math.min((color & 0xFF) + 16, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int darken(int color) {
        int r = Math.max(((color >> 16) & 0xFF) - 16, 0);
        int g = Math.max(((color >> 8) & 0xFF) - 16, 0);
        int b = Math.max((color & 0xFF) - 16, 0);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    
    private static void setGLColor(float[] rgb){
        GlStateManager.color(rgb[0], rgb[1], rgb[2]);
    }

    private static void setGLColor(){
        GlStateManager.color(1f,1f,1f);
    }

}



