package com.flansmod.warforge.common;

import com.flansmod.warforge.common.util.IDynamicModels;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class ModelEventHandler {
    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        IDynamicModels.bakeModels(event);
    }

    @SubscribeEvent
    public static void onPreTextureStitch(TextureStitchEvent.Pre event) {
        IDynamicModels.registerSprites(event.getMap());
    }

    @SubscribeEvent
    public static void onRegisterModels(ModelRegistryEvent event) {
        IDynamicModels.registerModels();
        IDynamicModels.registerCustomStateMappers();
    }

    @SubscribeEvent
    public static void onItemColors(ColorHandlerEvent.Item event) {
        IDynamicModels.registerColorHandlers(event);
    }
}

