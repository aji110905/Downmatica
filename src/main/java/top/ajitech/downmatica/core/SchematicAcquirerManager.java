package top.ajitech.downmatica.core;

import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicAcquirer;

import java.util.ArrayList;
import java.util.Collection;

public class SchematicAcquirerManager {
    private static final int PAGE_SIZE = 20;
    public static final SchematicAcquirerManager INSTANCE = new SchematicAcquirerManager();

    private final ArrayList<SchematicAcquirer> acquirers = new ArrayList<>();
    private final ArrayList<Schematic> schematics = new ArrayList<>();

    private int acquirerIndex = 0;
    private String keyword = "";

    private SchematicAcquirerManager() {

    }

    public void register(SchematicAcquirer acquirer) {
        acquirers.add(acquirer);
    }

    public synchronized Paged getPage(Query query) {
        String queryKeyword = query.getKeyword();
        if (!queryKeyword.equals(keyword)) {
            keyword = queryKeyword;
            schematics.clear();
            acquirerIndex = 0;
            for (SchematicAcquirer acquirer : acquirers) {
                acquirer.setKeyword(keyword);
            }
        }
        int totalSchematic = 0;
        for (SchematicAcquirer acquirer : acquirers) {
            totalSchematic += acquirer.getTotalSchematic();
        }
        while (schematics.size() < query.getPage() * PAGE_SIZE) {
            if (acquirerIndex >= acquirers.size()) {
                break;
            }
            SchematicAcquirer acquirer = acquirers.get(acquirerIndex);
            if (acquirer.hasNext()) {
                Collection<Schematic> batch = acquirer.getNext();
                if (batch == null || batch.isEmpty()) {
                    acquirerIndex++;
                } else {
                    schematics.addAll(batch);
                }
            } else {
                acquirerIndex++;
            }
        }
        ArrayList<Schematic> rel = new ArrayList<>(schematics.subList(Math.min((query.getPage() - 1) * PAGE_SIZE, schematics.size()), Math.min(query.getPage() * PAGE_SIZE, schematics.size())));
        return Paged.of(rel, (totalSchematic + PAGE_SIZE - 1) / PAGE_SIZE);
    }
}