package top.ajitech.downmatica.builtin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.ajitech.downmatica.DownmaticaMod;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicAcquirer;
import top.ajitech.downmatica.core.HttpClientContainer;
import top.ajitech.downmatica.util.CompatibleUtil;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;

public class OpenSTSchematicAcquirer implements SchematicAcquirer {
    @Override
    public Collection<Schematic> getSchematics() {
        ArrayList<Schematic> rel = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://raw.githubusercontent.com/OpenST-mc/website/main/archive/data/database.json")).build();
            HttpResponse<String> response = HttpClientContainer.INSTANCE.get().send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            array.forEach(element -> {
                JsonObject object = element.getAsJsonObject();
                String subId = object.get("sub_id").getAsString();
                Schematic schematic = Schematic.builder()
                        .source("OpenST")
                        .title(object.get("name").getAsString())
                        .author(object.get("author").getAsString())
                        .description(object.get("description").getAsString())
                        .onDetailButtonClicked(() -> CompatibleUtil.openUri(URI.create("https://openstmc.com/archive?" + subId)))
                        .downloadFileInfo(object.get("filename").getAsString(), URI.create("https://openstmc.com/api/download?id=" + subId))
                        .build();
                rel.add(schematic);
            });
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get OpenST schematics", e);
            return rel;
        }
        return rel;
    }
}
