package aji.downmatica.gui;

import aji.downmatica.api.Schematic;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;

public class DownloadGui extends GuiListBase<Schematic, DownloadWidgetListEntry, DownloadWidgetList> {
    private static final int HORIZONTAL_MARGIN = 12;
    private static final int TOP_MARGIN = 30;
    private static final int BOTTOM_MARGIN = 6;
    private static final int BUTTON_HORIZONTAL_MARGIN = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LIST_BUTTON_SPACING = 6;

    public DownloadGui(Screen parent) {
        super(HORIZONTAL_MARGIN, TOP_MARGIN);
        title = StringUtils.translate("downmatica.gui.title");
        setParent(parent);
    }

    @Override
    public void initGui() {
        super.initGui();

        final GuiMainMenu.ButtonListenerChangeMenu.ButtonType type = GuiMainMenu.ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        final String label = StringUtils.translate(type.getLabelKey());
        final int width = getStringWidth(label) + BUTTON_HORIZONTAL_MARGIN * 2;
        final int x = this.width - width - HORIZONTAL_MARGIN;
        final int y = height - BUTTON_HEIGHT - BOTTOM_MARGIN;
        addButton(
                new ButtonGeneric(x, y, width, BUTTON_HEIGHT, label),
                new GuiMainMenu.ButtonListenerChangeMenu(type, getParent())
        );
    }

    @Override
    protected DownloadWidgetList createListWidget(int listX, int listY) {
        DownloadWidgetList list = new DownloadWidgetList(listX, listY, getBrowserWidth(), getBrowserHeight(), this);
        list.loadEntries();
        return list;
    }

    @Override
    protected int getBrowserWidth() {
        return width - HORIZONTAL_MARGIN * 2;
    }

    @Override
    protected int getBrowserHeight() {
        return height - BOTTOM_MARGIN - TOP_MARGIN - BUTTON_HEIGHT - LIST_BUTTON_SPACING;
    }

    @Override
    public void addMessage(Message.MessageType type, String translationKey, Object... args) {
        if (mc.isSameThread()) {
            super.addMessage(type, translationKey, args);
        } else {
            mc.execute(() -> super.addMessage(type, translationKey, args));
        }
    }
}
