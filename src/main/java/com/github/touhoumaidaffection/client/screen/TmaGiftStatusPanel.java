package com.github.touhoumaidaffection.client.screen;

import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusSelection;
import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusViewRules;
import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusWire;
import com.github.touhoumaidaffection.client.TmaGiftStatusClientState;
import com.github.touhoumaidaffection.client.screen.component.BondDropdown;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A subordinate maid selector and fixed-template metrics inside the Random Gift feature. */
final class TmaGiftStatusPanel {
    private static final int TOOLTIP_ITEMS = 6;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private final TmaGiftStatusClientState state = new TmaGiftStatusClientState();
    private BondDropdown<Option> dropdown;
    private List<Option> options = List.of();
    private List<Component> rows = List.of();
    private List<Component> itemDetails = List.of();
    private int itemOffset;
    private List<Component> itemTooltip = List.of();
    private TmaGiftStatusSelection.Entry projectedEntry;
    private Component queue;
    private Component contents;
    private Component last;
    private Component condition;
    private String nextDate;
    private int selectedIndex;
    private int renderedRevision = -1;
    private long renderedSecond = -1;
    private int left;
    private int fieldLeft;
    private int selectorLeft;
    private int right;
    private int rowTop;
    private int viewportTop;
    private int viewportBottom;
    private int dropdownWidth;

    TmaGiftStatusClientState state() { return state; }
    void refresh() { collapse(); state.refresh(); }
    void collapse() { if (dropdown != null) dropdown.collapse(); }
    boolean expanded() { return dropdown != null && dropdown.isExpanded(); }

    void layout(int left, int right, int rowTop, int top, int bottom, int screenHeight) {
        this.left = left;
        fieldLeft = left + TmaStatusLayout.FIELD_INDENT;
        selectorLeft = left + TmaStatusLayout.SUBSECTION_INDENT;
        this.right = right;
        this.rowTop = rowTop;
        viewportTop = top;
        viewportBottom = bottom;
        int width = right - selectorLeft;
        int selectorTop = rowTop + TmaStatusLayout.SELECTOR_TOP;
        if (dropdown == null || dropdownWidth != width) {
            dropdownWidth = width;
            dropdown = new BondDropdown<>(selectorLeft, selectorTop, width,
                    TmaStatusLayout.SELECTOR_HEIGHT, TmaStatusLayout.SELECTOR_HEIGHT, 5);
        }
        dropdown.setPosition(selectorLeft, selectorTop);
        update();
        int listHeight = dropdown.overlayHeight(options.size());
        dropdown.setOverlayAbove(selectorTop + TmaStatusLayout.SELECTOR_HEIGHT + listHeight > screenHeight
                && selectorTop - listHeight >= 0);
        if (selectorTop < top || selectorTop + TmaStatusLayout.SELECTOR_HEIGHT > bottom) collapse();
    }

    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        update();
        dropdown.renderBase(graphics, font, options, selectedIndex, mouseX, mouseY, TmaGiftStatusPanel::renderOption);
        for (int i = 0; i < rows.size(); i++) {
            graphics.drawString(font, font.plainSubstrByWidth(rows.get(i).getString(), right - fieldLeft),
                    fieldLeft, TmaStatusLayout.giftDetailTop(rowTop, i), BondGuiTokens.COLOR_TEXT_BODY, false);
        }
    }

    void renderOverlay(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        update();
        dropdown.renderOverlay(graphics, font, options, selectedIndex, mouseX, mouseY, TmaGiftStatusPanel::renderOption);
    }

    boolean click(double x, double y) {
        update();
        BondDropdown.ClickResult result = dropdown.mouseClicked(x, y, options.size());
        if (result.selectedIndex() >= 0) {
            String uuid = options.get(result.selectedIndex()).uuid();
            if (uuid != null) state.selection().select(uuid);
        }
        return result.handled();
    }

    boolean scroll(double x, double y, double delta) {
        if (delta == 0.0D) return false;
        update();
        if (dropdown != null && dropdown.mouseScrolled(x, y, delta, options.size())) return true;
        if (!expanded() && itemDetails.size() > TOOLTIP_ITEMS
                && TmaGiftStatusViewRules.inViewport(x, y, fieldLeft, viewportTop, right, viewportBottom)
                && TmaStatusLayout.giftDetailIndex(rowTop, y) == 1) {
            itemOffset = TmaGiftStatusViewRules.clampScroll(itemOffset + (delta > 0 ? -1 : 1), itemDetails.size(), TOOLTIP_ITEMS);
            updateItemTooltip();
            return true;
        }
        return false;
    }

    List<Component> tooltip(double x, double y) {
        update();
        if (expanded()) return List.of();
        if (!TmaGiftStatusViewRules.inViewport(x, y, left, viewportTop, right, viewportBottom)) return List.of();
        if (x >= selectorLeft && TmaStatusLayout.selectorContainsY(rowTop, y)) {
            Option selected = options.get(selectedIndex);
            return selected.uuid() == null ? List.of(selected.label())
                    : List.of(selected.label(), Component.literal(selected.uuid()));
        }
        if (x < fieldLeft) return List.of();
        int index = TmaStatusLayout.giftDetailIndex(rowTop, y);
        if (index < 0 || index >= rows.size()) return List.of();
        if (index == 1 && !itemDetails.isEmpty()) return itemTooltip;
        if (index == 2) return List.of(rows.get(index), tr("next.tip"));
        if (index == 4) return List.of(rows.get(index), tr("conditional"));
        return List.of(rows.get(index));
    }

    private void update() {
        TmaGiftStatusSelection selection = state.selection();
        long now = System.nanoTime();
        TmaGiftStatusSelection.Entry entry = selection.selected();
        long second = entry == null ? 0L : TmaGiftStatusSelection.elapsedMillis(entry, now) / 1000L;
        if (renderedRevision == selection.revision() && renderedSecond == second) return;
        if (renderedRevision != selection.revision()) rebuildOptions(selection);
        renderedRevision = selection.revision();
        renderedSecond = second;
        if (entry == null) {
            rows = List.of(tr(selection.loading() ? "loading" : selection.complete() ? "empty" : "unavailable"));
            itemDetails = List.of();
            return;
        }
        TmaGiftStatusWire.MaidStatus maid = entry.maid();
        if (projectedEntry != entry) project(entry);
        Component next = maid.nextReadyAtMs() > 0
                ? tr("next", nextDate, TmaGiftStatusSelection.preparationSeconds(entry, now))
                : tr(maid.queued() >= entry.page().maxQueued() ? "next.full" : "next.none");
        Component waiting = condition;
        if (maid.deliveryCooldownSeconds() > 0) {
            waiting = waiting.copy().append(tr("cooldown", TmaGiftStatusSelection.cooldownSeconds(entry, now)));
        }
        rows = List.of(queue, contents, next, last, waiting,
                selection.loading() ? tr("loading_count", selection.options().size())
                        : !selection.complete() ? tr("incomplete") : tr("snapshot", second));
    }

    private void project(TmaGiftStatusSelection.Entry entry) {
        projectedEntry = entry;
        TmaGiftStatusWire.MaidStatus maid = entry.maid();
        queue = tr("queue", maid.queued(), entry.page().maxQueued(), maid.preparedCount());
        List<Component> detail = new ArrayList<>();
        MutableComponent items = Component.empty();
        for (TmaGiftStatusWire.Gift gift : TmaGiftStatusSelection.preparedItems(maid)) {
            Component item = tr("item", itemName(gift.itemId()), gift.count());
            if (!detail.isEmpty()) items.append(Component.literal(", "));
            items.append(item);
            detail.add(item.copy().append(Component.literal(" [" + gift.itemId() + "]")));
        }
        itemDetails = List.copyOf(detail);
        itemOffset = 0;
        updateItemTooltip();
        contents = detail.isEmpty() ? tr("gifts.empty") : tr("contents", items);
        nextDate = maid.nextReadyAtMs() > 0 ? date(maid.nextReadyAtMs()) : "";
        last = maid.lastDeliveryAtMs() > 0
                ? tr("last", itemName(maid.lastGiftId()), date(maid.lastDeliveryAtMs())) : tr("last.none");
        condition = tr("waiting", tr("state." + maid.state().name().toLowerCase(Locale.ROOT)));
    }

    private void updateItemTooltip() {
        if (itemDetails.size() <= TOOLTIP_ITEMS) {
            itemTooltip = itemDetails;
            return;
        }
        int end = Math.min(itemDetails.size(), itemOffset + TOOLTIP_ITEMS);
        List<Component> lines = new ArrayList<>(itemDetails.subList(itemOffset, end));
        lines.add(tr("contents.scroll", itemOffset + 1, end, itemDetails.size()));
        itemTooltip = List.copyOf(lines);
    }

    private void rebuildOptions(TmaGiftStatusSelection selection) {
        List<Option> list = new ArrayList<>();
        selectedIndex = -1;
        for (TmaGiftStatusSelection.Entry entry : selection.options()) {
            TmaGiftStatusWire.MaidStatus maid = entry.maid();
            String uuid = maid.maidUuid();
            Component name = maid.name().isBlank() ? tr("unknown") : Component.literal(maid.name());
            if (uuid.equals(selection.selectedUuid())) selectedIndex = list.size();
            list.add(new Option(uuid, tr("maid", name, uuid.substring(0, Math.min(8, uuid.length())))));
        }
        if (selection.loading() || !selection.complete() || list.isEmpty()) {
            list.add(new Option(null, tr(selection.loading() ? "loading" : selection.complete() ? "empty" : "incomplete")));
        }
        if (selectedIndex < 0) selectedIndex = list.size() - 1;
        options = List.copyOf(list);
    }

    private static void renderOption(GuiGraphics graphics, Font font, Option item, int index, int left,
                                     int top, int right, int height, boolean hovered, boolean selectedHeader) {
        graphics.drawString(font, font.plainSubstrByWidth(item.label().getString(), right - left), left, top,
                hovered ? BondGuiTokens.COLOR_TEXT_TITLE : BondGuiTokens.COLOR_TEXT_BODY, false);
    }

    private static String date(long epoch) { return DATE.format(Instant.ofEpochMilli(epoch)); }
    private static Component itemName(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return Component.literal(itemId);
        return BuiltInRegistries.ITEM.get(id).getDescription();
    }
    private static Component tr(String key, Object... args) {
        return Component.translatable("bond.settings.gifts." + key, args);
    }
    private record Option(String uuid, Component label) { }
}
