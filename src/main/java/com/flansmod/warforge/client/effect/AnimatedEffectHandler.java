package com.flansmod.warforge.client.effect;

import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class AnimatedEffectHandler {
    public static final List<EffectAnimated<?>> effectQueue = new ArrayList<>();

    public static void add(EffectAnimated<?> effect) {
        effectQueue.add(effect);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        effectQueue.removeIf(effect -> {
            effect.tick();
            return effect.isComplete();
        });
    }


}
