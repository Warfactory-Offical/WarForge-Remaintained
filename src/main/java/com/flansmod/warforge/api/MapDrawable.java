package com.flansmod.warforge.api;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapDrawable implements IDrawable {


    private final String mapData;
    private final SiegeCampAttackInfo chunkState;
    private boolean[] adjesency;
    private final ResourceLocation attackIcon = new ResourceLocation(WarForgeMod.MODID, "gui/icon_siege_attack.png");

    public MapDrawable(String mapData, SiegeCampAttackInfo chunkState, boolean[] adjesency) {
        this.mapData = mapData;
        this.chunkState = chunkState;
        this.adjesency = adjesency;
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
        boolean hovered = context.getMouseX() >= x && context.getMouseX() < x + width &&
                context.getMouseY() >= y && context.getMouseY() < y + height;

        if (!chunkState.canAttack && chunkState.mFactionUUID.equals(Faction.nullUuid))
            GlStateManager.color(0.8f, 0.8f, 0.8f, 1f);
        else
            GlStateManager.color(1f, 1f, 1f, 1f);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(WarForgeMod.MODID, mapData));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, 16 * 4, 16 * 4);
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String numberText = extractNumbers(mapData);
        fontRenderer.drawString(numberText, x + 10, y + 10, 0xFFFFFF); // index


        GlStateManager.color(1f, 1f, 1f, 1f);
        if (!chunkState.mFactionUUID.equals(Faction.nullUuid)) {
            int THICKNESS = 2;
            int baseNoAlpha = chunkState.mFactionColour & 0x00FFFFFF;
            int base = baseNoAlpha | 0xFF000000;
            int lightColor = brighten(base);
            int darkColor = darken(base);

            Gui.drawRect(x, y, x + width+1, y + height+1, baseNoAlpha | 0x20_000000);

            // Top (light)
            if (adjesency[3])
                Gui.drawRect(x, y, x + width+1, y + THICKNESS, lightColor);
            // Left (light)
            if (adjesency[0])
                Gui.drawRect(x, y + THICKNESS-2, x + THICKNESS, y + height, lightColor);

            // Bottom (dark)
            if (adjesency[1])
                Gui.drawRect(x , y + height - THICKNESS, x + width, y + height, darkColor);
            // Right (dark)
            if (adjesency[2])
                Gui.drawRect(x + width - THICKNESS, y, x + width, y + height, darkColor);
        }
        if(chunkState.canAttack && hovered){
            Minecraft.getMinecraft().getTextureManager().bindTexture(attackIcon);
            int xOffset = x + (width - 46) / 2;
            int yOffset = y + (height - 46) / 2;
            Gui.drawModalRectWithCustomSizedTexture(

                    xOffset, yOffset, // top-left of texture, centered
                    0, 0,             // UV coords
                    46, 46,     // draw size
                    46, 46            // full texture size
            );
        }


        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.enableLighting();
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
}



