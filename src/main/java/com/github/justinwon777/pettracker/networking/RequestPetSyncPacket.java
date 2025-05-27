package com.github.justinwon777.pettracker.networking;

import com.github.justinwon777.pettracker.core.PetSyncManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class RequestPetSyncPacket {
    private final UUID uuid;

    public RequestPetSyncPacket(UUID uuid) {
        this.uuid = uuid;
    }

    public static void encode(RequestPetSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.uuid);
    }

    public static RequestPetSyncPacket decode(FriendlyByteBuf buf) {
        return new RequestPetSyncPacket(buf.readUUID());
    }

    public static void handle(RequestPetSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PetSyncManager.syncSinglePet(player, msg.uuid);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
