package aji.downmatica.gui;

import aji.downmatica.DownmaticaMod;
import aji.downmatica.api.Schematic;
import aji.downmatica.api.SchematicDownloadInfo;
import aji.downmatica.core.SchematicAcquirerManager;
import aji.downmatica.util.DownloadUtil;
import aji.downmatica.util.StringUtil;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.Message;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DownloadGui extends GuiListBase<Schematic, DownloadGui.WidgetList.Entry, DownloadGui.WidgetList> {
    public static final int STRING_HEIGHT = 8;

    public DownloadGui(Screen parent) {
        super(12, 30);
        title = StringUtils.translate("downmatica.gui.title");
        setParent(parent);
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

        public void loadEntries() {
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
            x += 4;//4表示左边留空
            y += 4;//4表示上方留空

            String unknown = StringUtils.translate("downmatica.gui.text.unknown");
            String none = StringUtils.translate("downmatica.gui.text.none");

            final int space = 4;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.title"), x, y, 0xC0C0C0C0);
            String title = entry.getTitle();
            y = drawInfoText(drawContext, !StringUtil.hasText(title) ? unknown : title, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.source"), x, y, 0xC0C0C0C0);
            String source = entry.getSource();
            y = drawInfoText(drawContext, source == null ? unknown : source, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.author"), x, y, 0xC0C0C0C0);
            String author = entry.getAuthor();
            y = drawInfoText(drawContext, !StringUtil.hasText(author) ? unknown : author, x, y, 0xFFFFFFFF) + space;

            y = drawInfoText(drawContext, StringUtils.translate("downmatica.gui.info.description"), x, y, 0xC0C0C0C0);
            String description = entry.getDescription();
            drawInfoText(drawContext, !StringUtil.hasText(description) ? none : description, x, y, 0xFFFFFFFF);
        }

        //#if MC < 12111
        private int drawInfoText(GuiGraphics drawContext, String text, int x, int y, int color) {
        //#else
        //$$ private int drawInfoText(GuiContext drawContext, String text, int x, int y, int color) {
        //#endif
            final int width = infoWidth - 4;//4表示右侧留空
            for (String line : text.split("\n", -1)) {
                if (!StringUtil.hasText(line)) {
                    y += STRING_HEIGHT + 2;
                    continue;
                }
                while (StringUtil.hasText(line)) {
                    if (getStringWidth(line) <= width) {
                        drawString(drawContext, line, x, y, color);
                        y += STRING_HEIGHT + 2;
                        break;
                    }
                    int length = line.length();
                    while (getStringWidth(line.substring(0, length)) > width) {
                        length--;
                    }
                    drawString(drawContext, line.substring(0, length), x, y, color);
                    y += STRING_HEIGHT + 2;
                    line = line.substring(length);
                }
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

                String saveAsDisplay = StringUtils.translate("downmatica.gui.button.download.display");
                String detailsDisplay = StringUtils.translate("downmatica.gui.button.details.display");
                int verticalMargin = 1;
                int buttonWidth = getButtonWidget(saveAsDisplay, detailsDisplay);
                int buttonHeight = height - verticalMargin * 2;
                x += width - buttonWidth - 2;//2表示右边留空
                y += verticalMargin;

                SchematicDownloadInfo downloadFileInfo = entry.getDownloadFileInfo();
                ButtonGeneric saveAsButton = addButton(
                        new ButtonGeneric(x, y, buttonWidth, buttonHeight, saveAsDisplay),
                        (button, mouseButton) -> new Thread(() -> {
                            String selectedFolder = TinyFileDialogs.tinyfd_selectFolderDialog("downmatica.gui.button.download.massage.select_folder", "");
                            if (selectedFolder == null) {
                                DownloadGui.this.addMessage(Message.MessageType.INFO, "downmatica.gui.button.download.massage.cancel");
                                return;
                            }
                            if (downloadFileInfo == null) {
                                DownloadGui.this.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.download.massage.no_file");
                                return;
                            }
                            Path path = Paths.get(selectedFolder, downloadFileInfo.getLocalFileName());
                            if (Files.exists(path)) {
                                DownloadGui.this.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.download.massage.file_exists");
                                return;
                            }
                            try {
                                DownloadUtil.download(downloadFileInfo.getRemoteFileURI(), path);
                            } catch (Exception e) {
                                String message = e.getMessage();
                                DownloadGui.this.addMessage(
                                        Message.MessageType.ERROR,
                                        "downmatica.gui.button.download.massage.error",
                                        StringUtil.hasText(message) ? message : StringUtils.translate("downmatica.gui.text.none")
                                );
                                DownmaticaMod.LOGGER.error("Download failed", e);
                                return;
                            }
                            DownloadGui.this.addMessage(Message.MessageType.SUCCESS, "downmatica.gui.button.download.massage.success");
                        }).start()
                );
                if (downloadFileInfo == null) {
                    saveAsButton.setEnabled(false);
                } else if (!downloadFileInfo.isValid()) {
                    saveAsButton.setEnabled(false);
                    DownmaticaMod.LOGGER.warn(
                            "Configuration validation failed for schematic download source \"{}\" from \"{}\". The data is invalid, so the download button has been disabled. If you can confirm which extension or core mod this source belongs to, please report this issue to that source's developer.",
                            entry.getTitle(),
                            entry.getSource()
                    );
                }

                x -= buttonWidth + 2;//2表示按钮之间的间隔

                Runnable runnable = entry.onDetailButtonClicked();
                ButtonGeneric detailsButton = addButton(
                        new ButtonGeneric(x, y, buttonWidth, buttonHeight, detailsDisplay),
                        (button, mouseButton) -> {
                            if (runnable == null) {
                                DownloadGui.this.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.details.massage.open_web_page_failed");
                                return;
                            }
                            runnable.run();
                        }
                );
                if (runnable == null) {
                    detailsButton.setEnabled(false);
                }
            }

            public int getButtonWidget(String... texts){
                int widget = 0;
                for (String text : texts) {
                    widget = Math.max(widget, getStringWidth(text) + 10 * 2);//10表示按钮左右留空
                }
                return widget;
            }

            @Override
            //#if MC < 12106
            public void render(int mouseX, int mouseY, boolean selected, GuiGraphics drawContext) {
                if (selected || isMouseOver(mouseX, mouseY)) {
                    RenderUtils.drawRect(x, y, width, height, 0x70FFFFFF);
                } else {
                    RenderUtils.drawRect(x, y, width, height, isOdd ? 0x20FFFFFF : 0x50FFFFFF);
                }
                String text = StringUtils.translate("downmatica.gui.text.unknown");
                if (entry != null){
                    String string = entry.getTitle();
                    if (StringUtil.hasText(string)) {
                        text = string;
                    }
                }
                drawString(x + 20, y + (height - STRING_HEIGHT) / 2 , 0xFFFFFFFF, text, drawContext);//20表示左边留空
                super.render(mouseX, mouseY, selected, drawContext);
            }
            //#elseif MC < 12111
            //$$ public void render(GuiGraphics drawContext, int mouseX, int mouseY, boolean selected) {
            //$$     if (selected || isMouseOver(mouseX, mouseY)) {
            //$$         RenderUtils.drawRect(drawContext, x, y, width, height, 0x70FFFFFF);
            //$$     } else {
            //$$         RenderUtils.drawRect(drawContext, x, y, width, height, isOdd ? 0x20FFFFFF : 0x50FFFFFF);
            //$$     }
            //$$     String text = StringUtils.translate("downmatica.gui.text.unknown");
            //$$     if (entry != null){
            //$$         String string = entry.getTitle();
            //$$         if (StringUtil.hasText(string)) {
            //$$             text = string;
            //$$         }
            //$$     }
            //$$     drawString(drawContext, x + 20, y + (height - STRING_HEIGHT) / 2 , 0xFFFFFFFF, text);
            //$$     super.render(drawContext, mouseX, mouseY, selected);
            //$$ }
            //#else
            //$$ public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
            //$$     if (selected || isMouseOver(mouseX, mouseY)) {
            //$$         RenderUtils.drawRect(ctx, x, y, width, height, 0x70FFFFFF);
            //$$     } else {
            //$$         RenderUtils.drawRect(ctx, x, y, width, height, isOdd ? 0x20FFFFFF : 0x50FFFFFF);
            //$$     }
            //$$     String text = StringUtils.translate("downmatica.gui.text.unknown");
            //$$     if (entry != null){
            //$$         String string = entry.getTitle();
            //$$         if (StringUtil.hasText(string)) {
            //$$             text = string;
            //$$         }
            //$$     }
            //$$     drawString(ctx, x + 20, y + (height - STRING_HEIGHT) / 2 , 0xFFFFFFFF, text);
            //$$     super.render(ctx, mouseX, mouseY, selected);
            //$$ }
            //#endif
        }
    }
}
