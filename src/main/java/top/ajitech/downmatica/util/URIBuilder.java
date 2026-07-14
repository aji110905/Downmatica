package top.ajitech.downmatica.util;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class URIBuilder {
    private final StringBuilder builder;
    private boolean hasParams = false;

    private URIBuilder(String base) {
        builder = new StringBuilder(base);
    }

    public synchronized URIBuilder addParam(String key, String value) {
        if (!hasParams) {
            builder.append('?');
            hasParams = true;
        } else {
            builder.append('&');
        }
        builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        return this;
    }

    public synchronized URI build() {
        return URI.create(builder.toString());
    }

    public static URIBuilder create(String base) {
        return new URIBuilder(base);
    }
}