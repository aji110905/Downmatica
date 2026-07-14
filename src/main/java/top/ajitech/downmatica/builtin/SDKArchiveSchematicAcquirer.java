package top.ajitech.downmatica.builtin;

import top.ajitech.downmatica.Downmatica;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicAcquirer;
import top.ajitech.downmatica.core.HttpClientContainer;
import top.ajitech.downmatica.util.CompatibleUtil;
import top.ajitech.downmatica.util.StringUtil;
import top.ajitech.downmatica.util.URIBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SDKArchiveSchematicAcquirer implements SchematicAcquirer {
    private static final String SDK_ARCHIVE_API_URL = "https://sdkarchive.com/api/";
    private static final String GET_USERS_URL = SDK_ARCHIVE_API_URL + "user/getUsers";
    private static final String GET_USER_SCHEMATICS_URL = SDK_ARCHIVE_API_URL + "schematics/geiSchematicsByPage";
    private static final String SDK_ARCHIVE_DETAIL_URL = "https://sdkarchive.com/schematics/detail/";

    @Override
    public Collection<Schematic> getSchematics() {
        ArrayList<Schematic> rel = new ArrayList<>();
        for (Map.Entry<String, String> entry : getAllUser().entrySet()) {
            String id = entry.getKey();
            rel.addAll(getUserSchematics(id, entry.getValue(), getUserSchematicsCount(id)));
        }
        return rel;
    }

    private HashMap<String, String> getAllUser() {
        try {
            URI uri = URIBuilder.create(GET_USERS_URL).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();
            HttpResponse<String> response = HttpClientContainer.INSTANCE.get().send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            HashMap<String, String> rel = new HashMap<>();
            for (JsonElement element : jsonObject.get("data").getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                rel.put(object.get("_id").getAsString(), object.get("username").getAsString());
            }
            return rel;
        } catch (Exception e) {
            Downmatica.LOGGER.error("Failed to get all user", e);
            return new HashMap<>();
        }
    }

    private ArrayList<Schematic> getUserSchematics(String userId, String userName, int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        try {
            URI uri = URIBuilder.create(GET_USER_SCHEMATICS_URL)
                    .addParam("page", "1")
                    .addParam("size", String.valueOf(count))
                    .addParam("auth", userId)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();
            HttpResponse<String> response = HttpClientContainer.INSTANCE.get().send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            ArrayList<Schematic> rel = new ArrayList<>();
            for (JsonElement element : jsonObject.get("data").getAsJsonArray()) {
                Schematic schematic = createSchematic(element, userName);
                if (schematic != null) {
                    rel.add(schematic);
                }
            }
            return rel;
        } catch (Exception e) {
            Downmatica.LOGGER.error("Failed to get user schematics", e);
            return new ArrayList<>();
        }
    }

    @Nullable
    private Schematic createSchematic(JsonElement jsonElement, String author) {
        try {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray jsonArray = jsonObject.get("file").getAsJsonArray();
            Schematic.Builder builder = Schematic.builder()
                    .source(StringUtils.translate("downmatica.source.sdk_archive"))
                    .title(jsonObject.get("title").getAsString())
                    .author(author)
                    .description(jsonObject.get("description").getAsString())
                    .onDetailButtonClicked(() -> CompatibleUtil.openUri(URI.create(SDK_ARCHIVE_DETAIL_URL + jsonObject.get("_id").getAsString())));
            if (!jsonArray.isEmpty()) {
                JsonObject object = jsonArray.get(0).getAsJsonObject();
                String name = object.get("name").getAsString();
                if (StringUtil.hasText(name)) {
                    builder.downloadFileInfo(name, URI.create(object.get("url").getAsString()));
                }
            }
            return builder.build();
        } catch (Exception e) {
            Downmatica.LOGGER.error("Failed to create schematic", e);
            return null;
        }
    }

    private int getUserSchematicsCount(String userId) {
        try {
            URI uri = URIBuilder.create(GET_USER_SCHEMATICS_URL)
                    .addParam("page", "1")
                    .addParam("size", "1")
                    .addParam("auth", userId)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();
            HttpResponse<String> response = HttpClientContainer.INSTANCE.get().send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonObject.get("total").getAsInt();
        } catch (Exception e) {
            Downmatica.LOGGER.error("Failed to get user schematics count", e);
            return -1;
        }
    }
}
