package top.ajitech.downmatica.core;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import top.ajitech.downmatica.Downmatica;

public class InitializationHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(Downmatica.MOD_ID, ConfigHandler.INSTANCE);

        InputEventHandler.getKeybindManager().registerKeybindProvider(KeybindProvider.INSTANCE);
    }
}
