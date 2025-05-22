package com.github.justinwon777.pettracker.util;

import com.github.justinwon777.pettracker.client.TrackerList;
import com.github.justinwon777.pettracker.core.PacketHandler;
import com.github.justinwon777.pettracker.item.Tracker;
import com.github.justinwon777.pettracker.networking.RefreshTrackerListPacket;
import com.github.justinwon777.pettracker.client.TrackerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class DeepScanManager {

    private static boolean cancelled = false;
    private static TrackerList trackerList = null;
    private static TrackerScreen trackerScreen = null;

    public static void cancel() {
        cancelled = true;
    }

    public static void start(ServerLevel level, ServerPlayer player, int maxRings, TrackerScreen screen, TrackerList list) {
        trackerList = list;
        trackerScreen = screen;

        maxRings = maxRings * 2;

        if (trackerScreen != null) {
            ItemStack trackerStack = trackerScreen.getItemStack();
            CompoundTag tag = trackerStack.getOrCreateTag();
            ListTag original = tag.contains(Tracker.TRACKING) ? tag.getList(Tracker.TRACKING, 10) : new ListTag();
            ListTag cleaned = new ListTag();

            for (int i = 0; i < original.size(); i++) {
                CompoundTag entry = original.getCompound(i);
                if (!"deepscan".equals(entry.getString("source"))) {
                    cleaned.add(entry);
                }
            }

            tag.put(Tracker.TRACKING, cleaned);
            trackerStack.setTag(tag);

            InteractionHand hand = trackerScreen.getHandKey().equals("m") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            player.setItemInHand(hand, trackerStack);
        }

        player.sendSystemMessage(Component.literal("[DeepScan] Scanning for valid chunks."));

        List<ChunkPos> savedChunks = SavedChunkFinder.findAllSavedChunks(level);
        Set<ChunkPos> savedChunkSet = new HashSet<>(savedChunks);

        player.sendSystemMessage(Component.literal("[DeepScan] Found " + savedChunks.size() + " saved chunks to scan. Starting scan for pets.."));

        ChunkPos playerChunk = new ChunkPos(player.blockPosition());

        int scanned = 0;
        int ring = 0;
        boolean found = false;

        while (ring < maxRings) {
            if (cancelled) {
                player.sendSystemMessage(Component.literal("[DeepScan] Scan cancelled."));
                cancelled = false;
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                        new RefreshTrackerListPacket(true));
                return;
            }

            List<ChunkPos> ringChunks = getNextRing(playerChunk, ring);
            boolean didScanAny = false;

            for (ChunkPos pos : ringChunks) {

                if (!savedChunkSet.contains(pos)) {
                    System.out.println(Component.literal("[DeepScan] Skipping " + pos + " – not in saved chunks"));
                    continue;
                }
                if (level.hasChunk(pos.x, pos.z)) {
                    System.out.println(Component.literal("[DeepScan] Skipping " + pos + " – already loaded"));
                    continue;
                }

                level.setChunkForced(pos.x, pos.z, true);

                DelayedTaskManager.schedule(5, tick -> {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                    if (chunk != null) {
                        BlockPos min = new BlockPos(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ());
                        BlockPos max = new BlockPos(pos.getMaxBlockX(), level.getMaxBuildHeight(), pos.getMaxBlockZ());

                        for (Entity entity : level.getEntities((Entity) null, new AABB(min, max), e -> true)) {
                            if (entity instanceof TamableAnimal pet) {
                                player.sendSystemMessage(Component.literal(
                                        "[DeepScan Fast] Pet found at " +
                                                entity.blockPosition().getX() + ", " +
                                                entity.blockPosition().getY() + ", " +
                                                entity.blockPosition().getZ() +
                                                " in chunk " + pos.x + ", " + pos.z + ")"
                                ));

                                // ⬇️ Add this block to write to NBT
                                if (trackerScreen != null) {
                                    ItemStack trackerStack = trackerScreen.getItemStack();
                                    CompoundTag tag = trackerStack.getOrCreateTag();
                                    ListTag trackingList = tag.contains(Tracker.TRACKING) ? tag.getList(Tracker.TRACKING, 10) : new ListTag();

                                    CompoundTag petTag = new CompoundTag();
                                    petTag.putUUID("uuid", pet.getUUID());
                                    petTag.putString("name", pet.getDisplayName().getString());
                                    petTag.putInt("x", (int) pet.getX());
                                    petTag.putInt("y", (int) pet.getY());
                                    petTag.putInt("z", (int) pet.getZ());
                                    petTag.putBoolean("active", true);
                                    petTag.putString("source", "deepscan");

                                    trackingList.add(petTag);
                                    tag.put(Tracker.TRACKING, trackingList);
                                    trackerStack.setTag(tag);

                                    InteractionHand hand = trackerScreen.getHandKey().equals("m") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                                    player.setItemInHand(hand, trackerStack);

                                    PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                                            new RefreshTrackerListPacket(false));
                                }
                            }

                        }
                    }

                    level.setChunkForced(pos.x, pos.z, false);
                });

                found = true;
                didScanAny = true;
                scanned++;
            }

            if (!didScanAny && found && ring > 20) {
                player.sendSystemMessage(Component.literal("[DeepScan] No scanable chunks found in ring " + ring + ". Stopping."));
                break;
            }

            ring++;
        }

        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new RefreshTrackerListPacket(true));

        player.sendSystemMessage(Component.literal("[DeepScan] Finished scanning " + scanned + " chunks."));
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
