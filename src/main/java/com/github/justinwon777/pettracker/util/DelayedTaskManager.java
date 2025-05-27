package com.github.justinwon777.pettracker.util;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public class DelayedTaskManager {

    private static final List<DelayedTask> tasks = new LinkedList<>();

    public static void schedule(int ticksDelay, Consumer<TickEvent.ServerTickEvent> action) {
        tasks.add(new DelayedTask(ticksDelay, action));
    }


    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        List<DelayedTask> toRun = new ArrayList<>();

        synchronized (tasks) {
            Iterator<DelayedTask> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                DelayedTask task = iterator.next();
                task.delay--;
                if (task.delay <= 0) {
                    toRun.add(task);
                    iterator.remove(); // safe now because we do it here, outside `forEach`
                }
            }
        }

        // Run delayed tasks outside of synchronized block
        for (DelayedTask task : toRun) {
            task.action.accept(event);
        }
    }



    private static class DelayedTask {
        int delay;
        Consumer<TickEvent.ServerTickEvent> action;

        DelayedTask(int delay, Consumer<TickEvent.ServerTickEvent> action) {
            this.delay = delay;
            this.action = action;
        }
    }
}
