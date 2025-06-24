package com.flansmod.warforge.common;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

public class Sounds {
    public static List<SoundEvent> SOUNDS = new ArrayList<>();

    public static final SoundEvent sfxUpgrade = createSoundEvent("sfx.upgrade");

    private static SoundEvent createSoundEvent(String name) {
        ResourceLocation loc = new ResourceLocation(WarForgeMod.MODID, name);
        SoundEvent event = new SoundEvent(loc).setRegistryName(loc);
        SOUNDS.add(event);
        return event;
    }

    public static void register(IForgeRegistry<SoundEvent> registry) {
        SOUNDS.stream().forEach(sound -> registry.register(sound));
    }

}
