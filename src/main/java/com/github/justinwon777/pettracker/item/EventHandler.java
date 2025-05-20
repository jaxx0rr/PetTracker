package com.github.justinwon777.pettracker.item;

import com.github.justinwon777.pettracker.PetTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelData;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.checkerframework.checker.units.qual.C;

import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = PetTracker.MOD_ID)
public class EventHandler {

//    public static final String LOAD_CHUNK = "load_chunk";
//    public static final String PREV_CHUNK = "prev_chunk";
//    public static final String CHUNK_TIMER = "chunk_timer";

    @SubscribeEvent
    public static void trackerInteract(final PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof LivingEntity target) {
            Player player = event.getEntity();
            InteractionHand hand = event.getHand();
            ItemStack itemstack = player.getItemInHand(hand);

            if (!target.level().isClientSide) {
                if (itemstack.getItem() instanceof Tracker) {
                    itemstack.getItem().interactLivingEntity(itemstack, player, target, hand);
                    event.setCanceled(true);
                }
            }
        }
    }


}
