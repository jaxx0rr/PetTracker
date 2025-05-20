package com.github.justinwon777.pettracker.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PetPositionTracker {
    private static final Map<UUID, TrackedPet> trackedPets = new ConcurrentHashMap<>();

    public static void updatePet(Entity entity) {
        if (entity != null && !entity.level().isClientSide) {
            trackedPets.put(entity.getUUID(), new TrackedPet(entity.getX(), entity.getY(), entity.getZ()));
        }
    }

    public static TrackedPet get(UUID uuid) {
        return trackedPets.get(uuid);
    }

    public record TrackedPet(double x, double y, double z) {}
}
