package com.github.justinwon777.pettracker.core;

import com.github.justinwon777.pettracker.client.TrackerList;
import com.github.justinwon777.pettracker.client.TrackerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.TamableAnimal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

public class PetScanner {

    //this is the new comment: code 1257

    //public static List<TrackerList.Entry> scanLoadedPets(Set<UUID> alreadyTracked) {
    public static List<TrackerList.Entry> scanLoadedPets(Set<UUID> alreadyTracked, TrackerScreen screen, int listWidth, TrackerList parentList) {

    List<TrackerList.Entry> untrackedPets = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;

        if (level == null || player == null) return untrackedPets;

        StreamSupport.stream(level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof TamableAnimal)
                .map(e -> (TamableAnimal) e)
                .filter(t -> t.isTame() && t.isOwnedBy(player))
                .filter(t -> !alreadyTracked.contains(t.getUUID()))
                .forEach(t -> {

                    com.github.justinwon777.pettracker.core.PacketHandler.INSTANCE.sendToServer(
                            new com.github.justinwon777.pettracker.networking.UpdatePetPacket(t.getUUID())
                    );

                    String name = t.getDisplayName().getString();
                    int x = (int) t.getX();
                    int y = (int) t.getY();
                    int z = (int) t.getZ();

                    untrackedPets.add(new TrackerList.Entry(name, x, y, z, true, t.getUUID(), false, screen, listWidth, parentList));

                });

        return untrackedPets;
    }


    public static List<TrackerList.Entry> scanAllPetsInCurrentDimension(Set<UUID> alreadyTracked, TrackerScreen screen, int listWidth, TrackerList parentList) {
        List<TrackerList.Entry> foundPets = new ArrayList<>();
        var client = Minecraft.getInstance();
        var player = client.player;
        var level = client.level;

        if (player == null || level == null) return foundPets;

        var server = client.getSingleplayerServer();
        if (server == null) return foundPets; // not singleplayer

        var serverLevel = server.getLevel(level.dimension());
        if (serverLevel == null) return foundPets;

        var clientLevel = Minecraft.getInstance().level;

        StreamSupport.stream(serverLevel.getEntities().getAll().spliterator(), false)
                .filter(e -> e instanceof TamableAnimal)
                .map(e -> (TamableAnimal) e)
                .filter(TamableAnimal::isTame)
                .filter(t -> !alreadyTracked.contains(t.getUUID()))
                .forEach(t -> {
                    PetPositionTracker.updatePet(t);

                    boolean isClientLoaded = StreamSupport.stream(
                                    clientLevel.entitiesForRendering().spliterator(), false)
                            .anyMatch(e -> e.getUUID().equals(t.getUUID()));

                    String name = t.getDisplayName().getString();
                    int x = (int) t.getX();
                    int y = (int) t.getY();
                    int z = (int) t.getZ();

                    foundPets.add(new TrackerList.Entry(name, x, y, z, true,
                            t.getUUID(), false, screen, listWidth, parentList, !isClientLoaded));
                });

        return foundPets;
    }

}