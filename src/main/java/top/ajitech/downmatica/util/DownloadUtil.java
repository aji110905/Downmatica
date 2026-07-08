package top.ajitech.downmatica.util;

import top.ajitech.downmatica.DownmaticaMod;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class DownloadUtil {
    private DownloadUtil() {

    }

    public static void download(String sourceUrl, String targetPath) throws IOException, URISyntaxException, InterruptedException {
        download(sourceUrl, Paths.get(targetPath));
    }

    public static void download(String sourceUrl, Path targetPath) throws IOException, URISyntaxException, InterruptedException {
        download(new URI(sourceUrl), targetPath);
    }

    public static void download(String sourceUrl, File targetFile) throws IOException, URISyntaxException, InterruptedException {
        download(sourceUrl, targetFile.toPath());
    }

    public static void download(URI sourceUri, String targetPath) throws IOException, InterruptedException {
        download(sourceUri, Paths.get(targetPath));
    }

    public static void download(URI sourceUri, Path targetPath) throws IOException, InterruptedException {
        Path parentDir = targetPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(sourceUri)
                .build();
        HttpResponse<InputStream> response = DownmaticaMod.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void download(URI sourceUri, File targetFile) throws IOException, InterruptedException {
        download(sourceUri, targetFile.toPath());
    }

    public static void download(URL sourceUrl, String targetPath) throws IOException, InterruptedException {
        download(sourceUrl, Paths.get(targetPath));
    }

    public static void download(URL sourceUrl, Path targetPath) throws IOException, InterruptedException {
        download(URI.create(sourceUrl.toString()), targetPath);
    }

    public static void download(URL sourceUrl, File targetFile) throws IOException, InterruptedException {
        download(sourceUrl, targetFile.toPath());
    }
}