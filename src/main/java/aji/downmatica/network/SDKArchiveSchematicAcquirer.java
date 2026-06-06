package aji.downmatica.network;

import aji.downmatica.DownmaticaMod;
import aji.downmatica.entry.FileInfo;
import aji.downmatica.entry.Schematic;
import aji.downmatica.entry.SchematicSources;
import aji.downmatica.util.URIBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SDKArchiveSchematicAcquirer implements SchematicAcquirer{
    private static final String SDK_ARCHIVE_API_URL = "https://sdkarchive.com/api/";
    private static final String GET_USERS_URL = SDK_ARCHIVE_API_URL + "user/getUsers";
    private static final String GET_USER_SCHEMATICS_URL = SDK_ARCHIVE_API_URL + "schematics/geiSchematicsByPage";
    private static final String SDK_ARCHIVE_DETAIL_URL = "https://sdkarchive.com/schematics/detail/";

    private final HttpClient client = HttpClient.newHttpClient();

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
        URI uri = URIBuilder.create(GET_USERS_URL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get all user", e);
            return new HashMap<>();
        }
        HashMap<String, String> rel = new HashMap<>();
        try {
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            for (JsonElement element : jsonObject.get("data").getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                rel.put(object.get("_id").getAsString(), object.get("username").getAsString());
            }
        } catch (Exception e){
            DownmaticaMod.LOGGER.error("Failed to get all user", e);
            return new HashMap<>();
        }
        return rel;
    }

    private ArrayList<Schematic> getUserSchematics(String userId, String userName, int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        URI uri = URIBuilder.create(GET_USER_SCHEMATICS_URL)
                .addParam("page", "1")
                .addParam("size", String.valueOf(count))
                .addParam("auth", userId)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get user schematics", e);
            return new ArrayList<>();
        }
        ArrayList<Schematic> rel = new ArrayList<>();
        try {
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            for (JsonElement element : jsonObject.get("data").getAsJsonArray()) {
                Schematic schematic = createSchematic(element, userName);
                if (schematic != null) {
                    rel.add(schematic);
                }
            }
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get user schematics", e);
            return new ArrayList<>();
        }
        return rel;
    }

    @Nullable
    private Schematic createSchematic(JsonElement jsonElement, String author) {
        try {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray file = jsonObject.get("file").getAsJsonArray();
            FileInfo fileInfo = null;
            if (!file.isEmpty()) {
                fileInfo = new FileInfo(
                        file.get(0).getAsJsonObject().get("name").getAsString(),
                        file.get(0).getAsJsonObject().get("url").getAsString()
                );
            }
            return new Schematic(
                    SchematicSources.SDK_ARCHIVE,
                    jsonObject.get("title").getAsString(),
                    author,
                    jsonObject.get("description").getAsString(),
                    fileInfo,
                    SDK_ARCHIVE_DETAIL_URL + jsonObject.get("_id").getAsString()
            );
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to create schematic", e);
            return null;
        }
    }

    private int getUserSchematicsCount(String userId) {
        URI uri = URIBuilder.create(GET_USER_SCHEMATICS_URL)
                .addParam("page", "1")
                .addParam("size", "1")
                .addParam("auth", userId)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get user schematics count", e);
            return -1;
        }
        try {
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonObject.get("total").getAsInt();
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get user schematics count", e);
            return -1;
        }
    }
}
