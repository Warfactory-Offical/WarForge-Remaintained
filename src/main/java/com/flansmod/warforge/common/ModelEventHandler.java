package com.flansmod.warforge.common;

import com.flansmod.warforge.common.util.IDynamicModels;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber
public class ModelEventHandler {
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelBake(ModelBakeEvent event) {
        IDynamicModels.bakeModels(event);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onPreTextureStitch(TextureStitchEvent.Pre event) {
        IDynamicModels.registerSprites(event.getMap());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRegisterModels(ModelRegistryEvent event) {
        IDynamicModels.registerModels();
        IDynamicModels.registerCustomStateMappers();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onItemColors(ColorHandlerEvent.Item event) {
        IDynamicModels.registerColorHandlers(event);
    }
}

