package top.ajitech.downmatica.api;

import top.ajitech.downmatica.core.HttpClientContainer;

import java.net.http.HttpClient;

public final class HttpClientGetter {
    private HttpClientGetter() {
    }

    /**
     * 获取受用户配置的HttpClient实例
     * <p>
     * 每次更改配置返回的HttpClient实例可能不同 因此不要缓存
     * @return HttpClient实例
     */
    public static HttpClient get() {
        return HttpClientContainer.INSTANCE.get();
    }
}
