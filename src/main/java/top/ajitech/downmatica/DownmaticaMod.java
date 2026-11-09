package top.ajitech.downmatica;

import com.google.gson.Gson;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.fabricmc.loader.api.FabricLoader;
import top.ajitech.downmatica.api.SchematicAcquirer;
import top.ajitech.downmatica.builtin.OpenSTSchematicAcquirer;
import top.ajitech.downmatica.builtin.RedenMCSchematicAcquirer;
import top.ajitech.downmatica.builtin.SDKArchiveSchematicAcquirer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ajitech.downmatica.gui.ConfigGui;

public class DownmaticaMod implements ModInitializer {
    public static final String MOD_ID = "downmatica";
    public static final String MOD_NAME = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getName();
    public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getVersion().toString();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Gson GSON = new Gson();

    @Override
    public void onInitialize() {
        InitializationHandler.getInstance().registerInitializationHandler(new top.ajitech.downmatica.core.InitializationHandler());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(
                MOD_ID, MOD_NAME, () -> ConfigGui.INSTANCE
        ));

        SchematicAcquirer.register(new SDKArchiveSchematicAcquirer());
        SchematicAcquirer.register(new OpenSTSchematicAcquirer());
        SchematicAcquirer.register(new RedenMCSchematicAcquirer());
    }
}
