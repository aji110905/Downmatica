package top.ajitech.downmatica.core;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.loader.api.FabricLoader;
import top.ajitech.downmatica.Downmatica;
import top.ajitech.downmatica.gui.ConfigGui;
import top.ajitech.downmatica.gui.DownloadGui;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

public class ConfigHandler implements IConfigHandler {
    public static final ConfigHandler INSTANCE = new ConfigHandler();

    public final ImmutableList<IConfigBase> configs;

    public final ConfigHotkey openDownloadInterface = new ConfigHotkey("openDownloadInterface", "");
    public final ConfigHotkey openConfigInterface = new ConfigHotkey("openConfigInterface", "D,C");

    public final ConfigBoolean litematicaMainMenuButton = new ConfigBoolean("litematicaMainMenuButton", true);
    public final ConfigBoolean downloadToSchematicBaseDirectory = new ConfigBoolean("downloadToSchematicBaseDirectory", false);
    public final ConfigBoolean downloadOverlayFile = new ConfigBoolean("downloadOverlayFile", false);
    public final ConfigInteger totalTimeout = new ConfigInteger("totalTimeout", 20000, -1, Integer.MAX_VALUE);

    public final ConfigInteger httpTimeout = new ConfigInteger("httpTimeout", 5000, -1, Integer.MAX_VALUE);
    public final ConfigBoolean enableHttp2 = new ConfigBoolean("enableHttp2", false);
    public final ConfigOptionList redirectPolicy = new ConfigOptionList("redirectPolicy", RedirectPolicyConfigOptionListEntry.NORMAL);

    private ConfigHandler(){
        ArrayList<IConfigBase> configs = new ArrayList<>();
        for (Field field : getClass().getFields()) {
            field.setAccessible(true);
            try {
                if (field.get(this) instanceof IConfigBase config) {
                    if (config instanceof ConfigBase<?> configBase) {
                        configBase.apply(Downmatica.MOD_ID + ".config");
                    }
                    configs.add(config);
                }
            } catch (Exception e) {
                Downmatica.LOGGER.error("Failed to get config option.", e);
            }
        }
        this.configs = ImmutableList.copyOf(configs);

        openConfigInterface.getKeybind().setCallback((keyAction, keybind) -> {
            GuiBase.openGui(new ConfigGui(GuiUtils.getCurrentScreen(), null));
            return true;
        });
        openDownloadInterface.getKeybind().setCallback((keyAction, keybind) -> {
            GuiBase.openGui(new DownloadGui(GuiUtils.getCurrentScreen(), null));
            return true;
        });

        httpTimeout.setValueChangeCallback((config) -> HttpClientContainer.INSTANCE.setTimeout(config.getIntegerValue()));
        enableHttp2.setValueChangeCallback((config) -> HttpClientContainer.INSTANCE.setEnableHttp2(config.getBooleanValue()));
        redirectPolicy.setValueChangeCallback((config) -> HttpClientContainer.INSTANCE.setRedirectPolicy(((ConfigHandler.RedirectPolicyConfigOptionListEntry) ConfigHandler.INSTANCE.redirectPolicy.getOptionListValue()).getRedirectPolicy())
);
    }

    @Override
    public void load() {
        Path path = getConfigDirectory().resolve(getConfigFileName());
        if (!(Files.exists(path) && Files.isReadable(path))){
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            for (IConfigBase config : configs) {
                if (object.has(config.getName())) {
                    config.setValueFromJsonElement(object.get(config.getName()));
                }
            }
        } catch (Exception e) {
            Downmatica.LOGGER.error("Failed to load config.", e);
        }
    }

    @Override
    public void save() {
        JsonObject object = new JsonObject();
        for (IConfigBase option : configs) {
            object.add(option.getName(), option.getAsJsonElement());
        }
        Path dirPath = getConfigDirectory();
        if (!Files.exists(dirPath)) {
            FileUtils.createDirectoriesIfMissing(dirPath);
        }
        Path finalPath = dirPath.resolve(getConfigFileName());
        Path tempPath = Path.of(finalPath + ".tmp");
        if (Files.exists(tempPath)) {
            tempPath = Path.of(finalPath.toString() + UUID.randomUUID() + ".tmp");
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(tempPath), StandardCharsets.UTF_8)) {
            writer.write(Downmatica.GSON.toJson(object));
            writer.close();
            if (Files.exists(finalPath)){
                Files.delete(finalPath);
            }
            Files.move(tempPath, finalPath);
        } catch (Exception e) {
            Downmatica.LOGGER.error("Failed to save config.", e);
        }
    }

    private String getConfigFileName(){
        return Downmatica.MOD_ID + ".json";
    }

    private Path getConfigDirectory(){
        return FabricLoader.getInstance().getConfigDir();
    }

    enum RedirectPolicyConfigOptionListEntry implements IConfigOptionListEntry {
        NEVER("never", "downmatica.config.option.redirectPolicy.never", HttpClient.Redirect.NEVER),
        ALWAYS("always", "downmatica.config.option.redirectPolicy.always", HttpClient.Redirect.ALWAYS),
        NORMAL("normal", "downmatica.config.option.redirectPolicy.normal", HttpClient.Redirect.NORMAL);

        private final String name;
        private final String translationKey;
        private final HttpClient.Redirect redirectPolicy;

        RedirectPolicyConfigOptionListEntry(String name, String translationKey, HttpClient.Redirect redirectPolicy){
            this.name = name;
            this.translationKey = translationKey;
            this.redirectPolicy = redirectPolicy;
        }

        @Override
        public String getStringValue() {
            return name;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(translationKey);
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id % values().length];
        }

        @Override
        public IConfigOptionListEntry fromString(String value) {
            for (RedirectPolicyConfigOptionListEntry redirectPolicyConfigOptionListEntry : RedirectPolicyConfigOptionListEntry.values()) {
                if (redirectPolicyConfigOptionListEntry.name.equals(value)) {
                    return redirectPolicyConfigOptionListEntry;
                }
            }
            return NORMAL;
        }

        public HttpClient.Redirect getRedirectPolicy() {
            return redirectPolicy;
        }
    }
}
