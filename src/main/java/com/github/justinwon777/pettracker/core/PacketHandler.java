package com.github.justinwon777.pettracker.core;

import com.github.justinwon777.pettracker.PetTracker;
import com.github.justinwon777.pettracker.client.TrackerScreen;
import com.github.justinwon777.pettracker.networking.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE =
            NetworkRegistry.newSimpleChannel(new ResourceLocation(PetTracker.MOD_ID,
                    "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, OpenTrackerPacket.class, OpenTrackerPacket::encode, OpenTrackerPacket::decode, OpenTrackerPacket::handle);
        INSTANCE.registerMessage(id++, TeleportPacket.class, TeleportPacket::encode, TeleportPacket::decode, TeleportPacket::handle);
        INSTANCE.registerMessage(id++, RemovePacket.class, RemovePacket::encode, RemovePacket::decode, RemovePacket::handle);
        INSTANCE.registerMessage(id++, UpdatePetPacket.class, UpdatePetPacket::encode, UpdatePetPacket::decode, UpdatePetPacket::handle);
        INSTANCE.registerMessage(id++, AddPetsToTrackerPacket.class, AddPetsToTrackerPacket::encode, AddPetsToTrackerPacket::decode, AddPetsToTrackerPacket::handle);
        INSTANCE.registerMessage(id++, RefreshTrackerListPacket.class, RefreshTrackerListPacket::encode, RefreshTrackerListPacket::decode, RefreshTrackerListPacket::handle);
        INSTANCE.registerMessage(id++, SyncTrackerStackPacket.class,
                SyncTrackerStackPacket::encode,
                SyncTrackerStackPacket::decode,
                SyncTrackerStackPacket::handle);
        INSTANCE.registerMessage(id++, StartDeepScanPacket.class,
                StartDeepScanPacket::encode,
                StartDeepScanPacket::decode,
                StartDeepScanPacket::handle);
        INSTANCE.registerMessage(id++, CancelDeepScanPacket.class,
                CancelDeepScanPacket::encode,
                CancelDeepScanPacket::decode,
                CancelDeepScanPacket::handle);
        INSTANCE.registerMessage(id++, RequestPetSyncPacket.class,
                RequestPetSyncPacket::encode,
                RequestPetSyncPacket::decode,
                RequestPetSyncPacket::handle);
    }

    @SuppressWarnings("resource")
    @OnlyIn(Dist.CLIENT)
    public static void openTracker(OpenTrackerPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Minecraft.getInstance().setScreen(new TrackerScreen(packet.getItemStack(), packet.getHand(),
                    packet.getX(), packet.getY(), packet.getZ()));
        }
    }
}
