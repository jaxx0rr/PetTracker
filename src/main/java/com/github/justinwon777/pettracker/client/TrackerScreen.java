package com.github.justinwon777.pettracker.client;

import com.github.justinwon777.pettracker.PetTracker;
import com.github.justinwon777.pettracker.core.PacketHandler;
import com.github.justinwon777.pettracker.item.Tracker;
import com.github.justinwon777.pettracker.networking.RemovePacket;
import com.github.justinwon777.pettracker.networking.TeleportPacket;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.List;

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
                        PacketHandler.INSTANCE.sendToServer(new TeleportPacket(entry.uuid));
                    }
                }).bounds(leftPos + 5, topPos + imageHeight - 10 - 15, 83, 20)
                        .build()
        );

        this.removeButton = addRenderableWidget(
                new Button.Builder(Component.literal("Remove"), btn -> {
                    TrackerList.Entry entry = this.trackerList.getSelected();
                    if (entry != null) {
                        PacketHandler.INSTANCE.sendToServer(new RemovePacket(entry.uuid, this.itemStack, this.hand));
                        this.trackerList.delete(entry);
                        updateRemoveButtonStatus(false);
                        updateTeleportButtonStatus(false);
                    }
                }).bounds(leftPos + 89, topPos + imageHeight - 10 - 15, 83, 20)
                        .build()
        );

        updateTeleportButtonStatus(false);
        updateRemoveButtonStatus(false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
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

    @OnlyIn(Dist.CLIENT)
    class TrackerList extends ObjectSelectionList<TrackerScreen.TrackerList.Entry> {
        private final TrackerScreen screen;

        public TrackerList(Minecraft pMinecraft, ItemStack itemstack, TrackerScreen screen, int top) {
            super(pMinecraft, TrackerScreen.this.width, TrackerScreen.this.height, top + 30,
                    top + 180, 37);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.screen = screen;
            CompoundTag tag = itemstack.getTag();
            if (tag != null) {
                ListTag listTag = tag.getList(Tracker.TRACKING, 10);
                for (int i = 0; i < listTag.size(); ++i) {
                    CompoundTag entityTag = listTag.getCompound(i);
                    String name = entityTag.getString("name");
                    int x = entityTag.getInt("x");
                    int y = entityTag.getInt("y");
                    int z = entityTag.getInt("z");
                    boolean active = entityTag.getBoolean("active");
                    UUID uuid = entityTag.getUUID("uuid");
                    this.addEntry(new Entry(name, x, y, z, active, uuid));
                }

            }

            if (this.getSelected() != null) {
                this.centerScrollOn(this.getSelected());
            }

        }

        public void setSelected(@Nullable TrackerList.Entry pSelected) {
            super.setSelected(pSelected);
            if (pSelected != null) {
                this.screen.updateRemoveButtonStatus(true);
                this.screen.updateTeleportButtonStatus(pSelected.active);
            }

        }

        protected int getScrollbarPosition() {
            return super.getScrollbarPosition() - 45;
        }

        public int getRowWidth() {
            return super.getRowWidth() - 64;
        }

        protected void renderItem(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int index, int left, int top, int width, int height) {
            TrackerList.Entry e = this.getEntry(index);
            int i = this.x0 + (this.width - width) / 2;
            int j = this.x0 + (this.width + width) / 2;

            guiGraphics.fill(i, top - 2, j, top + height + 2, 0xFFAAAAAA);
            guiGraphics.fill(i + 1, top - 1, j - 1, top + height + 1, 0xFFFFFFFF);

            if (this.isSelectedItem(index)) {
                this.renderSelection(guiGraphics, top, width, height, 0xFF000000, 0xFFE0E0E0);
            }

            e.render(guiGraphics, index, top, left, width, height, mouseX, mouseY, Objects.equals(this.getHovered(), e), partialTick);
        }

        public boolean isFocused() {
            return TrackerScreen.this.getFocused() == this;
        }

        public void delete(Entry entry) {
            this.removeEntry(entry);
        }

        @OnlyIn(Dist.CLIENT)
        public class Entry extends ObjectSelectionList.Entry<TrackerScreen.TrackerList.Entry> {
            public String name;
            public int x;
            public int y;
            public int z;
            public boolean active;
            public UUID uuid;

            public Entry(String name, int x, int y, int z, boolean active, UUID uuid) {
                this.name = name;
                this.x = x;
                this.y = y;
                this.z = z;
                this.active = active;
                this.uuid = uuid;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                String location = "Location: " + this.x + ", " + this.y + ", " + this.z;
                String distance = distanceTo(this.x, this.y, this.z) + " blocks away";

                guiGraphics.drawString(TrackerScreen.this.font, this.name,
                        TrackerScreen.TrackerList.this.width / 2 - TrackerScreen.this.font.width(name) / 2,
                        y + 1, 4210752, false);
                guiGraphics.drawString(TrackerScreen.this.font, location,
                        TrackerScreen.TrackerList.this.width / 2 - TrackerScreen.this.font.width(location) / 2,
                        y + 12, 4210752, false);
                guiGraphics.drawString(TrackerScreen.this.font, distance,
                        TrackerScreen.TrackerList.this.width / 2 - TrackerScreen.this.font.width(distance) / 2,
                        y + 23, 4210752, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    this.select();
                    return true;
                } else {
                    return false;
                }
            }

            private void select() {
                TrackerScreen.TrackerList.this.setSelected(this);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.name);
            }

            public int distanceTo(int x, int y, int z) {
                float f = (float) (TrackerScreen.this.px - x);
                float f1 = (float) (TrackerScreen.this.py - y);
                float f2 = (float) (TrackerScreen.this.pz - z);
                return (int) Mth.sqrt(f * f + f1 * f1 + f2 * f2);
            }
        }
    }
}
