package com.github.justinwon777.pettracker.networking;

import com.github.justinwon777.pettracker.core.PetPositionTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class UpdatePetPacket {
    private final UUID uuid;

    public UpdatePetPacket(UUID uuid) {
        this.uuid = uuid;
    }

    public static UpdatePetPacket decode(FriendlyByteBuf buf) {
        return new UpdatePetPacket(buf.readUUID());
    }

    public static void encode(UpdatePetPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.uuid);
    }

    public static void handle(UpdatePetPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(msg.uuid);
                if (entity != null) {
                    PetPositionTracker.updatePet(entity);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
