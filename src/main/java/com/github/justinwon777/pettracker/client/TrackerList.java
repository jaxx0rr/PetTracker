package com.github.justinwon777.pettracker.client;


import com.github.justinwon777.pettracker.core.PetPositionTracker;
import com.github.justinwon777.pettracker.item.Tracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

@OnlyIn(Dist.CLIENT)
public class TrackerList extends ObjectSelectionList<TrackerList.Entry> {
    private final TrackerScreen screen;

    public TrackerList(Minecraft pMinecraft, ItemStack itemstack, TrackerScreen screen, int top) {
        super(pMinecraft, screen.width, screen.height, top + 30, top + 180, 37);
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        this.screen = screen;

        // Extract UUIDs from tag, including tracking metadata
        CompoundTag tag = itemstack.getTag();
        if (tag != null) {
            ListTag listTag = tag.getList(Tracker.TRACKING, 10);
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag entityTag = listTag.getCompound(i);
                UUID uuid = entityTag.getUUID("uuid");
                String source = entityTag.contains("source") ? entityTag.getString("source") : "tracked";

                Entity entity = StreamSupport.stream(
                                Minecraft.getInstance().level.entitiesForRendering().spliterator(), false)
                        .filter(e -> uuid.equals(e.getUUID()))
                        .findFirst()
                        .orElse(null);

                if (entity instanceof TamableAnimal tamable) {
                    String name = tamable.getDisplayName().getString();
                    BlockPos pos = entity.blockPosition();
                    boolean active = tamable.isAlive();
                    this.addEntry(new Entry(name, pos.getX(), pos.getY(), pos.getZ(), active, uuid, source.equals("tracked"), this.screen, this.width, this, source));
                } else {
                    String name = entityTag.getString("name");
                    this.addEntry(new Entry(name, 0, 0, 0, false, uuid, false, this.screen, this.width, this, source));
                }

            }
        }

        if (this.getSelected() != null) {
            this.centerScrollOn(this.getSelected());
        }
    }

    public void addEntriesFromNBT() {
        ItemStack tracker = this.screen.getItemStack();
        CompoundTag tag = tracker.getOrCreateTag();
        if (!tag.contains(Tracker.TRACKING)) return;

        ListTag listTag = tag.getList(Tracker.TRACKING, 10);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag entityTag = listTag.getCompound(i);
            UUID uuid = entityTag.getUUID("uuid");
            String source = entityTag.getString("source");

            Entity entity = StreamSupport.stream(
                            Minecraft.getInstance().level.entitiesForRendering().spliterator(), false)
                    .filter(e -> uuid.equals(e.getUUID()))
                    .findFirst()
                    .orElse(null); // ✅ working UUID match


            if (entity instanceof TamableAnimal tamable) {
                String name = tamable.getDisplayName().getString();
                BlockPos pos = entity.blockPosition();
                boolean active = tamable.isAlive();
                this.addEntry(new Entry(name, pos.getX(), pos.getY(), pos.getZ(), active, uuid, source.equals("tracked"), this.screen, this.width, this, source));
            } else {
                String name = entityTag.getString("name");
                int x = entityTag.getInt("x");
                int y = entityTag.getInt("y");
                int z = entityTag.getInt("z");
                boolean active = entityTag.getBoolean("active");
                this.addEntry(new Entry(name, x, y, z, active, uuid, false, this.screen, this.width, this, source));
            }
        }
    }


    public void refresh() {
        this.children().clear();
        this.addEntriesFromNBT();
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
        //return TrackerScreen.this.getFocused() == this;
        return screen.getFocused() == this;
    }

    public void delete(Entry entry) {
        // Remove from visual list
        this.removeEntry(entry);

        // Remove from itemstack's NBT on client
        ItemStack tracker = this.screen.getItemStack();
        CompoundTag tag = tracker.getOrCreateTag();

        if (tag.contains(Tracker.TRACKING)) {
            ListTag list = tag.getList(Tracker.TRACKING, 10);
            ListTag newList = new ListTag();

            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                if (!t.getUUID("uuid").equals(entry.getUuid())) {
                    newList.add(t); // keep others
                }
            }

            tag.put(Tracker.TRACKING, newList);
            tracker.setTag(tag);
        }

        this.refresh(); // now it reflects the real NBT
    }



    public void addUntrackedEntry(TrackerList.Entry entry) {
        addEntry(entry); // ✅ this is the real method from AbstractSelectionList
    }

    @OnlyIn(Dist.CLIENT)
    public static class Entry extends ObjectSelectionList.Entry<TrackerList.Entry> {    private final String name;
        private final int x, y, z;
        private final UUID uuid;
        private final boolean active;
        private final boolean tracked;
        private final TrackerScreen screen;
        private final int listWidth;
        private final TrackerList parentList;
        private final String source;

        public Entry(String name, int x, int y, int z, boolean active, UUID uuid, boolean tracked, TrackerScreen screen, int listWidth, TrackerList parentList, String source) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.active = active;
            this.uuid = uuid;
            this.tracked = tracked;
            this.screen = screen;
            this.listWidth = listWidth;
            this.parentList = parentList;
            this.source = source;
        }

        public String getSource() {
            return this.source;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public boolean isTracked() {
            return this.tracked;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            PetPositionTracker.TrackedPet pos = PetPositionTracker.get(this.uuid);

            String location = pos != null
                    ? "Location: " + (int) pos.x() + ", " + (int) pos.y() + ", " + (int) pos.z()
                    : "Location: ???";

            String distance = pos != null
                    ? distanceTo((int) pos.x(), (int) pos.y(), (int) pos.z()) + " blocks away"
                    : "Distance: ???";

            int color;
            String label;

            switch (this.source) {
                case "scan" -> {
                    color = 0xFF6666;     // Red: nearby but untracked
                    label = " \uD83E\uDDED";
                }
                case "extscan" -> {
                    color = 0x3399FF;     // Blue: far but loaded
                    label = " \uD83D\uDCE1";
                }
                case "deepscan" -> {
                    color = 0x000000;     // Black: from unloaded chunks
                    label = " \uD83D\uDEF0";
                }
                default -> { // "tracked" or anything unknown
                    color = 0xAAAAAA;     // Gray: default tracked pets
                    label = " \uD83D\uDCCC";
                }
            }


            String displayName = this.name + label;

            int centerX = listWidth / 2;

            guiGraphics.drawString(screen.getFont(), displayName,
                    centerX - screen.getFont().width(displayName) / 2,
                    y + 1, color, false);

            guiGraphics.drawString(screen.getFont(), location,
                    centerX - screen.getFont().width(location) / 2,
                    y + 12, color, false);

            guiGraphics.drawString(screen.getFont(), distance,
                    centerX - screen.getFont().width(distance) / 2,
                    y + 23, color, false);
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
            parentList.setSelected(this);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
        }

        public int distanceTo(int x, int y, int z) {
            float f = (float) (screen.getPlayerX() - x);
            float f1 = (float) (screen.getPlayerY() - y);
            float f2 = (float) (screen.getPlayerZ() - z);
            return (int) Mth.sqrt(f * f + f1 * f1 + f2 * f2);
        }

    }
}