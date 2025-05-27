package com.github.justinwon777.pettracker.core;

import com.github.justinwon777.pettracker.item.Tracker;
import com.github.justinwon777.pettracker.networking.RefreshTrackerListPacket;
import com.github.justinwon777.pettracker.networking.SyncTrackerStackPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class PetSyncManager {

    public static void syncSinglePet(ServerPlayer player, UUID uuid) {
        Entity pet = player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(128),
                e -> e.getUUID().equals(uuid)).stream().findFirst().orElse(null);

        if (pet == null) return;

        ItemStack trackerStack = getTrackerStack(player);
        if (trackerStack == null) return;

        CompoundTag tag = trackerStack.getOrCreateTag();
        if (!tag.contains(Tracker.TRACKING)) return;

        ListTag list = tag.getList(Tracker.TRACKING, 10);
        boolean changed = false;

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getUUID("uuid").equals(uuid)) {
                entry.putInt("x", (int) pet.getX());
                entry.putInt("y", (int) pet.getY());
                entry.putInt("z", (int) pet.getZ());
                entry.putBoolean("active", pet.isAlive());
                changed = true;
                break;
            }
        }

        if (changed) {
            tag.put(Tracker.TRACKING, list);
            trackerStack.setTag(tag);

            String handKey = player.getMainHandItem().equals(trackerStack) ? "m" : "o";

            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncTrackerStackPacket(trackerStack, handKey));

            // 🔥 This ensures the UI refreshes the list
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new RefreshTrackerListPacket(false));

        }
    }


    private static ItemStack getTrackerStack(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getItem() instanceof Tracker) {
                return stack;
            }
        }
        return null;
    }
}
