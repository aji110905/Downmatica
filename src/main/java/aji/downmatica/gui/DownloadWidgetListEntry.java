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
    private static final int HORIZONTAL_MARGIN = 20;
    private static final int VERTICAL_MARGIN  = 1;
    private static final int BUTTON_GAP = 2;
    private static final int BUTTON_HORIZONTAL_MARGIN = 15;
    private static final int STRING_HEIGHT = 8;

    private final boolean isOdd;

    public DownloadWidgetListEntry(int x, int y, int width, int height, Schematic entry, int listIndex, DownloadGui gui) {
        super(x, y, width, height, entry, listIndex);
        this.isOdd = listIndex % 2 == 1;

        String url = getUrl(entry);
        FileInfo fileInfo = getFileInfo(entry);

        String saveAsDisplay = StringUtils.translate("downmatica.gui.button.download.display");
        String detailsDisplay = StringUtils.translate("downmatica.gui.button.details.display");

        int buttonWidth = getButtonWidget(saveAsDisplay, detailsDisplay);
        int buttonHeight = height - VERTICAL_MARGIN * 2;
        y += VERTICAL_MARGIN;
        x += width - buttonWidth - BUTTON_GAP;

        ButtonGeneric saveAsButton = addButton(
                new ButtonGeneric(x, y, buttonWidth, buttonHeight, saveAsDisplay),
                (button, mouseButton) -> new Thread(() -> {
                    try {
                        String selectedFolder = TinyFileDialogs.tinyfd_selectFolderDialog("downmatica.gui.button.download.massage.select_folder", "");
                        if (selectedFolder == null) {
                            gui.addMessage(Message.MessageType.INFO, "downmatica.gui.button.download.massage.cancel");
                            return;
                        }
                        if (fileInfo == null) {
                            gui.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.download.massage.no_file");
                            return;
                        }
                        String fileName = fileInfo.name();
                        String fileUrl = fileInfo.url();
                        if (fileName == null) {
                            String[] split = fileUrl.split("/");
                            fileName = split[split.length - 1];
                        }
                        DownloadUtil.download(fileUrl, selectedFolder + File.separator + fileName);
                        gui.addMessage(Message.MessageType.SUCCESS, "downmatica.gui.button.download.massage.success");
                    } catch (Exception e) {
                        gui.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.download.massage.error", e.getMessage());
                        DownmaticaMod.LOGGER.error("Download failed", e);
                    }
                }).start()
        );
        if (fileInfo == null) {
            saveAsButton.setEnabled(false);
        }

        x -= buttonWidth + BUTTON_GAP;

        ButtonGeneric detailsButton = addButton(
                new ButtonGeneric(x, y, buttonWidth, buttonHeight, detailsDisplay),
                (button, mouseButton) -> {
                    if (url == null) {
                        gui.addMessage(Message.MessageType.ERROR, "downmatica.gui.button.details.massage.open_web_page_failed");
                        return;
                    }
                    Util.getPlatform().openUri(URI.create(url));
                }
        );
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

    public int getButtonWidget(String... texts){
        int widget = 0;
        for (String text : texts) {
            widget = Math.max(widget, getStringWidth(text) + BUTTON_HORIZONTAL_MARGIN);
        }
        return widget;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, GuiGraphics drawContext) {
        if (selected || isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(x, y, width, height, 0x70FFFFFF);
        } else {
            RenderUtils.drawRect(x, y, width, height, isOdd ? 0x20FFFFFF : 0x50FFFFFF);
        }
        String text = StringUtils.translate("downmatica.gui.text.unknown");
        if (entry != null){
            String string = entry.title();
            if (StringUtil.hasText(string)) {
                text = string;
            }
        }
        drawString(x + HORIZONTAL_MARGIN, y + (height - STRING_HEIGHT) / 2 , 0xFFFFFFFF, text, drawContext);
        super.render(mouseX, mouseY, selected, drawContext);
    }
}
