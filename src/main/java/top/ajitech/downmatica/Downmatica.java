package top.ajitech.downmatica;

import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.fabricmc.loader.api.FabricLoader;
import top.ajitech.downmatica.api.SchematicAcquirer;
import top.ajitech.downmatica.builtin.SDKArchiveSchematicAcquirer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ajitech.downmatica.gui.ConfigGui;

public class Downmatica implements ModInitializer {
    public static final String MOD_ID = "downmatica";
    public static final String MOD_NAME = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getName();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        InitializationHandler.getInstance().registerInitializationHandler(new top.ajitech.downmatica.core.InitializationHandler());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(
                Downmatica.MOD_ID, Downmatica.MOD_NAME, () -> new ConfigGui(null, null)
        ));

        SchematicAcquirer.register(new SDKArchiveSchematicAcquirer());
    }
}
