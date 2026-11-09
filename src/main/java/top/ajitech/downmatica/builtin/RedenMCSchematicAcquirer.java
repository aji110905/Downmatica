package top.ajitech.downmatica.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import top.ajitech.downmatica.DownmaticaMod;
import top.ajitech.downmatica.api.HttpClientGetter;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicAcquirer;
import top.ajitech.downmatica.api.SchematicDownloadInfo;
import top.ajitech.downmatica.util.CompatibleUtil;
import top.ajitech.downmatica.util.URIBuilder;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static top.ajitech.downmatica.util.JsonUtil.getString;
import static top.ajitech.downmatica.util.JsonUtil.hasNumber;

/*
 * Copyright (c) 2026 阿寂 / RedenMC.
 * All rights reserved.
 *
 * 此文件/类中与 RedenMC 相关的接口集成逻辑受单独的闭源授权保护。
 * 请勿将此文件中的特定逻辑提取用于其他项目。
 *
 * 此限制仅适用于本文件/类，Downmatica 项目的其余部分仍遵循原有的MIT开源协议。
 */
public class RedenMCSchematicAcquirer implements SchematicAcquirer {
    private static final String REDEN_BASE_URI = "https://api.redenmc.com/api/mc-services/";
    private static final String LIST_URI = REDEN_BASE_URI + "yisibite/";
    private static final String SEARCH_URI = REDEN_BASE_URI + "litematica/search";
    private static final String INFO_URI = REDEN_BASE_URI + "yisibite/%s/info/%s";
    private static final String DOWNLOAD_URI_TEMPLATE = REDEN_BASE_URI + "yisibite/%s/download/%s";
    private static final String DETAIL_URI_TEMPLATE = "https://redenmc.com/%s/litematica/%s";
    private static final int PAGE_SIZE = 24;
    private static final String SOURCE_NAME = "RedenMC";

    private String keyword = "";
    @Nullable
    private String probedKeyword = null;
    private int totalSchematic = 0;
    private int nextPage = 1;
    private int maxPages = 0;

    @Override
    public void setKeyword(String keyword) {
        this.keyword = keyword;
        probedKeyword = null;
        nextPage = 1;
        maxPages = 0;
    }

    @Override
    public int getTotalSchematic() {
        ensureProbed();
        return totalSchematic;
    }

    @Override
    public Collection<Schematic> getNext() {
        ensureProbed();
        if (nextPage > maxPages) {
            return List.of();
        }
        JsonObject object = request(nextPage, PAGE_SIZE);
        if (object == null) {
            maxPages = 0;
            return List.of();
        }
        nextPage++;
        ArrayList<Schematic> rel = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray("d")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject jsonObject = element.getAsJsonObject();
            String key = getString(jsonObject, "key");
            if (key == null) {
                continue;
            }
            rel.add(new RedenSchematic(jsonObject, key));
        }
        return rel;
    }

    @Override
    public boolean hasNext() {
        ensureProbed();
        return nextPage <= maxPages;
    }

    private void ensureProbed() {
        if (keyword.equals(probedKeyword)) {
            return;
        }
        JsonObject object = request(1, 1);
        if (object == null) {
            return;
        }
        int hits;
        if (!keyword.isEmpty() && hasNumber(object, "estimatedTotalHits")) {
            hits = object.get("estimatedTotalHits").getAsInt();
        } else if (hasNumber(object, "count")) {
            hits = object.get("count").getAsInt();
        } else {
            hits = 0;
        }
        totalSchematic = Math.max(0, hits);
        maxPages = (totalSchematic + PAGE_SIZE - 1) / PAGE_SIZE;
        probedKeyword = keyword;
    }

    @Nullable
    private JsonObject request(int page, int pageSize) {
        boolean search = !keyword.isEmpty();
        try {
            URIBuilder builder = URIBuilder.create(search ? SEARCH_URI : LIST_URI)
                    .addParam("page", String.valueOf(page))
                    .addParam("pageSize", String.valueOf(Math.min(pageSize, PAGE_SIZE)))
                    .addParam("lang", getRedenLanguageCode());
            if (search) {
                builder.addParam("q", keyword);
            }
            HttpRequest.Builder request = HttpRequest.newBuilder(builder.build());
            HttpResponse<String> response = HttpClientGetter.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                DownmaticaMod.LOGGER.warn("RedenMC {} request failed with status {} (page {}, keyword '{}'), body: {}", search ? "search" : "list", response.statusCode(), page, keyword, response.body());
                return null;
            }
            JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement array = object.get("d");
            if (array == null || !array.isJsonArray()) {
                DownmaticaMod.LOGGER.warn("RedenMC {} response has no 'd' array (page {}, keyword '{}'), body: {}", search ? "search" : "list", page, keyword, response.body());
                return null;
            }
            return object;
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get RedenMC {} page {} (keyword: '{}')", search ? "search" : "list", page, keyword, e);
            return null;
        }
    }

    private static String getRedenLanguageCode() {
        return switch (CompatibleUtil.getLanguageCode().toLowerCase()) {
            case "zh_cn" -> "zh_cn";
            case "zh_tw" -> "zh_tw";
            case "es_es" -> "es";
            case "ru_ru" -> "ru";
            default -> "en";
        };
    }

    /**
     * 为避免连续发起24个HTTP请求 单独实现Schematic 采用懒加载
     */
    private static class RedenSchematic implements Schematic {
        private final String title;
        private final String author;
        private final boolean canDownload;
        private final Runnable onDetailButtonClicked;
        private final String key;

        private String description = null;
        private List<SchematicDownloadInfo> downloadFileInfos = List.of();
        private boolean isLoaded = false;

        private RedenSchematic(JsonObject object, String key) {
            this.key = key;
            title = getString(object, "name");
            canDownload = "LitematicaShare".equals(getString(object, "type"));
            onDetailButtonClicked = () -> CompatibleUtil.openUri(URI.create(DETAIL_URI_TEMPLATE.formatted(RedenMCSchematicAcquirer.getRedenLanguageCode(), key)));
            JsonElement element = object.get("author");
            if (element == null || element.isJsonNull()) {
                author = null;
                return;
            }
            if (element.isJsonObject()) {
                author = getString(element.getAsJsonObject(), "username");
                return;
            }
            author = element.isJsonPrimitive() ? element.getAsString() : null;
        }

        @Override
        public String getSource() {
            return SOURCE_NAME;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getAuthor() {
            return author;
        }

        @Override
        public @Nullable String getDescription() {
            if (!isLoaded) {
                load();
            }
            return description;
        }

        @Override
        public Collection<SchematicDownloadInfo> getDownloadFileInfos() {
            if (!isLoaded) {
                load();
            }
            return downloadFileInfos;
        }

        @Override
        public boolean canDownload() {
            return canDownload;
        }

        @Override
        public Runnable onDetailButtonClicked() {
            return onDetailButtonClicked;
        }

        /**
         * 懒加载
         */
        private void load() {
            try {
                URI uri = URI.create(INFO_URI.formatted(key, RedenMCSchematicAcquirer.getRedenLanguageCode()));
                HttpResponse<String> response = HttpClientGetter.send(HttpRequest.newBuilder(uri), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    DownmaticaMod.LOGGER.warn("RedenMC info request failed with status {} (key '{}')", response.statusCode(), key);
                    return;
                }
                JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject().get("d").getAsJsonArray().get(0).getAsJsonObject();
                JsonElement element = object.get("description");
                if (element != null && element.isJsonPrimitive()) {
                    description = element.getAsString();
                }
                JsonElement attachments = object.get("attachments");
                if (canDownload() && attachments != null && attachments.isJsonArray()) {
                    downloadFileInfos = new ArrayList<>();
                    for (JsonElement jsonElement : attachments.getAsJsonArray()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        String string = jsonObject.get("url").getAsString();
                        downloadFileInfos.add(SchematicDownloadInfo.of(jsonObject.get("name").getAsString(), URI.create(DOWNLOAD_URI_TEMPLATE.formatted(key, string.substring(string.lastIndexOf('/') + 1)))));
                    }
                }
            } catch (Exception e) {
                DownmaticaMod.LOGGER.error("Failed to load RedenMC schematic", e);
            }
            isLoaded = true;
        }
    }
}
