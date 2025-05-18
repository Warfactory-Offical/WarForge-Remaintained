package com.flansmod.warforge.api;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.world.storage.MapData;

public class MapDrawable implements IDrawable {

   private final MapData mapData;
   private final MapItemRenderer mapRenderer;

    public MapDrawable(MapData mapData, MapItemRenderer mapRenderer) {
        this.mapData = mapData;
        this.mapRenderer = mapRenderer;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        mapRenderer.renderMap(mapData, false);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

    }
}
