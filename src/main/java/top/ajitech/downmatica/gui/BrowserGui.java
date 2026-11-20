package top.ajitech.downmatica.gui;

import fi.dy.masa.malilib.gui.*;
import top.ajitech.downmatica.DownmaticaMod;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.core.Paged;
import top.ajitech.downmatica.core.Query;
import top.ajitech.downmatica.core.SchematicAcquirerManager;
import top.ajitech.downmatica.util.GuiUtil;
import top.ajitech.downmatica.util.StringUtil;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
//#if MC >= 12111
//$$ import fi.dy.masa.malilib.render.GuiContext;
//#endif
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
//#if MC < 12111
import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

public class BrowserGui extends GuiListBase<Schematic, BrowserGui.WidgetList.Entry, BrowserGui.WidgetList> {
    private ButtonGeneric lastPageButton = null;
    private ButtonGeneric nextPageButton = null;
    private int totalPages = 0;
    private int page = 1;
    private int pageTextX = 0;

    public BrowserGui(Screen parent) {
        super(12, 62);
        setParent(parent);
        useTitleHierarchy = false;
        title = DownmaticaMod.MOD_NAME + " => " + StringUtils.translate("downmatica.gui.browser.title");
    }

    @Override
    public void initGui() {
        super.initGui();

        int y = 30;
        int x = getListX();

        String downloadButtonDisplay = StringUtils.translate("downmatica.gui.common.button.browser.display");
        int downloadButtonWidth = GuiUtil.getStringWidth(downloadButtonDisplay) + 10 * 2;
        addButton(new ButtonGeneric(x, y, downloadButtonWidth, 20, downloadButtonDisplay), null).setEnabled(false);

        x += downloadButtonWidth + 4;//4表示按钮间的间隔
        String configButtonDisplay = StringUtils.translate("downmatica.gui.common.button.config.display");
        int configButtonWidth = GuiUtil.getStringWidth(configButtonDisplay) + 10 * 2;
        addButton(new ButtonGeneric(x, y, configButtonWidth, 20, configButtonDisplay), (button, mouseButton) -> GuiBase.openGui(new ConfigGui(getParent())));

        String nextButtonDisplay = StringUtils.translate("downmatica.gui.browser.button.next.display");
        int nextButtonWidth = GuiUtil.getStringWidth(nextButtonDisplay) + 10 * 2;
        x = width - nextButtonWidth - 12;
        nextPageButton = addButton(new ButtonGeneric(x, y, nextButtonWidth, 20, nextButtonDisplay), (button, mouseButton) -> setPage(page + 1));

        String lastButtonDisplay = StringUtils.translate("downmatica.gui.browser.button.last.display");
        int lastButtonWidth = GuiUtil.getStringWidth(lastButtonDisplay) + 10 * 2;
        x = x - 4 - lastButtonWidth;
        lastPageButton = addButton(new ButtonGeneric(x, y, lastButtonWidth, 20, lastButtonDisplay), (button, mouseButton) -> setPage(page - 1));

        pageTextX = lastPageButton.getX() - 4;
        updatePageButtons();
    }

    private void setPage(int page) {
        if (page < 1) {
            return;
        }
        WidgetList listWidget = getListWidget();
        if (listWidget != null) {
            listWidget.loadPage(page, false);
        }
        updatePageButtons();
    }

    private void updatePageButtons() {
        WidgetList listWidget = getListWidget();
        if (listWidget != null && listWidget.entries == null) {
            lastPageButton.setEnabled(false);
            nextPageButton.setEnabled(false);
            return;
        }
        lastPageButton.setEnabled(page > 1);
        nextPageButton.setEnabled(totalPages < 1 || page < totalPages);
    }

    @Override
    //#if MC < 12111
    public void drawContents(GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks) {
    //#else
    //$$ public void drawContents(GuiContext drawContext, int mouseX, int mouseY, float partialTicks) {
    //#endif
        super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        String pageDisplay = StringUtils.translate("downmatica.gui.browser.text.page", page, totalPages < 1 ? "?" : totalPages);
        drawString(drawContext, pageDisplay, pageTextX  - GuiUtil.getStringWidth(pageDisplay), 36, 0xFFFFFFFF);
    }

    @Override
    protected WidgetList createListWidget(int listX, int listY) {
        return new WidgetList(listX, listY, getBrowserWidth(), getBrowserHeight());
    }

    @Override
    protected int getBrowserWidth() {
        return width - getListX() * 2;
    }

    @Override
    protected int getBrowserHeight() {
        return height - getListY() - 6;//6表示下方留空
    }

    public class WidgetList extends WidgetListBase<Schematic, BrowserGui.WidgetList.Entry> {
        private final AtomicLong requestGeneration = new AtomicLong();
        private final AtomicLong infoGeneration = new AtomicLong();
        private int infoHeight;
        private int infoWidth;
        @Nullable
        private Collection<Schematic> entries;
        @Nullable
        private String keyword;
        @Nullable
        private Schematic infoEntry;
        @Nullable
        private Info info;

        private WidgetList(int x, int y, int width, int height) {
            super(x, y, width, height, null);
            final int searchBarHeight = 14;
            browserEntriesOffsetY = searchBarHeight + 2;//2表示搜索栏和实体的间隔
            browserEntryHeight = 22;
            widgetSearchBar = new WidgetSearchBar(x + 1 , y + 4, browserWidth - 1, searchBarHeight, 0, MaLiLibIcons.SEARCH, LeftRight.LEFT);
        }

        private void loadPage(int page, boolean debounce) {
            entries = null;
            BrowserGui.this.page = page;
            refreshEntries();
            if (lastPageButton != null && nextPageButton != null) {
                updatePageButtons();
            }
            long generation = requestGeneration.addAndGet(1L);
            Thread.ofVirtual().name("SchematicLoader").start(() -> {
                if (debounce) {
                    try {
                        Thread.sleep(300L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (generation != requestGeneration.get()) {
                        return;
                    }
                }
                Paged paged = SchematicAcquirerManager.INSTANCE.getPage(Query.of(page, keyword));
                mc.execute(() -> {
                    if (generation != requestGeneration.get()) {
                        return;
                    }
                    totalPages = paged.getTotalPages();
                    entries = paged.getEntries();
                    refreshEntries();
                    updatePageButtons();
                });
            });
        }

        @Override
        public void setSize(int width, int height) {
            super.setSize(width, height);
            infoWidth = width / 3;
            infoHeight = height;
            browserWidth = width - infoWidth - 4;//浏览器和信息之间的间隔
            browserEntryWidth = browserWidth - 2 - 8 - 1 - 1;//2表示左边留空，8表示滚动条的宽度，1表示滚动条和实体间的间隔，1表示右边留空。
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
                drawInfo(drawContext, entry, x, y);
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
        //$$         drawInfo(drawContext, entry, x, y);
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
        //$$         drawInfo(drawContext, entry, x, y);
        //$$     }
        //$$ }
        //#endif

        //#if MC < 12111
        private void drawLoading(GuiGraphics drawContext, int x, int y) {
        //#else
        //$$ private void drawLoading(GuiContext drawContext, int x, int y) {
        //#endif
            String string = StringUtils.translate("downmatica.gui.browser.text.loading");
            x += (infoWidth - GuiUtil.getStringWidth(string)) / 2;
            y += (infoHeight - GuiUtil.getFontHeight()) / 2;
            drawString(drawContext, string, x, y, 0xFFFFFFFF);
        }

        //#if MC < 12111
        private void drawInfo(GuiGraphics drawContext, Schematic entry, int x, int y) {
        //#else
        //$$ private void drawInfo(GuiContext drawContext, Schematic entry, int x, int y) {
        //#endif
            if (entry != infoEntry) {
                infoEntry = entry;
                info = null;
                long generation = infoGeneration.incrementAndGet();
                Thread.ofVirtual().name("SchematicInfoLoader").start(() -> {
                    Info snapshot;
                    try {
                        snapshot = new Info(entry.getSource(), entry.getAuthor(), entry.getDescription());
                    } catch (Exception e) {
                        DownmaticaMod.LOGGER.error("Failed to load schematic info", e);
                        snapshot = new Info(null, null, null);
                    }
                    Info info = snapshot;
                    mc.execute(() -> {
                        if (generation == infoGeneration.get()) {
                            this.info = info;
                        }
                    });
                });
            }
            if (info == null) {
                drawLoading(drawContext, x, y);
                return;
            }
            x += 4;//4表示左边留空
            y += 4;//4表示上方留空

            String unknown = StringUtils.translate("downmatica.gui.browser.text.unknown");
            String none = StringUtils.translate("downmatica.gui.browser.text.none");

            final int space = 4;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.browser.info.title"), x, y, 0xC0C0C0C0);
            String title = entry.getTitle();
            y = drawInfoText(drawContext, !StringUtil.hasText(title) ? unknown : title, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.browser.info.source"), x, y, 0xC0C0C0C0);
            String source = info.source();
            y = drawInfoText(drawContext, source == null ? unknown : source, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.browser.info.author"), x, y, 0xC0C0C0C0);
            String author = info.author();
            y = drawInfoText(drawContext, !StringUtil.hasText(author) ? unknown : author, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.browser.info.description"), x, y, 0xC0C0C0C0);
            String description = info.description();
            drawInfoText(drawContext, !StringUtil.hasText(description) ? none : description, x, y, 0xFFFFFFFF);
        }

        private record Info(String source, String author, String description) {
        }

        //#if MC < 12111
        private int drawInfoText(GuiGraphics drawContext, String text, int x, int y, int color) {
        //#else
        //$$ private int drawInfoText(GuiContext drawContext, String text, int x, int y, int color) {
        //#endif
            ArrayList<String> lines = new ArrayList<>();
            StringUtils.splitTextToLines(lines, text, infoWidth - 4);
            for (String line : lines) {
                drawString(drawContext, line, x, y, color);
                y += GuiUtil.getFontHeight() + 2;//每行间间隔2
            }
            return y;
        }

        @Override
        protected Entry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, Schematic entry) {
            return new Entry(x, y, browserEntryWidth, browserEntryHeight, entry, listIndex, isOdd);
        }

        @Override
        protected Collection<Schematic> getAllEntries() {
            return entries == null ? Collections.emptyList() : entries;
        }

        @Override
        protected void refreshBrowserEntries() {
            String filterText = getFilterText();
            // 第一次加载在这里
            if (!filterText.equals(keyword)) {
                boolean firstLoad = keyword == null;
                this.keyword = filterText;
                loadPage(1, !firstLoad);
            }
            super.refreshBrowserEntries();
        }

        @Override
        protected void addFilteredContents(Collection<Schematic> entries) {
            this.listContents.addAll(entries);
        }

        public class Entry extends WidgetListEntryBase<Schematic> {
            private final boolean isOdd;

            private Entry(int x, int y, int width, int height, Schematic entry, int listIndex, boolean isOdd) {
                super(x, y, width, height, entry, listIndex);
                this.isOdd = isOdd;
                int verticalMargin = 1;
                int buttonHeight = height - verticalMargin * 2;
                y += verticalMargin;

                String downloadButtonDisplay = StringUtils.translate("downmatica.gui.browser.button.download.display");
                int downloadButtonWidth = GuiUtil.getStringWidth(downloadButtonDisplay) + 10 * 2;
                x += width - downloadButtonWidth - 2;//2表示右边留空
                ButtonGeneric downloadButton = addButton(
                        new ButtonGeneric(x, y, downloadButtonWidth, buttonHeight, downloadButtonDisplay),
                        (button, mouseButton) -> GuiBase.openGui(new DownloadGui(BrowserGui.this, entry.getDownloadFileInfos()))
                );
                if (!entry.canDownload()) {
                    downloadButton.setEnabled(false);
                }

                String detailsButtonDisplay = StringUtils.translate("downmatica.gui.browser.button.details.display");
                int detailsButtonWidth = GuiUtil.getStringWidth(detailsButtonDisplay) + 10 * 2;
                x -= detailsButtonWidth + 2;//2表示按钮之间的间隔
                Runnable runnable = entry.onDetailButtonClicked();
                ButtonGeneric detailsButton = addButton(
                        new ButtonGeneric(x, y, detailsButtonWidth, buttonHeight, detailsButtonDisplay),
                        (button, mouseButton) -> {
                            if (runnable == null) {
                                BrowserGui.this.addMessage(Message.MessageType.ERROR, "downmatica.gui.browser.button.details.massage.open_web_page_failed");
                                return;
                            }
                            runnable.run();
                        }
                );
                if (runnable == null) {
                    detailsButton.setEnabled(false);
                }
            }

            @Override
            //#if MC < 12106
            public void render(int mouseX, int mouseY, boolean selected, GuiGraphics drawContext) {
                if (selected || isMouseOver(mouseX, mouseY)) {
                    RenderUtils.drawRect(x, y, width, height, 0x70FFFFFF);
                } else {
                    RenderUtils.drawRect(x, y, width, height, isOdd ? 0x20FFFFFF : 0x50FFFFFF);
                }
                String text = StringUtils.translate("downmatica.gui.browser.text.unknown");
                if (entry != null){
                    String string = entry.getTitle();
                    if (StringUtil.hasText(string)) {
                        text = string;
                    }
                }
                drawString(x + 20, y + (height - GuiUtil.getFontHeight()) / 2 , 0xFFFFFFFF, text, drawContext);//20表示左边留空
                super.render(mouseX, mouseY, selected, drawContext);
            }
            //#elseif MC < 12111
            //$$ public void render(GuiGraphics drawContext, int mouseX, int mouseY, boolean selected) {
            //$$     if (selected || isMouseOver(mouseX, mouseY)) {
            //$$         RenderUtils.drawRect(drawContext, x, y, width, height, 0x70FFFFFF);
            //$$     } else {
            //$$         RenderUtils.drawRect(drawContext, x, y, width, height, isOdd ? 0x20FFFFFF : 0x50FFFFFF);
            //$$     }
            //$$     String text = StringUtils.translate("downmatica.gui.browser.text.unknown");
            //$$     if (entry != null){
            //$$         String string = entry.getTitle();
            //$$         if (StringUtil.hasText(string)) {
            //$$             text = string;
            //$$         }
            //$$     }
            //$$     drawString(drawContext, x + 20, y + (height - GuiUtil.getFontHeight()) / 2 , 0xFFFFFFFF, text);
            //$$     super.render(drawContext, mouseX, mouseY, selected);
            //$$ }
            //#else
            //$$ public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
            //$$     if (selected || isMouseOver(mouseX, mouseY)) {
            //$$         RenderUtils.drawRect(ctx, x, y, width, height, 0x70FFFFFF);
            //$$     } else {
            //$$         RenderUtils.drawRect(ctx, x, y, width, height, isOdd ? 0x20FFFFFF : 0x50FFFFFF);
            //$$     }
            //$$     String text = StringUtils.translate("downmatica.gui.browser.text.unknown");
            //$$     if (entry != null){
            //$$         String string = entry.getTitle();
            //$$         if (StringUtil.hasText(string)) {
            //$$             text = string;
            //$$         }
            //$$     }
            //$$     drawString(ctx, x + 20, y + (height - GuiUtil.getFontHeight()) / 2 , 0xFFFFFFFF, text);
            //$$     super.render(ctx, mouseX, mouseY, selected);
            //$$ }
            //#endif
        }
    }
}
