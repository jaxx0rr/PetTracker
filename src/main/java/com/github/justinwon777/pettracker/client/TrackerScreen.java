package com.github.justinwon777.pettracker.client;

import com.github.justinwon777.pettracker.PetTracker;
import com.github.justinwon777.pettracker.core.PacketHandler;
import com.github.justinwon777.pettracker.core.PetPositionTracker;
import com.github.justinwon777.pettracker.core.PetScanner;
import com.github.justinwon777.pettracker.networking.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@OnlyIn(Dist.CLIENT)
public class TrackerScreen extends Screen {

    private static final ResourceLocation TRACKER_BACKGROUND = new ResourceLocation(PetTracker.MOD_ID,
            "textures/trackerscreen.png");
    protected int imageWidth;
    protected int imageHeight;
    protected int leftPos;
    protected int topPos;

    private int offsetX;
    private int offsetY;

    private TrackerList trackerList;
    private Button teleportButton;
    private Button tpToButton;
    private Button removeButton;
    private Button scanButton;
    private Button extScanButton;
    private Button deepScanButton;
    private Button deepScanButtonFast;
    private Button stopScanButton;
    private final ItemStack itemStack;
    private final String hand;
    private final double px;
    private final double py;
    private final double pz;

    private static boolean deepScanRunning = false;

    private int syncTickCounter = 0;
    private static final int SYNC_INTERVAL_TICKS = 100; // once per second

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

    public TrackerList getTrackerList() {
        return this.trackerList;
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

    //public ItemStack getItemStack() { return this.itemStack; }


    @Override
    public void tick() {
        super.tick();

        syncTickCounter++;
        if (syncTickCounter >= SYNC_INTERVAL_TICKS) {
            syncTickCounter = 0;

            if (this.minecraft != null && this.minecraft.player != null) {
                this.trackerList.syncVisiblePets();
            }
        }
    }

    public ItemStack getItemStack() {
        return Minecraft.getInstance().player.getItemInHand(
                this.hand.equals("m") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND
        );
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
        this.trackerList.refresh();


        this.scanButton = this.addRenderableWidget(
                new Button.Builder(Component.literal("Area Scan"), btn -> {
                    PetScanner.scanLoadedPets(this);
                })
                        .bounds(leftPos + 10, topPos + 15, 60, 16)
                        .tooltip(Tooltip.create(Component.literal("Scan nearby area for pets")))
                        .build()
        );


        // Only show this button in singleplayer (integrated server)
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            this.extScanButton = this.addRenderableWidget(
                    new Button.Builder(Component.literal("Loaded Chunks"), btn -> {
                        trackerList.children().removeIf(e -> "extscan".equals(((TrackerList.Entry) e).getSource()));
                        PacketHandler.INSTANCE.sendToServer(new StartDeepScanPacket(this.hand, 0, 2)); // loaded scan
                    })
                            .bounds(leftPos + 75, topPos + 15, 90, 16)
                            .tooltip(Tooltip.create(Component.literal("Scan loaded chunks for pets.")))
                            .build()
            );
        }

        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            offsetX = this.leftPos + 11;
            offsetY = this.topPos + 180;
        } else {
            offsetX = this.leftPos + 75;
            offsetY = this.topPos + 15;
        }

        EditBox ringInput = new EditBox(this.font, offsetX, offsetY+2, 20, 14, Component.literal("Radius"));
        ringInput.setMaxLength(2);         // Allow up to 2 digits
        ringInput.setValue("20");            // Default
        this.addRenderableWidget(ringInput);

        this.deepScanButton = this.addRenderableWidget(
                new Button.Builder(Component.literal("Deep Scan"), btn -> {

                    if (this.minecraft != null && this.minecraft.player != null) {

                        if (!this.minecraft.player.isCreative()) {
                            this.minecraft.player.displayClientMessage(Component.literal("Deep Scan is only available in Creative mode."), true);
                            return;
                        }

                        trackerList.children().removeIf(e -> "deepscan".equals(((TrackerList.Entry) e).getSource()));

                        deepScanRunning = true;
                        updateScanButtonVisibility();

                        int radius;
                        try {
                            radius = Integer.parseInt(ringInput.getValue());
                            radius = Mth.clamp(radius, 10, 500);
                        } catch (NumberFormatException e) {
                            radius = 20;
                        }

                        PacketHandler.INSTANCE.sendToServer(new StartDeepScanPacket(this.hand, radius, 0)); // slow mode
                    }
                })
                        .bounds(offsetX+24, offsetY, 60, 16)
                        .tooltip(Tooltip.create(Component.literal("Scan unloaded chunks for pets (in radius) - this is slow but safe for servers! (and might generate chunks so backup your world!)")))
                        .build()
        );


        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            this.deepScanButtonFast = this.addRenderableWidget(
                    new Button.Builder(Component.literal("Fast Scan"), btn -> {
                        if (this.minecraft != null && this.minecraft.player != null) {

                            if (!this.minecraft.player.isCreative()) {
                                this.minecraft.player.displayClientMessage(Component.literal("Fast Scan is only available in Creative mode."), true);
                                return;
                            }

                            trackerList.children().removeIf(e -> "deepscan".equals(((TrackerList.Entry) e).getSource()));

                            deepScanRunning = true;
                            updateScanButtonVisibility();

                            int radius;
                            try {
                                radius = Integer.parseInt(ringInput.getValue());
                                radius = Mth.clamp(radius, 10, 500);
                            } catch (NumberFormatException e) {
                                radius = 20;
                            }

                            PacketHandler.INSTANCE.sendToServer(new StartDeepScanPacket(this.hand, radius, 1)); // fast mode
                        }
                    })
                            .bounds(leftPos + 100, topPos + 180, 60, 16)
                            .tooltip(Tooltip.create(Component.literal("Scan unloaded chunks for pets (same as Deep Scan but no queue so don't use a radius bigger than 50 if your PC is slow) - this is fast but bad for servers! (and might generate chunks so backup your world!)")))
                            .build()
            );
        }

        this.stopScanButton = this.addRenderableWidget(
                new Button.Builder(Component.literal("Stop Deep Scan"), btn -> {
                    PacketHandler.INSTANCE.sendToServer(new CancelDeepScanPacket());
                })
                        .bounds(leftPos + 15, topPos + 180, 145, 16)
                        .tooltip(Tooltip.create(Component.literal("Cancel current deep scan in progress")))
                        .build()
        );

        int buttonY = topPos + imageHeight - 10 - 15;
        int buttonW = 50; // Adjusted width
        int spacing = 5;

        this.teleportButton = addRenderableWidget(
                new Button.Builder(Component.literal("TP"), btn -> {
                    TrackerList.Entry entry = this.trackerList.getSelected();
                    if (entry != null) {
                        PacketHandler.INSTANCE.sendToServer(new TeleportPacket(entry.getUuid()));
                    }
                }).bounds(leftPos + 5, buttonY, buttonW, 20)
                        .build()
        );

        this.tpToButton = addRenderableWidget(
                new Button.Builder(Component.literal("TP To"), btn -> {
                    TrackerList.Entry entry = this.trackerList.getSelected();
                    if (entry != null) {
                        PacketHandler.INSTANCE.sendToServer(
                                new TeleportPlayerToLocationPacket(
                                        entry.getX(),
                                        entry.getY(),
                                        entry.getZ(),
                                        entry.getDimensionString() // ⬅️ include dimension
                                )
                        );
                    }
                }).bounds(leftPos + 5 + buttonW + spacing, buttonY, buttonW, 20)
                        .build()
        );

        this.removeButton = addRenderableWidget(
                new Button.Builder(Component.literal("Remove"), btn -> {
                    TrackerList.Entry entry = this.trackerList.getSelected();
                    if (entry != null) {
                        PacketHandler.INSTANCE.sendToServer(new RemovePacket(entry.getUuid(), this.itemStack, this.hand));
                        this.trackerList.delete(entry);
                        updateRemoveButtonStatus(false);
                        updateTeleportButtonStatus(false);
                        updateTpToButtonStatus(false);
                    }
                }).bounds(leftPos + 5 + 2 * (buttonW + spacing), buttonY, buttonW, 20)
                        .build()
        );


        updateTeleportButtonStatus(false);
        updateRemoveButtonStatus(false);
        updateTpToButtonStatus(false);
        updateScanButtonVisibility();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {

        for (ObjectSelectionList.Entry<?> entry : this.trackerList.children()) {
            if (entry instanceof TrackerList.Entry e) {
                Entity entity = StreamSupport.stream(
                                Minecraft.getInstance().level.entitiesForRendering().spliterator(), false)
                        .filter(en -> e.getUuid().equals(e.getUuid()))
                        .findFirst()
                        .orElse(null);

                if (entity != null) {
                    PetPositionTracker.updatePet(entity);
                } else {
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
                tooltips.add(Component.literal("Teleports pet to you")
                        .withStyle(ChatFormatting.GRAY)
                        .withStyle(ChatFormatting.ITALIC));
            } else {
                tooltips.add(Component.literal("Pet is either dead or in an unloaded chunk. " +
                                "Location is last known position.")
                        .withStyle(ChatFormatting.GRAY)
                        .withStyle(ChatFormatting.ITALIC));
            }
            guiGraphics.renderTooltip(this.font, tooltips, Optional.empty(), x, y);
        }

        if (this.tpToButton.isHoveredOrFocused()) {
            List<Component> tooltips = new ArrayList<>();

            tooltips.add(Component.literal("Teleport yourself to the pet’s location")
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle(ChatFormatting.ITALIC));

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

    public void updateTpToButtonStatus(boolean pActive) {
        this.tpToButton.active = pActive;
    }

    public void updateRemoveButtonStatus(boolean pActive) {
        this.removeButton.active = pActive;
    }

    private void updateScanButtonVisibility() {
        boolean scanRunning = deepScanRunning;
        if (scanButton != null) scanButton.visible = !scanRunning;         // 🔴 Red Scan
        if (extScanButton != null) extScanButton.visible = !scanRunning;   // 🔵 Blue Scan
        if (deepScanButton != null) deepScanButton.visible = !scanRunning; // ⚫ Black Scan
        if (deepScanButtonFast != null) deepScanButtonFast.visible = !scanRunning;
        if (teleportButton != null) teleportButton.visible = !scanRunning;
        if (tpToButton != null) tpToButton.visible = !scanRunning;
        if (removeButton != null) removeButton.visible = !scanRunning;
        if (stopScanButton != null) stopScanButton.visible = scanRunning;
    }


    public boolean isPauseScreen() {
        return false;
    }

    public void onScanFinished() {
        this.deepScanRunning = false;
        updateScanButtonVisibility();
    }


}
