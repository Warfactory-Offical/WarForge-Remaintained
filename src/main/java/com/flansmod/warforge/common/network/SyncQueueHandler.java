package com.flansmod.warforge.common.network;

import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.Deque;

public class SyncQueueHandler {
    private static final Deque<Runnable> syncTasks = new ArrayDeque<>();
    public static final int perTick  = 4;

    public static void enqueue(Runnable task) {
        syncTasks.add(task);
    }

    public static void sync(TickEvent.ServerTickEvent event){
        if(event.phase == TickEvent.Phase.END && !syncTasks.isEmpty()){
            for(int i = 0; i < perTick && !syncTasks.isEmpty(); i++)
                syncTasks.poll().run();
        }

    }

}
