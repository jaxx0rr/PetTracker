package com.github.justinwon777.pettracker.core;

import com.github.justinwon777.pettracker.PetTracker;
import com.github.justinwon777.pettracker.item.Tracker;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PetTracker.MOD_ID);

    public static final RegistryObject<Item> TRACKER =
            ITEMS.register("tracker", () -> new Tracker(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}