package top.ajitech.downmatica.core;

import top.ajitech.downmatica.Downmatica;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicAcquirer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class SchematicAcquirerManager {
    public static final SchematicAcquirerManager INSTANCE = new SchematicAcquirerManager();

    private final CopyOnWriteArrayList<SchematicAcquirer> acquirers = new CopyOnWriteArrayList<>();

    private SchematicAcquirerManager() {

    }

    /**
     * 此方法供内部调用，不推荐直接使用。
     * <p>
     * 未来版本中，该方法的签名或行为可能会在不预先通知的情况下发生变化。
     * <p>
     * 建议使用{@link SchematicAcquirer#register(SchematicAcquirer)}作为替代，
     * 该方法会保持向下兼容性，而此方法不会。
     */
    public void register(SchematicAcquirer acquirer) {
        acquirers.add(acquirer);
    }

    public Collection<Schematic> getAllSchematics() {
        if (acquirers.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<Collection<Schematic>>> futures = new ArrayList<>(acquirers.size());
        for (SchematicAcquirer acquirer : acquirers) {
            String name = getAcquirerName(acquirer);
            CompletableFuture<Collection<Schematic>> future = new CompletableFuture<>();
            Thread.ofVirtual().name("SchematicAcquirer-" + name).start(() -> {
                try {
                    future.complete(acquirer.getSchematics());
                } catch (Exception e) {
                    Downmatica.LOGGER.error("Failed to get schematics from acquirer {}", name, e);
                    future.complete(Collections.emptyList());
                }
            });
            futures.add(future);
        }

        int timeout = ConfigHandler.INSTANCE.totalTimeout.getIntegerValue();
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        if (timeout >= 0) {
            try {
                all.get(timeout, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                for (CompletableFuture<Collection<Schematic>> future : futures) {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                for (CompletableFuture<Collection<Schematic>> future : futures) {
                    future.cancel(true);
                }
            } catch (ExecutionException e) {
                //已在上层catch 理论不会发生
                Downmatica.LOGGER.error("Unexpected error while waiting for schematics", e);
            }
        } else {
            all.join();
        }

        List<Schematic> schematics = new ArrayList<>();
        for (CompletableFuture<Collection<Schematic>> future : futures) {
            if (future.isDone() && !future.isCancelled()) {
                schematics.addAll(future.join());
            }
        }
        return schematics;
    }

    private String getAcquirerName(SchematicAcquirer acquirer) {
        String name = acquirer.getName();
        if (name == null) {
            name = acquirer.getClass().getName();
        }
        return name;
    }
}