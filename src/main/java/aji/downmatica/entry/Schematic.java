package aji.downmatica.entry;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record Schematic(
        @Nullable SchematicSource source,
        @Nullable String title,
        @Nullable String author,
        @Nullable String description,
        @Nullable FileInfo fileInfo,
        @Nullable String url
) {

    public List<String> getSearchStrings() {
        ArrayList<String> list = new ArrayList<>();
        if (source != null) {
            list.add(source.getName());
        }
        list.add(title);
        if (author != null) {
            list.add(author);
        }
        if (description != null) {
            list.add(description);
        }
        return list;
    }
}
