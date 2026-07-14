package top.ajitech.downmatica.core;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import top.ajitech.downmatica.Downmatica;

import java.util.ArrayList;

public class KeybindProvider implements IKeybindProvider{
    public static final KeybindProvider INSTANCE = new KeybindProvider();

    private KeybindProvider() {
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (ConfigHotkey configHotkey : getHotkeys()) {
            manager.addKeybindToMap(configHotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(Downmatica.MOD_NAME, "downmatica.keybinds.category.general", getHotkeys());
    }

    private ArrayList<ConfigHotkey> getHotkeys() {
        ArrayList<ConfigHotkey> hotkeys = new ArrayList<>();
        for (IConfigBase config : ConfigHandler.INSTANCE.configs) {
            if (config instanceof ConfigHotkey configHotkey) {
                hotkeys.add(configHotkey);
            }
        }
        return hotkeys;
    }
}
