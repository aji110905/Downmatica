package aji.downmatica.gui;

import aji.downmatica.core.SchematicAcquirerManager;
import aji.downmatica.api.Schematic;
import aji.downmatica.util.StringUtil;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
//#if MC >= 12111
//$$ import fi.dy.masa.malilib.render.GuiContext;
//#endif
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
//#if MC < 12111
import net.minecraft.client.gui.GuiGraphics;
//#endif
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
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
    private int infoHeight;
    private int infoWidth;
    @Nullable private Collection<Schematic> entries;

    public DownloadWidgetList(int x, int y, int width, int height, DownloadGui gui) {
        super(x, y, width, height, null);
        this.gui = gui;
        browserEntriesOffsetY = SEARCH_BAR_HEIGHT + SEARCH_BAR_ENTRY_GAP;
        browserEntryHeight = 22;
        widgetSearchBar = new WidgetSearchBar(
                x + HORIZONTAL_MARGIN,
                y + VERTICAL_MARGIN,
                browserWidth - HORIZONTAL_MARGIN,
                SEARCH_BAR_HEIGHT,
                0,
                Icons.FILE_ICON_SEARCH,
                LeftRight.LEFT
        ){
            @Override
            public void setWidth(int width) {
                super.setWidth(width);
                searchBox.setWidth(width - iconSearch.getWidth() - 7);
            }
        };
    }

    public void loadEntriesAsync() {
        new Thread(() -> {
            Collection<Schematic> schematics = SchematicAcquirerManager.INSTANCE.getAllSchematics();
            Minecraft.getInstance().execute(() -> {
                entries = schematics;
                refreshEntries();
            });
        }).start();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        infoWidth = width / 4;
        infoHeight = height;
        browserWidth = width - infoWidth - INFO_BROWSER_GAP;
        browserEntryWidth = browserWidth - BROWSER_HORIZONTAL_MARGIN * 2 - SCROLL_BAR_WIDTH;
        if (widgetSearchBar != null) {
            widgetSearchBar.setWidth(width);
        }
    }

    @Override
    //#if MC < 12106
    public void drawContents(GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks) {
        RenderUtils.drawOutlinedBox(posX, posY, browserWidth, browserHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
        super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        int x = posX + totalWidth - infoWidth;
        int y = posY;
        RenderUtils.drawOutlinedBox(x, y, infoWidth, infoHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
        if (entries == null) {
            drawLoading(drawContext, x, y);
            return;
        }
        Schematic entry = getLastSelectedEntry();
        if (entry != null) {
            drawInfo(drawContext, entry, x + INFO_MARGIN, y + INFO_MARGIN);
        }
    }
    //#elseif MC < 12111
    //$$ public void drawContents(GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks) {
    //$$     RenderUtils.drawOutlinedBox(drawContext, posX, posY, browserWidth, browserHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
    //$$     super.drawContents(drawContext, mouseX, mouseY, partialTicks);
    //$$     int x = posX + totalWidth - infoWidth;
    //$$     int y = posY;
    //$$     RenderUtils.drawOutlinedBox(drawContext, x, y, infoWidth, infoHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
    //$$     if (entries == null) {
    //$$         drawLoading(drawContext, x, y);
    //$$         return;
    //$$     }
    //$$     Schematic entry = getLastSelectedEntry();
    //$$     if (entry != null) {
    //$$         drawInfo(drawContext, entry, x + INFO_MARGIN, y + INFO_MARGIN);
    //$$     }
    //$$ }
    //#else
    //$$ public void drawContents(GuiContext drawContext, int mouseX, int mouseY, float partialTicks) {
    //$$     RenderUtils.drawOutlinedBox(drawContext, posX, posY, browserWidth, browserHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
    //$$     super.drawContents(drawContext, mouseX, mouseY, partialTicks);
    //$$     int x = posX + totalWidth - infoWidth;
    //$$     int y = posY;
    //$$     RenderUtils.drawOutlinedBox(drawContext, x, y, infoWidth, infoHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
    //$$     if (entries == null) {
    //$$         drawLoading(drawContext, x, y);
    //$$         return;
    //$$     }
    //$$     Schematic entry = getLastSelectedEntry();
    //$$     if (entry != null) {
    //$$         drawInfo(drawContext, entry, x + INFO_MARGIN, y + INFO_MARGIN);
    //$$     }
    //$$ }
    //#endif

    //#if MC < 12111
    private void drawLoading(GuiGraphics drawContext, int x, int y) {
    //#else
    //$$ private void drawLoading(GuiContext drawContext, int x, int y) {
    //#endif
        String string = StringUtils.translate("downmatica.gui.text.loading");
        x += (infoWidth - getStringWidth(string)) / 2;
        y += (infoHeight - STRING_HEIGHT) / 2;
        drawString(drawContext, string, x, y, 0xFFFFFFFF);
    }

    //#if MC < 12111
    private void drawInfo(GuiGraphics drawContext, Schematic entry, int x, int y) {
    //#else
    //$$ private void drawInfo(GuiContext drawContext, Schematic entry, int x, int y) {
    //#endif
        final int textColor = 0xC0C0C0C0;
        final int valueColor = 0xFFFFFFFF;

        String unknown = StringUtils.translate("downmatica.gui.text.unknown");
        String none = StringUtils.translate("downmatica.gui.text.none");

        y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.title"), x, y, textColor);
        String title = entry.getTitle();
        y = drawInfoText(drawContext, !StringUtil.hasText(title) ? unknown : title, x, y, valueColor) + INFO_SPACING;

        y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.source"), x, y, textColor);
        String source = entry.getSource();
        y = drawInfoText(drawContext, source == null ? unknown : source, x, y, valueColor) + INFO_SPACING;

        y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.author"), x, y, textColor);
        String author = entry.getAuthor();
        y = drawInfoText(drawContext, !StringUtil.hasText(author) ? unknown : author, x, y, valueColor) + INFO_SPACING;

        y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.description"), x, y, textColor);
        String description = entry.getDescription();
        y = drawInfoText(drawContext, !StringUtil.hasText(description) ? none : description, x, y, valueColor) + INFO_SPACING;
        //这里赋值y没用 强迫症 看着不舒服 😄
    }

    //#if MC < 12111
    private int drawInfoText(GuiGraphics drawContext, String text, int x, int y, int color) {
    //#else
    //$$ private int drawInfoText(GuiContext drawContext, String text, int x, int y, int color) {
    //#endif
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
        return entries == null ? Collections.emptyList() : entries;
    }

    @Override
    protected List<String> getEntryStringsForFilter(Schematic entry) {
        return entry.getSearchStrings();
    }
}
