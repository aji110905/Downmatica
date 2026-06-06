package aji.downmatica.util;

import java.net.URI;

public class URIBuilder {
    private final StringBuilder builder;
    private boolean hasParams = false;

    private URIBuilder(String base) {
        builder = new StringBuilder(base);
    }

    public URIBuilder addParam(String key, String value) {
        if (!hasParams) {
            builder.append('?');
            hasParams = true;
        } else {
            builder.append('&');
        }
        builder.append(key).append('=').append(value);
        return this;
    }

    public URI build() {
        return URI.create(builder.toString());
    }

    public static URIBuilder create(String base) {
        return new URIBuilder(base);
    }
}
