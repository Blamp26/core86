package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.network.CorePackets;
import io.github.blamp26.core86.foundation.network.packets.ReactorControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ReactorConsoleScreen extends AbstractContainerScreen<ReactorConsoleMenu> {
    private static final int MAP = CoreReactorConstants.MAP_SIZE;
    private static final int[] GROUP_COLORS = {0xFFFFFFFF, 0xFFFF5555, 0xFF6699FF, 0xFFFFFF66, 0xFF55DD55};
    private static final String[] GROUP_NAMES = {"White", "Red", "Blue", "Yellow", "Green"};
    private static final String[] GROUP_CODES = {"W", "R", "B", "Y", "G"};

    private int selectedRodIndex = -1;
    private boolean isRodSettingsOpen = false;
    private Button expToggleButton;
    private InsertionSlider allSlider;
    private InsertionSlider rodSlider;
    private InsertionSlider groupSlider;

    private Layout layout;

    public ReactorConsoleScreen(ReactorConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 1000;
        this.imageHeight = 720;
    }

    @Override
    protected void init() {
        super.init();
        recalcLayout();
        buildButtons();
    }

    private void recalcLayout() {
        int maxW = Math.min(1400, width - 40);
        int maxH = Math.min(900, height - 30);
        int panelW = clampWithFallback((int) (width * 0.78f), 980, maxW);
        int panelH = clampWithFallback((int) (height * 0.82f), 620, maxH);
        this.imageWidth = panelW;
        this.imageHeight = panelH;
        this.layout = new Layout(width, height, panelW, panelH);
    }

    private void buildButtons() {
        allSlider = null;
        rodSlider = null;
        groupSlider = null;
        int[] presets = {0, 25, 50, 75, 100};
        ReactorConsoleBlockEntity be = menu.getBlockEntity();
        boolean authorized = menu.getData().get(14) > 0;
        boolean scramInProgress = menu.getData().get(15) > 0;

        Button scramButton = Button.builder(Component.literal("AZ-5 / SCRAM"), b -> sendControlPacket(0, 100))
                .pos(layout.rightX, layout.emergencyButtonY).size(layout.rightW, 22).build();
        scramButton.setTooltip(Tooltip.create(Component.literal("Emergency insertion: drives all control rods to 100%.")));
        addRenderableWidget(scramButton);

        addPresetButtons(layout.rightX, layout.allGridY, presets, "All", -1, scramInProgress, 0, true);
        allSlider = new InsertionSlider(layout.rightX, layout.allSliderY, layout.rightW, 18, "All rods target", menu.getData().get(0), scramInProgress);
        addRenderableWidget(allSlider);

        expToggleButton = Button.builder(Component.literal(expButtonLabel(authorized)),
                b -> sendControlPacket(6, 0)).pos(layout.rightX, layout.expButtonY).size(layout.rightW, 20).build();
        expToggleButton.active = authorized;
        expToggleButton.setTooltip(Tooltip.create(Component.literal(authorized
                ? "Toggle experiment mode. Auto-SCRAM is disabled while enabled."
                : "Authorize this console with a SIUR key token first.")));
        addRenderableWidget(expToggleButton);

        if (isRodSettingsOpen && selectedRodIndex != -1) {
            for (int i = 0; i < GROUP_CODES.length; i++) {
                final int colorId = i;
                int x = layout.rightX + i * (layout.groupBtnW + layout.groupBtnGap);
                Button b = Button.builder(Component.literal(GROUP_CODES[i]),
                                btn -> sendControlPacket(3, (selectedRodIndex << 16) | colorId))
                        .pos(x, layout.groupAssignY).size(layout.groupBtnW, 18).build();
                b.setTooltip(Tooltip.create(Component.literal("Assign selected rod to " + GROUP_NAMES[i] + " group.")));
                addRenderableWidget(b);
            }

            addPresetButtons(layout.rightX, layout.rodGridY, presets, "Rod", selectedRodIndex, scramInProgress, 2, false);
            rodSlider = new InsertionSlider(layout.rightX, layout.rodSliderY, layout.rightW, 18, "Selected rod target",
                    be.getChannelTargetInsertion(selectedRodIndex), scramInProgress);
            addRenderableWidget(rodSlider);
            int groupColorId = be.getChannelColor(selectedRodIndex);
            addPresetButtons(layout.rightX, layout.groupGridY, presets, "Group " + GROUP_CODES[groupColorId], groupColorId, scramInProgress, 4, false);
            groupSlider = new InsertionSlider(layout.rightX, layout.groupSliderY, layout.rightW, 18, "Group " + GROUP_CODES[groupColorId] + " target",
                    be.getChannelTargetInsertion(selectedRodIndex), scramInProgress);
            addRenderableWidget(groupSlider);

            Button close = Button.builder(Component.literal("Close rod controls"), b -> {
                        isRodSettingsOpen = false;
                        this.init(this.minecraft, this.width, this.height);
                    }).pos(layout.rightX, layout.closeButtonY).size(layout.rightW, 20).build();
            close.setTooltip(Tooltip.create(Component.literal("Close selected rod controls.")));
            addRenderableWidget(close);
        }
    }

    private void addPresetButtons(int startX, int startY, int[] presets, String label, int subject,
                                  boolean scramInProgress, int action, boolean compactLabel) {
        for (int i = 0; i < presets.length; i++) {
            final int val = presets[i];
            final int packetValue = action == 0 ? val : (subject << 16) | val;
            int x = startX + (i % 3) * (layout.presetW + layout.presetGap);
            int y = startY + (i / 3) * (layout.presetH + layout.presetRowGap);
            String text = makePresetText(label, val, compactLabel, layout.presetW);
            Button b = makePresetButton(text, x, y, layout.presetW, layout.presetH, action, packetValue, scramInProgress && val < 100);
            addRenderableWidget(b);
        }
    }

    private String makePresetText(String label, int val, boolean compact, int width) {
        if ("All".equals(label)) {
            return val + "%";
        }
        String full = label + " " + val + "%";
        if (compact && width < 58) {
            return label.substring(0, 1) + " " + val + "%";
        }
        return full;
    }

    private Button makePresetButton(String text, int x, int y, int w, int h, int action, int value, boolean blocked) {
        Button b = Button.builder(Component.literal(text), btn -> sendControlPacket(action, value))
                .pos(x, y).size(w, h).build();
        if (blocked) {
            b.active = false;
            b.setTooltip(Tooltip.create(Component.literal("Blocked while SCRAM is in progress.")));
        } else {
            b.setTooltip(Tooltip.create(Component.literal("Set target insertion to " + (value & 0xFFFF) + "%.")));
        }
        return b;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseInMap((int) mouseX, (int) mouseY)) {
            int relX = ((int) mouseX - layout.mapX) / layout.mapCell;
            int relZ = ((int) mouseY - layout.mapY) / layout.mapCell;
            handleGridClick(relX, relZ);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseInMap(int mouseX, int mouseY) {
        return mouseX >= layout.mapX && mouseX < layout.mapX + layout.mapSize
                && mouseY >= layout.mapY && mouseY < layout.mapY + layout.mapSize;
    }

    private void handleGridClick(int relX, int relZ) {
        int[] mapPos = toMapPos(relX, relZ);
        byte[][] map = menu.getBlockEntity().getReactorMap();
        if (map[mapPos[0]][mapPos[1]] == CoreReactorConstants.MAP_CONTROL) {
            selectedRodIndex = findRodIndex(mapPos[0], mapPos[1]);
            if (selectedRodIndex != -1) {
                isRodSettingsOpen = true;
                this.init(this.minecraft, this.width, this.height);
            }
        }
    }

    private int[] toMapPos(int relX, int relZ) {
        ReactorConsoleBlockEntity be = menu.getBlockEntity();
        Direction facing = be.getBlockState().getValue(ReactorConsoleBlock.FACING);
        int last = MAP - 1;
        return switch (facing) {
            case NORTH -> new int[] {last - relX, last - relZ};
            case SOUTH -> new int[] {relX, relZ};
            case WEST -> new int[] {last - relZ, relX};
            case EAST -> new int[] {relZ, last - relX};
            default -> new int[] {relX, relZ};
        };
    }

    private int findRodIndex(int relX, int relZ) {
        ReactorConsoleBlockEntity be = menu.getBlockEntity();
        BlockPos consolePos = be.getBlockPos();
        int worldX = consolePos.getX() + relX - CoreReactorConstants.MAP_OFFSET;
        int worldZ = consolePos.getZ() + relZ - CoreReactorConstants.MAP_OFFSET;

        for (int i = 0; i < be.getControlChannelCount(); i++) {
            BlockPos rodPos = be.getControlChannelPos(i);
            if (rodPos != null && rodPos.getX() == worldX && rodPos.getZ() == worldZ) {
                return i;
            }
        }
        return -1;
    }

    private void sendControlPacket(int action, int value) {
        CorePackets.CHANNEL.sendToServer(new ReactorControlPacket(menu.getBlockEntity().getBlockPos(), action, value));
    }

    private String expButtonLabel(boolean authorized) {
        if (!authorized) {
            return "EXP LOCKED";
        }
        return "EXP: " + (menu.getData().get(11) > 0 ? "ON" : "OFF");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (expToggleButton != null) {
            boolean authorized = menu.getData().get(14) > 0;
            expToggleButton.active = authorized;
            expToggleButton.setMessage(Component.literal(expButtonLabel(authorized)));
        }
        boolean scramInProgress = menu.getData().get(15) > 0;
        if (allSlider != null) {
            allSlider.setBlocked(scramInProgress);
            allSlider.syncFromServer(menu.getData().get(0));
        }
        ReactorConsoleBlockEntity be = menu.getBlockEntity();
        if (rodSlider != null && selectedRodIndex >= 0) {
            rodSlider.setBlocked(scramInProgress);
            rodSlider.syncFromServer(be.getChannelTargetInsertion(selectedRodIndex));
        }
        if (groupSlider != null && selectedRodIndex >= 0) {
            groupSlider.setBlocked(scramInProgress);
            groupSlider.syncFromServer(be.getChannelTargetInsertion(selectedRodIndex));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(layout.panelX, layout.panelY, layout.panelX + layout.panelW, layout.panelY + layout.panelH, 0xFF1A1A1A);
        graphics.renderOutline(layout.panelX, layout.panelY, layout.panelW, layout.panelH, 0xFF555555);

        renderMnemoscheme(graphics);
        renderLegend(graphics);
        renderStats(graphics);
        renderSidebarLabels(graphics);
    }

    private void renderSidebarLabels(GuiGraphics graphics) {
        int scram = menu.getData().get(15);
        renderSectionTitle(graphics, "Emergency", layout.rightX, layout.emergencyTitleY, layout.rightW);
        if (scram > 0) {
            graphics.drawString(this.font, "SCRAM IN PROGRESS", layout.rightX, layout.emergencyButtonY + 26, 0xFFFF5555, false);
        }

        renderSectionTitle(graphics, "All rods", layout.rightX, layout.allTitleY, layout.rightW);

        int auth = menu.getData().get(14);
        graphics.drawString(this.font, auth > 0 ? "AUTH: OK" : "AUTH: KEY", layout.rightX, layout.authY,
                auth > 0 ? 0xFF77CC77 : 0xFFFFAA44, false);

        if (isRodSettingsOpen && selectedRodIndex != -1) {
            renderSelectedRodPanel(graphics, layout.rightX, layout.selectedTitleY);
        } else {
            graphics.drawString(this.font, "Click a control rod", layout.rightX, layout.selectedTitleY + 14, 0xFFAAAAAA, false);
            graphics.drawString(this.font, "to edit rod/group.", layout.rightX, layout.selectedTitleY + 25, 0xFFAAAAAA, false);
        }
    }

    private void renderSectionTitle(GuiGraphics graphics, String title, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 11, 0xFF252525);
        graphics.drawString(this.font, title, x + 4, y + 2, 0xFFCCCCCC, false);
    }

    private void renderSelectedRodPanel(GuiGraphics graphics, int x, int y) {
        ReactorConsoleBlockEntity be = menu.getBlockEntity();
        int group = be.getChannelColor(selectedRodIndex);
        int current = be.getChannelInsertion(selectedRodIndex);
        int target = be.getChannelTargetInsertion(selectedRodIndex);
        boolean moving = be.isChannelMoving(selectedRodIndex);

        renderSectionTitle(graphics, "Selected rod", x, y, layout.rightW);
        graphics.drawString(this.font, "Rod #" + (selectedRodIndex + 1), x, y + 14, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "Group: " + GROUP_NAMES[group], x, y + 25, GROUP_COLORS[group], false);
        graphics.drawString(this.font, "Insert: " + current + "% -> " + target + "%", x, y + 36, 0xFFCCCCCC, false);
        graphics.drawString(this.font, moving ? "Drive: MOVING" : "Drive: STABLE", x, y + 47, moving ? 0xFFFFAA44 : 0xFF77CC77, false);

        renderSectionTitle(graphics, "Group control", x, layout.groupSectionTitleY, layout.rightW);
        graphics.drawString(this.font, "Target group: " + GROUP_CODES[group], x, layout.groupSectionTitleY + 14, GROUP_COLORS[group], false);
    }

    private void renderMnemoscheme(GuiGraphics graphics) {
        ReactorConsoleBlockEntity be = menu.getBlockEntity();
        byte[][] map = be.getReactorMap();

        for (int i = 0; i < MAP; i++) {
            for (int j = 0; j < MAP; j++) {
                int[] mapPos = toMapPos(i, j);
                int mapX = mapPos[0];
                int mapZ = mapPos[1];

                int bx = layout.mapX + i * layout.mapCell;
                int by = layout.mapY + j * layout.mapCell;
                int color = blockColor(map[mapX][mapZ]);
                graphics.fill(bx, by, bx + layout.mapCell - 1, by + layout.mapCell - 1, color);

                if (map[mapX][mapZ] == CoreReactorConstants.MAP_CONTROL) {
                    int rodIndex = findRodIndex(mapX, mapZ);
                    if (rodIndex != -1) {
                        int groupColorId = be.getChannelColor(rodIndex);
                        int renderColor = GROUP_COLORS[groupColorId % GROUP_COLORS.length];
                        boolean sameGroup = isRodSettingsOpen && selectedRodIndex != -1
                                && be.getChannelColor(selectedRodIndex) == groupColorId;
                        graphics.renderOutline(bx, by, layout.mapCell - 1, layout.mapCell - 1, sameGroup ? renderColor : 0xFF777777);

                        int insertion = be.getChannelInsertion(rodIndex);
                        if (insertion > 0) {
                            int fillSize = (insertion * Math.max(1, layout.mapCell - 3)) / 100;
                            graphics.fill(bx + 1, by + 1, bx + 1 + fillSize, by + layout.mapCell - 2, renderColor & 0x88FFFFFF);
                        }

                        if (rodIndex == selectedRodIndex && isRodSettingsOpen) {
                            graphics.renderOutline(bx - 1, by - 1, layout.mapCell + 1, layout.mapCell + 1, 0xFFFFFFFF);
                        }
                    }
                }
            }
        }
    }

    private void renderLegend(GuiGraphics graphics) {
        int cursor = layout.legendX;
        cursor = renderLegendItem(graphics, cursor, layout.legendY, blockColor(CoreReactorConstants.MAP_FUEL), "Fuel");
        cursor = renderLegendItem(graphics, cursor, layout.legendY, blockColor(CoreReactorConstants.MAP_CONTROL), "Control");
        cursor = renderLegendItem(graphics, cursor, layout.legendY, blockColor(CoreReactorConstants.MAP_STEAM), "Steam");
        cursor = renderLegendItem(graphics, cursor, layout.legendY, blockColor(CoreReactorConstants.MAP_GRAPHITE), "Graphite");
        renderLegendItem(graphics, cursor, layout.legendY, blockColor(CoreReactorConstants.MAP_REFLECTOR), "Reflector");
    }

    private int renderLegendItem(GuiGraphics graphics, int x, int y, int color, String label) {
        graphics.fill(x, y + 3, x + 8, y + 11, color);
        graphics.renderOutline(x, y + 3, 8, 8, 0xFF555555);
        graphics.drawString(this.font, label, x + 11, y + 3, 0xFFBBBBBB, false);
        return x + 11 + this.font.width(label) + 10;
    }

    private int blockColor(byte type) {
        return switch (type) {
            case CoreReactorConstants.MAP_GRAPHITE -> 0xFF444444;
            case CoreReactorConstants.MAP_FUEL -> 0xFF008800;
            case CoreReactorConstants.MAP_CONTROL -> 0xFF666666;
            case CoreReactorConstants.MAP_STEAM -> 0xFF3366AA;
            case CoreReactorConstants.MAP_REFLECTOR -> 0xFF8888CC;
            default -> 0xFF111111;
        };
    }

    private String blockName(byte type) {
        return switch (type) {
            case CoreReactorConstants.MAP_GRAPHITE -> "Graphite moderator";
            case CoreReactorConstants.MAP_FUEL -> "Fuel channel";
            case CoreReactorConstants.MAP_CONTROL -> "Control rod";
            case CoreReactorConstants.MAP_STEAM -> "Steam channel";
            case CoreReactorConstants.MAP_REFLECTOR -> "Neutron reflector";
            default -> "Empty / unknown";
        };
    }

    private void renderStats(GuiGraphics graphics) {
        int temp = menu.getData().get(1);
        float power = menu.getData().get(2) / 100f;
        int fuel = menu.getData().get(3);
        int stateIdx = menu.getData().get(4);
        int water = menu.getData().get(8);
        int steam = menu.getData().get(9);
        int controlCount = menu.getData().get(7);
        int avgInsertion = menu.getData().get(6);
        int moving = menu.getData().get(16);
        int auth = menu.getData().get(14);
        int mode = menu.getData().get(11);
        ReactorState state = ReactorState.values()[stateIdx];

        int x = layout.statsX;
        int y = layout.statsY;
        int lineH = 12;
        int col2 = x + layout.statsCol2Offset;

        graphics.drawString(this.font, "Power: " + String.format("%.2f", power), x, y, 0xFFFFFF, false);
        graphics.drawString(this.font, "Temp: " + temp + " C", col2, y, temp >= 1000 ? 0xFFFF5555 : 0xFFFFFF, false);
        graphics.drawString(this.font, "Fuel: " + fuel, x, y + lineH, 0xFFFFFF, false);
        graphics.drawString(this.font, "State: " + state.name(), col2, y + lineH, 0xFFFFFF, false);

        String controlLine = controlCount > 0 ? "Rods: " + controlCount + " | avg " + avgInsertion + "%" : "Rods: none";
        graphics.drawString(this.font, controlLine, x, y + lineH * 2, controlCount > 0 ? 0xFFCCCCCC : 0xFFFF6666, false);
        graphics.drawString(this.font, "Drive: " + (moving > 0 ? "MOVING" : "STABLE"), col2, y + lineH * 2,
                moving > 0 ? 0xFFFFAA44 : 0xFF77CC77, false);

        int meterY = y + lineH * 3 + 4;
        drawMeter(graphics, "Water", water, CoreReactorConstants.TANK_CAPACITY, x, meterY, layout.statsBarW, 7, 0xFF6699FF, 0xFF3366CC);
        drawMeter(graphics, "Steam", steam, CoreReactorConstants.TANK_CAPACITY, col2, meterY, layout.statsBarW, 7, 0xFFBBBBBB, 0xFF999999);

        int xenon = menu.getData().get(10);
        int expY = meterY + 29;
        drawPercentMeter(graphics, "Xenon", xenon, x, expY, layout.statsBarW, 7, xenonColor(xenon));
        graphics.drawString(this.font, "Experiment: " + (auth > 0 ? (mode > 0 ? "ON" : "OFF") : "LOCKED"),
                col2, expY, mode > 0 ? 0xFFFF5555 : auth > 0 ? 0xFF77CC77 : 0xFFFFAA44, false);
        graphics.drawString(this.font, "Void: " + menu.getData().get(12) + "%", col2, expY + lineH, 0xFFCCEEFF, false);

        boolean showRisk = auth > 0 || mode > 0 || temp >= 800;
        if (showRisk && avgInsertion < 80) {
            int riskColor = avgInsertion < 15 ? 0xFFFF4444 : 0xFFFFAA44;
            String risk = avgInsertion < 15
                    ? "AZ-5 transient risk: HIGH (low insertion)"
                    : "AZ-5 transient risk: MEDIUM";
            graphics.drawString(this.font, risk, x, layout.panelY + layout.panelH - 20, riskColor, false);
        }
    }

    private void drawMeter(GuiGraphics graphics, String label, int value, int max, int x, int y, int w, int h, int textColor, int fillColor) {
        graphics.drawString(this.font, label + ": " + value + "/" + max, x, y, textColor, false);
        int barY = y + 11;
        graphics.fill(x, barY, x + w, barY + h, 0xFF0A0A0A);
        int fill = (int) (w * Math.max(0.0D, Math.min(1.0D, value / (double) max)));
        if (fill > 0) {
            graphics.fill(x, barY, x + fill, barY + h, fillColor);
        }
    }

    private void drawPercentMeter(GuiGraphics graphics, String label, int value, int x, int y, int w, int h, int fillColor) {
        graphics.drawString(this.font, label + ": " + value + "%", x, y, 0xFFCCEEFF, false);
        int barY = y + 11;
        graphics.fill(x, barY, x + w, barY + h, 0xFF0A0A0A);
        int fill = (int) (w * Math.max(0.0D, Math.min(1.0D, value / 100.0D)));
        if (fill > 0) {
            graphics.fill(x, barY, x + fill, barY + h, fillColor);
        }
    }

    private int xenonColor(int xenon) {
        return xenon <= 25 ? 0xFF44CC44 : xenon <= 50 ? 0xFFFFFF66 : xenon <= 75 ? 0xFFFFAA33 : 0xFFFF4444;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderGridTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderGridTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isMouseInMap(mouseX, mouseY)) {
            return;
        }

        int relX = (mouseX - layout.mapX) / layout.mapCell;
        int relZ = (mouseY - layout.mapY) / layout.mapCell;
        int[] mapPos = toMapPos(relX, relZ);
        ReactorConsoleBlockEntity be = menu.getBlockEntity();
        byte type = be.getReactorMap()[mapPos[0]][mapPos[1]];

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(blockName(type)));
        lines.add(Component.literal("Grid: " + (mapPos[0] - CoreReactorConstants.MAP_OFFSET)
                + ", " + (mapPos[1] - CoreReactorConstants.MAP_OFFSET)));

        if (type == CoreReactorConstants.MAP_CONTROL) {
            int rodIndex = findRodIndex(mapPos[0], mapPos[1]);
            if (rodIndex != -1) {
                int group = be.getChannelColor(rodIndex);
                lines.add(Component.literal("Rod #" + (rodIndex + 1) + " | Group " + GROUP_CODES[group]));
                lines.add(Component.literal("Insertion: " + be.getChannelInsertion(rodIndex)
                        + "% -> " + be.getChannelTargetInsertion(rodIndex) + "%"));
                lines.add(Component.literal(be.isChannelMoving(rodIndex) ? "Drive: MOVING" : "Drive: STABLE"));
            }
        }

        graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private static int clampWithFallback(int value, int min, int max) {
        if (max <= 0) {
            return min;
        }
        if (max < min) {
            return Math.max(200, max);
        }
        return Math.max(min, Math.min(max, value));
    }

    private static final class Layout {
        final int panelX;
        final int panelY;
        final int panelW;
        final int panelH;
        final int rightX;
        final int rightW;
        final int mapX;
        final int mapY;
        final int mapCell;
        final int mapSize;
        final int legendX;
        final int legendY;
        final int statsX;
        final int statsY;
        final int statsCol2Offset;
        final int statsBarW;
        final int emergencyTitleY;
        final int emergencyButtonY;
        final int allTitleY;
        final int allGridY;
        final int allSliderY;
        final int expButtonY;
        final int authY;
        final int selectedTitleY;
        final int groupAssignY;
        final int rodGridY;
        final int rodSliderY;
        final int groupSectionTitleY;
        final int groupGridY;
        final int groupSliderY;
        final int closeButtonY;
        final int presetW;
        final int presetH;
        final int presetGap;
        final int presetRowGap;
        final int groupBtnW;
        final int groupBtnGap;

        Layout(int screenW, int screenH, int panelW, int panelH) {
            int pad = 12;
            int gap = 12;
            this.panelW = panelW;
            this.panelH = panelH;
            this.panelX = (screenW - panelW) / 2;
            this.panelY = (screenH - panelH) / 2;

            int innerW = panelW - pad * 2;
            int rightWCalc = (int) (innerW * 0.38f);
            this.rightW = Math.max(290, rightWCalc);
            int leftW = innerW - rightW - gap;
            this.rightX = panelX + pad + leftW + gap;

            int statsH = Math.max(150, Math.min(230, (int) (panelH * 0.30f)));
            int topH = panelH - pad * 2 - statsH - gap;
            int legendH = 18;
            int mapAreaH = topH - legendH - 6;
            int mapCellCalc = Math.max(6, Math.min(12, Math.min(leftW / MAP, mapAreaH / MAP)));
            this.mapCell = mapCellCalc;
            this.mapSize = mapCell * MAP;
            int leftX = panelX + pad;
            this.mapX = leftX + (leftW - mapSize) / 2;
            this.mapY = panelY + pad + Math.max(0, (mapAreaH - mapSize) / 2);
            this.legendX = leftX + 2;
            this.legendY = panelY + pad + mapAreaH + 2;
            this.statsX = leftX;
            this.statsY = panelY + panelH - pad - statsH;
            this.statsCol2Offset = Math.max(220, leftW / 2);
            this.statsBarW = Math.max(120, Math.min(240, leftW / 2 - 14));

            this.presetH = 20;
            this.presetGap = 6;
            this.presetRowGap = 6;
            this.presetW = Math.max(54, (rightW - presetGap * 2) / 3);
            this.groupBtnGap = 6;
            this.groupBtnW = Math.max(22, (rightW - groupBtnGap * 4) / 5);

            this.emergencyTitleY = panelY + pad;
            this.emergencyButtonY = emergencyTitleY + 14;
            this.allTitleY = emergencyButtonY + 36 + 10;
            this.allGridY = allTitleY + 15;
            this.allSliderY = allGridY + presetH * 2 + presetRowGap + 4;
            this.expButtonY = allSliderY + 22 + 8;
            this.authY = expButtonY + 24;
            this.selectedTitleY = authY + 12;
            this.groupAssignY = selectedTitleY + 62;
            this.rodGridY = groupAssignY + 24;
            this.rodSliderY = rodGridY + presetH * 2 + presetRowGap + 4;
            this.groupSectionTitleY = rodSliderY + 24;
            this.groupGridY = groupSectionTitleY + 25;
            this.groupSliderY = groupGridY + presetH * 2 + presetRowGap + 4;
            this.closeButtonY = groupSliderY + 24 + 8;
        }
    }

    private final class InsertionSlider extends AbstractSliderButton {
        private final String label;
        private boolean blocked;
        private boolean dragging;
        private int pendingValue;

        private InsertionSlider(int x, int y, int width, int height, String label, int initialValue, boolean blocked) {
            super(x, y, width, height, Component.empty(), Mth.clamp(initialValue, 0, 100) / 100.0D);
            this.label = label;
            this.blocked = blocked;
            this.pendingValue = Mth.clamp(initialValue, 0, 100);
            setBlocked(blocked);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(label + ": " + pendingValue + "%"));
        }

        @Override
        protected void applyValue() {
            pendingValue = Mth.clamp((int) Math.round(this.value * 100.0D), 0, 100);
            updateMessage();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            super.onClick(mouseX, mouseY);
            dragging = true;
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            super.onDrag(mouseX, mouseY, dragX, dragY);
            dragging = true;
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            dragging = false;
            if (blocked && pendingValue < 100) {
                return;
            }
            if (this == allSlider) {
                sendControlPacket(0, pendingValue);
            } else if (this == rodSlider && selectedRodIndex >= 0) {
                sendControlPacket(2, (selectedRodIndex << 16) | pendingValue);
            } else if (this == groupSlider && selectedRodIndex >= 0) {
                int group = menu.getBlockEntity().getChannelColor(selectedRodIndex);
                sendControlPacket(4, (group << 16) | pendingValue);
            }
        }

        private void syncFromServer(int serverValue) {
            if (dragging) {
                return;
            }
            pendingValue = Mth.clamp(serverValue, 0, 100);
            this.value = pendingValue / 100.0D;
            updateMessage();
        }

        private void setBlocked(boolean blocked) {
            this.blocked = blocked;
            this.active = !blocked;
            if (blocked) {
                this.setTooltip(Tooltip.create(Component.literal("Blocked while SCRAM is in progress.")));
            } else {
                this.setTooltip(Tooltip.create(Component.literal("Drag for precise insertion control.")));
            }
        }
    }
}
