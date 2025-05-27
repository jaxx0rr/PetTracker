package com.github.justinwon777.pettracker.networking;

import com.github.justinwon777.pettracker.core.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import static com.github.justinwon777.pettracker.item.Tracker.TRACKING;

public class RemovePacket {
    private final UUID id;
    private final ItemStack tracker;
    private final String hand;


    public RemovePacket(UUID id, ItemStack tracker, String hand) {
        this.id = id;
        this.tracker = tracker;
        this.hand = hand;
    }

    public static RemovePacket decode(FriendlyByteBuf buf) {
        return new RemovePacket(buf.readUUID(), buf.readItem(), buf.readUtf());
    }

    public static void encode(RemovePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.id);
        buf.writeItem(msg.tracker);
        buf.writeUtf(msg.hand);
    }

    public UUID getUUID() {
        return this.id;
    }

    public ItemStack getTracker() { return this.tracker; }

    public String getHand() {return this.hand; }

    public static void handle(RemovePacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            InteractionHand hand = msg.getHand().equals("m") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack tracker = player.getItemInHand(hand);

            if (!tracker.hasTag() || !tracker.getTag().contains(TRACKING)) return;

            CompoundTag tag = tracker.getTag();
            ListTag listTag = tag.getList(TRACKING, 10);

            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entityTag = listTag.getCompound(i);
                if (entityTag.getUUID("uuid").equals(msg.getUUID())) {
                    listTag.remove(i);
                    break;
                }
            }

            tag.put(TRACKING, listTag);
            tracker.setTag(tag);
            player.setItemInHand(hand, tracker);
        });
        context.get().setPacketHandled(true);
    }

}
