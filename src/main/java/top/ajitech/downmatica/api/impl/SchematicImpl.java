package top.ajitech.downmatica.api.impl;

import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicDownloadInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SchematicImpl implements Schematic {
    private final String source;
    private final String title;
    private final String author;
    private final String description;
    private final SchematicDownloadInfo downloadFileInfo;
    private final Runnable runnable;

    public SchematicImpl(String source, String title, String author, String description, SchematicDownloadInfo downloadFileInfo, Runnable runnable) {
        this.source = source;
        this.title = title;
        this.author = author;
        this.description = description;
        this.downloadFileInfo = downloadFileInfo;
        this.runnable = runnable;
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
    public @Nullable SchematicDownloadInfo getDownloadFileInfo() {
        return downloadFileInfo;
    }

    @Override
    public @Nullable Runnable onDetailButtonClicked() {
        return runnable;
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
