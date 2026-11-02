package top.ajitech.downmatica.api;

import top.ajitech.downmatica.core.ConfigHandler;
import top.ajitech.downmatica.core.HttpClientContainer;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class HttpClientGetter {
    private HttpClientGetter() {
    }

    public static <T> HttpResponse<T> send(HttpRequest.Builder request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        return HttpClientContainer.INSTANCE.get().send(request.header("User-Agent", ConfigHandler.INSTANCE.userAgent.getStringValue()).build(), responseBodyHandler);
    }
}
