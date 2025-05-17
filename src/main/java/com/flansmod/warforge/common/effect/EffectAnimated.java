package com.flansmod.warforge.common.effect;

import java.util.function.BiConsumer;


public class EffectAnimated<T> {
    private final T context;
    private final BiConsumer<T, Integer> tickFunction;
    private final int totalTicks;
    private int tick;

    public EffectAnimated(T context, int totalTicks, BiConsumer<T, Integer> tickFunction) {
        this.context = context;
        this.totalTicks = totalTicks;
        this.tickFunction = tickFunction;
    }

    public void tick() {
        if (tick < totalTicks) {
            tickFunction.accept(context, totalTicks);
            tick++;
        }
    }

    public boolean isComplete() {
        return tick >= totalTicks;
    }

}
