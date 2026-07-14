package top.ajitech.downmatica.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.gui.*;
import top.ajitech.downmatica.Downmatica;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicDownloadInfo;
import top.ajitech.downmatica.core.ConfigHandler;
import top.ajitech.downmatica.core.HttpClientContainer;
import top.ajitech.downmatica.core.SchematicAcquirerManager;
import top.ajitech.downmatica.util.Stopwatch;
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
import net.minecraft.client.Minecraft;
//#if MC < 12111
import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DownloadGui extends GuiListBase<Schematic, DownloadGui.WidgetList.Entry, DownloadGui.WidgetList> {
    private final ConfigGui configGui;

    public DownloadGui(Screen parent, @Nullable ConfigGui configGui) {
        super(12, 62);
        useTitleHierarchy = false;
        title = Downmatica.MOD_NAME + " => " + StringUtils.translate("downmatica.gui.download.title");
        setParent(parent);
        if (configGui == null) {
            configGui = new ConfigGui(parent, this);
        }
        this.configGui = configGui;
    }

    @Override
    public void initGui() {
        super.initGui();

        int y = 30;
        int x = getListX();

        String downloadButtonDisplay = StringUtils.translate("downmatica.gui.common.button.download.display");
        int downloadButtonWidth = GuiUtil.getStringWidth(downloadButtonDisplay) + 10 * 2;//10为按钮的左右留白
        addButton(new ButtonGeneric(x, y, downloadButtonWidth, 20, downloadButtonDisplay), null).setEnabled(false);

        x += downloadButtonWidth + 4;//4表示按钮间的间隔
        String configButtonDisplay = StringUtils.translate("downmatica.gui.common.button.config.display");
        int configButtonWidth = GuiUtil.getStringWidth(configButtonDisplay) + 10 * 2;
        addButton(
                new ButtonGeneric(x, y, configButtonWidth, 20, configButtonDisplay),
                (button, mouseButton) -> GuiBase.openGui(configGui)
        );

        String reloadButtonDisplay = StringUtils.translate("downmatica.gui.download.button.reload.display");
        int reloadButtonWidth = GuiUtil.getStringWidth(reloadButtonDisplay) + 10 * 2;
        x = width - getListX() - reloadButtonWidth;
        WidgetList listWidget = getListWidget();
        boolean bl = listWidget == null;
        ButtonGeneric reloadButton = addButton(
                new ButtonGeneric(x, y, reloadButtonWidth, 20, reloadButtonDisplay),
                (button, mouseButton) -> {
                    if (!bl) {
                        listWidget.reloadEntries();
                    }
                }
        );
        if (bl){
            reloadButton.setEnabled(false);
        }
    }

    @Override
    protected WidgetList createListWidget(int listX, int listY) {
        WidgetList list = new WidgetList(listX, listY, getBrowserWidth(), getBrowserHeight());
        list.loadEntries();
        return list;
    }

    @Override
    protected int getBrowserWidth() {
        return width - getListX() * 2;
    }

    @Override
    protected int getBrowserHeight() {
        return height - getListY() - 6;//6表示下方留空
    }

    @Override
    public void addMessage(Message.MessageType type, String translationKey, Object... args) {
        if (mc.isSameThread()) {
            super.addMessage(type, translationKey, args);
        } else {
            mc.execute(() -> super.addMessage(type, translationKey, args));
        }
    }

    public void loadEntries() {
        WidgetList list = getListWidget();
        if (list != null) {
            list.loadEntries();
        }
    }

    public class WidgetList extends WidgetListBase<Schematic, DownloadGui.WidgetList.Entry> {
        private int infoHeight;
        private int infoWidth;
        @Nullable
        private Collection<Schematic> entries;

        public WidgetList(int x, int y, int width, int height) {
            super(x, y, width, height, null);
            final int searchBarHeight = 14;
            browserEntriesOffsetY = searchBarHeight + 2;//2表示搜索栏和实体的间隔
            browserEntryHeight = 22;
            widgetSearchBar = new WidgetSearchBar(
                    x + 1 ,//1我也不知道是什么 但是加一个像素会更好看些
                    y + 4,//4表示上方留空
                    getSearchBarWidth(),
                    searchBarHeight,
                    0,
                    MaLiLibIcons.SEARCH,
                    LeftRight.LEFT
            ){
                @Override
                public void setWidth(int width) {
                    super.setWidth(width);
                    searchBox.setWidth(width - iconSearch.getWidth() - 7);
                }
            };
        }

        public void loadEntries() {
            Thread.ofVirtual().name("SchematicLoader").start(() -> {
                Collection<Schematic> schematics = SchematicAcquirerManager.INSTANCE.getAllSchematics();
                Minecraft.getInstance().execute(() -> {
                    entries = schematics;
                    refreshEntries();
                });
            });
        }

        public void reloadEntries() {
            entries = null;
            refreshEntries();
            loadEntries();
        }

        @Override
        public void setSize(int width, int height) {
            super.setSize(width, height);
            infoWidth = width / 3;
            infoHeight = height;
            browserWidth = width - infoWidth - 4;//浏览器和信息之间的间隔
            browserEntryWidth = browserWidth - 2 - 8 - 1 - 1;//2表示左边留空，8表示滚动条的宽度，1表示滚动条和实体间的间隔，1表示右边留空。
            if (widgetSearchBar != null) {
                widgetSearchBar.setWidth(getSearchBarWidth());
            }
        }

        private int getSearchBarWidth() {
            return browserWidth - 1;//1我也不知道是什么 但是减一个像素会更好看些
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
            String string = StringUtils.translate("downmatica.gui.download.text.loading");
            x += (infoWidth - GuiUtil.getStringWidth(string)) / 2;
            y += (infoHeight - GuiUtil.getFontHeight()) / 2;
            drawString(drawContext, string, x, y, 0xFFFFFFFF);
        }

        //#if MC < 12111
        private void drawInfo(GuiGraphics drawContext, Schematic entry, int x, int y) {
        //#else
        //$$ private void drawInfo(GuiContext drawContext, Schematic entry, int x, int y) {
        //#endif
            x += 4;//4表示左边留空
            y += 4;//4表示上方留空

            String unknown = StringUtils.translate("downmatica.gui.download.text.unknown");
            String none = StringUtils.translate("downmatica.gui.download.text.none");

            final int space = 4;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.download.info.title"), x, y, 0xC0C0C0C0);
            String title = entry.getTitle();
            y = drawInfoText(drawContext, !StringUtil.hasText(title) ? unknown : title, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.download.info.source"), x, y, 0xC0C0C0C0);
            String source = entry.getSource();
            y = drawInfoText(drawContext, source == null ? unknown : source, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.download.info.author"), x, y, 0xC0C0C0C0);
            String author = entry.getAuthor();
            y = drawInfoText(drawContext, !StringUtil.hasText(author) ? unknown : author, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.download.info.description"), x, y, 0xC0C0C0C0);
            String description = entry.getDescription();
            drawInfoText(drawContext, !StringUtil.hasText(description) ? none : description, x, y, 0xFFFFFFFF);
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
        protected List<String> getEntryStringsForFilter(Schematic entry) {
            return entry.getSearchStrings();
        }

        public class Entry extends WidgetListEntryBase<Schematic> {
            private final boolean isOdd;

            public Entry(int x, int y, int width, int height, Schematic entry, int listIndex, boolean isOdd) {
                super(x, y, width, height, entry, listIndex);
                this.isOdd = isOdd;
                int verticalMargin = 1;
                int buttonHeight = height - verticalMargin * 2;
                y += verticalMargin;

                String downloadButtonDisplay = StringUtils.translate("downmatica.gui.download.button.download.display");
                int downloadButtonWidth = GuiUtil.getStringWidth(downloadButtonDisplay) + 10 * 2;
                x += width - downloadButtonWidth - 2;//2表示右边留空
                String title = entry.getTitle();
                SchematicDownloadInfo downloadFileInfo = entry.getDownloadFileInfo();
                ButtonGeneric downloadButton = addButton(
                        new ButtonGeneric(x, y, downloadButtonWidth, buttonHeight, downloadButtonDisplay),
                        (button, mouseButton) -> Thread.ofVirtual().name("SchematicDownloader").start(() -> {
                            Path path;
                            if (ConfigHandler.INSTANCE.downloadToSchematicBaseDirectory.getBooleanValue()) {
                                //#if MC < 1216
                                path = DataManager.getSchematicsBaseDirectory().toPath();
                                //#else
                                //$$ path = DataManager.getSchematicsBaseDirectory();
                                //#endif
                            } else {
                                String selectFolder = TinyFileDialogs.tinyfd_selectFolderDialog("downmatica.gui.download.button.download.massage.select_folder", "");
                                if (selectFolder == null) {
                                    DownloadGui.this.addMessage(Message.MessageType.INFO, "downmatica.gui.download.button.download.massage.cancel");
                                    return;
                                }
                                path = Paths.get(selectFolder);
                            }
                            if (downloadFileInfo == null) {
                                DownloadGui.this.addMessage(Message.MessageType.ERROR, "downmatica.gui.download.button.download.massage.no_file");
                                return;
                            }
                            path = path.resolve(downloadFileInfo.getLocalFileName());
                            if (Files.exists(path) && !ConfigHandler.INSTANCE.downloadOverlayFile.getBooleanValue()) {
                                DownloadGui.this.addMessage(Message.MessageType.ERROR, "downmatica.gui.download.button.download.massage.file_exists");
                                return;
                            }
                            DownloadGui.this.addMessage(
                                    Message.MessageType.INFO,
                                    "downmatica.gui.download.button.download.massage.start",
                                    title, path
                            );
                            Stopwatch stopwatch = new Stopwatch();
                            try {
                                Path parent = path.getParent();
                                if (!Files.exists(parent)) {
                                    Files.createDirectories(parent);
                                }
                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(downloadFileInfo.getRemoteFileURI())
                                        .build();
                                HttpResponse<InputStream> response = HttpClientContainer.INSTANCE.get().send(request, HttpResponse.BodyHandlers.ofInputStream());
                                try (InputStream inputStream = response.body()) {
                                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (Exception e) {
                                String message = e.getMessage();
                                DownloadGui.this.addMessage(
                                        Message.MessageType.ERROR,
                                        "downmatica.gui.download.button.download.massage.error",
                                        StringUtil.hasText(message) ? message : StringUtils.translate("downmatica.gui.download.text.none")
                                );
                                Downmatica.LOGGER.error("Download failed", e);
                                return;
                            }
                            DownloadGui.this.addMessage(
                                    Message.MessageType.SUCCESS,
                                    "downmatica.gui.download.button.download.massage.complete",
                                    title, TimeUnit.NANOSECONDS.toMillis(stopwatch.getTime())
                            );
                        })
                );
                if (downloadFileInfo == null) {
                    downloadButton.setEnabled(false);
                } else if (!downloadFileInfo.isValid()) {
                    downloadButton.setEnabled(false);
                    Downmatica.LOGGER.warn(
                            "Configuration validation failed for schematic download source \"{}\" from \"{}\". The data is invalid, so the download button has been disabled. If you can confirm which extension or core mod this source belongs to, please report this issue to that source's developer.",
                            title,
                            entry.getSource()
                    );
                }

                String detailsButtonDisplay = StringUtils.translate("downmatica.gui.download.button.details.display");
                int detailsButtonWidth = GuiUtil.getStringWidth(detailsButtonDisplay) + 10 * 2;
                x -= detailsButtonWidth + 2;//2表示按钮之间的间隔
                Runnable runnable = entry.onDetailButtonClicked();
                ButtonGeneric detailsButton = addButton(
                        new ButtonGeneric(x, y, detailsButtonWidth, buttonHeight, detailsButtonDisplay),
                        (button, mouseButton) -> {
                            if (runnable == null) {
                                DownloadGui.this.addMessage(Message.MessageType.ERROR, "downmatica.gui.download.button.details.massage.open_web_page_failed");
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
                String text = StringUtils.translate("downmatica.gui.download.text.unknown");
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
            //$$     String text = StringUtils.translate("downmatica.gui.download.text.unknown");
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
            //$$     String text = StringUtils.translate("downmatica.gui.download.text.unknown");
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
