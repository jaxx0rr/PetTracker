package com.github.justinwon777.pettracker.util;

import com.github.justinwon777.pettracker.client.TrackerList;
import com.github.justinwon777.pettracker.client.TrackerScreen;
import com.github.justinwon777.pettracker.item.Tracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class DeepScanManagerSlowMode {

    private static final List<ChunkPos> scanQueue = new LinkedList<>();
    private static int scanned = 0;
    private static final Map<ChunkPos, Integer> chunkToRingMap = new HashMap<>();
    private static int lastReportedRing = -1;
    private static TrackerList trackerList = null;
    private static TrackerScreen trackerScreen = null;

    public static void start(ServerLevel level, ServerPlayer player, int maxRings, TrackerScreen screen, TrackerList list) {
        scanned = 0;
        scanQueue.clear();
        chunkToRingMap.clear();
        lastReportedRing = -1;
        trackerList = list;
        trackerScreen = screen;

        player.sendSystemMessage(Component.literal("[DeepScan] Scanning for valid chunks (" + maxRings + " rings)."));

        List<ChunkPos> savedChunks = SavedChunkFinder.findAllSavedChunks(level);
        Set<ChunkPos> savedChunkSet = new HashSet<>(savedChunks);

        player.sendSystemMessage(Component.literal("[DeepScan] Found " + savedChunks.size() + " saved chunks to scan. Starting scan for pets.."));

        ChunkPos playerChunk = new ChunkPos(player.blockPosition());

        int ring = 0;
        while (ring < maxRings) {
            List<ChunkPos> ringChunks = getNextRing(playerChunk, ring);
            boolean addedAny = false;

            for (ChunkPos pos : ringChunks) {
                if (!savedChunkSet.contains(pos)) {
                    String log = "[DeepScan] Skipping " + pos + " – not in saved chunks";
                    System.out.println(log);
                    player.sendSystemMessage(Component.literal(log));
                    continue;
                }
                if (level.hasChunk(pos.x, pos.z)) {
                    String log = "[DeepScan] Skipping " + pos + " – already loaded";
                    System.out.println(log);
                    player.sendSystemMessage(Component.literal(log));
                    continue;
                }
                scanQueue.add(pos);
                chunkToRingMap.put(pos, ring);
                addedAny = true;
            }

            if (!addedAny) {
                player.sendSystemMessage(Component.literal("[DeepScan] Ring " + ring + " contained no valid chunks."));
            }

            ring++;
        }

        processNextChunk(level, player);
    }

    private static void processNextChunk(ServerLevel level, ServerPlayer player) {
        if (scanQueue.isEmpty()) {
            player.sendSystemMessage(Component.literal("[DeepScan] Finished scanning " + scanned + " chunks."));
            return;
        }

        ChunkPos pos = scanQueue.remove(0);

        int currentRing = chunkToRingMap.getOrDefault(pos, -1);
        if (currentRing != lastReportedRing) {
            lastReportedRing = currentRing;
            player.sendSystemMessage(Component.literal("[DeepScan] Processing ring " + currentRing));
        }

        level.setChunkForced(pos.x, pos.z, true);

        DelayedTaskManager.schedule(2, tick -> {
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) {
                BlockPos min = new BlockPos(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ());
                BlockPos max = new BlockPos(pos.getMaxBlockX(), level.getMaxBuildHeight(), pos.getMaxBlockZ());

                int petCount = 0;
                for (Entity entity : level.getEntities((Entity) null, new AABB(min, max), e -> true)) {
                    if (entity instanceof TamableAnimal pet) {
                        petCount++;
                        String message = "[DeepScan] Pet at " + pet.blockPosition() + " in chunk " + pos.x + ", " + pos.z;
                        player.sendSystemMessage(Component.literal(message));
                        System.out.println(message);

                        if (trackerScreen != null) {
                            ItemStack trackerStack = trackerScreen.getItemStack(); // You may need to expose this via a getter
                            CompoundTag tag = trackerStack.getOrCreateTag();
                            ListTag trackingList = tag.contains(Tracker.TRACKING) ? tag.getList(Tracker.TRACKING, 10) : new ListTag();

                            CompoundTag petTag = new CompoundTag();
                            petTag.putUUID("uuid", pet.getUUID());
                            petTag.putString("name", pet.getDisplayName().getString());
                            petTag.putInt("x", (int) pet.getX());
                            petTag.putInt("y", (int) pet.getY());
                            petTag.putInt("z", (int) pet.getZ());
                            petTag.putBoolean("active", true);
                            trackingList.add(petTag);

                            tag.put(Tracker.TRACKING, trackingList);
                            trackerStack.setTag(tag);

                            // Write updated stack back to player hand
                            InteractionHand hand = trackerScreen.getHandKey().equals("m") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                            player.setItemInHand(hand, trackerStack);

                            TrackerList.Entry entry = new TrackerList.Entry(
                                    pet.getDisplayName().getString(),
                                    (int) pet.getX(), (int) pet.getY(), (int) pet.getZ(),
                                    true,
                                    pet.getUUID(),
                                    false,
                                    trackerScreen,
                                    trackerList.getWidth(),
                                    trackerList,
                                    "deepscan"
                            );

                        }
                    }
                }

                if (pos.x == 19 && pos.z == 31) {
                    player.sendSystemMessage(Component.literal("[DeepScan] Chunk 19 31 scanned – found: " + petCount));
                }
            }

            level.setChunkForced(pos.x, pos.z, false);
            scanned++;
            processNextChunk(level, player);
        });
    }

    private static List<ChunkPos> getNextRing(ChunkPos origin, int radius) {
        List<ChunkPos> ring = new ArrayList<>();
        int ox = origin.x;
        int oz = origin.z;

        if (radius == 0) {
            ring.add(new ChunkPos(ox, oz));
            return ring;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            ring.add(new ChunkPos(ox + dx, oz - radius));
            ring.add(new ChunkPos(ox + dx, oz + radius));
        }
        for (int dz = -radius + 1; dz < radius; dz++) {
            ring.add(new ChunkPos(ox - radius, oz + dz));
            ring.add(new ChunkPos(ox + radius, oz + dz));
        }

        return ring;
    }
}
