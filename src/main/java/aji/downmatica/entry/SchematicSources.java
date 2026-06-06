package aji.downmatica.entry;

import fi.dy.masa.malilib.util.StringUtils;

public enum SchematicSources implements SchematicSource {
    SDK_ARCHIVE("sdk_archive");

    private final String name;

    SchematicSources(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return StringUtils.translate(String.format("downmatica.source.%s", name));
    }
}