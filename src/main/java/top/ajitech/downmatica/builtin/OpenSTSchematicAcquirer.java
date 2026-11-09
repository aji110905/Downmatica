package top.ajitech.downmatica.builtin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.ajitech.downmatica.DownmaticaMod;
import top.ajitech.downmatica.api.HttpClientGetter;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.impl.CachedListSchematicAcquirer;
import top.ajitech.downmatica.util.CompatibleUtil;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;

public class OpenSTSchematicAcquirer extends CachedListSchematicAcquirer {

    private static final String OPEN_ST_DATABASE_URL = "https://raw.githubusercontent.com/OpenST-mc/website/main/archive/data/database.json";
    private static final String OPEN_ST_BASE_URL = "https://openstmc.com/";
    private static final String OPEN_ST_DETAIL_URL_PREFIX = OPEN_ST_BASE_URL + "archive?";
    private static final String OPEN_ST_DOWNLOAD_URL_PREFIX = OPEN_ST_BASE_URL + "api/download?id=";

    @Override
    protected Collection<Schematic> getSchematics() {
        ArrayList<Schematic> rel = new ArrayList<>();
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(OPEN_ST_DATABASE_URL));
            HttpResponse<String> response = HttpClientGetter.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            array.forEach(element -> {
                JsonObject object = element.getAsJsonObject();
                String subId = object.get("sub_id").getAsString();
                Schematic schematic = Schematic.builder()
                        .source("OpenST")
                        .title(object.get("name").getAsString())
                        .author(object.get("author").getAsString())
                        .description(object.get("description").getAsString())
                        .onDetailButtonClicked(() -> CompatibleUtil.openUri(URI.create(OPEN_ST_DETAIL_URL_PREFIX + subId)))
                        .downloadFileInfo(
                                object.get("filename").getAsString(),
                                URI.create(OPEN_ST_DOWNLOAD_URL_PREFIX + subId)
                        )
                        .build();
                rel.add(schematic);
            });
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get OpenST schematics", e);
        }
        return rel;
    }
}
