package com.github.justinwon777.pettracker;

import com.github.justinwon777.pettracker.core.ItemInit;
import com.github.justinwon777.pettracker.core.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PetTracker.MOD_ID)
public class PetTracker
{
    public static final String MOD_ID = "pettracker";

    public PetTracker() {
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ItemInit.register(eventBus);
        PacketHandler.register();
    }

}
