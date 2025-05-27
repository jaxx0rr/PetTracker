package com.github.justinwon777.pettracker.networking;

import com.github.justinwon777.pettracker.core.DeepScanManager;
import com.github.justinwon777.pettracker.core.DeepScanManagerSlowMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StartDeepScanPacket {
    private final String hand;
    private final int radius;
    private final int mode; // 0 = slow, 1 = fast, 2 = loaded

    public StartDeepScanPacket(String hand, int radius, int mode) {
        this.hand = hand;
        this.radius = radius;
        this.mode = mode;
    }

    public static void encode(StartDeepScanPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.hand);
        buf.writeInt(msg.radius);
        buf.writeInt(msg.mode);
    }

    public static StartDeepScanPacket decode(FriendlyByteBuf buf) {
        String hand = buf.readUtf();
        int radius = buf.readInt();
        int mode = buf.readInt();
        return new StartDeepScanPacket(hand, radius, mode);
    }

    public static void handle(StartDeepScanPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            var level = player.serverLevel();
            InteractionHand hand = msg.hand.equals("m") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

            switch (msg.mode) {
                case 0 -> DeepScanManagerSlowMode.start(level, player, msg.radius, null, null); // slow
                case 1 -> DeepScanManager.start(level, player, msg.radius, null, null);         // fast
                case 2 -> DeepScanManager.startScanLoadedChunks(level, player, null, null);     // loaded
                default -> {} // no-op
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
