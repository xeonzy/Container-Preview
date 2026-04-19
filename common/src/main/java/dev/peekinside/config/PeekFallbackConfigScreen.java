package dev.peekinside.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.peekinside.PeekKeys;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@SuppressWarnings({"null", "unused"})
public final class PeekFallbackConfigScreen extends Screen {
    private static final int PANEL_MARGIN = 24;
    private static final int HEADER_HEIGHT = 60;
    private static final int ROW_HEIGHT = 24;
    private static final int FIELD_HEIGHT = 20;
    private static final int SCROLL_SPEED = 14;

    private final Screen parent;
    private final Draft draft;
    private Page page = Page.GENERAL;
    private int panelHeight = 0;
    
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<Button> footerButtons = new ArrayList<>();
    private final Map<Page, List<WidgetEntry>> pageWidgets = new HashMap<>();
    private double scrollY = 0;

    public static Screen create(Screen parent) {
        return new PeekFallbackConfigScreen(parent);
    }

    private PeekFallbackConfigScreen(Screen parent) {
        super(Component.translatable("config.peekinside.title"));
        this.parent = parent;
        this.draft = Draft.from(PeekConfig.INSTANCE);
        for (Page p : Page.values()) pageWidgets.put(p, new ArrayList<>());
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.tabButtons.clear();
        this.footerButtons.clear();
        for (Page p : Page.values()) pageWidgets.get(p).clear();

        this.createTabs();
        this.createFooter();
        
        this.createControlsPage();
        this.createGeneralPage();
        this.createLayoutPage();
        this.createAdvancedPage();
        
        this.updatePageState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF141414, 0xFF080808);
        
        int panelWidth = Math.min(this.width - PANEL_MARGIN * 2, 450);
        int panelHeight = this.getPanelHeight();
        this.panelHeight = panelHeight;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = PANEL_MARGIN;
        int footerY = panelY + panelHeight + 5;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD000000);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0x44FFFFFF);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 12, 0xFFFFFFFF);
        graphics.fill(panelX + 40, panelY + 28, panelX + panelWidth - 40, panelY + 29, 0x22FFFFFF);

        for (Button b : tabButtons) b.render(graphics, mouseX, mouseY, delta);
        this.layoutFooter(footerY);
        for (Button b : footerButtons) b.render(graphics, mouseX, mouseY, delta);

        int clipTop = panelY + HEADER_HEIGHT;
        int clipBottom = panelY + panelHeight - 15;
        
        graphics.enableScissor(panelX, clipTop, panelX + panelWidth, clipBottom);
        
        this.drawFieldLabels(graphics, panelX, clipTop - (int)this.scrollY);
        for (WidgetEntry entry : pageWidgets.get(this.page)) {
            entry.widget.render(graphics, mouseX, mouseY, delta);
        }
        
        graphics.disableScissor();

        int contentHeight = this.getContentHeight();
        int viewHeight = clipBottom - clipTop;
        if (contentHeight > viewHeight) {
            int scrollbarX = panelX + panelWidth - 6;
            graphics.fill(scrollbarX, clipTop, scrollbarX + 2, clipBottom, 0x22FFFFFF);
            
            double progress = this.scrollY / (contentHeight - viewHeight);
            int knobHeight = Math.max(10, (int) (viewHeight * ((double)viewHeight / contentHeight)));
            int knobY = clipTop + (int) (progress * (viewHeight - knobHeight));
            graphics.fill(scrollbarX, knobY, scrollbarX + 2, knobY + knobHeight, 0xAAFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollY -= scrollY * SCROLL_SPEED;
        this.clampScroll();
        this.updateWidgetPositions();
        return true;
    }

    private void clampScroll() {
        this.scrollY = Math.max(0, Math.min(this.scrollY, Math.max(0, this.getContentHeight() - this.getViewHeight())));
    }

    private int getViewHeight() {
        return Math.max(0, this.getPanelHeight() - HEADER_HEIGHT - 15);
    }

    private int getContentHeight() {
        int maxY = 0;
        for (WidgetEntry entry : pageWidgets.get(this.page)) {
            maxY = Math.max(maxY, entry.baseY + entry.widget.getHeight());
        }
        return Math.max(0, maxY - (PANEL_MARGIN + HEADER_HEIGHT) + 10);
    }

    private int getPanelHeight() {
        int maxPanelHeight = this.height - PANEL_MARGIN * 2 - 20;
        int desiredHeight = HEADER_HEIGHT + this.getContentHeight() + 34;
        return clamp(Math.max(260, desiredHeight), 0, Math.max(0, maxPanelHeight));
    }

    @Override
    public void onClose() {
        PeekConfig.reload();
        PeekKeys.applyFromConfig();
        KeyMapping.resetMapping();
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void createTabs() {
        int tabWidth = 90;
        int totalTabsWidth = (tabWidth + 4) * Page.values().length - 4;
        int startX = (this.width - totalTabsWidth) / 2;
        int y = PANEL_MARGIN + 35;

        for (Page p : Page.values()) {
            Button b = Button.builder(Component.translatable("config.peekinside.category." + p.key), button -> {
                this.page = p;
                this.updatePageState();
            }).bounds(startX, y, tabWidth, 20).build();
            this.tabButtons.add(b);
            this.addRenderableWidget(b);
            startX += tabWidth + 4;
        }
    }

    private void createFooter() {
        int buttonWidth = 100;
        int y = PANEL_MARGIN + HEADER_HEIGHT + this.getContentHeight() + 39;
        footerButtons.add(this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.saveAndClose()).bounds(this.width/2 - buttonWidth - 5, y, buttonWidth, 20).build()));
        footerButtons.add(this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose()).bounds(this.width/2 + 5, y, buttonWidth, 20).build()));
        footerButtons.add(this.addRenderableWidget(Button.builder(Component.translatable("controls.reset"), b -> this.resetToDefaults()).bounds(this.width - PANEL_MARGIN - 55, y, 50, 20).build()));
    }

    private void resetToDefaults() {
        this.draft.reset();
        this.init();
    }

    private int centerX() { return this.width / 2; }

    private void createControlsPage() {
        int y = PANEL_MARGIN + HEADER_HEIGHT + 10;
        int editX = centerX() + 5;
        int editWidth = 150;
        this.createKeyButton(editX, y, editWidth, this.draft.previewKey, key -> {
            String value = KeyNameUtil.displayName(key.getName());
            this.draft.previewKey = value;
            PeekConfig.INSTANCE.previewKey = value;
            PeekKeys.PREVIEW.setKey(key);
            KeyMapping.resetMapping();
        }, "config.peekinside.preview_key", "config.peekinside.preview_key.capture", Page.CONTROLS);
        this.createKeyButton(editX, y + ROW_HEIGHT, editWidth, this.draft.detailKey, key -> {
            String value = KeyNameUtil.displayName(key.getName());
            this.draft.detailKey = value;
            PeekConfig.INSTANCE.detailKey = value;
            PeekKeys.DETAIL.setKey(key);
            KeyMapping.resetMapping();
        }, "config.peekinside.detail_key", "config.peekinside.detail_key.capture", Page.CONTROLS);
        this.createKeyButton(editX, y + ROW_HEIGHT * 2, editWidth, this.draft.lockKey, key -> {
            String value = KeyNameUtil.displayName(key.getName());
            this.draft.lockKey = value;
            PeekConfig.INSTANCE.lockKey = value;
            PeekKeys.LOCK.setKey(key);
            KeyMapping.resetMapping();
        }, "config.peekinside.lock_key", "config.peekinside.lock_key.capture", Page.CONTROLS);
    }

    private void createGeneralPage() {
        int y = PANEL_MARGIN + HEADER_HEIGHT + 10;
        int btnWidth = 180;
        int leftX = centerX() - btnWidth - 5;
        int rightX = centerX() + 5;

        this.toggleButton(leftX, y + ROW_HEIGHT * 0, btnWidth, "config.peekinside.preview_enabled", "config.peekinside.preview_enabled.desc", () -> this.draft.previewEnabled, v -> this.draft.previewEnabled = v, Page.GENERAL);
        this.toggleButton(rightX, y + ROW_HEIGHT * 0, btnWidth, "config.peekinside.hold_to_preview", "config.peekinside.hold_to_preview.desc", () -> !this.draft.alwaysShow, v -> this.draft.alwaysShow = !v, Page.GENERAL);
        this.toggleButton(rightX, y + ROW_HEIGHT * 1, btnWidth, "config.peekinside.locking_enabled", "config.peekinside.locking_enabled.desc", () -> this.draft.lockingEnabled, v -> this.draft.lockingEnabled = v, Page.GENERAL);
        this.toggleButton(leftX, y + ROW_HEIGHT * 1, btnWidth, "config.peekinside.compression_enabled", "config.peekinside.compression_enabled.desc", () -> this.draft.compressionEnabled, v -> this.draft.compressionEnabled = v, Page.GENERAL);
        this.toggleButton(rightX, y + ROW_HEIGHT * 2, btnWidth, "config.peekinside.detail_preview_enabled", "config.peekinside.detail_preview_enabled.desc", () -> this.draft.detailPreviewEnabled, v -> this.draft.detailPreviewEnabled = v, Page.GENERAL);
        this.toggleButton(leftX, y + ROW_HEIGHT * 2, btnWidth, "config.peekinside.nested_previews_enabled", "config.peekinside.nested_previews_enabled.desc", () -> this.draft.nestedPreviewsEnabled, v -> this.draft.nestedPreviewsEnabled = v, Page.GENERAL);
        this.toggleButton(rightX, y + ROW_HEIGHT * 3, btnWidth, "config.peekinside.show_market_info", "config.peekinside.show_market_info.desc", () -> this.draft.showMarketInfo, v -> this.draft.showMarketInfo = v, Page.GENERAL);
        this.toggleButton(leftX, y + ROW_HEIGHT * 3, btnWidth, "config.peekinside.show_statistics_bar", "config.peekinside.show_statistics_bar.desc", () -> this.draft.showStatisticsBar, v -> this.draft.showStatisticsBar = v, Page.GENERAL);
        this.toggleButton(rightX, y + ROW_HEIGHT * 4, btnWidth, "config.peekinside.show_search_filter", "config.peekinside.show_search_filter.desc", () -> this.draft.showSearchBar, v -> this.draft.showSearchBar = v, Page.GENERAL);
        this.toggleButton(leftX, y + ROW_HEIGHT * 4, btnWidth, "config.peekinside.show_slot_counts", "config.peekinside.show_slot_counts.desc", () -> this.draft.showSlotCounts, v -> this.draft.showSlotCounts = v, Page.GENERAL);
        this.toggleButton(leftX, y + ROW_HEIGHT * 5, btnWidth, "config.peekinside.show_empty_containers", "config.peekinside.show_empty_containers.desc", () -> this.draft.showEmpty, v -> this.draft.showEmpty = v, Page.GENERAL);
        this.toggleButton(rightX, y + ROW_HEIGHT * 5, btnWidth, "config.peekinside.show_empty_label", "config.peekinside.show_empty_label.desc", () -> this.draft.showEmptyLabel, v -> this.draft.showEmptyLabel = v, Page.GENERAL);
        this.toggleButton(leftX, y + ROW_HEIGHT * 6, btnWidth, "config.peekinside.hide_vanilla_lines", "config.peekinside.hide_vanilla_lines.desc", () -> this.draft.hideVanillaLines, v -> this.draft.hideVanillaLines = v, Page.GENERAL);
        this.toggleButton(rightX, y + ROW_HEIGHT * 6, btnWidth, "config.peekinside.tint_background", "config.peekinside.tint_background.desc", () -> this.draft.tintBackground, v -> this.draft.tintBackground = v, Page.GENERAL);
        this.toggleButton(leftX, y + ROW_HEIGHT * 7, btnWidth, "config.peekinside.compact_number_formatting", "config.peekinside.compact_number_formatting.desc", () -> this.draft.compactNumberFormatting, v -> this.draft.compactNumberFormatting = v, Page.GENERAL);
    }

    private void createLayoutPage() {
        int y = PANEL_MARGIN + HEADER_HEIGHT + 10;
        int editX = centerX() + 5;
        int editWidth = 150;
        this.createTextBox(editX, y, editWidth, Integer.toString(this.draft.slotSize), v -> this.draft.slotSize = parseInt(v, this.draft.slotSize), Page.LAYOUT);
        this.createTextBox(editX, y + ROW_HEIGHT, editWidth, Integer.toString(this.draft.padding), v -> this.draft.padding = parseInt(v, this.draft.padding), Page.LAYOUT);
        this.createTextBox(editX, y + ROW_HEIGHT * 2, editWidth, Integer.toString(this.draft.uiScale), v -> this.draft.uiScale = parseInt(v, this.draft.uiScale), Page.LAYOUT);
        this.createTextBox(editX, y + ROW_HEIGHT * 3, editWidth, Integer.toString(this.draft.maxGridHeight), v -> this.draft.maxGridHeight = parseInt(v, this.draft.maxGridHeight), Page.LAYOUT);
    }

    private void createAdvancedPage() {
        int y = PANEL_MARGIN + HEADER_HEIGHT + 10;
        int editX = centerX() + 5;
        int editWidth = 150;
        this.createTextBox(editX, y, editWidth, Integer.toString(this.draft.maxNestedDepth), v -> this.draft.maxNestedDepth = parseInt(v, this.draft.maxNestedDepth), Page.ADVANCED);
        this.createTextBox(editX, y + ROW_HEIGHT, editWidth, this.draft.blacklistText, v -> this.draft.blacklistText = v, Page.ADVANCED);
    }

    private void updatePageState() {
        this.scrollY = 0;
        for (int i = 0; i < Page.values().length; i++) this.tabButtons.get(i).active = Page.values()[i] != this.page;
        this.updateWidgetPositions();
    }

    private void updateWidgetPositions() {
        int clipTop = PANEL_MARGIN + HEADER_HEIGHT;
        int clipBottom = PANEL_MARGIN + this.getPanelHeight() - 15;
        for (Page p : Page.values()) {
            boolean isCurrent = (p == this.page);
            for (WidgetEntry entry : this.pageWidgets.get(p)) {
                int newY = entry.baseY - (int)this.scrollY;
                entry.widget.setY(newY);
                entry.widget.visible = isCurrent && (newY + entry.widget.getHeight() > clipTop && newY < clipBottom);
                entry.widget.active = entry.widget.visible;
            }
        }
    }

    private void layoutFooter(int y) {
        int buttonWidth = 100;
        footerButtons.get(0).setY(y);
        footerButtons.get(0).setX(this.width / 2 - buttonWidth - 5);
        footerButtons.get(1).setY(y);
        footerButtons.get(1).setX(this.width / 2 + 5);
        footerButtons.get(2).setY(y);
        footerButtons.get(2).setX(this.width - PANEL_MARGIN - 55);
    }

    private void drawFieldLabels(GuiGraphics graphics, int x, int y) {
        if (this.page == Page.GENERAL) return;
        int labelX = centerX() - 5;
        
        List<WidgetEntry> entries = pageWidgets.get(this.page);
        if (this.page == Page.CONTROLS && entries.size() >= 3) {
            this.drawRightAligned(graphics, "config.peekinside.preview_key", labelX, entries.get(0).widget);
            this.drawRightAligned(graphics, "config.peekinside.detail_key", labelX, entries.get(1).widget);
            this.drawRightAligned(graphics, "config.peekinside.lock_key", labelX, entries.get(2).widget);
        } else if (this.page == Page.LAYOUT && entries.size() >= 4) {
            this.drawRightAligned(graphics, "config.peekinside.slot_size", labelX, entries.get(0).widget);
            this.drawRightAligned(graphics, "config.peekinside.padding", labelX, entries.get(1).widget);
            this.drawRightAligned(graphics, "config.peekinside.ui_scale", labelX, entries.get(2).widget);
            this.drawRightAligned(graphics, "config.peekinside.max_visible_rows", labelX, entries.get(3).widget);
        } else if (this.page == Page.ADVANCED && entries.size() >= 2) {
            this.drawRightAligned(graphics, "config.peekinside.max_nested_depth", labelX, entries.get(0).widget);
            this.drawRightAligned(graphics, "config.peekinside.blacklist", labelX, entries.get(1).widget);
        }
    }

    private void drawRightAligned(GuiGraphics graphics, String key, int x, AbstractWidget widget) {
        Component text = Component.translatable(key);
        int y = widget.getY() + (widget.getHeight() - 8) / 2;
        graphics.drawString(this.font, text, x - this.font.width(text), y, 0xFFE0E0E0, false);
    }

    private void saveAndClose() {
        this.applyDraft();
        PeekKeys.applyFromConfig();
        PeekConfig.INSTANCE.save();
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        this.onClose();
    }

    private void applyDraft() {
        PeekConfig config = PeekConfig.INSTANCE;
        config.previewKey = this.draft.previewKey; config.detailKey = this.draft.detailKey; config.lockKey = this.draft.lockKey;
        config.previewEnabled = this.draft.previewEnabled; config.altLockEnabled = this.draft.lockingEnabled;
        config.compressionEnabled = this.draft.compressionEnabled; config.detailPreviewEnabled = this.draft.detailPreviewEnabled;
        config.nestedPreviewEnabled = this.draft.nestedPreviewsEnabled; config.showSellerMetadata = this.draft.showMarketInfo;
        config.slotSize = clamp(this.draft.slotSize, 8, 512); config.padding = clamp(this.draft.padding, 0, 512);
        config.uiScale = clamp(this.draft.uiScale, 10, 1000); config.maxVisibleRows = clamp(this.draft.maxGridHeight, 1, 128);
        config.showSummaryBar = this.draft.showStatisticsBar; config.showSearchBar = this.draft.showSearchBar;
        config.showSlotItemCounts = this.draft.showSlotCounts; config.compactNumberFormatting = this.draft.compactNumberFormatting;
        config.alwaysShow = this.draft.alwaysShow;
        config.showEmpty = this.draft.showEmpty; config.showEmptyContainerMessage = this.draft.showEmptyLabel;
        config.hideVanillaContainerLines = this.draft.hideVanillaLines; config.tintContainerBackgrounds = this.draft.tintBackground;
        config.maxNestedDepth = clamp(this.draft.maxNestedDepth, 0, 128); config.blacklist = parseBlacklist(this.draft.blacklistText);
    }

    private void createKeyButton(int x, int y, int width, String currentValue, Consumer<InputConstants.Key> onSelect, String nameKey, String promptKey, Page p) {
        Button button = Button.builder(Component.literal(KeyNameUtil.displayName(currentValue)), btn -> KeyCaptureScreen.open(
            this,
            Component.translatable(nameKey),
            Component.translatable(promptKey),
            Component.literal(KeyNameUtil.displayName(currentValue)),
            key -> {
                onSelect.accept(key);
                btn.setMessage(Component.literal(KeyNameUtil.displayName(key.getName())));
            }
        )).bounds(x, y, width, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable(promptKey)));
        this.pageWidgets.get(p).add(new WidgetEntry(button, y));
        this.addRenderableWidget(button);
    }

    private void createTextBox(int x, int y, int width, String initialValue, Consumer<String> onChange, Page p) {
        EditBox editBox = new EditBox(this.font, x, y, width, FIELD_HEIGHT, Component.empty());
        editBox.setValue(initialValue == null ? "" : initialValue);
        editBox.setResponder(value -> onChange.accept(value == null ? "" : value));
        editBox.setMaxLength(256);
        this.pageWidgets.get(p).add(new WidgetEntry(editBox, y));
        this.addRenderableWidget(editBox);
    }

    private void toggleButton(int x, int y, int w, String k, String d, Supplier<Boolean> g, Consumer<Boolean> s, Page p) {
        Button b = Button.builder(toggleLabel(k, g.get()), btn -> {
            boolean n = !g.get(); s.accept(n); btn.setMessage(toggleLabel(k, n));
        }).bounds(x, y, w, 20).build();
        b.setTooltip(Tooltip.create(Component.translatable(d)));
        this.pageWidgets.get(p).add(new WidgetEntry(b, y)); this.addRenderableWidget(b);
    }

    private static Component toggleLabel(String k, boolean v) {
        return Component.translatable(k).append(": ").append(Component.translatable(v ? "options.on" : "options.off").withStyle(v?ChatFormatting.GREEN:ChatFormatting.RED));
    }

    private static List<String> parseBlacklist(String s) {
        List<String> l = new ArrayList<>(); if (s==null||s.isBlank()) return l;
        for (String p : s.split(",")) { String t = p.trim(); if (!t.isEmpty()) l.add(t); } return l;
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static int parseInt(String v, int f) { try { return Integer.parseInt(v.trim()); } catch (Exception e) { return f; } }

    private enum Page {
        GENERAL("general"), CONTROLS("controls"), LAYOUT("layout"), ADVANCED("advanced");
        private final String key; Page(String key) { this.key = key; }
    }

    private static record WidgetEntry(AbstractWidget widget, int baseY) {}

    private static final class Draft {
        String previewKey, detailKey, lockKey;
        boolean previewEnabled, lockingEnabled, compressionEnabled, detailPreviewEnabled, nestedPreviewsEnabled, showMarketInfo;
        int slotSize, padding, uiScale, maxGridHeight;
        boolean showStatisticsBar, showSearchBar, showSlotCounts, compactNumberFormatting, alwaysShow, showEmpty, showEmptyLabel, hideVanillaLines, tintBackground;
        int maxNestedDepth; String blacklistText;

        static Draft from(PeekConfig c) {
            Draft d = new Draft();
            d.previewKey = c.previewKey; d.detailKey = c.detailKey; d.lockKey = c.lockKey;
            d.previewEnabled = c.previewEnabled; d.lockingEnabled = c.altLockEnabled;
            d.compressionEnabled = c.compressionEnabled; d.detailPreviewEnabled = c.detailPreviewEnabled;
            d.nestedPreviewsEnabled = c.nestedPreviewEnabled; d.showMarketInfo = c.showSellerMetadata;
            d.slotSize = c.slotSize; d.padding = c.padding; d.uiScale = c.uiScale; d.maxGridHeight = c.maxVisibleRows;
            d.showStatisticsBar = c.showSummaryBar; d.showSearchBar = c.showSearchBar; d.showSlotCounts = c.showSlotItemCounts;
            d.compactNumberFormatting = c.compactNumberFormatting;
            d.alwaysShow = c.alwaysShow; d.showEmpty = c.showEmpty; d.showEmptyLabel = c.showEmptyContainerMessage;
            d.hideVanillaLines = c.hideVanillaContainerLines; d.tintBackground = c.tintContainerBackgrounds;
            d.maxNestedDepth = c.maxNestedDepth; d.blacklistText = String.join(", ", c.blacklist);
            return d;
        }

        void reset() {
            PeekConfig c = new PeekConfig();
            this.previewKey = c.previewKey; this.detailKey = c.detailKey; this.lockKey = c.lockKey;
            this.previewEnabled = c.previewEnabled; this.lockingEnabled = c.altLockEnabled;
            this.compressionEnabled = c.compressionEnabled; this.detailPreviewEnabled = c.detailPreviewEnabled;
            this.nestedPreviewsEnabled = c.nestedPreviewEnabled; this.showMarketInfo = c.showSellerMetadata;
            this.slotSize = c.slotSize; this.padding = c.padding; this.uiScale = c.uiScale;
            this.maxGridHeight = c.maxVisibleRows; this.showStatisticsBar = c.showSummaryBar;
            this.showSearchBar = c.showSearchBar; this.showSlotCounts = c.showSlotItemCounts;
            this.compactNumberFormatting = c.compactNumberFormatting;
            this.alwaysShow = c.alwaysShow; this.showEmpty = c.showEmpty;
            this.showEmptyLabel = c.showEmptyContainerMessage; this.hideVanillaLines = c.hideVanillaContainerLines;
            this.tintBackground = c.tintContainerBackgrounds; this.maxNestedDepth = c.maxNestedDepth;
            this.blacklistText = String.join(", ", c.blacklist);
        }
    }
}
