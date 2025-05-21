package com.github.justinwon777.pettracker.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SavedChunkFinder {

    public static List<ChunkPos> findAllSavedChunks(ServerLevel serverLevel) {
        List<ChunkPos> result = new ArrayList<>();

        //File regionDir = serverLevel.getChunkSource().chunkMap.storage.getDimensionPath().resolve("region").toFile();
        /*
        File regionDir = serverLevel.getServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve(serverLevel.dimension().location().getNamespace())
                .resolve(serverLevel.dimension().location().getPath())
                .resolve("region")
                .toFile();
        */
        File regionDir = getRegionFolder(serverLevel);

        if (!regionDir.exists() || !regionDir.isDirectory()) {
            System.out.println("[DEBUG] Region folder not found.");
            return result;
        }

        System.out.println("[DEBUG] Region folder: " + regionDir.getAbsolutePath());

        File[] regionFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
        if (regionFiles == null) return result;

        for (File regionFile : regionFiles) {
            try (RegionFile region = new RegionFile(regionFile.toPath(), regionDir.toPath(), false)) {
                String[] parts = regionFile.getName().replace(".mca", "").replace("r.", "").split("\\.");
                int regionX = Integer.parseInt(parts[0]);
                int regionZ = Integer.parseInt(parts[1]);

                for (int localX = 0; localX < 32; localX++) {
                    for (int localZ = 0; localZ < 32; localZ++) {
                        ChunkPos pos = new ChunkPos((regionX << 5) + localX, (regionZ << 5) + localZ);
                        if (region.hasChunk(pos)) {
                            //System.out.println("✔ Found chunk: " + pos);
                            result.add(pos);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[SavedChunkFinder] Failed to read region file: " + regionFile.getName());
                e.printStackTrace();
            }
        }

        return result;
    }

    public static List<ChunkPos> findAllNonEdgeChunks(ServerLevel level) {
        Set<ChunkPos> saved = new HashSet<>(findAllSavedChunks(level));
        List<ChunkPos> filtered = new ArrayList<>();

        for (ChunkPos pos : saved) {
            boolean hasAllNeighbors =
                    saved.contains(new ChunkPos(pos.x + 1, pos.z)) &&
                            saved.contains(new ChunkPos(pos.x - 1, pos.z)) &&
                            saved.contains(new ChunkPos(pos.x, pos.z + 1)) &&
                            saved.contains(new ChunkPos(pos.x, pos.z - 1));

            if (hasAllNeighbors) {
                filtered.add(pos);
            }
        }

        return filtered;
    }

    public static File getRegionFolder(ServerLevel serverLevel) {
        File root = serverLevel.getServer().getWorldPath(LevelResource.ROOT).toFile();
        ResourceKey<Level> dimension = serverLevel.dimension();

        if (dimension == Level.OVERWORLD) {
            return new File(root, "region");
        } else if (dimension == Level.NETHER) {
            return new File(root, "DIM-1/region");
        } else if (dimension == Level.END) {
            return new File(root, "DIM1/region");
        } else {
            // ✅ For custom/modded dimensions like jx:london
            return new File(root, "dimensions/" +
                    dimension.location().getNamespace() + "/" +
                    dimension.location().getPath() + "/region");
        }
    }

}
