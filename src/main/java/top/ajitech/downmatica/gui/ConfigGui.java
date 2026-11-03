package top.ajitech.downmatica.gui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import top.ajitech.downmatica.DownmaticaMod;
import top.ajitech.downmatica.core.ConfigHandler;

import java.util.List;

public class ConfigGui extends GuiConfigsBase {
    public static final ConfigGui INSTANCE = new ConfigGui(null);

    public ConfigGui(Screen parent) {
        super(12, 62, DownmaticaMod.MOD_ID, null, "downmatica.gui.config.title");
        setParent(parent);
        useTitleHierarchy = false;
        title = DownmaticaMod.MOD_NAME + " => " + title;
    }

    @Override
    public void initGui() {
        super.initGui();

        int y = 30;
        int x = getListX();

        String downloadButtonDisplay = StringUtils.translate("downmatica.gui.common.button.download.display");
        int downloadButtonWidth = getStringWidth(downloadButtonDisplay) + 10 * 2;//10为按钮的左右留白
        addButton(
                new ButtonGeneric(x, y, downloadButtonWidth, 20, downloadButtonDisplay),
                (button, mouseButton) -> DownloadGui.open(getParent())
        );

        x += downloadButtonWidth + 4;//4表示按钮间的间隔
        String configButtonDisplay = StringUtils.translate("downmatica.gui.common.button.config.display");
        int configButtonWidth = getStringWidth(configButtonDisplay) + 10 * 2;
        addButton(new ButtonGeneric(x, y, configButtonWidth, 20, configButtonDisplay), null).setEnabled(false);
    }



    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(ConfigHandler.INSTANCE.configs);
    }

    public static void open(Screen parent) {
        INSTANCE.setParent(parent);
        GuiBase.openGui(INSTANCE);
    }
}
