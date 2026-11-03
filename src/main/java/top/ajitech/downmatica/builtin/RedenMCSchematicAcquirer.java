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
    private static final String LIST_URI = REDEN_BASE_URI + "yisibite";
    private static final String INFO_URI = REDEN_BASE_URI + "yisibite/%s/info/%s";
    private static final String DOWNLOAD_URI_TEMPLATE = REDEN_BASE_URI + "yisibite/%s/download/%s";
    private static final String DETAIL_URI_TEMPLATE = "https://redenmc.com/%s/litematica/%s";
    private static final int MAX_PAGE_SIZE = 24;
    private static final String SOURCE_NAME = "RedenMC";

    @Override
    public Collection<Schematic> getSchematics() {
        ArrayList<Schematic> rel = new ArrayList<>();
        int pageCount = getPageCount();
        for (int i = 1; i <= pageCount; i++) {
            rel.addAll(getPageSchematics(i));
            try {
                //避免429 但仍然可能
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return rel;
    }

    private int getPageCount() {
        try {
            URI uri = URIBuilder.create(LIST_URI).addParam("pageSize", "1").build();
            HttpRequest.Builder request = HttpRequest.newBuilder(uri);
            HttpResponse<String> response = HttpClientGetter.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
            return (object.get("count").getAsInt() + MAX_PAGE_SIZE - 1) / MAX_PAGE_SIZE;
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get Reden page count", e);
        }
        return 0;
    }

    private ArrayList<Schematic> getPageSchematics(int page) {
        ArrayList<Schematic> rel = new ArrayList<>();
        try {
            URI uri = URIBuilder.create(LIST_URI)
                    .addParam("page", String.valueOf(page))
                    .addParam("pageSize", String.valueOf(MAX_PAGE_SIZE))
                    .addParam("lang", getRedenLanguageCode())
                    .build();
            HttpRequest.Builder request = HttpRequest.newBuilder(uri);
            HttpResponse<String> response = HttpClientGetter.send(request, HttpResponse.BodyHandlers.ofString());
            for (JsonElement element : JsonParser.parseString(response.body()).getAsJsonObject().get("d").getAsJsonArray()) {
                rel.add(new RedenSchematic(element.getAsJsonObject()));
            }
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get Reden page {}", page, e);
        }
        return rel;
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
     * 为避免连续发起1000多个HTTP请求 单独实现Schematic 采用懒加载
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

        public RedenSchematic(JsonObject object) {
            title = object.get("name").getAsString();
            author = object.get("author").getAsJsonObject().get("username").getAsString();
            key = object.get("key").getAsString();
            canDownload = object.get("type").getAsString().equals("LitematicaShare");
            onDetailButtonClicked = () -> CompatibleUtil.openUri(URI.create(DETAIL_URI_TEMPLATE.formatted(RedenMCSchematicAcquirer.getRedenLanguageCode(), key)));
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

        @Override
        public List<String> getSearchStrings() {
            ArrayList<String> rel = new ArrayList<>();
            rel.add(getSource());
            rel.add(getTitle());
            rel.add(getAuthor());
            if (isLoaded) {
                rel.add(getDescription());
            }
            return rel;
        }

        /**
         * 懒加载
         */
        private void load() {
            try {
                URI uri = URIBuilder.create(INFO_URI.formatted(key, RedenMCSchematicAcquirer.getRedenLanguageCode())).build();
                HttpResponse<String> response = HttpClientGetter.send(HttpRequest.newBuilder(uri), HttpResponse.BodyHandlers.ofString());
                JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject().get("d").getAsJsonArray().get(0).getAsJsonObject();
                JsonElement element = object.get("description");
                if (element.isJsonPrimitive()) {
                    description = element.getAsString();
                }
                if (canDownload()) {
                    downloadFileInfos = new ArrayList<>();
                    for (JsonElement jsonElement : object.get("attachments").getAsJsonArray()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        String string = jsonObject.get("url").getAsString();
                        downloadFileInfos.add(SchematicDownloadInfo.of(jsonObject.get("name").getAsString(), URI.create(DOWNLOAD_URI_TEMPLATE.formatted(key, string.substring(string.lastIndexOf('/') + 1)))));
                    }
                }
            } catch (Exception e) {
                DownmaticaMod.LOGGER.error("Failed to load Reden schematic", e);
            }
            isLoaded = true;
        }
    }
}