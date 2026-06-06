package aji.downmatica.gui;

import aji.downmatica.entry.Schematic;
import aji.downmatica.entry.SchematicSource;
import aji.downmatica.network.SchematicAcquirers;
import aji.downmatica.util.StringUtil;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
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
    private final DownloadGui gui;
    private final Collection<Schematic> entries = new ArrayList<>();
    private int infoHeight;
    private int infoWidth;

    public DownloadWidgetList(int x, int y, int width, int height, DownloadGui gui) {
        super(x, y, width, height, null);
        this.gui = gui;
        widgetSearchBar = new WidgetSearchBar(posX + 2, posY + 4, width - infoWidth - 4 - 2, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT);
        new Thread(() -> {
            entries.addAll(SchematicAcquirers.getAllSchematics());
            refreshEntries();
        }).start();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        infoWidth = 170;
        infoHeight = height;
        browserWidth = width - infoWidth - 4;
        browserEntryHeight = 22;
        browserEntryWidth = browserWidth - 14;
        if (widgetSearchBar != null) {
            browserEntriesOffsetY = widgetSearchBar.getHeight() + 3;
        }
    }

    @Override
    public void drawContents(GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks) {
        RenderUtils.drawOutlinedBox(posX, posY, browserWidth, browserHeight, 0xB0000000, GuiBase.COLOR_HORIZONTAL_BAR);
        super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        int x = posX + totalWidth - infoWidth;
        int y = posY + 3;
        RenderUtils.drawOutlinedBox(x, posY, infoWidth, infoHeight, 0xA0000000, GuiBase.COLOR_HORIZONTAL_BAR);
        if (entries.isEmpty()) {
            String string = StringUtils.translate("downmatica.gui.text.loading");
            int centerX = x + (infoWidth - getStringWidth(string)) / 2;
            int centerY = posY + (infoHeight - 12) / 2;
            drawString(drawContext, string, centerX, centerY, 0xFFFFFFFF);
            return;
        }
        Schematic entry = getLastSelectedEntry();
        final int textColor = 0xC0C0C0C0;
        final int valueColor = 0xFFFFFFFF;
        final int fieldSpacing = 4;
        if (entry == null) {
            return;
        }
        x = x + 3;
        String unknown = StringUtils.translate("downmatica.gui.text.unknown");
        y = drawWrappedText(drawContext, StringUtils.translate("downmatica.gui.info.title"), x, y, textColor);
        String title = entry.title();
        y = drawWrappedText(drawContext, !StringUtil.hasText(title) ? unknown : title, x, y, valueColor) + fieldSpacing;
        y = drawWrappedText(drawContext, StringUtils.translate("downmatica.gui.info.source"), x, y, textColor);
        SchematicSource source = entry.source();
        y = drawWrappedText(drawContext, source == null ? unknown : source.getName(), x, y, valueColor) + fieldSpacing;
        y = drawWrappedText(drawContext, StringUtils.translate("downmatica.gui.info.author"), x, y, textColor);
        String author = entry.author();
        y = drawWrappedText(drawContext, !StringUtil.hasText(author) ? unknown : author, x, y, valueColor) + fieldSpacing;
        y = drawWrappedText(drawContext, StringUtils.translate("downmatica.gui.info.description"), x, y, textColor);
        String description = entry.description();
        drawWrappedText(drawContext, !StringUtil.hasText(description) ? StringUtils.translate("downmatica.gui.text.none") : description, x, y, valueColor);
    }

    private int drawWrappedText(GuiGraphics drawContext, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) {
            return y;
        }
        int currentY = y;
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            currentY = drawLineWithWrap(drawContext, line, x, currentY, color);
        }
        return currentY;
    }

    private int drawLineWithWrap(GuiGraphics drawContext, String text, int x, int y, int color) {
        final int infoMaxWidth = infoWidth - 6;
        if (text.isEmpty()) {
            return y + 12;
        }
        int currentY = y;
        String remaining = text;
        while (!remaining.isEmpty()) {
            if (getStringWidth(remaining) <= infoMaxWidth) {
                drawString(drawContext, remaining, x, currentY, color);
                currentY += 12;
                break;
            }
            int cutPos = remaining.length();
            while (cutPos > 0 && getStringWidth(remaining.substring(0, cutPos)) > infoMaxWidth) {
                cutPos--;
            }
            if (cutPos == 0) cutPos = 1;
            String line = remaining.substring(0, cutPos);
            drawString(drawContext, line, x, currentY, color);
            currentY += 12;
            remaining = remaining.substring(cutPos);
        }
        return currentY;
    }

    @Override
    protected DownloadWidgetListEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, Schematic entry) {
        return new DownloadWidgetListEntry(x, y, browserEntryWidth, browserEntryHeight, entry, listIndex, gui);
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
