package aji.downmatica.gui;

import aji.downmatica.entry.Schematic;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;

public class DownloadGui extends GuiListBase<Schematic, DownloadWidgetListEntry, DownloadWidgetList> {

    public DownloadGui(Screen parent) {
        super(12, 30);
        title = StringUtils.translate("downmatica.gui.title");
        setParent(parent);
    }

    @Override
    public void initGui() {
        super.initGui();
        final GuiMainMenu.ButtonListenerChangeMenu.ButtonType type = GuiMainMenu.ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        final String label = StringUtils.translate(type.getLabelKey());
        final int buttonWidth = getStringWidth(label) + 20;
        final int x = width - buttonWidth - 10;
        final int y = height - 26;
        final ButtonGeneric button = new ButtonGeneric(x, y, buttonWidth, 20, label);
        addButton(button, new GuiMainMenu.ButtonListenerChangeMenu(type, getParent()));
    }

    @Override
    protected DownloadWidgetList createListWidget(int listX, int listY) {
        return new DownloadWidgetList(listX, listY, getBrowserWidth(), getBrowserHeight(), this);
    }

    @Override
    protected int getBrowserWidth() {
        return width - 20;
    }

    @Override
    protected int getBrowserHeight() {
        return height - 68;
    }
}
