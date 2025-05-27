package com.github.justinwon777.pettracker.networking;

import com.github.justinwon777.pettracker.client.TrackerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RefreshTrackerListPacket {
    private final boolean isFinal;

    public RefreshTrackerListPacket(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isFinal);
    }

    public static RefreshTrackerListPacket decode(FriendlyByteBuf buf) {
        return new RefreshTrackerListPacket(buf.readBoolean());
    }

    public static void handle(RefreshTrackerListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            doClientRefresh(msg.isFinal());
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void doClientRefresh(boolean isFinalUpdate) {
        if (Minecraft.getInstance().screen instanceof TrackerScreen screen) {
            screen.getTrackerList().refresh();
            if (isFinalUpdate) {
                screen.onScanFinished();
            }
        }
    }
}
