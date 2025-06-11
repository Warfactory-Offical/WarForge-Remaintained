package com.flansmod.warforge.api;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

public class MapDrawable implements IDrawable {


    private final String mapData;

    public MapDrawable(String mapData) {
        this.mapData = mapData;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
        GlStateManager.color(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(WarForgeMod.MODID, mapData));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, 16*4, 16*4);
    }



}
