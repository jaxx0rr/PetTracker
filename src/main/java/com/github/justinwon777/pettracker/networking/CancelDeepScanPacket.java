package com.github.justinwon777.pettracker.networking;

import com.github.justinwon777.pettracker.core.DeepScanManager;
import com.github.justinwon777.pettracker.core.DeepScanManagerSlowMode;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CancelDeepScanPacket {

    public static void encode(CancelDeepScanPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
    }

    public static CancelDeepScanPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new CancelDeepScanPacket();
    }

    public static void handle(CancelDeepScanPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DeepScanManager.cancel();
            DeepScanManagerSlowMode.cancel();
        });
        ctx.get().setPacketHandled(true);
    }
}
