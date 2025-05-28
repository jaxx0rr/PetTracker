package com.github.justinwon777.pettracker.core;

import com.github.justinwon777.pettracker.client.TrackerScreen;
import com.github.justinwon777.pettracker.item.Tracker;
import com.github.justinwon777.pettracker.networking.AddPetsToTrackerPacket;
import com.github.justinwon777.pettracker.networking.RefreshTrackerListPacket;
import com.github.justinwon777.pettracker.networking.SyncTrackerStackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class PetScanner {

    public static void scanLoadedPets(TrackerScreen screen) {
        var level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) return;

        ItemStack stack = screen.getItemStack();
        CompoundTag tag = stack.getOrCreateTag();

        Set<UUID> alreadyTracked = new HashSet<>();
        if (tag.contains(Tracker.TRACKING)) {
            ListTag listTag = tag.getList(Tracker.TRACKING, 10);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag petTag = listTag.getCompound(i);
                alreadyTracked.add(petTag.getUUID("uuid"));
            }
        }

        ListTag newScanPets = new ListTag();
        List<AddPetsToTrackerPacket.PetData> serverSyncPets = new ArrayList<>();

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof TamableAnimal pet)) continue;
            if (!pet.isTame()) continue;
            if (alreadyTracked.contains(pet.getUUID())) continue;
            //if (pet.isOwnedBy(player)) continue; // ⛔ skip gray cats

            String petName = pet.getDisplayName().getString();
            String ownerName = pet.getOwner() != null ? pet.getOwner().getName().getString() : "Unknown";
            String displayName = ownerName + "'s " + petName;

            CompoundTag petTag = new CompoundTag();
            petTag.putUUID("uuid", pet.getUUID());
            petTag.putString("name", displayName);
            petTag.putInt("x", (int) pet.getX());
            petTag.putInt("y", (int) pet.getY());
            petTag.putInt("z", (int) pet.getZ());
            petTag.putBoolean("active", true);
            petTag.putString("source", "scan");
            petTag.putString("dimension", entity.level().dimension().location().toString()); // ✅ Added dimension

            newScanPets.add(petTag);
            serverSyncPets.add(new AddPetsToTrackerPacket.PetData(
                    pet.getUUID(),
                    displayName,
                    (int) pet.getX(),
                    (int) pet.getY(),
                    (int) pet.getZ(),
                    true,
                    "scan",
                    pet.level().dimension().location().toString()  // ⬅️ Add this for dimension
            ));
        }


        // Append to NBT list
        if (!newScanPets.isEmpty()) {
            ListTag current = tag.contains(Tracker.TRACKING) ? tag.getList(Tracker.TRACKING, 10) : new ListTag();
            for (int i = 0; i < newScanPets.size(); i++) {
                current.add(newScanPets.getCompound(i));
            }
            tag.put(Tracker.TRACKING, current);
            stack.setTag(tag);

            PacketHandler.INSTANCE.sendToServer(new AddPetsToTrackerPacket(serverSyncPets, screen.getHandKey()));
            PacketHandler.INSTANCE.sendToServer(new SyncTrackerStackPacket(stack.copy(), screen.getHandKey()));
            //PacketHandler.INSTANCE.sendToServer(new RefreshTrackerListPacket(true));

        }
    }

}