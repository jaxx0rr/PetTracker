package com.github.justinwon777.pettracker.client;

import com.github.justinwon777.pettracker.PetTracker;
import com.github.justinwon777.pettracker.core.PacketHandler;
import com.github.justinwon777.pettracker.core.PetPositionTracker;
import com.github.justinwon777.pettracker.core.PetScanner;
import com.github.justinwon777.pettracker.networking.RemovePacket;
import com.github.justinwon777.pettracker.networking.TeleportPacket;
import com.github.justinwon777.pettracker.util.DeepScanManager;
import com.github.justinwon777.pettracker.util.DeepScanManagerSlowMode;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.List;
import java.util.stream.StreamSupport;

@OnlyIn(Dist.CLIENT)
public class TrackerScreen extends Screen {

    private static final ResourceLocation TRACKER_BACKGROUND = new ResourceLocation(PetTracker.MOD_ID,
            "textures/trackerscreen.png");
    protected int imageWidth;
    protected int imageHeight;
    protected int leftPos;
    protected int topPos;
    private TrackerList trackerList;
    private Button teleportButton;
    private Button removeButton;
    private Button scanButton;
    private Button extScanButton;
    private Button deepScanButton;
    private Button deepScanButtonFast;
    private final ItemStack itemStack;
    private final String hand;
    private final double px;
    private final double py;
    private final double pz;

    public TrackerScreen(ItemStack tracker, String hand, double x, double y, double z) {
        super(tracker.getItem().getDescription());
        this.imageHeight = 222;
        this.imageWidth = 176;
        this.itemStack = tracker;
        this.hand = hand;
        this.px = x;
        this.py = y;
        this.pz = z;
    }



    public Font getFont() {
        return this.font;
    }

    public double getPlayerX() {
        return this.px;
    }

    public double getPlayerY() {
        return this.py;
    }

    public double getPlayerZ() {
        return this.pz;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public String getHandKey() {
        return this.hand;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        this.trackerList = addWidget(new TrackerList(this.minecraft, this.itemStack, this, this.topPos));

        this.teleportButton = addRenderableWidget(
                new Button.Builder(Component.literal("Teleport"), btn -> {
                    TrackerList.Entry entry = this.trackerList.getSelected();
                    if (entry != null) {
                        //PacketHandler.INSTANCE.sendToServer(new TeleportPacket(entry.uuid));
                        PacketHandler.INSTANCE.sendToServer(new TeleportPacket(entry.getUuid()));
                    }
                }).bounds(leftPos + 5, topPos + imageHeight - 10 - 15, 83, 20)
                        .build()
        );

        this.removeButton = addRenderableWidget(
                new Button.Builder(Component.literal("Remove"), btn -> {
                    TrackerList.Entry entry = this.trackerList.getSelected();
                    if (entry != null) {
                        //PacketHandler.INSTANCE.sendToServer(new RemovePacket(entry.uuid, this.itemStack, this.hand));
                        PacketHandler.INSTANCE.sendToServer(new RemovePacket(entry.getUuid(), this.itemStack, this.hand));
                        this.trackerList.delete(entry);
                        updateRemoveButtonStatus(false);
                        updateTeleportButtonStatus(false);
                    }
                }).bounds(leftPos + 89, topPos + imageHeight - 10 - 15, 83, 20)
                        .build()
        );

        this.scanButton = this.addRenderableWidget(
                new Button.Builder(Component.literal("Scan"), btn -> {
                    Set<UUID> alreadyTracked = new HashSet<>();

                    // Collect tracked UUIDs
                    for (ObjectSelectionList.Entry<?> entry : this.trackerList.children()) {
                        if (entry instanceof TrackerList.Entry typedEntry && typedEntry.isTracked()) {
                            alreadyTracked.add(typedEntry.getUuid());
                        }
                    }

//                    trackerList.children().removeIf(e ->
//                            e instanceof TrackerList.Entry typed && "scan".equals(typed.getSource())
//                    );
                    trackerList.children().removeIf(e -> "scan".equals(((TrackerList.Entry) e).getSource()));

                    // Scan for new untracked pets and display them
                    List<TrackerList.Entry> untrackedPets = PetScanner.scanLoadedPets(
                            alreadyTracked, this, this.trackerList.getWidth(), this.trackerList);

                    for (TrackerList.Entry entry : untrackedPets) {
                        this.trackerList.addUntrackedEntry(entry);
                    }
                })
                        .bounds(leftPos + 10, topPos + 15, 40, 16)
                        .tooltip(Tooltip.create(Component.literal("Scan nearby area for pets")))
                        .build()
        );

        this.extScanButton = this.addRenderableWidget(
                new Button.Builder(Component.literal("Ext. Scan"), btn -> {
                    Set<UUID> alreadyTracked = new HashSet<>();
                    for (ObjectSelectionList.Entry<?> entry : this.trackerList.children()) {
                        if (entry instanceof TrackerList.Entry typed && typed.isTracked()) {
                            alreadyTracked.add(typed.getUuid());
                        }
                    }

                    // Clean up previously added global pets
//                    trackerList.children().removeIf(e ->
//                            e instanceof TrackerList.Entry typed && "extscan".equals(typed.getSource())
//                    );
                    trackerList.children().removeIf(e -> "extscan".equals(((TrackerList.Entry) e).getSource()));

                    List<TrackerList.Entry> found = PetScanner.scanAllPetsInCurrentDimension(
                            alreadyTracked, this, this.trackerList.getWidth(), this.trackerList);

                    for (TrackerList.Entry e : found) {
                        this.trackerList.addUntrackedEntry(e);
                    }
                })
                        .bounds(leftPos + 50, topPos + 15, 60, 16)
                        .tooltip(Tooltip.create(Component.literal("Scan loaded chunks for pets.")))
                        .build()
        );

        this.deepScanButton = this.addRenderableWidget(
                new Button.Builder(Component.literal("Deep Scan"), btn -> {
                    if (this.minecraft != null && this.minecraft.player != null && this.minecraft.level != null) {
                        if (!this.minecraft.isSingleplayer()) {
                            this.minecraft.player.sendSystemMessage(Component.literal("Deep Scan only works in singleplayer."));
                            return;
                        }

//                        trackerList.children().removeIf(e ->
//                                e instanceof TrackerList.Entry typed && "deepscan".equals(typed.getSource())
//                        );
                        trackerList.children().removeIf(e -> "deepscan".equals(((TrackerList.Entry) e).getSource()));

                        ServerPlayer serverPlayer = this.minecraft.getSingleplayerServer()
                                .getPlayerList()
                                .getPlayer(this.minecraft.player.getUUID());

                        if (serverPlayer != null) {
                            this.minecraft.getSingleplayerServer().execute(() -> {
                                //DeepScanManagerSlowMode.start(this.minecraft.getSingleplayerServer().getLevel(this.minecraft.level.dimension()), serverPlayer, 30);
                                DeepScanManagerSlowMode.start(
                                        this.minecraft.getSingleplayerServer().getLevel(this.minecraft.level.dimension()),
                                        serverPlayer,
                                        16,
                                        this,
                                        this.trackerList
                                );
                            });
                        }

                    }
                })
                        .bounds(leftPos + 110, topPos + 15, 60, 16)
                        .tooltip(Tooltip.create(Component.literal("Scan unloaded chunks for pets - this is slow but safe for servers! (and might generate chunks so backup your world!)")))
                        .build()
        );
        this.deepScanButtonFast = this.addRenderableWidget(
                new Button.Builder(Component.literal("Fast S."), btn -> {
                    if (this.minecraft != null && this.minecraft.player != null && this.minecraft.level != null) {
                        if (!this.minecraft.isSingleplayer()) {
                            this.minecraft.player.sendSystemMessage(Component.literal("Deep Scan only works in singleplayer."));
                            return;
                        }

//                        trackerList.children().removeIf(e ->
//                                e instanceof TrackerList.Entry typed && "deepscan".equals(typed.getSource())
//                        );
                        trackerList.children().removeIf(e -> "deepscan".equals(((TrackerList.Entry) e).getSource()));

                        ServerPlayer serverPlayer = this.minecraft.getSingleplayerServer()
                                .getPlayerList()
                                .getPlayer(this.minecraft.player.getUUID());

                        if (serverPlayer != null) {
                            this.minecraft.getSingleplayerServer().execute(() -> {
                                DeepScanManager.start(this.minecraft.getSingleplayerServer().getLevel(this.minecraft.level.dimension()), serverPlayer, 16);
                            });
                        }

                    }
                })
                        .bounds(leftPos + 110, topPos + 15, 60, 16)
                        .tooltip(Tooltip.create(Component.literal("Scan unloaded chunks for pets (same as Deep Scan but no queue) - this is fast but bad for servers! (and might generate chunks so backup your world!)")))
                        .build()
        );


        updateTeleportButtonStatus(false);
        updateRemoveButtonStatus(false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {

        for (ObjectSelectionList.Entry<?> entry : this.trackerList.children()) {
            if (entry instanceof TrackerList.Entry e && !e.isTracked()) {
                Entity entity = StreamSupport.stream(
                                Minecraft.getInstance().level.entitiesForRendering().spliterator(), false)
                        .filter(en -> e.getUuid().equals(e.getUuid()))
                        .findFirst()
                        .orElse(null);

                if (entity != null) {
                    //System.out.println("[UPDATE] Found untracked entity: " + e.getUuid());
                    PetPositionTracker.updatePet(entity);
                } else {
                    //System.out.println("[MISS] Could NOT find entity: " + e.getUuid());
                }
            }
        }

        this.renderBackground(guiGraphics);
        this.trackerList.render(guiGraphics, pMouseX, pMouseY, pPartialTick);

        String header = "Pet Tracker";
        guiGraphics.drawString(this.font, header,
                (this.width / 2 - this.font.width(header) / 2),
                this.topPos + 5,
                4210752, false);

        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(guiGraphics, pMouseX, pMouseY);
    }

    public void renderBackground(GuiGraphics guiGraphics) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TRACKER_BACKGROUND);
        guiGraphics.blit(TRACKER_BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }


    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.teleportButton.isHoveredOrFocused()) {
            List<Component> tooltips = new ArrayList<>();
            if (this.teleportButton.isActive() || this.trackerList.getSelected() == null) {
                tooltips.add(Component.literal("Teleports mob to you")
                        .withStyle(ChatFormatting.GRAY)
                        .withStyle(ChatFormatting.ITALIC));
            } else {
                tooltips.add(Component.literal("Mob is either dead or in an unloaded chunk. " +
                                "Location is last known position.")
                        .withStyle(ChatFormatting.GRAY)
                        .withStyle(ChatFormatting.ITALIC));
            }
            guiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
        }

        if (this.removeButton.isHoveredOrFocused()) {
            List<Component> tooltips = new ArrayList<>();
            tooltips.add(Component.literal("Removes mob from the list.")
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle(ChatFormatting.ITALIC));
            guiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
        }
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(pKeyCode, pScanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    public void updateTeleportButtonStatus(boolean pActive) {
        this.teleportButton.active = pActive;
    }

    public void updateRemoveButtonStatus(boolean pActive) {
        this.removeButton.active = pActive;
    }

    public boolean isPauseScreen() {
        return false;
    }

}
