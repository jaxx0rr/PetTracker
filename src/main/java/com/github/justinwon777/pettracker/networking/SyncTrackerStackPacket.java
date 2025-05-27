package com.github.justinwon777.pettracker.networking;

import com.github.justinwon777.pettracker.client.TrackerScreen;
import com.github.justinwon777.pettracker.item.Tracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncTrackerStackPacket {
    private final ItemStack stack;
    private final String hand;

    public SyncTrackerStackPacket(ItemStack stack, String hand) {
        this.stack = stack;
        this.hand = hand;
    }

    public static void encode(SyncTrackerStackPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.stack);
        buf.writeUtf(msg.hand);
    }

    public static SyncTrackerStackPacket decode(FriendlyByteBuf buf) {
        return new SyncTrackerStackPacket(buf.readItem(), buf.readUtf());
    }

    public static void handle(SyncTrackerStackPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (!ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().setPacketHandled(true);
            return;
        }

        ctx.get().enqueueWork(() -> {
            // ✅ Safe because we're now guaranteed to be on client
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var player = minecraft.player;
            if (player == null) return;

            InteractionHand hand = msg.hand.equals("m") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            player.setItemInHand(hand, msg.stack);

            if (minecraft.screen instanceof TrackerScreen screen) {
                screen.getTrackerList().refresh();
            }
        });

        ctx.get().setPacketHandled(true);
    }


}
