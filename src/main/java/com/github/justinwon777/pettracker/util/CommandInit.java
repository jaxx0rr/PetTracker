package com.github.justinwon777.pettracker.util;

import com.github.justinwon777.pettracker.PetTracker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = PetTracker.MOD_ID)
public class CommandInit {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        //register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jxpf")
                .then(Commands.literal("scan")
                        .requires(cs -> cs.hasPermission(2)) // ops only
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();

                            List<ChunkPos> chunks = SavedChunkFinder.findAllSavedChunks(level);
                            int count = chunks.size();

                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("✅ Found " + count + " saved chunks in " + level.dimension().location()),
                                    false
                            );

                            return count;
                        })
                )
                .then(Commands.literal("deepscan")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("rings", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();
                                    int rings = IntegerArgumentType.getInteger(ctx, "rings");
                                    //DeepScanManagerSlowMode.start(level, player, rings);
                                    return 1;
                                }))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();
                            //DeepScanManagerSlowMode.start(level, player, 30); // default to 30 rings
                            return 1;
                        })
                )
                .then(Commands.literal("deepscanfast")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("rings", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();
                                    int rings = IntegerArgumentType.getInteger(ctx, "rings");
                                    //DeepScanManager.start(level, player, rings);
                                    return 1;
                                }))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();
                            //DeepScanManager.start(level, player, 30); // default to 30 rings
                            return 1;
                        })
                )


        );
    }
}
