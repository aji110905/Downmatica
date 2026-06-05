package aji.downmatica.gui;

import aji.downmatica.DownmaticaMod;
import aji.downmatica.entry.FileInfo;
import aji.downmatica.entry.Schematic;
import aji.downmatica.util.DownloadUtil;
import aji.downmatica.util.StringUtil;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.net.URI;
import java.util.List;

public class DownloadWidgetListEntry extends WidgetListEntryBase<Schematic> {
    private final DownloadGui gui;
    private final boolean isOdd;

    public DownloadWidgetListEntry(int x, int y, int width, int height, Schematic entry, int listIndex, DownloadGui gui) {
        super(x, y, width, height, entry, listIndex);
        this.gui = gui;
        this.isOdd = listIndex % 2 == 1;

        String url = getUrl(entry);
        FileInfo fileInfo = getFileInfo(entry);

        int buttonWidth = getButtonWidget(List.of(
                StringUtils.translate("downmatica.gui.button.details.display"),
                StringUtils.translate("downmatica.gui.button.download.display")
        ));
        int buttonHeight = height - 2;
        int posY = y + 1;
        int posX = x + width - buttonWidth;

        ButtonGeneric saveAsButton = createSaveAsButton(posX, posY, buttonWidth, buttonHeight);
        posX -= buttonWidth + 2;
        ButtonGeneric detailsButton = createDetailsButton(posX, posY, buttonWidth, buttonHeight, url);

        if (fileInfo == null) {
            saveAsButton.setEnabled(false);
        }
        if (url == null) {
            detailsButton.setEnabled(false);
        }
    }

    @Nullable
    private String getUrl(Schematic entry) {
        return entry != null ? entry.url() : null;
    }

    @Nullable
    private FileInfo getFileInfo(Schematic entry) {
        return entry != null ? entry.fileInfo() : null;
    }

    private ButtonGeneric createSaveAsButton(int x, int y, int width, int height) {
        String saveAs = StringUtils.translate("downmatica.gui.button.download.display");
        return addButton(
                new ButtonGeneric(x, y, width, height, saveAs),
                (button, mouseButton) -> new Thread(() -> {
                    try {
                        String selectedFolder = TinyFileDialogs.tinyfd_selectFolderDialog("downmatica.gui.button.download.massage.select_folder", "");
                        if (selectedFolder == null) {
                            gui.addMessage(Message.MessageType.INFO, "downmatica.gui.button.download.massage.cancel");
                            return;
                        }
                        FileInfo info = getFileInfo(entry);
                        if (info == null) {
                            gui.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.download.massage.no_file");
                            return;
                        }
                        String fileName = info.name();
                        String url = info.url();
                        if (fileName == null) {
                            String[] split = url.split("/");
                            fileName = split[split.length - 1];
                        }
                        DownloadUtil.download(url, selectedFolder + File.separator + fileName);
                        gui.addMessage(Message.MessageType.SUCCESS, "downmatica.gui.button.download.massage.success");
                    } catch (Exception e) {
                        gui.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.download.massage.error", e.getMessage());
                        DownmaticaMod.LOGGER.error("Download failed", e);
                    }
                }).start()
        );
    }

    private ButtonGeneric createDetailsButton(int x, int y, int width, int height, String url) {
        String materialGathering = StringUtils.translate("downmatica.gui.button.details.display");
        return addButton(
                new ButtonGeneric(x, y, width, height, materialGathering),
                (button, mouseButton) -> {
                    if (url == null) {
                        gui.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.details.massage.open_web_page_failed");
                        return;
                    }
                    Util.getPlatform().openUri(URI.create(url));
                }
        );
    }

    public int getButtonWidget(List<String> texts){
        int widget = 0;
        for (String text : texts) {
            widget = Math.max(widget, getStringWidth(text) + 20);
        }
        return widget;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, GuiGraphics drawContext) {
        if (selected || isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(x, y, width, height, 0x70FFFFFF);
        } else if (isOdd) {
            RenderUtils.drawRect(x, y, width, height, 0x20FFFFFF);
        } else {
            RenderUtils.drawRect(x, y, width, height, 0x50FFFFFF);
        }
        String text = StringUtils.translate("未知");
        if (entry != null){
            String string = entry.title();
            if (StringUtil.hasText(string)) {
                text = string;
            }
        }
        drawString(x + 20, y + 7, 0xFFFFFFFF, text, drawContext);
        super.render(mouseX, mouseY, selected, drawContext);
    }
}
