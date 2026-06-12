package aji.downmatica.api.impl;

import aji.downmatica.api.Schematic;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SchematicImpl implements Schematic {
    private final String source;
    private final String title;
    private final String author;
    private final String description;
    private final URI downloadURI;
    private final URI webURI;

    public SchematicImpl(String source, String title, String author, String description, URI downloadURI, URI webURI) {
        this.source = source;
        this.title = title;
        this.author = author;
        this.description = description;
        this.downloadURI = downloadURI;
        this.webURI = webURI;
    }

    @Override
    public @Nullable String getSource() {
        return source;
    }

    @Override
    public @Nullable String getTitle() {
        return title;
    }

    @Override
    public @Nullable String getAuthor() {
        return author;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public @Nullable URI getDownloadURI() {
        return downloadURI;
    }

    @Override
    public @Nullable URI getWebURI() {
        return webURI;
    }

    @Override
    public List<String> getSearchStrings() {
        ArrayList<String> list = new ArrayList<>();
        if (source != null) {
            list.add(source);
        }
        if (title != null) {
            list.add(title);
        }
        if (author != null) {
            list.add(author);
        }
        if (description != null) {
            list.add(description);
        }
        return list;
    }
}
