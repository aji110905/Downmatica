package top.ajitech.downmatica.gui;

//#if MC >= 260300
//$$ import com.sun.jna.Function;
//$$ import com.sun.jna.Pointer;
//#endif
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.gui.*;
//#if MC < 260300
import org.lwjgl.util.tinyfd.TinyFileDialogs;
//#else
//$$ import org.lwjgl.sdl.SDLDialog;
//$$ import org.lwjgl.sdl.SDL_DialogFileCallback;
//$$ import org.lwjgl.sdl.SDLProperties;
//$$ import org.lwjgl.sdl.SDLVideo;
//$$ import org.lwjgl.system.MemoryUtil;
//#endif
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
//#if MC >= 12111
//$$ import fi.dy.masa.malilib.render.GuiContext;
//#endif
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
//#if MC < 12111
import net.minecraft.client.gui.GuiGraphics;
//#endif
import org.jetbrains.annotations.Nullable;
import top.ajitech.downmatica.DownmaticaMod;
import top.ajitech.downmatica.api.HttpClientGetter;
import top.ajitech.downmatica.api.SchematicDownloadInfo;
import top.ajitech.downmatica.compatible.LitematicaCompatible;
import top.ajitech.downmatica.core.ConfigHandler;
import top.ajitech.downmatica.util.GuiUtil;
import top.ajitech.downmatica.util.Stopwatch;
import top.ajitech.downmatica.util.StringUtil;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
//#if MC >= 260300
//$$ import java.util.concurrent.CountDownLatch;
//$$ import java.util.concurrent.atomic.AtomicReference;
//#endif
import java.util.concurrent.TimeUnit;

public class DownloadGui extends GuiListBase<SchematicDownloadInfo, DownloadGui.WidgetList.Entry, DownloadGui.WidgetList> {
    private final Collection<SchematicDownloadInfo> downloadInfos;

    public DownloadGui(BrowserGui parent, Collection<SchematicDownloadInfo> downloadInfos) {
        super(12, 62);//12表示列表左边留空，62表示列表上方留空
        setParent(parent);
        this.downloadInfos = downloadInfos;
        useTitleHierarchy = false;
        title = StringUtils.translate("downmatica.gui.download.title");
    }

    @Override
    public void initGui() {
        super.initGui();

        int y = 30;
        int x = getListX();

        String backButtonDisplay = StringUtils.translate("downmatica.gui.download.button.back.display");
        int backButtonWidth = GuiUtil.getStringWidth(backButtonDisplay) + 10 * 2;//10为按钮的左右留白
        addButton(new ButtonGeneric(x, y, backButtonWidth, 20, backButtonDisplay),
                (button, mouseButton) -> GuiBase.openGui(getParent())
        );

        x += backButtonWidth + 4;//4表示按钮间的间隔

        String downloadAllButtonDisplay = StringUtils.translate("downmatica.gui.download.button.download_all.display");
        int downloadAllButtonWidth = GuiUtil.getStringWidth(downloadAllButtonDisplay) + 10 * 2;
        addButton(new ButtonGeneric(x, y, downloadAllButtonWidth, 20, downloadAllButtonDisplay),
                (button, mouseButton) -> Thread.ofVirtual().name("SchematicDownloader").start(() -> {
                    Path path = getDownloadDirectory();
                    if (path == null) {
                        return;
                    }
                    for (SchematicDownloadInfo downloadFileInfo : downloadInfos) {
                        download(downloadFileInfo, path);
                    }
                })
        );
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

    @Override
    public void addMessage(Message.MessageType type, String translationKey, Object... args) {
        if (mc.isSameThread()) {
            super.addMessage(type, translationKey, args);
        } else {
            mc.execute(() -> super.addMessage(type, translationKey, args));
        }
    }

    private void download(SchematicDownloadInfo downloadFileInfo, Path path) {
        if (!downloadFileInfo.isValid()) {
            addMessage(Message.MessageType.ERROR, "downmatica.gui.download.massage.invalid_file");
            return;
        }
        String localFileName = downloadFileInfo.getLocalFileName();
        Path file = path.resolve(localFileName);
        if (Files.exists(file) && !ConfigHandler.INSTANCE.downloadOverlayFile.getBooleanValue()) {
            addMessage(Message.MessageType.ERROR, "downmatica.gui.download.massage.file_exists", localFileName);
            return;
        }
        addMessage(Message.MessageType.INFO, "downmatica.gui.download.massage.start", localFileName, file);
        Stopwatch stopwatch = new Stopwatch();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            HttpRequest.Builder request = HttpRequest.newBuilder().uri(downloadFileInfo.getRemoteFileURI());
            HttpResponse<InputStream> response = HttpClientGetter.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream inputStream = response.body()) {
                Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            addMessage(Message.MessageType.ERROR, "downmatica.gui.download.massage.error", StringUtil.hasText(message) ? message : StringUtils.translate("downmatica.gui.browser.text.none"));
            DownmaticaMod.LOGGER.error("Download failed", e);
            return;
        }
        addMessage(Message.MessageType.SUCCESS, "downmatica.gui.download.massage.complete", localFileName, TimeUnit.NANOSECONDS.toMillis(stopwatch.getTime()));
    }

    @Nullable
    private Path getDownloadDirectory() {
        if (ConfigHandler.INSTANCE.downloadToSchematicBaseDirectory.getBooleanValue() && LitematicaCompatible.isLitematicaInstalled()) {
            //#if MC < 12105
            return DataManager.getSchematicsBaseDirectory().toPath();
            //#else
            //$$ return DataManager.getSchematicsBaseDirectory();
            //#endif
        }
        //#if MC < 260300
        String selectFolder = TinyFileDialogs.tinyfd_selectFolderDialog(StringUtils.translate("downmatica.gui.download.dialog.select_folder"), "");
        //#else
        //$$ CountDownLatch latch = new CountDownLatch(1);
        //$$ AtomicReference<String> rel = new AtomicReference<>(null);
        //$$ long windowHandle = mc.getWindow().handle();
        //$$ long hwnd = SDLProperties.SDL_GetPointerProperty(SDLVideo.SDL_GetWindowProperties(windowHandle), SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER, 0L);
        //$$ if (hwnd != 0L) {
        //$$     Function.getFunction("user32", "EnableWindow").invokeInt(new Object[]{ Pointer.createConstant(hwnd), 0 });
        //$$ }
        //$$ SDL_DialogFileCallback callback = SDL_DialogFileCallback.create((_, filelist, _) -> {
        //$$     if (hwnd != 0L) {
        //$$         Function.getFunction("user32", "EnableWindow").invokeInt(new Object[]{ Pointer.createConstant(hwnd), 1 });
        //$$     }
        //$$     if (filelist == 0L) {
        //$$         rel.set(null);
        //$$     } else {
        //$$         long pathPtr = MemoryUtil.memGetAddress(filelist);
        //$$         rel.set(pathPtr == 0L ? null : MemoryUtil.memUTF8(pathPtr));
        //$$     }
        //$$     latch.countDown();
        //$$ });
        //$$ try {
        //$$     mc.execute(() -> SDLDialog.SDL_ShowOpenFolderDialog(callback, windowHandle, 0L, "", false));
        //$$     latch.await();
        //$$ } catch (InterruptedException e) {
        //$$     Thread.currentThread().interrupt();
        //$$     if (hwnd != 0L) {
        //$$         Function.getFunction("user32", "EnableWindow").invokeInt(new Object[]{ Pointer.createConstant(hwnd), 1 });
        //$$     }
        //$$     rel.set(null);
        //$$ }
        //$$ mc.execute(callback::close);
        //$$ String selectFolder = rel.get();
        //#endif
        if (selectFolder == null) {
            addMessage(Message.MessageType.INFO, "downmatica.gui.download.massage.cancel");
            return null;
        }
        return Path.of(selectFolder);
    }

    public class WidgetList extends WidgetListBase<SchematicDownloadInfo, WidgetList.Entry> {
        private WidgetList(int x, int y, int width, int height) {
            super(x, y, width, height, null);
            browserEntryHeight = 22;
        }

        @Override
        public void setSize(int width, int height) {
            super.setSize(width, height);
            browserEntryWidth = width - 2 - 8 - 1 - 1;//2表示左边留空，8表示滚动条的宽度，1表示滚动条和实体间的间隔，1表示右边留空。
        }

        @Override
        protected Collection<SchematicDownloadInfo> getAllEntries() {
            return downloadInfos;
        }

        @Override
        protected Entry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, SchematicDownloadInfo entry) {
            return new Entry(x, y, browserEntryWidth, browserEntryHeight, entry, listIndex, isOdd);
        }

        @Override
        //#if MC < 12106
        public void drawContents(GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks) {
            RenderUtils.drawOutlinedBox(posX, posY, browserWidth, browserHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
            super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        }
        //#elseif MC < 12111
        //$$ public void drawContents(GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks) {
        //$$     RenderUtils.drawOutlinedBox(drawContext, posX, posY, browserWidth, browserHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
        //$$     super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        //$$ }
        //#else
        //$$ public void drawContents(GuiContext drawContext, int mouseX, int mouseY, float partialTicks) {
        //$$     RenderUtils.drawOutlinedBox(drawContext, posX, posY, browserWidth, browserHeight, GuiListBase.TOOLTIP_BACKGROUND, GuiBase.COLOR_HORIZONTAL_BAR);
        //$$     super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        //$$ }
        //#endif

        public class Entry extends WidgetListEntryBase<SchematicDownloadInfo> {
            private final boolean isOdd;

            private Entry(int x, int y, int width, int height, SchematicDownloadInfo entry, int listIndex, boolean isOdd) {
                super(x, y, width, height, entry, listIndex);
                this.isOdd = isOdd;
                int verticalMargin = 1;
                int buttonHeight = height - verticalMargin * 2;
                y += verticalMargin;

                String downloadButtonDisplay = StringUtils.translate("downmatica.gui.download.button.download.display");
                int downloadButtonWidth = GuiUtil.getStringWidth(downloadButtonDisplay) + 10 * 2;
                x += width - downloadButtonWidth - 2;//2表示右边留空
                addButton(
                        new ButtonGeneric(x, y, downloadButtonWidth, buttonHeight, downloadButtonDisplay),
                        (button, mouseButton) -> Thread.ofVirtual().name("SchematicDownloader").start(() -> {
                            Path path = getDownloadDirectory();
                            if (path == null) {
                                return;
                            }
                            DownloadGui.this.download(entry, path);
                        })
                );
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
                if (entry != null) {
                    String string = entry.getLocalFileName();
                    if (StringUtil.hasText(string)) {
                        text = string;
                    }
                }
                drawString(x + 20, y + (height - GuiUtil.getFontHeight()) / 2, 0xFFFFFFFF, text, drawContext);//20表示左边留空
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
            //$$     if (entry != null) {
            //$$         String string = entry.getLocalFileName();
            //$$         if (StringUtil.hasText(string)) {
            //$$             text = string;
            //$$         }
            //$$     }
            //$$     drawString(drawContext, x + 20, y + (height - GuiUtil.getFontHeight()) / 2, 0xFFFFFFFF, text);
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
            //$$     if (entry != null) {
            //$$         String string = entry.getLocalFileName();
            //$$         if (StringUtil.hasText(string)) {
            //$$             text = string;
            //$$         }
            //$$     }
            //$$     drawString(ctx, x + 20, y + (height - GuiUtil.getFontHeight()) / 2, 0xFFFFFFFF, text);
            //$$     super.render(ctx, mouseX, mouseY, selected);
            //$$ }
            //#endif
        }
    }
}
