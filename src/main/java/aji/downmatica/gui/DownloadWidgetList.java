package aji.downmatica.gui;

import aji.downmatica.entry.Schematic;
import aji.downmatica.entry.SchematicSource;
import aji.downmatica.entry.SchematicAcquirers;
import aji.downmatica.util.StringUtil;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DownloadWidgetList extends WidgetListBase<Schematic, DownloadWidgetListEntry> {
    private static final int HORIZONTAL_MARGIN = 2;
    private static final int VERTICAL_MARGIN  = 4;
    private static final int INFO_MARGIN = 4;
    private static final int INFO_SPACING = 4;
    private static final int BROWSER_HORIZONTAL_MARGIN = 2;
    private static final int INFO_BROWSER_GAP = 4;
    private static final int SEARCH_BAR_ENTRY_GAP = 3;
    private static final int SEARCH_BAR_HEIGHT = 14;
    private static final int SCROLL_BAR_WIDTH = 8;
    private static final int STRING_HEIGHT = 8;

    private final DownloadGui gui;
    private final Collection<Schematic> entries = new ArrayList<>();
    private int infoHeight;
    private int infoWidth;

    public DownloadWidgetList(int x, int y, int width, int height, DownloadGui gui) {
        super(x, y, width, height, null);
        this.gui = gui;
        widgetSearchBar = new WidgetSearchBar(
                x + HORIZONTAL_MARGIN,
                y + VERTICAL_MARGIN,
                browserWidth - HORIZONTAL_MARGIN,
                SEARCH_BAR_HEIGHT,
                0,
                Icons.FILE_ICON_SEARCH,
                LeftRight.LEFT
        );
        new Thread(() -> {
            entries.addAll(SchematicAcquirers.getAllSchematics());
            refreshEntries();
        }).start();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        infoWidth = width / 4;
        infoHeight = height;
        browserWidth = width - infoWidth - INFO_BROWSER_GAP;
        browserEntryHeight = 22;
        browserEntryWidth = browserWidth - BROWSER_HORIZONTAL_MARGIN * 2 - SCROLL_BAR_WIDTH;
        browserEntriesOffsetY = SEARCH_BAR_HEIGHT + SEARCH_BAR_ENTRY_GAP;
    }

    @Override
    public void drawContents(GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks) {
        RenderUtils.drawOutlinedBox(posX, posY, browserWidth, browserHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);

        super.drawContents(drawContext, mouseX, mouseY, partialTicks);

        int x = posX + totalWidth - infoWidth;
        int y = posY;
        RenderUtils.drawOutlinedBox(x, y, infoWidth, infoHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
        if (entries.isEmpty()) {
            drawLoading(drawContext, x, y);
            return;
        }
        Schematic entry = getLastSelectedEntry();
        if (entry != null) {
            drawInfo(drawContext, entry, x + INFO_MARGIN, y + INFO_MARGIN);
        }
    }

    private void drawLoading(GuiGraphics drawContext, int x, int y) {
        String string = StringUtils.translate("downmatica.gui.text.loading");
        x += (infoWidth - getStringWidth(string)) / 2;
        y += (infoHeight - STRING_HEIGHT) / 2;
        drawString(drawContext, string, x, y, 0xFFFFFFFF);
    }

    private void drawInfo(GuiGraphics drawContext, Schematic entry, int x, int y) {
        final int textColor = 0xC0C0C0C0;
        final int valueColor = 0xFFFFFFFF;

        String unknown = StringUtils.translate("downmatica.gui.text.unknown");
        String none = StringUtils.translate("downmatica.gui.text.none");

        y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.title"), x, y, textColor);
        String title = entry.title();
        y = drawInfoText(drawContext, !StringUtil.hasText(title) ? unknown : title, x, y, valueColor) + INFO_SPACING;

        y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.source"), x, y, textColor);
        SchematicSource source = entry.source();
        y = drawInfoText(drawContext, source == null ? unknown : source.getName(), x, y, valueColor) + INFO_SPACING;

        y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.author"), x, y, textColor);
        String author = entry.author();
        y = drawInfoText(drawContext, !StringUtil.hasText(author) ? unknown : author, x, y, valueColor) + INFO_SPACING;

        y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.description"), x, y, textColor);
        String description = entry.description();
        y = drawInfoText(drawContext, !StringUtil.hasText(description) ? none : description, x, y, valueColor) + INFO_SPACING;
        //这里赋值y没用 强迫症 看着不舒服 😄
    }

    private int drawInfoText(GuiGraphics drawContext, String text, int x, int y, int color) {
        final int infoMaxWidth = infoWidth - INFO_MARGIN;
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            if (!StringUtil.hasText(line)) {
                y += STRING_HEIGHT + INFO_SPACING;
                continue;
            }
            while (StringUtil.hasText(line)) {
                if (getStringWidth(line) <= infoMaxWidth) {
                    drawString(drawContext, line, x, y, color);
                    y += STRING_HEIGHT + INFO_SPACING;
                    break;
                }
                int length = line.length();
                while (getStringWidth(line.substring(0, length)) > infoMaxWidth) {
                    length--;
                }
                drawString(drawContext, line.substring(0, length), x, y, color);
                y += STRING_HEIGHT + INFO_SPACING;
                line = line.substring(length);
            }
        }
        return y;
    }

    @Override
    protected DownloadWidgetListEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, Schematic entry) {
        return new DownloadWidgetListEntry(x, y, browserEntryWidth, browserEntryHeight, entry, listIndex, isOdd, gui);
    }

    @Override
    protected Collection<Schematic> getAllEntries() {
        return entries;
    }

    @Override
    protected List<String> getEntryStringsForFilter(Schematic entry) {
        return entry.getSearchStrings();
    }
}
