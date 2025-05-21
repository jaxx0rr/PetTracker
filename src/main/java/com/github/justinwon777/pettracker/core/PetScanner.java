package com.github.justinwon777.pettracker.core;

import com.github.justinwon777.pettracker.client.TrackerList;
import com.github.justinwon777.pettracker.client.TrackerScreen;
import com.github.justinwon777.pettracker.item.Tracker;
import com.github.justinwon777.pettracker.networking.AddPetsToTrackerPacket;
import com.github.justinwon777.pettracker.networking.UpdatePetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.StreamSupport;

public class PetScanner {

    public static List<TrackerList.Entry> scanLoadedPets(Set<UUID> alreadyTracked, TrackerScreen screen, int listWidth, TrackerList parentList) {
        List<TrackerList.Entry> untrackedPets = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;

        if (level == null || player == null) return untrackedPets;

        List<AddPetsToTrackerPacket.PetData> pets = new ArrayList<>();

        StreamSupport.stream(level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof TamableAnimal)
                .map(e -> (TamableAnimal) e)
                .filter(t -> t.isTame() && t.isOwnedBy(player))
                .filter(t -> !alreadyTracked.contains(t.getUUID()))
                .forEach(t -> {
                    PacketHandler.INSTANCE.sendToServer(new UpdatePetPacket(t.getUUID()));

                    String petName = t.getDisplayName().getString();
                    String ownerName = t.getOwner() != null ? t.getOwner().getName().getString() : "Unknown";
                    String name = t.isOwnedBy(player) ? petName : ownerName + "'s " + petName;

                    int x = (int) t.getX();
                    int y = (int) t.getY();
                    int z = (int) t.getZ();

                    TrackerList.Entry entry = new TrackerList.Entry(name, x, y, z, true, t.getUUID(), false, screen, listWidth, parentList, "scan");
                    untrackedPets.add(entry);

                    pets.add(new AddPetsToTrackerPacket.PetData(
                            t.getUUID(), name, x, y, z, true, "scan"
                    ));
                });

        PacketHandler.INSTANCE.sendToServer(new AddPetsToTrackerPacket(pets, screen.getHandKey()));

        return untrackedPets;

    }

    public static List<TrackerList.Entry> scanAllPetsInCurrentDimension(Set<UUID> alreadyTracked, TrackerScreen screen, int listWidth, TrackerList parentList) {
        List<TrackerList.Entry> foundPets = new ArrayList<>();
        var client = Minecraft.getInstance();
        var player = client.player;
        var level = client.level;

        if (player == null || level == null) return foundPets;

        var server = client.getSingleplayerServer();
        if (server == null) return foundPets;

        var serverLevel = server.getLevel(level.dimension());
        if (serverLevel == null) return foundPets;

        Set<UUID> redPetsSeen = new HashSet<>();
        List<AddPetsToTrackerPacket.PetData> pets = new ArrayList<>();

        // First pass: nearby rendered pets
        StreamSupport.stream(level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof TamableAnimal)
                .map(e -> (TamableAnimal) e)
                .filter(TamableAnimal::isTame)
                .filter(t -> !alreadyTracked.contains(t.getUUID()))
                .forEach(t -> {
                    redPetsSeen.add(t.getUUID());
                });

        // Second pass: full dimension scan
        StreamSupport.stream(serverLevel.getEntities().getAll().spliterator(), false)
                .filter(e -> e instanceof TamableAnimal)
                .map(e -> (TamableAnimal) e)
                .filter(TamableAnimal::isTame)
                .filter(t -> !alreadyTracked.contains(t.getUUID()))
                .filter(t -> !redPetsSeen.contains(t.getUUID())) // avoid duplicates
                .forEach(t -> {
                    PetPositionTracker.updatePet(t);

                    String petName = t.getDisplayName().getString();
                    String ownerName = t.getOwner() != null ? t.getOwner().getName().getString() : "Unknown";
                    String name = t.isOwnedBy(player) ? petName : ownerName + "'s " + petName;

                    int x = (int) t.getX();
                    int y = (int) t.getY();
                    int z = (int) t.getZ();

                    TrackerList.Entry entry = new TrackerList.Entry(name, x, y, z, true, t.getUUID(), false, screen, listWidth, parentList, "extscan");
                    foundPets.add(entry);

                    pets.add(new AddPetsToTrackerPacket.PetData(
                            t.getUUID(), name, x, y, z, true, "extscan"
                    ));
                });

        PacketHandler.INSTANCE.sendToServer(new AddPetsToTrackerPacket(pets, screen.getHandKey()));

        return foundPets;
    }
}