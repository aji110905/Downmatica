package top.ajitech.downmatica.core;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import top.ajitech.downmatica.Downmatica;
import top.ajitech.downmatica.gui.ConfigGui;
import top.ajitech.downmatica.gui.DownloadGui;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
        Path path = FileUtils.getConfigDirectoryAsPath().resolve(getConfigFileName());
        if (!(Files.exists(path) && Files.isReadable(path))){
            return;
        }
        JsonElement element = JsonUtils.parseJsonFileAsPath(path);
        if (element == null || !element.isJsonObject()) {
            Downmatica.LOGGER.error("Failed to load config.");
            return;
        }
        JsonObject object = element.getAsJsonObject();
        for (IConfigBase config : configs) {
            if (object.has(config.getName())) {
                config.setValueFromJsonElement(object.get(config.getName()));
            }
        }
    }

    @Override
    public void save() {
        JsonObject object = new JsonObject();
        for (IConfigBase option : configs) {
            object.add(option.getName(), option.getAsJsonElement());
        }
        Path path = FileUtils.getConfigDirectoryAsPath();
        if (!Files.exists(path)) {
            FileUtils.createDirectoriesIfMissing(path);
        }
        JsonUtils.writeJsonToFileAsPath(object, path.resolve(getConfigFileName()));
    }

    private String getConfigFileName(){
        return Downmatica.MOD_ID + ".json";
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
