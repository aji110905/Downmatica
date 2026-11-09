package top.ajitech.downmatica.core;

import top.ajitech.downmatica.api.Schematic;

import java.util.ArrayList;

public class Paged {
    private final ArrayList<Schematic> entries;
    private final int totalPages;

    private Paged(ArrayList<Schematic> entries, int totalPages) {
        this.entries = entries;
        this.totalPages = totalPages;
    }

    public ArrayList<Schematic> getEntries() {
        return entries;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public static Paged of(ArrayList<Schematic> entries, int totalPages) {
        return new Paged(entries, totalPages);
    }
}
