package com.github.justinwon777.pettracker.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class AddPetsToTrackerPacket {
    public record PetData(UUID uuid, String name, int x, int y, int z, boolean active, String source) {}

    private final List<PetData> pets;
    private final String handKey;

    public AddPetsToTrackerPacket(List<PetData> pets, String handKey) {
        this.pets = pets;
        this.handKey = handKey;
    }

    public static void encode(AddPetsToTrackerPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.handKey);
        buf.writeInt(msg.pets.size());
        for (PetData pet : msg.pets) {
            buf.writeUUID(pet.uuid());
            buf.writeUtf(pet.name());
            buf.writeInt(pet.x());
            buf.writeInt(pet.y());
            buf.writeInt(pet.z());
            buf.writeBoolean(pet.active());
            buf.writeUtf(pet.source());
        }
    }

    public static AddPetsToTrackerPacket decode(FriendlyByteBuf buf) {
        String handKey = buf.readUtf();
        int size = buf.readInt();
        List<PetData> pets = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            boolean active = buf.readBoolean();
            String source = buf.readUtf();
            pets.add(new PetData(uuid, name, x, y, z, active, source));
        }
        return new AddPetsToTrackerPacket(pets, handKey);
    }

    public static void handle(AddPetsToTrackerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || msg.pets.isEmpty()) return;

            String sourceToClear = msg.pets.get(0).source(); // assume all share same source
            InteractionHand hand = msg.handKey.equals("m") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack trackerStack = player.getItemInHand(hand);

            CompoundTag tag = trackerStack.getOrCreateTag();
            ListTag originalList = tag.contains("Tracking") ? tag.getList("Tracking", 10) : new ListTag();
            ListTag filtered = new ListTag();

            for (int i = 0; i < originalList.size(); i++) {
                CompoundTag entry = originalList.getCompound(i);
                if (!sourceToClear.equals(entry.getString("source"))) {
                    filtered.add(entry);
                }
            }

            for (PetData pet : msg.pets) {
                CompoundTag petTag = new CompoundTag();
                petTag.putUUID("uuid", pet.uuid());
                petTag.putString("name", pet.name());
                petTag.putInt("x", pet.x());
                petTag.putInt("y", pet.y());
                petTag.putInt("z", pet.z());
                petTag.putBoolean("active", pet.active());
                petTag.putString("source", pet.source());
                filtered.add(petTag);
            }

            tag.put("Tracking", filtered);
            trackerStack.setTag(tag);
            player.setItemInHand(hand, trackerStack);
        });
        ctx.get().setPacketHandled(true);
    }
}
