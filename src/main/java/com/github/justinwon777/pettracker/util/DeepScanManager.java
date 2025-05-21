package com.github.justinwon777.pettracker.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class DeepScanManager {

    public static void start(ServerLevel level, ServerPlayer player, int maxRings) {

        player.sendSystemMessage(Component.literal("[DeepScan] Scanning for valid chunks."));

        List<ChunkPos> savedChunks = SavedChunkFinder.findAllSavedChunks(level);
        Set<ChunkPos> savedChunkSet = new HashSet<>(savedChunks);

        player.sendSystemMessage(Component.literal("[DeepScan] Found " + savedChunks.size() + " saved chunks to scan. Starting scan for pets.."));

        ChunkPos playerChunk = new ChunkPos(player.blockPosition());

        int scanned = 0;
        int ring = 0;
        boolean found = false;
        int petCount = 0;

        while (ring < maxRings) {
            List<ChunkPos> ringChunks = getNextRing(playerChunk, ring);
            boolean didScanAny = false;

            for (ChunkPos pos : ringChunks) {

                if (!savedChunkSet.contains(pos)) {
                    //player.sendSystemMessage(Component.literal("[DeepScan] Skipping " + pos + " – not in saved chunks"));
                    System.out.println(Component.literal("[DeepScan] Skipping " + pos + " – not in saved chunks"));
                    continue;
                }
                if (level.hasChunk(pos.x, pos.z)) {
                    //player.sendSystemMessage(Component.literal("[DeepScan] Skipping " + pos + " – already loaded"));
                    System.out.println(Component.literal("[DeepScan] Skipping " + pos + " – already loaded"));
                    continue;
                }

                // Step 1: Request the chunk to be loaded
                level.setChunkForced(pos.x, pos.z, true);

                // Step 2: Schedule the scan after a few ticks (e.g. 5)
                DelayedTaskManager.schedule(5, tick -> {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                    if (chunk != null) {
                        // ✅ Chunk is safely loaded and entities are now accessible
                        BlockPos min = new BlockPos(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ());
                        BlockPos max = new BlockPos(pos.getMaxBlockX(), level.getMaxBuildHeight(), pos.getMaxBlockZ());

                        for (Entity entity : level.getEntities((Entity) null, new AABB(min, max), e -> true)) {
                            if (entity instanceof TamableAnimal pet) {
                                player.sendSystemMessage(Component.literal(
                                        "[DeepScan] Pet found at " +
                                                entity.blockPosition().getX() + ", " +
                                                entity.blockPosition().getY() + ", " +
                                                entity.blockPosition().getZ() +
                                                " in chunk " + pos.x + ", " + pos.z + ")"
                                ));
                                //petCount++;
                            }
                        }
                    }

                    // Step 3: Always release the forced chunk
                    level.setChunkForced(pos.x, pos.z, false);
                });


                if (pos.x == 19 && pos.z == 31) player.sendSystemMessage(Component.literal("[DeepScan] Chunk 19 31 scanned – found:"+petCount));

                found = true;
                didScanAny = true; // ✅ Only set true if a chunk was successfully scanned
                scanned++;
            }

            if (!didScanAny && found) {
                player.sendSystemMessage(Component.literal("[DeepScan] No scanable chunks found in ring " + ring + ". Stopping."));
                break;
            }

            player.sendSystemMessage(Component.literal("[DeepScan] Processing ring " + ring));
            ring++;
        }


        player.sendSystemMessage(Component.literal("[DeepScan] Finished scanning " + scanned + " chunks."));
    }

    private static int chunkDistance(ChunkPos a, ChunkPos b) {
        int dx = a.x - b.x;
        int dz = a.z - b.z;
        return dx * dx + dz * dz;
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
            ring.add(new ChunkPos(ox + dx, oz - radius)); // top
            ring.add(new ChunkPos(ox + dx, oz + radius)); // bottom
        }
        for (int dz = -radius + 1; dz < radius; dz++) {
            ring.add(new ChunkPos(ox - radius, oz + dz)); // left
            ring.add(new ChunkPos(ox + radius, oz + dz)); // right
        }

        return ring;
    }
}
