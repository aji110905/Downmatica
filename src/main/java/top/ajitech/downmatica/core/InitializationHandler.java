package top.ajitech.downmatica.core;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import top.ajitech.downmatica.DownmaticaMod;

public class InitializationHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(DownmaticaMod.MOD_ID, ConfigHandler.INSTANCE);

        InputEventHandler.getKeybindManager().registerKeybindProvider(KeybindProvider.INSTANCE);
    }
}
