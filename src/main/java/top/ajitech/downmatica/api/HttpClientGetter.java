package top.ajitech.downmatica.api;

import top.ajitech.downmatica.core.ConfigHandler;
import top.ajitech.downmatica.core.HttpClientContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class HttpClientGetter {
    private HttpClientGetter() {
    }

    public static <T> HttpResponse<T> send(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        HttpClient client = HttpClientContainer.INSTANCE.get();
        String userAgent = ConfigHandler.INSTANCE.userAgent.getStringValue();
        HttpRequest request = builder
                .header("User-Agent", userAgent)
                .build();
        while (true) {
            HttpResponse<T> response = client.send(request, responseBodyHandler);
            int statusCode = response.statusCode();
            if (statusCode != 301 && statusCode != 302 && statusCode != 303 && statusCode != 307 && statusCode != 308) {
                return response;
            }
            String location = response.headers().firstValue("Location").orElse(null);
            if (location == null || location.isEmpty()) {
                return response;
            }
            URI uri = request.uri();
            request = HttpRequest.newBuilder(uri.resolve(location)).header("Referer", uri.toString()).build();
        }
    }
}
