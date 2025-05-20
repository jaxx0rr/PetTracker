package com.github.justinwon777.pettracker.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class TeleportPacket {
    private final UUID id;


    public TeleportPacket(UUID id) {
        this.id = id;
    }

    public static TeleportPacket decode(FriendlyByteBuf buf) {
        return new TeleportPacket(buf.readUUID());
    }

    public static void encode(TeleportPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.id);
    }

    public UUID getUUID() {
        return this.id;
    }

    public static void handle(TeleportPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.level() instanceof ServerLevel) {
                ServerLevel level = (ServerLevel) player.level();
                Mob entity = (Mob) level.getEntity(msg.getUUID());
                if (entity != null) {
                    BlockPos blockpos = player.blockPosition();
                    if (msg.canTeleportTo(blockpos, (ServerLevel) player.level(), entity)) {
                        entity.moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(),
                                entity.getYRot(),
                                entity.getXRot());
                        entity.getNavigation().stop();
                    } else {
                        player.sendSystemMessage(Component.literal("Cannot teleport to current location"));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("Mob doesn't exist"));
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    private boolean canTeleportTo(BlockPos pPos, ServerLevel level, Entity entity) {
        BlockPathTypes blockpathtypes = WalkNodeEvaluator.getBlockPathTypeStatic(level, pPos.mutable());
        if (blockpathtypes != BlockPathTypes.WALKABLE) {
            return false;
        } else {
            BlockState blockstate = level.getBlockState(pPos.below());
            if (blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = pPos.subtract(entity.blockPosition());
                return level.noCollision(entity, entity.getBoundingBox().move(blockpos));
            }
        }
    }
}
